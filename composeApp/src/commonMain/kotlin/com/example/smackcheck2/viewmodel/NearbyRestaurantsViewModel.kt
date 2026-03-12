package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.GeofenceEvent
import com.example.smackcheck2.data.repository.GeofenceRepository
import com.example.smackcheck2.platform.GeofencingService
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
        val currentLocation: LocationResult?,
        val geofencingEnabled: Boolean = false,
        val monitoredCount: Int = 0
    ) : NearbyRestaurantsUiState()
    data class Error(val message: String) : NearbyRestaurantsUiState()
}

/**
 * ViewModel for managing nearby restaurants with geofencing support
 */
class NearbyRestaurantsViewModel(
    private val locationService: LocationService?,
    private val placesService: PlacesService?,
    geofencingService: GeofencingService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<NearbyRestaurantsUiState>(NearbyRestaurantsUiState.Initial)
    val uiState: StateFlow<NearbyRestaurantsUiState> = _uiState.asStateFlow()

    private var currentLocation: LocationResult? = null
    
    // Geofencing support
    private val geofenceRepository = GeofenceRepository(geofencingService)
    
    private val _geofenceEnabled = MutableStateFlow(false)
    val geofenceEnabled: StateFlow<Boolean> = _geofenceEnabled.asStateFlow()
    
    val geofenceEvents: StateFlow<GeofenceEvent?> = geofenceRepository.lastEvent

    init {
        // Automatically load nearby restaurants when ViewModel is created
        loadNearbyRestaurants()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up geofences when ViewModel is destroyed
        if (_geofenceEnabled.value) {
            geofenceRepository.stopMonitoringAll()
        }
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

                // Update geofences if enabled
                if (_geofenceEnabled.value && restaurants.isNotEmpty()) {
                    geofenceRepository.startMonitoringRestaurants(restaurants)
                }

                _uiState.value = NearbyRestaurantsUiState.Success(
                    restaurants = restaurants,
                    currentLocation = location,
                    geofencingEnabled = _geofenceEnabled.value,
                    monitoredCount = if (_geofenceEnabled.value) geofenceRepository.monitoredCount() else 0
                )

            } catch (e: Exception) {
                _uiState.value = NearbyRestaurantsUiState.Error(
                    "Error loading restaurants: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle geofencing on/off
     */
    fun toggleGeofencing() {
        _geofenceEnabled.value = !_geofenceEnabled.value
        
        val currentState = _uiState.value
        if (currentState is NearbyRestaurantsUiState.Success) {
            if (_geofenceEnabled.value) {
                // Start monitoring
                geofenceRepository.startMonitoringRestaurants(currentState.restaurants)
            } else {
                // Stop monitoring
                geofenceRepository.stopMonitoringAll()
            }
            
            // Update UI state
            _uiState.value = currentState.copy(
                geofencingEnabled = _geofenceEnabled.value,
                monitoredCount = if (_geofenceEnabled.value) geofenceRepository.monitoredCount() else 0
            )
        }
    }
    
    /**
     * Enable geofencing for nearby restaurants
     */
    fun enableGeofencing() {
        if (!_geofenceEnabled.value) {
            toggleGeofencing()
        }
    }
    
    /**
     * Disable geofencing
     */
    fun disableGeofencing() {
        if (_geofenceEnabled.value) {
            toggleGeofencing()
        }
    }
    
    /**
     * Clear the last geofence event after handling
     */
    fun clearGeofenceEvent() {
        geofenceRepository.clearLastEvent()
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
