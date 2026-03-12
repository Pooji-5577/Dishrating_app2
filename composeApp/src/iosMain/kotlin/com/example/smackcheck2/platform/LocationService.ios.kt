package com.example.smackcheck2.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.NSProcessInfo
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/**
 * iOS implementation of LocationService using CoreLocation framework
 */
@OptIn(ExperimentalForeignApi::class)
actual class LocationService {

    private val geocoder = CLGeocoder()

    /**
     * Get current location using CoreLocation.
     * Returns null if location cannot be determined or permission denied.
     */
    actual suspend fun getCurrentLocation(): LocationResult? {
        if (!hasLocationPermission()) {
            return null
        }

        if (!CLLocationManager.locationServicesEnabled()) {
            return null
        }

        return withTimeoutOrNull(15000L) {
            try {
                val location = requestSingleLocation()
                if (location != null) {
                    val placemark = reverseGeocode(location)
                    LocationResult(
                        latitude = location.coordinate.useContents { latitude },
                        longitude = location.coordinate.useContents { longitude },
                        cityName = placemark?.locality
                            ?: placemark?.subAdministrativeArea
                            ?: placemark?.administrativeArea,
                        fullAddress = buildFullAddress(placemark)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Get current location with detailed error information.
     * Returns Success with location or Error with specific reason.
     */
    actual suspend fun getCurrentLocationWithDetails(): LocationOperationResult {
        val emulator = isEmulator()

        if (!hasLocationPermission()) {
            val status = CLLocationManager.authorizationStatus()
            return if (status == kCLAuthorizationStatusNotDetermined) {
                LocationOperationResult.Error(LocationErrorReason.PERMISSION_DENIED, emulator)
            } else {
                LocationOperationResult.Error(LocationErrorReason.PERMISSION_DENIED, emulator)
            }
        }

        if (!CLLocationManager.locationServicesEnabled()) {
            return LocationOperationResult.Error(LocationErrorReason.LOCATION_SERVICES_DISABLED, emulator)
        }

        val result = withTimeoutOrNull(15000L) {
            try {
                val location = requestSingleLocation()
                if (location != null) {
                    val placemark = reverseGeocode(location)
                    LocationResult(
                        latitude = location.coordinate.useContents { latitude },
                        longitude = location.coordinate.useContents { longitude },
                        cityName = placemark?.locality
                            ?: placemark?.subAdministrativeArea
                            ?: placemark?.administrativeArea,
                        fullAddress = buildFullAddress(placemark)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        return if (result != null) {
            LocationOperationResult.Success(result)
        } else {
            LocationOperationResult.Error(LocationErrorReason.NO_LOCATION_AVAILABLE, emulator)
        }
    }

    /**
     * Search for places by query string using CLGeocoder.
     * Returns a list of matching location results.
     */
    actual suspend fun searchPlaces(query: String): List<LocationResult> {
        if (query.isBlank()) return emptyList()

        return try {
            val placemarks = geocodeAddressString(query)
            placemarks.mapNotNull { placemark ->
                val location = placemark.location ?: return@mapNotNull null
                val cityName = placemark.locality
                    ?: placemark.subAdministrativeArea
                    ?: placemark.administrativeArea
                    ?: return@mapNotNull null

                LocationResult(
                    latitude = location.coordinate.useContents { latitude },
                    longitude = location.coordinate.useContents { longitude },
                    cityName = cityName,
                    fullAddress = buildFullAddress(placemark)
                )
            }.distinctBy { it.cityName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if location permissions are granted.
     */
    actual fun hasLocationPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways
    }

    /**
     * Check if running on the iOS Simulator.
     */
    actual fun isEmulator(): Boolean {
        val environment = NSProcessInfo.processInfo.environment
        return environment["SIMULATOR_DEVICE_NAME"] != null
    }

    /**
     * Get coordinates for a city name using geocoding.
     * Returns LocationResult with coordinates or null if geocoding fails.
     */
    actual suspend fun getCoordinatesForCity(cityName: String): LocationResult? {
        if (cityName.isBlank()) return null

        return try {
            val placemarks = geocodeAddressString(cityName)
            val placemark = placemarks.firstOrNull() ?: return null
            val location = placemark.location ?: return null

            LocationResult(
                latitude = location.coordinate.useContents { latitude },
                longitude = location.coordinate.useContents { longitude },
                cityName = cityName,
                fullAddress = buildFullAddress(placemark)
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- Private helpers ---

    /**
     * Request a single location update from CLLocationManager using a delegate.
     * Bridges the delegate callback pattern to coroutines via suspendCancellableCoroutine.
     */
    private suspend fun requestSingleLocation(): CLLocation? {
        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            val manager = CLLocationManager()

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>
                ) {
                    if (!resumed) {
                        resumed = true
                        manager.stopUpdatingLocation()
                        val location = didUpdateLocations.lastOrNull() as? CLLocation
                        continuation.resume(location)
                    }
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError
                ) {
                    if (!resumed) {
                        resumed = true
                        manager.stopUpdatingLocation()
                        continuation.resume(null)
                    }
                }
            }

            dispatch_async(dispatch_get_main_queue()) {
                manager.delegate = delegate
                manager.desiredAccuracy = kCLLocationAccuracyBest
                manager.startUpdatingLocation()
            }

            continuation.invokeOnCancellation {
                dispatch_async(dispatch_get_main_queue()) {
                    manager.stopUpdatingLocation()
                }
            }
        }
    }

    /**
     * Reverse geocode a CLLocation to get a CLPlacemark.
     */
    private suspend fun reverseGeocode(location: CLLocation): CLPlacemark? {
        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                if (!resumed) {
                    resumed = true
                    if (error != null || placemarks == null) {
                        continuation.resume(null)
                    } else {
                        val placemark = placemarks.firstOrNull() as? CLPlacemark
                        continuation.resume(placemark)
                    }
                }
            }
        }
    }

    /**
     * Forward geocode an address string to get a list of CLPlacemarks.
     */
    private suspend fun geocodeAddressString(address: String): List<CLPlacemark> {
        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            geocoder.geocodeAddressString(address) { placemarks, error ->
                if (!resumed) {
                    resumed = true
                    if (error != null || placemarks == null) {
                        continuation.resume(emptyList())
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val result = placemarks.filterIsInstance<CLPlacemark>()
                        continuation.resume(result)
                    }
                }
            }
        }
    }

    /**
     * Build a full address string from a CLPlacemark.
     */
    private fun buildFullAddress(placemark: CLPlacemark?): String? {
        if (placemark == null) return null

        val components = listOfNotNull(
            placemark.subThoroughfare,
            placemark.thoroughfare,
            placemark.locality,
            placemark.administrativeArea,
            placemark.postalCode,
            placemark.country
        )

        return if (components.isNotEmpty()) {
            components.joinToString(", ")
        } else {
            null
        }
    }
}
