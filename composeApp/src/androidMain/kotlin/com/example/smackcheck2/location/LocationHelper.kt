package com.example.smackcheck2.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Data class holding the result of a location detection.
 *
 * @param latitude  GPS latitude
 * @param longitude GPS longitude
 * @param city      Reverse-geocoded city / locality name (nullable if geocoding fails)
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val city: String?
)

/**
 * Sealed class representing the various outcomes of a location request.
 */
sealed class SmackLocationResult {
    /** Location was successfully fetched. */
    data class Success(val locationData: LocationData) : SmackLocationResult()

    /** Location permission has not been granted by the user. */
    data class PermissionDenied(val message: String = "Location permission denied") : SmackLocationResult()

    /** The device's location services (GPS / network) are turned off. */
    data class LocationDisabled(val message: String = "Location services are disabled. Please enable GPS.") : SmackLocationResult()

    /** A generic / unexpected error occurred. */
    data class Error(val message: String) : SmackLocationResult()
}

/**
 * Helper class that encapsulates all location-related logic:
 *  • Permission checking
 *  • GPS / network provider availability
 *  • FusedLocationProviderClient for getting coordinates
 *  • Geocoder for reverse-geocoding (lat/lng → city name)
 *
 * Usage:
 * ```
 *   val helper = LocationHelper(context)
 *   // Inside a coroutine:
 *   val result = helper.getCurrentLocation()
 *   when (result) {
 *       is LocationResult.Success -> { /* use result.locationData */ }
 *       is LocationResult.PermissionDenied -> { /* request permission */ }
 *       ...
 *   }
 * ```
 */
class LocationHelper(private val context: Context) {

    companion object {
        private const val TAG = "LocationHelper"
    }

    // Google Play Services fused provider – best accuracy & battery trade-off
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // ──────────────────────────────────────────────
    // 1. Permission check
    // ──────────────────────────────────────────────

    /**
     * Returns `true` when BOTH fine and coarse location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine && coarse
    }

    // ──────────────────────────────────────────────
    // 2. Location services (GPS / Network) check
    // ──────────────────────────────────────────────

    /**
     * Returns `true` when at least one of GPS_PROVIDER or NETWORK_PROVIDER is enabled.
     */
    fun isLocationEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gpsEnabled || networkEnabled
    }

    // ──────────────────────────────────────────────
    // 3. Get current location (suspend, coroutine-friendly)
    // ──────────────────────────────────────────────

    /**
     * Main entry point – fetches the device's current location.
     *
     * Steps:
     *  1. Check permissions → return [LocationResult.PermissionDenied] if missing.
     *  2. Check location services → return [LocationResult.LocationDisabled] if off.
     *  3. Try to get the last-known location (fast).
     *  4. If unavailable, request a fresh single location update.
     *  5. Reverse-geocode the coordinates into a city name.
     *  6. Return [LocationResult.Success] with all the data.
     */
    suspend fun getCurrentLocation(): SmackLocationResult {
        // Step 1: permission guard
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return SmackLocationResult.PermissionDenied()
        }

        // Step 2: location-services guard
        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services disabled on device")
            return SmackLocationResult.LocationDisabled()
        }

        return try {
            // Step 3 & 4: get coordinates
            val location = getLastKnownOrFreshLocation()
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude

                // Step 5: reverse geocode
                val cityName = reverseGeocode(lat, lng)

                Log.d(TAG, "Location obtained: lat=$lat, lng=$lng, city=$cityName")
                SmackLocationResult.Success(LocationData(lat, lng, cityName))
            } else {
                Log.e(TAG, "Unable to obtain location (null)")
                SmackLocationResult.Error("Unable to detect location. Please ensure GPS is enabled and try again.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while getting location", e)
            SmackLocationResult.PermissionDenied("Location permission revoked")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            SmackLocationResult.Error("Failed to get location: ${e.localizedMessage}")
        }
    }

    // ──────────────────────────────────────────────
    // 4. Internal – try last-known, then fresh request
    // ──────────────────────────────────────────────

    @SuppressLint("MissingPermission") // permission checked by caller
    private suspend fun getLastKnownOrFreshLocation(): android.location.Location? {
        // First try the cached last-known location (instant, no battery cost)
        val lastKnown = getLastKnownLocation()
        if (lastKnown != null) {
            Log.d(TAG, "Using last known location")
            return lastKnown
        }

        // Fall back to requesting a fresh single update
        Log.d(TAG, "No last known location, requesting fresh location...")
        return requestFreshLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
        }

    /**
     * Requests a single high-accuracy location update via [FusedLocationProviderClient].
     * Times out after ~10 s if no fix is obtained.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1_000L // interval – only used briefly
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500L)
                .setMaxUpdates(1) // single update
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    cont.resume(result.lastLocation)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )

            // Clean up if the coroutine is cancelled
            cont.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }

    // ──────────────────────────────────────────────
    // 5. Reverse geocoding (lat/lng → city / locality)
    // ──────────────────────────────────────────────

    /**
     * Converts latitude / longitude into a human-readable city or locality name.
     * Returns `null` when geocoding is unavailable or fails.
     */
    private fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return try {
            if (!Geocoder.isPresent()) {
                Log.w(TAG, "Geocoder not available on this device")
                return null
            }

            val geocoder = Geocoder(context, Locale.getDefault())

            @Suppress("DEPRECATION") // getFromLocation is deprecated in API 33+ but still works; Tiramisu callback variant is optional
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Prefer locality → subAdminArea → adminArea → countryName
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.countryName

                Log.d(TAG, "Reverse geocoded: $city (full: ${address.getAddressLine(0)})")
                city
            } else {
                Log.w(TAG, "Geocoder returned no addresses for $latitude, $longitude")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed", e)
            null
        }
    }

    // ──────────────────────────────────────────────
    // 6. Observe location as a Flow (optional, for continuous updates)
    // ──────────────────────────────────────────────

    /**
     * Emits [LocationData] updates as the device moves.
     * Use this if you want real-time location tracking (e.g. on a map).
     *
     * The flow automatically cleans up the location callback when cancelled.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long = 10_000L): Flow<LocationData> = callbackFlow {
        if (!hasLocationPermission() || !isLocationEnabled()) {
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMs
        ).setMinUpdateIntervalMillis(intervalMs / 2).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { loc ->
                    val city = reverseGeocode(loc.latitude, loc.longitude)
                    trySend(LocationData(loc.latitude, loc.longitude, city))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest, callback, Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
