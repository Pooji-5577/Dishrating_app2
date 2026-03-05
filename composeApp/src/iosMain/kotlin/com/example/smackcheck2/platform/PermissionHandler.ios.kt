package com.example.smackcheck2.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of PermissionHandler using CoreLocation.
 */
actual class PermissionHandler {
    /**
     * Check if location permission is granted by inspecting CLLocationManager.authorizationStatus().
     */
    actual fun hasLocationPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways
    }
}

/**
 * Composable for requesting location permission on iOS.
 *
 * Creates a CLLocationManager, sets up a delegate to observe authorization changes,
 * and calls requestWhenInUseAuthorization() when the user triggers the permission request.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RequestLocationPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    var hasPermission by remember {
        mutableStateOf(
            CLLocationManager.authorizationStatus() == kCLAuthorizationStatusAuthorizedWhenInUse ||
                    CLLocationManager.authorizationStatus() == kCLAuthorizationStatusAuthorizedAlways
        )
    }

    val locationManager = remember { CLLocationManager() }

    val delegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                val status = CLLocationManager.authorizationStatus()
                val granted = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                        status == kCLAuthorizationStatusAuthorizedAlways
                hasPermission = granted
                // Only call back when status is determined (not the initial undetermined)
                if (status != kCLAuthorizationStatusNotDetermined) {
                    onPermissionResult(granted)
                }
            }
        }
    }

    DisposableEffect(locationManager) {
        locationManager.delegate = delegate
        onDispose {
            locationManager.delegate = null
        }
    }

    content {
        if (hasPermission) {
            // Already have permission, report immediately
            onPermissionResult(true)
        } else {
            // Request permission on main queue
            dispatch_async(dispatch_get_main_queue()) {
                locationManager.requestWhenInUseAuthorization()
            }
        }
    }
}

/**
 * Composable for requesting camera permission on iOS.
 *
 * Uses AVCaptureDevice.requestAccessForMediaType to prompt the user.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RequestCameraPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    var hasPermission by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
        )
    }

    content {
        if (hasPermission) {
            // Already have permission, report immediately
            onPermissionResult(true)
        } else {
            val currentStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
            if (currentStatus == AVAuthorizationStatusNotDetermined) {
                // Request access; the completion handler fires on an arbitrary queue
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    hasPermission = granted
                    dispatch_async(dispatch_get_main_queue()) {
                        onPermissionResult(granted)
                    }
                }
            } else {
                // Permission was previously denied or restricted
                onPermissionResult(false)
            }
        }
    }
}
