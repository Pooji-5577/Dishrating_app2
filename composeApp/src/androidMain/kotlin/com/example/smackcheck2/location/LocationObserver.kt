package com.example.smackcheck2.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.smackcheck2.viewmodel.LocationHomeViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Composable that observes [AppLocationManager.locationState] and automatically
 * pushes detected location data into the given [LocationHomeViewModel].
 *
 * Drop this into any screen that should react to automatic location detection.
 *
 * When AppLocationManager detects a new location (Success state), this composable
 * calls [LocationHomeViewModel.selectLocationWithCoordinates] to update the UI.
 */
@Composable
fun LocationObserver(locationHomeViewModel: LocationHomeViewModel) {
    val locationState by AppLocationManager.locationState.collectAsState()

    LaunchedEffect(locationState) {
        when (val state = locationState) {
            is LocationState.Success -> {
                val data = state.locationData
                val city = data.city ?: "Unknown Location"
                locationHomeViewModel.selectLocationWithCoordinates(
                    city = city,
                    latitude = data.latitude,
                    longitude = data.longitude
                )
            }
            else -> { /* Other states handled by the UI directly */ }
        }
    }
}

/**
 * Composable helper to trigger location detection from the UI.
 *
 * Call this when the user taps "Use Current Location" or "Allow Location".
 * If permission is already granted, it will detect location immediately.
 * If permission is not granted, use RequestLocationPermission composable instead.
 */
@Composable
fun rememberLocationPermissionRequester(): () -> Unit {
    return remember {
        {
            if (AppLocationManager.hasPermission()) {
                // Already have permission – just detect
                MainScope().launch {
                    AppLocationManager.detectLocation()
                }
            }
            // If permission is not granted, use RequestLocationPermission composable
            // from PermissionHandler.android.kt to request permission properly
        }
    }
}

/**
 * Composable helper that triggers location detection if permission is already granted.
 * Useful for "Use Current Location" after permission has been granted previously.
 */
@Composable
fun rememberLocationDetector(): () -> Unit {
    return remember {
        {
            MainScope().launch {
                AppLocationManager.detectLocation()
            }
        }
    }
}
