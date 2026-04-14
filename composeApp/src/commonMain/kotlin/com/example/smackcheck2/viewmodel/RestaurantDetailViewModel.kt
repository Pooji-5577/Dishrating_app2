package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.model.RestaurantDetailUiState
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.NearbyRestaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Restaurant Detail Screen
 * Manages loading and displaying restaurant details, dishes, and reviews
 */
class RestaurantDetailViewModel(
    private val placesService: PlacesService? = null
) : ViewModel() {

    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(RestaurantDetailUiState(isLoading = true))
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()

    private fun isUuid(id: String): Boolean {
        val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return uuidPattern.matches(id)
    }

    /**
     * Load restaurant details, dishes, and reviews
     */
    fun loadRestaurant(restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                if (restaurantId.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Restaurant ID is missing") }
                    return@launch
                }

                // 1) Try database by primary ID (works for UUID and any custom IDs)
                if (loadDatabaseRestaurantById(restaurantId)) return@launch

                // 2) Try database by Google Place ID column for mirrored/external places
                if (!isUuid(restaurantId) && loadDatabaseRestaurantByGooglePlaceId(restaurantId)) return@launch

                // 3) Fallback to Google Places detail lookup for external place IDs
                loadGooglePlaceRestaurant(restaurantId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    /**
     * Load restaurant from Google Places API
     */
    private suspend fun loadGooglePlaceRestaurant(placeId: String) {
        if (placesService == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Places service not available"
                )
            }
            return
        }

        // Fetch place details from Google
        val nearbyRestaurant = placesService.getPlaceDetails(placeId)

        if (nearbyRestaurant != null) {
            // Convert NearbyRestaurant to Restaurant model
            val restaurant = Restaurant(
                id = nearbyRestaurant.id,
                name = nearbyRestaurant.name,
                city = nearbyRestaurant.address ?: "Unknown",
                cuisine = "Restaurant", // Google doesn't provide cuisine type
                imageUrls = emptyList(), // Could fetch from photo reference
                averageRating = nearbyRestaurant.rating?.toFloat() ?: 0f,
                reviewCount = nearbyRestaurant.userRatingsTotal ?: 0,
                latitude = nearbyRestaurant.latitude,
                longitude = nearbyRestaurant.longitude
            )

            _uiState.update { it.copy(restaurant = restaurant) }

            // Note: Google Places restaurants won't have dishes or user reviews
            // Set dishes and reviews to empty, mark loading complete
            _uiState.update {
                it.copy(
                    dishes = emptyList(),
                    reviews = emptyList(),
                    isLoading = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Restaurant details not available from Google Places"
                )
            }
        }
    }

    /**
     * Load restaurant from Supabase database (existing logic)
     */
    private suspend fun loadDatabaseRestaurantById(restaurantId: String): Boolean {
        val restaurantResult = databaseRepository.getRestaurantById(restaurantId)

        restaurantResult.onSuccess { restaurant ->
            if (restaurant != null) {
                _uiState.update { it.copy(restaurant = restaurant) }

                // Load dishes for this restaurant
                loadDishes(restaurantId)

                // Load reviews for this restaurant
                loadReviews(restaurantId)
            }
        }

        return restaurantResult.getOrNull() != null
    }

    private suspend fun loadDatabaseRestaurantByGooglePlaceId(placeId: String): Boolean {
        val restaurantResult = databaseRepository.getRestaurantByGooglePlaceId(placeId)

        restaurantResult.onSuccess { restaurant ->
            if (restaurant != null) {
                _uiState.update { it.copy(restaurant = restaurant) }
                loadDishes(restaurant.id)
                loadReviews(restaurant.id)
            }
        }

        return restaurantResult.getOrNull() != null
    }

    /**
     * Load dishes for the restaurant
     */
    private suspend fun loadDishes(restaurantId: String) {
        val dishesResult = databaseRepository.getDishesForRestaurant(restaurantId)

        dishesResult.onSuccess { dishes ->
            val topDishes = dishes.sortedByDescending { it.rating }.take(6)
            _uiState.update { it.copy(dishes = dishes, topDishes = topDishes) }
        }.onFailure { error ->
            println("RestaurantDetailViewModel: Failed to load dishes: ${error.message}")
            // Don't update error state, just leave dishes empty
        }
    }

    /**
     * Load reviews for the restaurant
     */
    private suspend fun loadReviews(restaurantId: String) {
        val reviewsResult = databaseRepository.getRatingsForRestaurant(restaurantId)

        reviewsResult.onSuccess { reviews ->
            _uiState.update {
                it.copy(
                    reviews = reviews,
                    isLoading = false
                )
            }
        }.onFailure { error ->
            println("RestaurantDetailViewModel: Failed to load reviews: ${error.message}")
            // Still set loading to false even if reviews fail
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Retry loading the restaurant data
     */
    fun retry(restaurantId: String) {
        loadRestaurant(restaurantId)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
