package com.example.smackcheck2.location

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Android implementation: triggers location detection via AppLocationManager.
 *
 * If permission is already granted, it detects immediately.
 * If not, the permission request must be triggered from the Activity layer
 * (which is handled by MainActivity's ActivityResultLauncher).
 *
 * Note: For permission requests that need Activity context, the UI composable
 * should call MainActivity.requestLocationPermissions() instead. This function
 * handles the case where permissions are already granted.
 */
actual fun requestCurrentLocationDetection() {
    MainScope().launch {
        if (AppLocationManager.hasPermission()) {
            AppLocationManager.detectLocation()
        } else {
            // Signal that permission is needed — the UI layer will handle
            // requesting it via ActivityResultLauncher
            SharedLocationState.setPermissionRequired("Location permission required. Please grant access.")
        }
    }
}
