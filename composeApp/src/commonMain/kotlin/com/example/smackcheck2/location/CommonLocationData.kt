package com.example.smackcheck2.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Common data class for location results (shared between platforms).
 */
data class CommonLocationData(
    val latitude: Double,
    val longitude: Double,
    val city: String?
)

/**
 * Common sealed class for location state (shared between platforms).
 */
sealed class CommonLocationState {
    data object Idle : CommonLocationState()
    data object Loading : CommonLocationState()
    data class Success(val data: CommonLocationData) : CommonLocationState()
    data class PermissionRequired(val message: String) : CommonLocationState()
    data class LocationDisabled(val message: String) : CommonLocationState()
    data class Error(val message: String) : CommonLocationState()
}

/**
 * Shared location state holder that can be accessed from common code.
 * The Android platform layer (AppLocationManager) writes to this,
 * and common UI code (NavHost, ViewModels) reads from it.
 */
object SharedLocationState {
    private val _locationState = MutableStateFlow<CommonLocationState>(CommonLocationState.Idle)
    val locationState: StateFlow<CommonLocationState> = _locationState.asStateFlow()

    private val _currentLocation = MutableStateFlow<CommonLocationData?>(null)
    val currentLocation: StateFlow<CommonLocationData?> = _currentLocation.asStateFlow()

    /**
     * Called by the platform layer when location is detected
     */
    fun onLocationDetected(latitude: Double, longitude: Double, city: String?) {
        val data = CommonLocationData(latitude, longitude, city)
        _currentLocation.value = data
        _locationState.value = CommonLocationState.Success(data)
    }

    /**
     * Called when user selects a city manually from the list
     */
    fun onManualLocationSelected(city: String) {
        val data = CommonLocationData(0.0, 0.0, city)
        _currentLocation.value = data
        _locationState.value = CommonLocationState.Success(data)
    }

    fun setLoading() {
        _locationState.value = CommonLocationState.Loading
    }

    fun setPermissionRequired(message: String = "Location permission required") {
        _locationState.value = CommonLocationState.PermissionRequired(message)
    }

    fun setLocationDisabled(message: String = "Location services disabled") {
        _locationState.value = CommonLocationState.LocationDisabled(message)
    }

    fun setError(message: String) {
        _locationState.value = CommonLocationState.Error(message)
    }

    fun reset() {
        _locationState.value = CommonLocationState.Idle
        _currentLocation.value = null
    }
}
