package com.example.smackcheck2.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.example.smackcheck2.data.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sealed class representing the overall state of location detection
 * in the UI layer. ViewModels observe this to update the screen.
 */
sealed class LocationState {
    /** Initial state – nothing has been attempted yet. */
    data object Idle : LocationState()

    /** Currently detecting the user's location. */
    data object Loading : LocationState()

    /** Location was successfully obtained. */
    data class Success(val locationData: LocationData) : LocationState()

    /** Permission was not granted (user denied or hasn't been asked yet). */
    data class PermissionRequired(val message: String) : LocationState()

    /** Device location services (GPS) are turned off. */
    data class LocationDisabled(val message: String) : LocationState()

    /** Some other error occurred. */
    data class Error(val message: String) : LocationState()
}

/**
 * Application-level singleton that manages location state.
 *
 * It wraps [LocationHelper] and exposes a [StateFlow] of [LocationState]
 * so that any ViewModel or Composable can observe the current location status
 * reactively.
 *
 * Typical flow:
 *  1. UI calls [detectLocation] (e.g. after permission is granted).
 *  2. State transitions: Idle → Loading → Success / Error / PermissionRequired / LocationDisabled.
 *  3. ViewModels read [locationState] and drive the UI accordingly.
 */
object AppLocationManager {

    private const val TAG = "AppLocationManager"

    private var locationHelper: LocationHelper? = null
    
    // Repository for syncing location to Supabase
    private val locationRepository = LocationRepository()

    // Observable state
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    // Cache the last successful location so screens can read it synchronously
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    /**
     * Must be called once (e.g. in [MainActivity.onCreate]) to supply an
     * application-scoped [Context].
     */
    fun initialize(context: Context) {
        if (locationHelper == null) {
            locationHelper = LocationHelper(context.applicationContext)
            Log.d(TAG, "LocationHelper initialized")
        }
    }

    /**
     * Checks whether the app currently holds location permissions.
     */
    fun hasPermission(): Boolean = locationHelper?.hasLocationPermission() ?: false

    /**
     * Checks whether at least one location provider (GPS / network) is enabled.
     */
    fun isLocationEnabled(): Boolean = locationHelper?.isLocationEnabled() ?: false

    /**
     * Main entry point – triggers location detection.
     *
     * Call this **after** the user has granted permission (e.g. inside the
     * `ActivityResultLauncher` callback, or when "Use Current Location" is tapped).
     */
    suspend fun detectLocation() {
        val helper = locationHelper
        if (helper == null) {
            _locationState.value = LocationState.Error("LocationHelper not initialized. Call initialize() first.")
            SharedLocationState.setError("LocationHelper not initialized")
            return
        }

        _locationState.value = LocationState.Loading
        SharedLocationState.setLoading()

        when (val result = helper.getCurrentLocation()) {
            is SmackLocationResult.Success -> {
                _currentLocation.value = result.locationData
                _locationState.value = LocationState.Success(result.locationData)
                // ── Push to shared (common) state so the UI layer can observe ──
                SharedLocationState.onLocationDetected(
                    latitude = result.locationData.latitude,
                    longitude = result.locationData.longitude,
                    city = result.locationData.city
                )
                Log.d(TAG, "Location detected: ${result.locationData}")
                
                // ── Sync location to Supabase backend ──
                syncLocationToSupabase(
                    city = result.locationData.city,
                    latitude = result.locationData.latitude,
                    longitude = result.locationData.longitude
                )
            }

            is SmackLocationResult.PermissionDenied -> {
                _locationState.value = LocationState.PermissionRequired(result.message)
                SharedLocationState.setPermissionRequired(result.message)
                Log.w(TAG, "Permission denied: ${result.message}")
            }

            is SmackLocationResult.LocationDisabled -> {
                _locationState.value = LocationState.LocationDisabled(result.message)
                SharedLocationState.setLocationDisabled(result.message)
                Log.w(TAG, "Location disabled: ${result.message}")
            }

            is SmackLocationResult.Error -> {
                _locationState.value = LocationState.Error(result.message)
                SharedLocationState.setError(result.message)
                Log.e(TAG, "Location error: ${result.message}")
            }
        }
    }

    /**
     * Manually set a location (e.g. when the user picks a city from the list).
     */
    fun setManualLocation(city: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        val data = LocationData(latitude, longitude, city)
        _currentLocation.value = data
        _locationState.value = LocationState.Success(data)
        SharedLocationState.onManualLocationSelected(city)
    }

    /**
     * Reset location state back to Idle.
     */
    fun reset() {
        _locationState.value = LocationState.Idle
        _currentLocation.value = null
    }

    /**
     * Helper to open the device's app settings page so the user can
     * manually enable location permission after permanently denying it.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Helper to open the device's location settings so the user can
     * toggle GPS on.
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Sync the detected location to Supabase backend.
     * This runs in the background and doesn't block the UI.
     */
    private suspend fun syncLocationToSupabase(
        city: String?,
        latitude: Double,
        longitude: Double
    ) {
        if (city == null) {
            Log.w(TAG, "City is null, skipping Supabase sync")
            return
        }
        
        try {
            val userId = locationRepository.getCurrentUserId()
            if (userId != null) {
                val success = locationRepository.updateUserLocation(
                    userId = userId,
                    city = city,
                    latitude = latitude,
                    longitude = longitude
                )
                if (success) {
                    Log.d(TAG, "✅ Location synced to Supabase: $city ($latitude, $longitude)")
                } else {
                    Log.w(TAG, "⚠️ Failed to sync location to Supabase")
                }
            } else {
                Log.w(TAG, "⚠️ User not authenticated, skipping Supabase sync")
            }
        } catch (e: Exception) {
            // Don't crash the app if Supabase sync fails
            Log.e(TAG, "❌ Error syncing to Supabase: ${e.message}")
        }
    }
}
