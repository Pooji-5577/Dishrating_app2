package com.example.smackcheck2

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.smackcheck2.location.AppLocationManager
import com.example.smackcheck2.location.LocationState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // ─── Step 1: Register the permission launcher using the modern Activity Result API ───
    // This replaces the deprecated onRequestPermissionsResult callback.
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        when {
            // ✅ Both permissions granted → immediately detect location
            fineGranted && coarseGranted -> {
                Log.d(TAG, "Both FINE and COARSE location permissions granted")
                detectLocationNow()
            }
            // ⚠️ Only coarse granted → can still get approximate location
            coarseGranted -> {
                Log.d(TAG, "Only COARSE location permission granted")
                detectLocationNow()
            }
            // ❌ All denied
            else -> {
                Log.w(TAG, "Location permissions denied by user")
                Toast.makeText(
                    this,
                    "Location permission denied. You can select a city manually.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ─── Step 2: Initialize the AppLocationManager with application context ───
        AppLocationManager.initialize(this)

        // ─── Step 3: If permissions are already granted, auto-detect location on app start ───
        if (AppLocationManager.hasPermission()) {
            detectLocationNow()
        } else {
            // Request permissions on first launch
            requestLocationPermissions()
        }

        setContent {
            App()
        }
    }

    /**
     * Launches the system permission dialog for fine + coarse location.
     * Called from the UI layer when the "Allow Location" button is pressed.
     */
    fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Triggers actual GPS detection via coroutines.
     * The result flows into [AppLocationManager.locationState] which the UI observes.
     */
    private fun detectLocationNow() {
        lifecycleScope.launch {
            AppLocationManager.detectLocation()

            // Show a toast with the result for user feedback
            when (val state = AppLocationManager.locationState.value) {
                is LocationState.Success -> {
                    val city = state.locationData.city ?: "Unknown"
                    Log.d(TAG, "Detected city: $city")
                    Toast.makeText(
                        this@MainActivity,
                        "📍 Location: $city",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is LocationState.LocationDisabled -> {
                    Toast.makeText(
                        this@MainActivity,
                        "Please enable GPS in device settings",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is LocationState.Error -> {
                    Log.e(TAG, "Location error: ${state.message}")
                }
                else -> { /* Loading, Idle, PermissionRequired – handled by UI */ }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}