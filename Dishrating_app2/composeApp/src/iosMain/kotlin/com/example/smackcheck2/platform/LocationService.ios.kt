package com.example.smackcheck2.platform

/**
 * iOS implementation of LocationService
 * Note: Full implementation requires native iOS code for CoreLocation
 */
actual class LocationService {
    actual suspend fun getCurrentLocation(): LocationResult? {
        // iOS location requires native implementation with CoreLocation
        // For now, return null - implement with native Swift/Objective-C interop
        return null
    }

    actual suspend fun getCurrentLocationWithDetails(): LocationOperationResult {
        // iOS location requires native implementation with CoreLocation
        // For now, return error - implement with native Swift/Objective-C interop
        return LocationOperationResult.Error(LocationErrorReason.UNKNOWN, isEmulator = false)
    }

    actual suspend fun searchPlaces(query: String): List<LocationResult> {
        // iOS places search requires MapKit or Google Places SDK
        // For now, return empty - implement with native code
        return emptyList()
    }

    actual fun hasLocationPermission(): Boolean {
        // iOS permission check requires CLLocationManager
        // For now, return false - implement with native code
        return false
    }

    actual fun isEmulator(): Boolean {
        // iOS simulator detection - would need native implementation
        // For now, return false
        return false
    }
}
