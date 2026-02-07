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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentState = _uiState.value

                // Fetch restaurants from database
                val result = databaseRepository.searchRestaurants(
                    query = currentState.query.takeIf { it.isNotBlank() },
                    cuisines = currentState.selectedCuisines,
                    city = currentState.selectedCity,
                    minRating = currentState.selectedRating
                )

                // Fetch nearby restaurants from Google Places API
                val nearbyRestaurants = if (placesService != null && locationService != null) {
                    try {
                        val currentLoc = locationService.getCurrentLocation()
                        if (currentLoc != null) {
                            val nearby = placesService.findNearbyRestaurants(
                                latitude = currentLoc.latitude,
                                longitude = currentLoc.longitude,
                                radiusInMeters = 5000
                            )
                            // Convert NearbyRestaurant to Restaurant
                            nearby.map { nearbyRestaurant ->
                                Restaurant(
                                    id = nearbyRestaurant.id,
                                    name = nearbyRestaurant.name,
                                    city = nearbyRestaurant.address ?: "Unknown",
                                    cuisine = "Restaurant",
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
                        val combinedRestaurants = (databaseRestaurants + nearbyRestaurants)
                            .distinctBy { it.id }
                            // Apply filters to nearby restaurants as well
                            .filter { restaurant ->
                                // Apply query filter
                                val matchesQuery = if (currentState.query.isNotBlank()) {
                                    restaurant.name.contains(currentState.query, ignoreCase = true) ||
                                    restaurant.cuisine.contains(currentState.query, ignoreCase = true)
                                } else {
                                    true
                                }

                                // Apply cuisine filter
                                val matchesCuisine = if (currentState.selectedCuisines.isNotEmpty()) {
                                    currentState.selectedCuisines.any { cuisine ->
                                        restaurant.cuisine.contains(cuisine, ignoreCase = true)
                                    }
                                } else {
                                    true
                                }

                                // Apply rating filter
                                val matchesRating = if (currentState.selectedRating != null) {
                                    restaurant.averageRating >= currentState.selectedRating
                                } else {
                                    true
                                }

                                matchesQuery && matchesCuisine && matchesRating
                            }

                        _uiState.update {
                            it.copy(
                                results = combinedRestaurants,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        // Even if database fails, show nearby restaurants if available
                        val filteredNearby = nearbyRestaurants.filter { restaurant ->
                            val matchesQuery = if (currentState.query.isNotBlank()) {
                                restaurant.name.contains(currentState.query, ignoreCase = true)
                            } else {
                                true
                            }

                            val matchesRating = if (currentState.selectedRating != null) {
                                restaurant.averageRating >= currentState.selectedRating
                            } else {
                                true
                            }

                            matchesQuery && matchesRating
                        }

                        _uiState.update {
                            it.copy(
                                results = filteredNearby,
                                isLoading = false,
                                errorMessage = if (filteredNearby.isEmpty())
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
}

/**
 * ViewModel for Restaurant Detail screen
 */
class RestaurantDetailViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()

    fun loadRestaurant(restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val restaurantResult = databaseRepository.getRestaurantById(restaurantId)
                val reviewsResult = databaseRepository.getRatingsForRestaurant(restaurantId)

                val restaurant = restaurantResult.getOrNull()
                val reviews = reviewsResult.getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        restaurant = restaurant,
                        reviews = reviews,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load restaurant"
                    )
                }
            }
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
