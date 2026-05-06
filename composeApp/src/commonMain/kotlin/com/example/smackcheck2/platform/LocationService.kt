package com.example.smackcheck2.platform

/**
 * Enum representing reasons why location detection might fail
 */
enum class LocationErrorReason {
    PERMISSION_DENIED,
    LOCATION_SERVICES_DISABLED,
    NO_LOCATION_AVAILABLE,  // Includes emulator without simulated location
    TIMEOUT,
    UNKNOWN
}

/**
 * Sealed class for location operation results
 */
sealed class LocationOperationResult {
    data class Success(val location: LocationResult) : LocationOperationResult()
    data class Error(val reason: LocationErrorReason, val isEmulator: Boolean = false) : LocationOperationResult()
}

/**
 * Data class representing a location with coordinates and city name
 */
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityName: String?,
    val fullAddress: String? = null,
    val countryCode: String? = null
)

/**
 * Expected platform-specific location service
 */
expect class LocationService {
    /**
     * Get current location using GPS
     * Returns null if location cannot be determined or permission denied
     */
    suspend fun getCurrentLocation(): LocationResult?

    /**
     * Get current location with detailed error information
     * Returns Success with location or Error with specific reason
     */
    suspend fun getCurrentLocationWithDetails(): LocationOperationResult

    /**
     * Search for places by query string
     * Returns a list of matching location results
     */
    suspend fun searchPlaces(query: String): List<LocationResult>

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean

    /**
     * Check if running on an emulator
     */
    fun isEmulator(): Boolean

    /**
     * Get coordinates for a city name using geocoding
     * Returns LocationResult with coordinates or null if geocoding fails
     */
    suspend fun getCoordinatesForCity(cityName: String): LocationResult?
}
