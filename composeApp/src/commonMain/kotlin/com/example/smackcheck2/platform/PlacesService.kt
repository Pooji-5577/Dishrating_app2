package com.example.smackcheck2.platform

import com.example.smackcheck2.data.SupabaseClientProvider
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
 * Places API service that proxies requests through a Supabase Edge Function.
 *
 * The Google Places API key is stored server-side as a Supabase secret
 * (GOOGLE_PLACES_API_KEY), keeping it out of client code entirely.
 */
class PlacesService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val supabase = SupabaseClientProvider.client

    /**
     * Find nearby restaurants based on current location using the
     * google-places Supabase Edge Function.
     *
     * @param latitude User's current latitude
     * @param longitude User's current longitude
     * @param radiusInMeters Search radius in meters (default: 2000m = 2km)
     * @param keyword Optional keyword to search for (e.g., "Italian", "Japanese")
     * @param minRating Optional minimum rating filter (applied client-side)
     * @return List of nearby restaurants
     */
    suspend fun findNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Int = 2000,
        keyword: String? = null,
        minRating: Double? = null
    ): List<NearbyRestaurant> {
        return try {
            val requestBody = NearbySearchRequest(
                action = "nearby-search",
                latitude = latitude,
                longitude = longitude,
                radiusInMeters = radiusInMeters,
                keyword = keyword
            )

            val response = supabase.functions.invoke(
                function = "google-places",
                body = requestBody
            )

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                println("PlacesService: Edge Function error (${response.status.value}): $errorText")
                return emptyList()
            }

            val responseText = response.body<String>()
            val placesResponse = json.decodeFromString<PlacesEdgeResponse>(responseText)

            if (!placesResponse.error.isNullOrBlank()) {
                println("PlacesService: Server error: ${placesResponse.error}")
                return emptyList()
            }

            val results = placesResponse.results.map { it.toNearbyRestaurant() }

            // Filter by minimum rating client-side if specified
            if (minRating != null) {
                results.filter { restaurant ->
                    restaurant.rating != null && restaurant.rating >= minRating
                }
            } else {
                results
            }
        } catch (e: Exception) {
            println("PlacesService: Exception: ${e::class.simpleName} - ${e.message}")
            emptyList()
        }
    }

    /**
     * Get detailed information about a specific place using the
     * google-places Supabase Edge Function.
     */
    suspend fun getPlaceDetails(placeId: String): NearbyRestaurant? {
        return try {
            val requestBody = PlaceDetailsRequest(
                action = "place-details",
                placeId = placeId
            )

            val response = supabase.functions.invoke(
                function = "google-places",
                body = requestBody
            )

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                println("PlacesService: Edge Function error (${response.status.value}): $errorText")
                return null
            }

            val responseText = response.body<String>()
            val detailsResponse = json.decodeFromString<PlaceDetailsEdgeResponse>(responseText)

            if (!detailsResponse.error.isNullOrBlank()) {
                println("PlacesService: Server error: ${detailsResponse.error}")
                return null
            }

            detailsResponse.result?.toNearbyRestaurant()
        } catch (e: Exception) {
            println("PlacesService: Exception: ${e::class.simpleName} - ${e.message}")
            null
        }
    }

    /**
     * Geocode a city name to get its coordinates using Google Places Text Search.
     * This is more reliable than device geocoders for smaller/regional cities.
     *
     * @param cityName The name of the city to geocode (e.g., "Rajampet", "Hyderabad")
     * @return GeocodedCity with coordinates or null if geocoding fails
     */
    suspend fun geocodeCity(cityName: String): GeocodedCity? {
        return try {
            println("PlacesService: Geocoding city: $cityName")
            
            val requestBody = GeocodeCityRequest(
                action = "geocode-city",
                cityName = cityName
            )

            val response = supabase.functions.invoke(
                function = "google-places",
                body = requestBody
            )

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                println("PlacesService: Geocode error (${response.status.value}): $errorText")
                return null
            }

            val responseText = response.body<String>()
            val geocodeResponse = json.decodeFromString<GeocodeCityResponse>(responseText)

            if (!geocodeResponse.error.isNullOrBlank()) {
                println("PlacesService: Geocode server error: ${geocodeResponse.error}")
                return null
            }

            if (geocodeResponse.latitude == null || geocodeResponse.longitude == null) {
                println("PlacesService: No geocode results for: $cityName")
                return null
            }

            val result = GeocodedCity(
                latitude = geocodeResponse.latitude,
                longitude = geocodeResponse.longitude,
                formattedAddress = geocodeResponse.formattedAddress
            )
            println("PlacesService: Geocoded $cityName to: ${result.latitude}, ${result.longitude}")
            result
        } catch (e: Exception) {
            println("PlacesService: Geocode exception: ${e::class.simpleName} - ${e.message}")
            null
        }
    }

    /**
     * Search for restaurants by text query (restaurant name + optional city).
     * Uses Google Places Text Search API.
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
            println("PlacesService: Text search for: $query (lat=$latitude, lng=$longitude)")

            val requestBody = TextSearchRequest(
                action = "text-search",
                query = "$query restaurant",
                latitude = latitude,
                longitude = longitude,
                radiusInMeters = if (latitude != null) radiusInMeters else null
            )

            val response = supabase.functions.invoke(
                function = "google-places",
                body = requestBody
            )

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                println("PlacesService: Text search error (${response.status.value}): $errorText")
                return emptyList()
            }

            val responseText = response.body<String>()
            val placesResponse = json.decodeFromString<PlacesEdgeResponse>(responseText)

            if (!placesResponse.error.isNullOrBlank()) {
                println("PlacesService: Text search server error: ${placesResponse.error}")
                return emptyList()
            }

            val results = placesResponse.results.map { it.toNearbyRestaurant() }
            println("PlacesService: Text search returned ${results.size} results")
            results
        } catch (e: Exception) {
            println("PlacesService: Text search exception: ${e::class.simpleName} - ${e.message}")
            emptyList()
        }
    }
}

// --- Edge Function Request DTOs ---

@Serializable
private data class NearbySearchRequest(
    val action: String,
    val latitude: Double,
    val longitude: Double,
    val radiusInMeters: Int,
    val keyword: String? = null
)

@Serializable
private data class PlaceDetailsRequest(
    val action: String,
    val placeId: String
)

@Serializable
private data class GeocodeCityRequest(
    val action: String,
    val cityName: String
)

@Serializable
private data class TextSearchRequest(
    val action: String,
    val query: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusInMeters: Int? = null
)

// --- Edge Function Response DTOs ---

@Serializable
private data class PlacesEdgeResponse(
    val results: List<RestaurantDto> = emptyList(),
    val error: String? = null
)

@Serializable
private data class PlaceDetailsEdgeResponse(
    val result: RestaurantDto? = null,
    val error: String? = null
)

@Serializable
private data class GeocodeCityResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val formattedAddress: String? = null,
    val error: String? = null
)

@Serializable
private data class RestaurantDto(
    val id: String = "",
    val name: String = "",
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double? = null,
    val userRatingsTotal: Int? = null,
    val priceLevel: Int? = null,
    val photoReference: String? = null,
    val photoUrl: String? = null,
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
        photoReference = photoReference,
        photoUrl = photoUrl,
        isOpen = isOpen
    )
}
