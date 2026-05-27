package com.example.smackcheck2.platform

import com.example.smackcheck2.data.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.smackcheck2.util.Logger

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
    val photoUrl: String?,  // Direct photo URL for displaying images
    val isOpen: Boolean?
)

/**
 * Data class representing geocoded city coordinates
 */
data class GeocodedCity(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String?
)

/**
 * Places API service that proxies requests through the custom backend.
 *
 * The Google Places API key is stored server-side, keeping it out of client code entirely.
 */
class PlacesService {

    /**
     * Find nearby restaurants based on current location.
     *
     * GET /api/places/nearby?lat=...&lng=...&radius=5000&keyword=...&minRating=...
     *
     * @param latitude User's current latitude
     * @param longitude User's current longitude
     * @param radiusInMeters Search radius in meters (default: 5000m)
     * @param keyword Optional keyword to search for (e.g., "Italian", "Japanese")
     * @param minRating Optional minimum rating filter (applied client-side)
     * @return List of nearby restaurants
     */
    suspend fun findNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Int = 5000,
        keyword: String? = null,
        minRating: Double? = null
    ): List<NearbyRestaurant> {
        return try {
            val params = buildMap<String, String?> {
                put("lat", latitude.toString())
                put("lng", longitude.toString())
                put("radius", radiusInMeters.toString())
                if (keyword != null) put("keyword", keyword)
                if (minRating != null) put("minRating", minRating.toString())
            }

            val response: List<RestaurantDto> = ApiClient.get(
                path = "places/nearby",
                params = params
            )

            val results = response.map { it.toNearbyRestaurant() }

            // Filter by minimum rating client-side if specified
            if (minRating != null) {
                results.filter { restaurant ->
                    restaurant.rating != null && restaurant.rating >= minRating
                }
            } else {
                results
            }
        } catch (e: Exception) {
            Logger.e("PlacesService", "PlacesService: Exception: ${e::class.simpleName} - ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get detailed information about a specific place.
     *
     * GET /api/places/details?placeId=...
     */
    suspend fun getPlaceDetails(placeId: String): NearbyRestaurant? {
        return try {
            val response: RestaurantDto = ApiClient.get(
                path = "places/details",
                params = mapOf("placeId" to placeId)
            )
            response.toNearbyRestaurant()
        } catch (e: Exception) {
            Logger.e("PlacesService", "PlacesService: Exception: ${e::class.simpleName} - ${e.message}", e)
            null
        }
    }

    /**
     * Geocode a city name to get its coordinates.
     *
     * GET /api/places/geocode?city=...
     *
     * @param cityName The name of the city to geocode (e.g., "Rajampet", "Hyderabad")
     * @return GeocodedCity with coordinates or null if geocoding fails
     */
    suspend fun geocodeCity(cityName: String): GeocodedCity? {
        return try {
            Logger.d("PlacesService", "PlacesService: Geocoding city: $cityName")

            val response: GeocodeCityResponse = ApiClient.get(
                path = "places/geocode",
                params = mapOf("city" to cityName)
            )

            if (response.latitude == null || response.longitude == null) {
                Logger.d("PlacesService", "PlacesService: No geocode results for: $cityName")
                return null
            }

            val result = GeocodedCity(
                latitude = response.latitude,
                longitude = response.longitude,
                formattedAddress = response.formattedAddress
            )
            Logger.d("PlacesService", "PlacesService: Geocoded $cityName to: ${result.latitude}, ${result.longitude}")
            result
        } catch (e: Exception) {
            Logger.e("PlacesService", "PlacesService: Geocode exception: ${e::class.simpleName} - ${e.message}", e)
            null
        }
    }

    /**
     * Search for restaurants by text query (restaurant name + optional location bias).
     *
     * GET /api/places/search?query=...&lat=...&lng=...&radius=10000
     *
     * @param query The search query (e.g., "Blue Nail restaurant Hyderabad")
     * @return List of matching restaurants
     */
    suspend fun searchRestaurantsByText(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusInMeters: Int = 10000
    ): List<NearbyRestaurant> {
        return try {
            Logger.d("PlacesService", "PlacesService: Text search for: $query (lat=$latitude, lng=$longitude)")

            val params = buildMap<String, String?> {
                put("query", "$query restaurant")
                if (latitude != null) put("lat", latitude.toString())
                if (longitude != null) put("lng", longitude.toString())
                if (latitude != null) put("radius", radiusInMeters.toString())
            }

            val response: List<RestaurantDto> = ApiClient.get(
                path = "places/search",
                params = params
            )

            val results = response.map { it.toNearbyRestaurant() }
            Logger.d("PlacesService", "PlacesService: Text search returned ${results.size} results")
            results
        } catch (e: Exception) {
            Logger.e("PlacesService", "PlacesService: Text search exception: ${e::class.simpleName} - ${e.message}", e)
            emptyList()
        }
    }
}

// ── Response DTOs ──────────────────────────────────────────────────

@Serializable
private data class GeocodeCityResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("formatted_address")
    val formattedAddress: String? = null,
    val error: String? = null
)

@Serializable
private data class RestaurantDto(
    @SerialName("place_id")
    val id: String = "",
    val name: String = "",
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double? = null,
    @SerialName("user_ratings_total")
    val userRatingsTotal: Int? = null,
    @SerialName("price_level")
    val priceLevel: Int? = null,
    @SerialName("photo_references")
    val photoReferences: List<String> = emptyList(),
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("is_open")
    val isOpen: Boolean? = null
) {
    fun toNearbyRestaurant() = NearbyRestaurant(
        id = id,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        rating = rating,
        userRatingsTotal = userRatingsTotal,
        priceLevel = priceLevel,
        photoReference = photoReferences.firstOrNull(),
        photoUrl = photoUrl,
        isOpen = isOpen
    )
}
