package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.platform.LocationResult
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.NearbyRestaurant
import com.example.smackcheck2.platform.PlacesService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for Nearby Restaurants Screen
 */
sealed class NearbyRestaurantsUiState {
    data object Initial : NearbyRestaurantsUiState()
    data object Loading : NearbyRestaurantsUiState()
    data class Success(
        val restaurants: List<NearbyRestaurant>,
        val currentLocation: LocationResult?
    ) : NearbyRestaurantsUiState()
    data class Error(val message: String) : NearbyRestaurantsUiState()
}

/**
 * ViewModel for managing nearby restaurants
 */
class NearbyRestaurantsViewModel(
    private val locationService: LocationService?,
    private val placesService: PlacesService?
) : ViewModel() {

    private val _uiState = MutableStateFlow<NearbyRestaurantsUiState>(NearbyRestaurantsUiState.Initial)
    val uiState: StateFlow<NearbyRestaurantsUiState> = _uiState.asStateFlow()

    private var currentLocation: LocationResult? = null

    init {
        // Automatically load nearby restaurants when ViewModel is created
        loadNearbyRestaurants()
    }

    /**
     * Load nearby restaurants based on current location
     */
    fun loadNearbyRestaurants(radiusInMeters: Int = 2000) {
        if (locationService == null || placesService == null) {
            _uiState.value = NearbyRestaurantsUiState.Error("Location or Places service not available")
            return
        }

        viewModelScope.launch {
            _uiState.value = NearbyRestaurantsUiState.Loading

            try {
                // Check location permission
                if (!locationService.hasLocationPermission()) {
                    _uiState.value = NearbyRestaurantsUiState.Error("Location permission not granted")
                    return@launch
                }

                // Get current location
                val location = locationService.getCurrentLocation()
                if (location == null) {
                    _uiState.value = NearbyRestaurantsUiState.Error("Unable to get current location")
                    return@launch
                }

                currentLocation = location

                // Find nearby restaurants
                val restaurants = placesService.findNearbyRestaurants(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusInMeters = radiusInMeters
                )

                if (restaurants.isEmpty()) {
                    _uiState.value = NearbyRestaurantsUiState.Success(
                        restaurants = emptyList(),
                        currentLocation = location
                    )
                } else {
                    _uiState.value = NearbyRestaurantsUiState.Success(
                        restaurants = restaurants,
                        currentLocation = location
                    )
                }

            } catch (e: Exception) {
                _uiState.value = NearbyRestaurantsUiState.Error(
                    "Error loading restaurants: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh nearby restaurants
     */
    fun refresh() {
        loadNearbyRestaurants()
    }

    /**
     * Change search radius and reload
     */
    fun changeRadius(radiusInMeters: Int) {
        loadNearbyRestaurants(radiusInMeters)
    }
}
