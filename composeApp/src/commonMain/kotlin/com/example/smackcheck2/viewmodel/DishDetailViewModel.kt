package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.model.Comment
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.DishDetailUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Detail Screen
 * Loads dish data, reviews, and related dishes from Supabase
 */
class DishDetailViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()
    private val socialRepository = SocialRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(DishDetailUiState(isLoading = true))
    val uiState: StateFlow<DishDetailUiState> = _uiState.asStateFlow()
    private var loadedDishId: String? = null

    /**
     * Load all data for a dish: dish info, restaurant, reviews, and related dishes
     */
    fun loadDish(dishId: String) {
        if (loadedDishId == dishId && _uiState.value.dish?.id == dishId && _uiState.value.errorMessage == null) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Load the dish itself; if not found, the ID may be a rating ID —
            //    resolve to the real dish ID and retry.
            var resolvedDishId = dishId
            var dishResult = databaseRepository.getDishById(resolvedDishId)
            var dish = dishResult.getOrNull()
            if (dish == null) {
                val realDishId = databaseRepository.getDishIdFromRating(dishId)
                if (realDishId != null) {
                    resolvedDishId = realDishId
                    dishResult = databaseRepository.getDishById(resolvedDishId)
                    dish = dishResult.getOrNull()
                }
            }
            dishResult.onSuccess {
                if (dish == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Dish not found")
                    }
                    return@launch
                }

                _uiState.update { it.copy(dish = dish) }
                loadedDishId = resolvedDishId

                coroutineScope {
                    val restaurantDeferred = async {
                        if (dish.restaurantId.isNotBlank()) {
                            databaseRepository.getRestaurantById(dish.restaurantId).getOrNull()
                        } else null
                    }
                    val relatedDeferred = async {
                        if (dish.restaurantId.isNotBlank()) {
                            databaseRepository.getDishesForRestaurant(dish.restaurantId)
                                .getOrDefault(emptyList())
                                .filter { it.id != resolvedDishId }
                        } else emptyList()
                    }
                    val reviewsDeferred = async {
                        databaseRepository.getRatingsForDish(
                            dishId = resolvedDishId,
                            restaurantId = dish.restaurantId.takeIf { it.isNotBlank() }
                        ).getOrDefault(emptyList())
                    }

                    val restaurant = restaurantDeferred.await()
                    val related = enrichRelatedDishesWithRatings(relatedDeferred.await())
                    val reviews = reviewsDeferred.await()
                    val featured = reviews
                        .filter { !it.dishImageUrl.isNullOrBlank() }
                        .maxByOrNull { it.rating }
                        ?: reviews.maxByOrNull { it.rating }
                    val anchorRatingId = featured?.id ?: reviews.firstOrNull()?.id
                    val comments = if (anchorRatingId.isNullOrBlank()) {
                        emptyList()
                    } else {
                        socialRepository.getCommentsForRating(anchorRatingId)
                            .getOrDefault(emptyList())
                    }

                    _uiState.update {
                        it.copy(
                            restaurant = restaurant,
                            relatedDishes = related,
                            reviews = reviews,
                            comments = comments,
                            featuredReview = featured
                        )
                    }
                }

                _uiState.update { it.copy(isLoading = false) }

            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load dish"
                    )
                }
            }
        }
    }

    /**
     * Toggle favorite state (local only for now)
     */
    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    /**
     * Retry loading
     */
    fun retry(dishId: String) {
        loadDish(dishId)
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onCommentDraftChange(value: String) {
        if (value.length <= 500) {
            _uiState.update { it.copy(commentDraft = value, commentErrorMessage = null) }
        }
    }

    fun submitComment() {
        val state = _uiState.value
        val anchorReviewId = state.featuredReview?.id ?: state.reviews.firstOrNull()?.id
        if (anchorReviewId.isNullOrBlank()) {
            _uiState.update { it.copy(commentErrorMessage = "No review available to comment on yet") }
            return
        }

        val draft = state.commentDraft.trim()
        if (draft.isBlank()) {
            _uiState.update { it.copy(commentErrorMessage = "Comment cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCommentSubmitting = true, commentErrorMessage = null) }

            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update {
                    it.copy(
                        isCommentSubmitting = false,
                        commentErrorMessage = "Please sign in to comment"
                    )
                }
                return@launch
            }

            socialRepository.addCommentAsCurrentUser(
                ratingId = anchorReviewId,
                content = draft
            ).fold(
                onSuccess = { createdComment ->
                    _uiState.update { current ->
                        current.copy(
                            commentDraft = "",
                            isCommentSubmitting = false,
                            comments = listOf(createdComment) + current.comments,
                            reviews = current.reviews.map { review ->
                                if (review.id == anchorReviewId) {
                                    review.copy(commentsCount = review.commentsCount + 1)
                                } else review
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCommentSubmitting = false,
                            commentErrorMessage = error.message ?: "Failed to post comment"
                        )
                    }
                }
            )
        }
    }

    private suspend fun enrichRelatedDishesWithRatings(dishes: List<Dish>): List<Dish> {
        if (dishes.isEmpty()) return dishes

        val ratingsByDish = databaseRepository
            .getRatingsByDishIds(dishes.map { it.id })
            .groupBy { it.dishId }

        return dishes.map { dish ->
            val ratings = ratingsByDish[dish.id].orEmpty()
            if (ratings.isEmpty()) return@map dish

            val firstRatingImage = ratings.firstOrNull { !it.imageUrl.isNullOrBlank() }?.imageUrl
            val averageRating = ratings.map { it.rating }.average().toFloat()

            dish.copy(
                imageUrl = dish.imageUrl?.takeIf { it.isNotBlank() } ?: firstRatingImage,
                rating = averageRating,
                ratingCount = ratings.size
            )
        }
    }
}
