package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.model.RestaurantDetailUiState
import com.example.smackcheck2.model.SearchUiState
import com.example.smackcheck2.model.ManualRestaurantUiState
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.NearbyRestaurant
import com.example.smackcheck2.platform.LocationOperationResult
import com.example.smackcheck2.platform.LocationErrorReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Search screen
 */
class SearchViewModel(
    private val locationService: LocationService? = null,
    private val placesService: PlacesService? = null
) : ViewModel() {

    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val availableCuisines = listOf(
        "Italian", "Japanese", "Indian", "Mexican", "Chinese",
        "Thai", "American", "French", "Mediterranean", "Korean"
    )

    val availableCities = listOf(
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
        "San Francisco", "Seattle", "Miami", "Boston", "Denver"
    )

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length >= 2) {
            search()
        }
    }

    fun onCuisineToggle(cuisine: String) {
        _uiState.update { state ->
            val updatedCuisines = if (state.selectedCuisines.contains(cuisine)) {
                state.selectedCuisines - cuisine
            } else {
                state.selectedCuisines + cuisine
            }
            state.copy(selectedCuisines = updatedCuisines)
        }
        search()
    }

    fun onRatingSelect(rating: Float?) {
        _uiState.update { it.copy(selectedRating = rating) }
        search()
    }

    fun onCitySelect(city: String?) {
        _uiState.update { it.copy(selectedCity = city) }
        search()
    }

    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, locationError = null) }

            try {
                val currentState = _uiState.value

                // Fetch restaurants from database
                val result = databaseRepository.searchRestaurants(
                    query = currentState.query.takeIf { it.isNotBlank() },
                    cuisines = currentState.selectedCuisines,
                    city = currentState.selectedCity,
                    minRating = currentState.selectedRating
                )

                // Fetch nearby restaurants from Google Places API with filters
                val nearbyRestaurants = if (placesService != null && locationService != null) {
                    try {
                        // Use geocoding for selected city, or GPS for current location
                        val currentLoc = if (currentState.selectedCity != null) {
                            // Use geocoding for selected city
                            locationService.getCoordinatesForCity(currentState.selectedCity)
                        } else {
                            // Use GPS for current location with detailed error handling
                            when (val locationResult = locationService.getCurrentLocationWithDetails()) {
                                is LocationOperationResult.Success -> locationResult.location
                                is LocationOperationResult.Error -> {
                                    val errorMessage = getLocationErrorMessage(locationResult.reason, locationResult.isEmulator)
                                    _uiState.update { it.copy(locationError = errorMessage) }
                                    null
                                }
                            }
                        }

                        if (currentLoc != null) {
                            // If cuisines are selected, search for each cuisine separately
                            val allNearby = if (currentState.selectedCuisines.isNotEmpty()) {
                                // Make a separate API call for each selected cuisine
                                currentState.selectedCuisines.flatMap { cuisine ->
                                    try {
                                        placesService.findNearbyRestaurants(
                                            latitude = currentLoc.latitude,
                                            longitude = currentLoc.longitude,
                                            radiusInMeters = 5000,
                                            keyword = cuisine,
                                            minRating = currentState.selectedRating?.toDouble()
                                        )
                                    } catch (e: Exception) {
                                        println("SearchViewModel: Failed to load $cuisine restaurants: ${e.message}")
                                        emptyList()
                                    }
                                }.distinctBy { it.id }
                            } else {
                                // No cuisine filter, just get all nearby restaurants
                                placesService.findNearbyRestaurants(
                                    latitude = currentLoc.latitude,
                                    longitude = currentLoc.longitude,
                                    radiusInMeters = 5000,
                                    keyword = currentState.query.takeIf { it.isNotBlank() },
                                    minRating = currentState.selectedRating?.toDouble()
                                )
                            }

                            // Convert NearbyRestaurant to Restaurant
                            allNearby.map { nearbyRestaurant ->
                                Restaurant(
                                    id = nearbyRestaurant.id,
                                    name = nearbyRestaurant.name,
                                    city = nearbyRestaurant.address ?: "Unknown",
                                    cuisine = "Restaurant", // Places API doesn't return cuisine type
                                    imageUrls = emptyList(),
                                    averageRating = nearbyRestaurant.rating?.toFloat() ?: 0f,
                                    reviewCount = nearbyRestaurant.userRatingsTotal ?: 0,
                                    latitude = nearbyRestaurant.latitude,
                                    longitude = nearbyRestaurant.longitude
                                )
                            }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        println("SearchViewModel: Failed to load nearby restaurants: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                result.fold(
                    onSuccess = { databaseRestaurants ->
                        // Combine database and nearby restaurants
                        // Database restaurants are already filtered by the database query
                        // Nearby restaurants are already filtered by the Places API
                        val combinedRestaurants = (databaseRestaurants + nearbyRestaurants)
                            .distinctBy { it.id }

                        _uiState.update {
                            it.copy(
                                results = combinedRestaurants,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        // Even if database fails, show nearby restaurants if available
                        _uiState.update {
                            it.copy(
                                results = nearbyRestaurants,
                                isLoading = false,
                                errorMessage = if (nearbyRestaurants.isEmpty())
                                    error.message ?: "Search failed"
                                else null
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Search failed"
                    )
                }
            }
        }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCuisines = emptySet(),
                selectedRating = null,
                selectedCity = null
            )
        }
        search()
    }

    fun clearLocationError() {
        _uiState.update { it.copy(locationError = null) }
    }

    private fun getLocationErrorMessage(reason: LocationErrorReason, isEmulator: Boolean): String {
        return when (reason) {
            LocationErrorReason.PERMISSION_DENIED ->
                "Location permission denied. Please grant location access in Settings to see nearby restaurants."

            LocationErrorReason.LOCATION_SERVICES_DISABLED ->
                "Location services are disabled. Please enable GPS in Settings to see nearby restaurants."

            LocationErrorReason.NO_LOCATION_AVAILABLE -> {
                if (isEmulator) {
                    "No location available. On emulator, use Extended Controls > Location to set a simulated location."
                } else {
                    "Could not get location. Please ensure GPS is enabled and try again outdoors."
                }
            }

            LocationErrorReason.TIMEOUT ->
                "Location request timed out. Please try again."

            LocationErrorReason.UNKNOWN ->
                "Failed to get location. Please try again or select a city manually."
        }
    }
}

/**
 * ViewModel for Manual Restaurant Entry screen
 */
class ManualRestaurantViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(ManualRestaurantUiState())
    val uiState: StateFlow<ManualRestaurantUiState> = _uiState.asStateFlow()

    fun onRestaurantNameChange(name: String) {
        _uiState.update { it.copy(restaurantName = name, restaurantNameError = null) }
    }

    fun onCityChange(city: String) {
        _uiState.update { it.copy(city = city, cityError = null) }
    }

    fun onCuisineChange(cuisine: String) {
        _uiState.update { it.copy(cuisine = cuisine, cuisineError = null) }
    }

    fun saveRestaurant(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        // Validate inputs
        var hasError = false
        var restaurantNameError: String? = null
        var cityError: String? = null
        var cuisineError: String? = null

        if (currentState.restaurantName.isBlank()) {
            restaurantNameError = "Restaurant name is required"
            hasError = true
        }

        if (currentState.city.isBlank()) {
            cityError = "City is required"
            hasError = true
        }

        if (currentState.cuisine.isBlank()) {
            cuisineError = "Cuisine is required"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    restaurantNameError = restaurantNameError,
                    cityError = cityError,
                    cuisineError = cuisineError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val restaurant = Restaurant(
                    name = currentState.restaurantName,
                    city = currentState.city,
                    cuisine = currentState.cuisine
                )

                val result = databaseRepository.createRestaurant(restaurant)

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to save restaurant"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to save restaurant"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
