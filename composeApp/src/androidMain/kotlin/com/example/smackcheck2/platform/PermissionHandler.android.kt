package com.example.smackcheck2.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
    
    Log.d(TAG, "RequestLocationPermission composable entered")

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Log.d(TAG, "Permission result callback: granted=$granted")
        onPermissionResult(granted)
    }
    
    // The requestPermission function that will be passed to content
    val requestPermission: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "requestPermission() invoked, hasPermission=$hasPermission")

        if (hasPermission) {
            Log.d(TAG, "Already have permission, calling onPermissionResult(true)")
            onPermissionResult(true)
        } else {
            Log.d(TAG, "Launching permission dialog NOW...")
            // Ensure we're on the main thread for ActivityResultLauncher
            Handler(Looper.getMainLooper()).post {
                try {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                    Log.d(TAG, "permissionLauncher.launch() called successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching permission dialog", e)
                }
            }
        }
    }
    
    Log.d(TAG, "Calling content() with requestPermission lambda")
    content(requestPermission)
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
