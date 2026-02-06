package com.example.smackcheck2.platform

/**
 * Data class representing a nearby restaurant
 */
data class NearbyRestaurant(
    val id: String,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val priceLevel: Int?,
    val photoReference: String?,
    val isOpen: Boolean?
)

/**
 * Expected platform-specific Places API service
 */
expect class PlacesService {
    /**
     * Find nearby restaurants based on current location
     * @param latitude User's current latitude
     * @param longitude User's current longitude
     * @param radiusInMeters Search radius in meters (default: 2000m = 2km)
     * @return List of nearby restaurants
     */
    suspend fun findNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Int = 2000
    ): List<NearbyRestaurant>

    /**
     * Get detailed information about a specific place
     */
    suspend fun getPlaceDetails(placeId: String): NearbyRestaurant?
}
