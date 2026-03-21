package com.example.smackcheck2.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val TAG = "PermissionHandler"

/**
 * Android implementation of PermissionHandler
 */
actual class PermissionHandler(private val context: Context) {
    actual fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Composable for requesting location permission on Android
 */
@Composable
actual fun RequestLocationPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val context = LocalContext.current

    // Track if we need to trigger location after permission
    var pendingLocationRequest by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Log.d(TAG, "Permission result: granted=$granted")

        if (granted && pendingLocationRequest) {
            pendingLocationRequest = false
            onPermissionResult(true)
        } else if (!granted) {
            pendingLocationRequest = false
            onPermissionResult(false)
        }
    }

    content {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "requestPermission called, hasPermission=$hasPermission")

        if (hasPermission) {
            // Already have permission, proceed directly
            onPermissionResult(true)
        } else {
            // Need to request permission
            pendingLocationRequest = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

/**
 * Composable for requesting camera permission on Android
 */
@Composable
actual fun RequestCameraPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val context = LocalContext.current

    var pendingCameraRequest by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Camera permission result: granted=$granted")
        if (pendingCameraRequest) {
            pendingCameraRequest = false
            onPermissionResult(granted)
        }
    }

    content {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "requestCameraPermission called, hasPermission=$hasPermission")

        if (hasPermission) {
            onPermissionResult(true)
        } else {
            pendingCameraRequest = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Composable for requesting notification permission on Android (API 33+).
 * On older versions, notifications are always enabled without a runtime permission.
 */
@Composable
actual fun RequestNotificationPermission(
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        content { onPermissionResult(true) }
        return
    }

    val context = LocalContext.current
    var pendingNotifRequest by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Notification permission result: granted=$granted")
        if (pendingNotifRequest) {
            pendingNotifRequest = false
            onPermissionResult(granted)
        }
    }

    content {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onPermissionResult(true)
        } else {
            pendingNotifRequest = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
