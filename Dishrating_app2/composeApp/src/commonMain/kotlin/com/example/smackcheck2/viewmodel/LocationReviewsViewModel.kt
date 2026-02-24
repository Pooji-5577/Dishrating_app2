package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for Location Reviews Screen
 */
data class LocationReviewsUiState(
    val isLoading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val location: String? = null,
    val sortByRating: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for Location-based Reviews Screen
 * Shows all reviews filtered by the selected location
 */
class LocationReviewsViewModel(
    private val databaseRepository: DatabaseRepository = DatabaseRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationReviewsUiState())
    val uiState: StateFlow<LocationReviewsUiState> = _uiState.asStateFlow()

    /**
     * Load reviews for a specific location
     */
    fun loadReviews(location: String?, sortByRating: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, location = location, sortByRating = sortByRating) }

            val result = databaseRepository.getAllReviewsByLocation(location, sortByRating)

            result.fold(
                onSuccess = { reviews ->
                    // Check if user has liked each review
                    val currentUserId = authRepository.getCurrentUser()?.id
                    val reviewsWithLikeStatus = if (currentUserId != null) {
                        reviews.map { review ->
                            val isLiked = databaseRepository.hasUserLiked(currentUserId, review.id)
                            review.copy(isLiked = isLiked)
                        }
                    } else {
                        reviews
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reviews = reviewsWithLikeStatus
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load reviews"
                        )
                    }
                }
            )
        }
    }

    /**
     * Toggle like on a review
     */
    fun toggleLike(reviewId: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUser()?.id ?: return@launch

            val result = databaseRepository.toggleLike(currentUserId, reviewId)

            result.fold(
                onSuccess = { isLiked ->
                    _uiState.update { state ->
                        val updatedReviews = state.reviews.map { review ->
                            if (review.id == reviewId) {
                                review.copy(
                                    isLiked = isLiked,
                                    likesCount = if (isLiked) review.likesCount + 1 else review.likesCount - 1
                                )
                            } else {
                                review
                            }
                        }
                        state.copy(reviews = updatedReviews)
                    }
                },
                onFailure = { exception ->
                    // Handle error silently or show a message
                }
            )
        }
    }

    /**
     * Change sort order
     */
    fun changeSortOrder(sortByRating: Boolean) {
        val currentLocation = _uiState.value.location
        loadReviews(currentLocation, sortByRating)
    }

    /**
     * Refresh reviews
     */
    fun refresh() {
        val currentLocation = _uiState.value.location
        val currentSortOrder = _uiState.value.sortByRating
        loadReviews(currentLocation, currentSortOrder)
    }
}
