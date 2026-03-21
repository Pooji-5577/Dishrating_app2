package com.example.smackcheck2.platform

import androidx.compose.runtime.Composable

/**
 * Expected platform-specific permission handler
 */
expect class PermissionHandler {
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean
}

/**
 * Expected composable for requesting location permission
 */
@Composable
expect fun RequestLocationPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
)

/**
 * Expected composable for requesting camera permission
 */
@Composable
expect fun RequestCameraPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
)

/**
 * Expected composable for requesting notification permission
 */
@Composable
expect fun RequestNotificationPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
)
