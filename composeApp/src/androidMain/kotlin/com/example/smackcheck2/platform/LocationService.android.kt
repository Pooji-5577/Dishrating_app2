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

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        Log.d(TAG, "GPS enabled: $isGpsEnabled, Network enabled: $isNetworkEnabled")

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.e(TAG, "Location services are disabled")
            return null
        }

        // Always request a fresh high-accuracy fix so distances are accurate.
        // Fall back to lastLocation only if the fresh request fails.
        return withTimeoutOrNull(20000L) {
            try {
                val fresh = getCurrentLocationFromProvider(Priority.PRIORITY_HIGH_ACCURACY)
                if (fresh != null) {
                    Log.d(TAG, "Got fresh location: ${fresh.latitude}, ${fresh.longitude}")
                    return@withTimeoutOrNull fresh
                }
                // Fallback to last known if fresh fails
                Log.d(TAG, "Fresh location null, falling back to last known")
                getLastKnownLocation()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                getLastKnownLocation()
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
                            val address = getFirstAddress(location.latitude, location.longitude)
                            continuation.resume(
                                LocationResult(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    cityName = address?.let { it.locality ?: it.subAdminArea ?: it.adminArea },
                                    fullAddress = address?.getAddressLine(0),
                                    countryCode = address?.countryCode
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
    private suspend fun getCurrentLocationFromProvider(
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): LocationResult? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            var resumed = false

            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (!resumed) {
                    resumed = true
                    if (location != null) {
                        Log.d(TAG, "Current location: ${location.latitude}, ${location.longitude}")
                        val address = getFirstAddress(location.latitude, location.longitude)
                        continuation.resume(
                            LocationResult(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                cityName = address?.let { it.locality ?: it.subAdminArea ?: it.adminArea },
                                fullAddress = address?.getAddressLine(0),
                                countryCode = address?.countryCode
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
                    fullAddress = address.getAddressLine(0),
                    countryCode = address.countryCode
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

        val result = withTimeoutOrNull(20000L) {
            try {
                val fresh = getCurrentLocationFromProvider(Priority.PRIORITY_HIGH_ACCURACY)
                if (fresh != null) return@withTimeoutOrNull fresh
                Log.d(TAG, "Fresh location null, falling back to last known")
                getLastKnownLocation()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                getLastKnownLocation()
            }
        }

        return if (result != null) {
            LocationOperationResult.Success(result)
        } else {
            Log.e(TAG, "Location detection failed - no location available (emulator: $emulator)")
            LocationOperationResult.Error(LocationErrorReason.NO_LOCATION_AVAILABLE, emulator)
        }
    }

    private fun getFirstAddress(latitude: Double, longitude: Double): android.location.Address? {
        return try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.also {
                Log.d(TAG, "Address from coordinates: city=${it.locality}, country=${it.countryCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reverse-geocoding coordinates", e)
            null
        }
    }

    /**
     * Get coordinates for a city name using geocoding
     * Returns LocationResult with coordinates or null if geocoding fails
     */
    actual suspend fun getCoordinatesForCity(cityName: String): LocationResult? {
        if (cityName.isBlank()) return null

        return try {
            Log.d(TAG, "Geocoding city: $cityName")
            @Suppress("DEPRECATION")
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(cityName, 1) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                geocoder.getFromLocationName(cityName, 1) ?: emptyList()
            }

            addresses.firstOrNull()?.let { address ->
                val result = LocationResult(
                    latitude = address.latitude,
                    longitude = address.longitude,
                    cityName = cityName,
                    fullAddress = address.getAddressLine(0),
                    countryCode = address.countryCode
                )
                Log.d(TAG, "Geocoded $cityName to: ${result.latitude}, ${result.longitude}")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding city: $cityName", e)
            null
        }
    }
}
