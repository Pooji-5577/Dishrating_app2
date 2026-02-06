package com.example.smackcheck2.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "LocationService"

/**
 * Android implementation of LocationService
 */
actual class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): LocationResult? {
        Log.d(TAG, "getCurrentLocation called")

        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return null
        }

        // Check if location services are enabled
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        Log.d(TAG, "GPS enabled: $isGpsEnabled, Network enabled: $isNetworkEnabled")

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.e(TAG, "Location services are disabled")
            return null
        }

        // Try to get location with timeout
        return withTimeoutOrNull(15000L) {
            try {
                // First try last known location for quick result
                val lastLocation = getLastKnownLocation()
                if (lastLocation != null) {
                    Log.d(TAG, "Got last known location: ${lastLocation.cityName}")
                    return@withTimeoutOrNull lastLocation
                }

                // If no last location, request current location
                Log.d(TAG, "No last location, requesting current location")
                getCurrentLocationFromProvider()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): LocationResult? {
        return suspendCancellableCoroutine { continuation ->
            var resumed = false

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (!resumed) {
                        resumed = true
                        if (location != null) {
                            Log.d(TAG, "Last location: ${location.latitude}, ${location.longitude}")
                            val cityName = getCityFromCoordinates(location.latitude, location.longitude)
                            continuation.resume(
                                LocationResult(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    cityName = cityName,
                                    fullAddress = getAddressFromCoordinates(location.latitude, location.longitude)
                                )
                            )
                        } else {
                            Log.d(TAG, "Last location is null")
                            continuation.resume(null)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (!resumed) {
                        resumed = true
                        Log.e(TAG, "Failed to get last location", e)
                        continuation.resume(null)
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationFromProvider(): LocationResult? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            var resumed = false

            // Use balanced power accuracy for faster results (uses network + GPS)
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (!resumed) {
                    resumed = true
                    if (location != null) {
                        Log.d(TAG, "Current location: ${location.latitude}, ${location.longitude}")
                        val cityName = getCityFromCoordinates(location.latitude, location.longitude)
                        continuation.resume(
                            LocationResult(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                cityName = cityName,
                                fullAddress = getAddressFromCoordinates(location.latitude, location.longitude)
                            )
                        )
                    } else {
                        Log.d(TAG, "Current location is null")
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener { e ->
                if (!resumed) {
                    resumed = true
                    Log.e(TAG, "Failed to get current location", e)
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    actual suspend fun searchPlaces(query: String): List<LocationResult> {
        if (query.isBlank()) return emptyList()

        return try {
            @Suppress("DEPRECATION")
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 10) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                geocoder.getFromLocationName(query, 10) ?: emptyList()
            }

            addresses.mapNotNull { address ->
                val cityName = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: return@mapNotNull null

                LocationResult(
                    latitude = address.latitude,
                    longitude = address.longitude,
                    cityName = cityName,
                    fullAddress = address.getAddressLine(0)
                )
            }.distinctBy { it.cityName }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching places", e)
            emptyList()
        }
    }

    actual fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "hasLocationPermission - Fine: $hasFine, Coarse: $hasCoarse")
        return hasFine || hasCoarse
    }

    /**
     * Detect if running on an Android emulator
     */
    actual fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocationWithDetails(): LocationOperationResult {
        Log.d(TAG, "getCurrentLocationWithDetails called")
        val emulator = isEmulator()
        Log.d(TAG, "Running on emulator: $emulator")

        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return LocationOperationResult.Error(LocationErrorReason.PERMISSION_DENIED, emulator)
        }

        // Check if location services are enabled
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        Log.d(TAG, "GPS enabled: $isGpsEnabled, Network enabled: $isNetworkEnabled")

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.e(TAG, "Location services are disabled")
            return LocationOperationResult.Error(LocationErrorReason.LOCATION_SERVICES_DISABLED, emulator)
        }

        // Try to get location with timeout
        val result = withTimeoutOrNull(15000L) {
            try {
                // First try last known location for quick result
                val lastLocation = getLastKnownLocation()
                if (lastLocation != null) {
                    Log.d(TAG, "Got last known location: ${lastLocation.cityName}")
                    return@withTimeoutOrNull lastLocation
                }

                // If no last location, request current location
                Log.d(TAG, "No last location, requesting current location")
                getCurrentLocationFromProvider()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                null
            }
        }

        return if (result != null) {
            LocationOperationResult.Success(result)
        } else {
            Log.e(TAG, "Location detection failed - no location available (emulator: $emulator)")
            LocationOperationResult.Error(LocationErrorReason.NO_LOCATION_AVAILABLE, emulator)
        }
    }

    private fun getCityFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                val city = address.locality ?: address.subAdminArea ?: address.adminArea
                Log.d(TAG, "City from coordinates: $city")
                city
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting city from coordinates", e)
            null
        }
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address from coordinates", e)
            null
        }
    }
}
