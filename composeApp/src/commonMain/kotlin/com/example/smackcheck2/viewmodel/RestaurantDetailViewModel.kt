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

    /**
     * Determines if an ID is a Google Place ID or a UUID
     * Google Place IDs start with "ChI" and are alphanumeric
     * UUIDs follow pattern: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */
    private fun isGooglePlaceId(id: String): Boolean {
        return id.startsWith("ChI") && !id.contains("-")
    }

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
                // Detect ID type and load accordingly
                if (isGooglePlaceId(restaurantId)) {
                    loadGooglePlaceRestaurant(restaurantId)
                } else if (isUuid(restaurantId)) {
                    loadDatabaseRestaurant(restaurantId)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid restaurant ID format"
                        )
                    }
                }
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
    private suspend fun loadDatabaseRestaurant(restaurantId: String) {
        val restaurantResult = databaseRepository.getRestaurantById(restaurantId)

        restaurantResult.onSuccess { restaurant ->
            if (restaurant != null) {
                _uiState.update { it.copy(restaurant = restaurant) }

                // Load dishes for this restaurant
                loadDishes(restaurantId)

                // Load reviews for this restaurant
                loadReviews(restaurantId)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Restaurant not found in database"
                    )
                }
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load restaurant from database"
                )
            }
        }
    }

    /**
     * Load dishes for the restaurant
     */
    private suspend fun loadDishes(restaurantId: String) {
        val dishesResult = databaseRepository.getDishesForRestaurant(restaurantId)

        dishesResult.onSuccess { dishes ->
            _uiState.update { it.copy(dishes = dishes) }
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
