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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Search screen
 * Searches restaurants from Supabase with support for
 * "Restaurants & Cafes Only" filter, cuisine, rating and city filters.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val locationService: LocationService? = null,
    private val placesService: PlacesService? = null
) : ViewModel() {

    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Debounce channel for search queries
    private val searchQuery = MutableStateFlow("")


    val availableCuisines = listOf(
        "Italian", "Japanese", "Indian", "Mexican", "Chinese",
        "Thai", "American", "French", "Mediterranean", "Korean"
    )

    val availableCities = listOf(
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
        "San Francisco", "Seattle", "Miami", "Boston", "Denver"
    )

    init {
        // Debounced search — waits 400ms after user stops typing
        viewModelScope.launch {
            searchQuery
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length >= 2) {
                        search()
                    } else if (query.isEmpty()) {
                        _uiState.update { it.copy(results = emptyList(), errorMessage = null) }
                    }
                }
        }
    }


    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchQuery.value = query
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


    /**
     * Toggle "Restaurants & Cafes Only" filter
     */
    fun toggleRestaurantsAndCafesFilter() {
        _uiState.update { 
            it.copy(restaurantsAndCafesOnly = !it.restaurantsAndCafesOnly) 
        }
        search()
    }

    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, locationError = null) }

            try {
                val currentState = _uiState.value
                println("SearchViewModel: Starting search with query='${currentState.query}'")

                // Fetch restaurants from database
                val result = databaseRepository.searchRestaurants(
                    query = currentState.query.takeIf { it.isNotBlank() },
                    cuisines = currentState.selectedCuisines,
                    city = currentState.selectedCity,
                    minRating = currentState.selectedRating,
                    restaurantsAndCafesOnly = currentState.restaurantsAndCafesOnly
                )

                // Fetch restaurants from Google Places - either by text search or nearby search
                val placesRestaurants = if (placesService != null) {
                    try {
                        // If user typed a query, use text search (doesn't require location)
                        if (currentState.query.isNotBlank()) {
                            println("SearchViewModel: Using text search for query: ${currentState.query}")
                            val textResults = placesService.searchRestaurantsByText(currentState.query)
                            println("SearchViewModel: Text search returned ${textResults.size} results")
                            
                            // Convert NearbyRestaurant to Restaurant and apply filters
                            textResults.mapNotNull { nearbyRestaurant ->
                                // Apply rating filter
                                if (currentState.selectedRating != null) {
                                    val rating = nearbyRestaurant.rating?.toFloat() ?: 0f
                                    if (rating < currentState.selectedRating) return@mapNotNull null
                                }
                                
                                Restaurant(
                                    id = nearbyRestaurant.id,
                                    name = nearbyRestaurant.name,
                                    city = nearbyRestaurant.address ?: "Unknown",
                                    cuisine = "Restaurant",
                                    imageUrls = emptyList(),
                                    averageRating = nearbyRestaurant.rating?.toFloat() ?: 0f,
                                    reviewCount = nearbyRestaurant.userRatingsTotal ?: 0,
                                    latitude = nearbyRestaurant.latitude,
                                    longitude = nearbyRestaurant.longitude,
                                    googlePlaceId = nearbyRestaurant.id
                                )
                            }
                        } else if (locationService != null) {
                            // No query - use location-based nearby search
                            val currentLoc = if (currentState.selectedCity != null) {
                                locationService.getCoordinatesForCity(currentState.selectedCity)
                            } else {
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
                                val allNearby = if (currentState.selectedCuisines.isNotEmpty()) {
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
                                    placesService.findNearbyRestaurants(
                                        latitude = currentLoc.latitude,
                                        longitude = currentLoc.longitude,
                                        radiusInMeters = 5000,
                                        keyword = null,
                                        minRating = currentState.selectedRating?.toDouble()
                                    )
                                }

                                allNearby.map { nearbyRestaurant ->
                                    Restaurant(
                                        id = nearbyRestaurant.id,
                                        name = nearbyRestaurant.name,
                                        city = nearbyRestaurant.address ?: "Unknown",
                                        cuisine = "Restaurant",
                                        imageUrls = emptyList(),
                                        averageRating = nearbyRestaurant.rating?.toFloat() ?: 0f,
                                        reviewCount = nearbyRestaurant.userRatingsTotal ?: 0,
                                        latitude = nearbyRestaurant.latitude,
                                        longitude = nearbyRestaurant.longitude,
                                        googlePlaceId = nearbyRestaurant.id
                                    )
                                }
                            } else {
                                emptyList()
                            }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        println("SearchViewModel: Failed to load Places restaurants: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                result.fold(
                    onSuccess = { databaseRestaurants ->
                        println("SearchViewModel: Database returned ${databaseRestaurants.size} results")
                        // Combine database and Places restaurants
                        val combinedRestaurants = (databaseRestaurants + placesRestaurants)
                            .distinctBy { it.id }
                        println("SearchViewModel: Combined total: ${combinedRestaurants.size} results")

                        _uiState.update {
                            it.copy(
                                results = combinedRestaurants,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        println("SearchViewModel: Database search failed: ${error.message}")
                        // Even if database fails, show Places restaurants if available
                        _uiState.update {
                            it.copy(
                                results = placesRestaurants,
                                isLoading = false,
                                errorMessage = if (placesRestaurants.isEmpty())
                                    error.message ?: "Search failed"
                                else null
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                println("SearchViewModel: Search exception: ${e.message}")
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
                selectedCity = null,
                restaurantsAndCafesOnly = false
            )
        }
        search()
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                query = "",
                selectedCuisines = emptySet(),
                selectedRating = null,
                selectedCity = null,
                restaurantsAndCafesOnly = false,
                results = emptyList()
            )
        }
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
