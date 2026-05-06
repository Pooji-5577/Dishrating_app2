package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.model.DishDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Detail Screen
 * Loads dish data, reviews, and related dishes from Supabase
 */
class DishDetailViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(DishDetailUiState(isLoading = true))
    val uiState: StateFlow<DishDetailUiState> = _uiState.asStateFlow()

    /**
     * Load all data for a dish: dish info, restaurant, reviews, and related dishes
     */
    fun loadDish(dishId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Load the dish itself
            val dishResult = databaseRepository.getDishById(dishId)
            dishResult.onSuccess { dish ->
                if (dish == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Dish not found")
                    }
                    return@launch
                }

                _uiState.update { it.copy(dish = dish) }

                // 2. Load the restaurant
                if (dish.restaurantId.isNotBlank()) {
                    val restaurantResult = databaseRepository.getRestaurantById(dish.restaurantId)
                    restaurantResult.onSuccess { restaurant ->
                        _uiState.update { it.copy(restaurant = restaurant) }
                    }

                    // 3. Load related dishes from the same restaurant
                    val relatedResult = databaseRepository.getDishesForRestaurant(dish.restaurantId)
                    relatedResult.onSuccess { dishes ->
                        // Exclude the current dish from related list
                        val related = dishes.filter { it.id != dishId }
                        _uiState.update { it.copy(relatedDishes = related) }
                    }
                }

                // 4. Load reviews for this dish, filtered to the dish's own restaurant
                //    so only location-specific reviews are shown.
                val reviewsResult = databaseRepository.getRatingsForDish(
                    dishId = dishId,
                    restaurantId = dish.restaurantId.takeIf { it.isNotBlank() }
                )
                reviewsResult.onSuccess { reviews ->
                    // Pick the featured review: highest-rated one that has a photo, else highest-rated
                    val featured = reviews.filter { !it.dishImageUrl.isNullOrBlank() }
                        .maxByOrNull { it.rating }
                        ?: reviews.maxByOrNull { it.rating }
                    _uiState.update { it.copy(reviews = reviews, featuredReview = featured) }
                }.onFailure { error ->
                    println("DishDetailViewModel: Failed to load reviews: ${error.message}")
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
}
