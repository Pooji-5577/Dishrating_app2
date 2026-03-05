package com.example.smackcheck2.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle

// --- Google Places API response models (mirrored from Android) ---

@Serializable
private data class PlacesApiResponse(
    val results: List<PlaceResult> = emptyList(),
    val status: String = "",
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
private data class PlaceResult(
    @SerialName("place_id") val placeId: String,
    val name: String,
    val vicinity: String? = null,
    val geometry: Geometry,
    val rating: Double? = null,
    @SerialName("user_ratings_total") val userRatingsTotal: Int? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    val photos: List<Photo>? = null,
    @SerialName("opening_hours") val openingHours: OpeningHours? = null
)

@Serializable
private data class Geometry(
    val location: LatLng
)

@Serializable
private data class LatLng(
    val lat: Double,
    val lng: Double
)

@Serializable
private data class Photo(
    @SerialName("photo_reference") val photoReference: String
)

@Serializable
private data class OpeningHours(
    @SerialName("open_now") val openNow: Boolean? = null
)

// --- Place Details response models ---

@Serializable
private data class PlaceDetailsApiResponse(
    val result: PlaceDetailResult? = null,
    val status: String = "",
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
private data class PlaceDetailResult(
    @SerialName("place_id") val placeId: String,
    val name: String,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    val vicinity: String? = null,
    val geometry: Geometry,
    val rating: Double? = null,
    @SerialName("user_ratings_total") val userRatingsTotal: Int? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    val photos: List<Photo>? = null,
    @SerialName("opening_hours") val openingHours: OpeningHours? = null
)

/**
 * iOS implementation of PlacesService.
 *
 * Uses Ktor HTTP client with the Darwin engine to call the Google Places API,
 * mirroring the approach used in the Android implementation.
 */
actual class PlacesService {

    private val httpClient = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    /**
     * Resolve the Google Maps API key.
     * Priority:
     * 1. Info.plist key "GOOGLE_MAPS_API_KEY"
     * 2. Hardcoded fallback (same as local.properties value for development)
     *
     * For production, add GOOGLE_MAPS_API_KEY to your iOS Info.plist.
     */
    private val apiKey: String by lazy {
        val plistKey = NSBundle.mainBundle.objectForInfoDictionaryKey("GOOGLE_MAPS_API_KEY") as? String
        plistKey ?: "SET_YOUR_API_KEY"
    }

    /**
     * Find nearby restaurants based on current location using Places API Nearby Search.
     *
     * @param latitude User's current latitude
     * @param longitude User's current longitude
     * @param radiusInMeters Search radius in meters (default: 2000m = 2km)
     * @param keyword Optional keyword to search for (e.g., "Italian", "Japanese")
     * @param minRating Optional minimum rating filter
     * @return List of nearby restaurants
     */
    actual suspend fun findNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Int,
        keyword: String?,
        minRating: Double?
    ): List<NearbyRestaurant> = withContext(Dispatchers.IO) {
        return@withContext try {
            var url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=$latitude,$longitude" +
                    "&radius=$radiusInMeters" +
                    "&type=restaurant"

            if (!keyword.isNullOrBlank()) {
                url += "&keyword=$keyword"
            }

            url += "&key=$apiKey"

            val response: PlacesApiResponse = httpClient.get(url).body()

            if (response.status != "OK" && response.status != "ZERO_RESULTS") {
                return@withContext emptyList()
            }

            // Filter by minimum rating if specified
            val filteredResults = if (minRating != null) {
                response.results.filter { place ->
                    place.rating != null && place.rating >= minRating
                }
            } else {
                response.results
            }

            filteredResults.map { place ->
                NearbyRestaurant(
                    id = place.placeId,
                    name = place.name,
                    address = place.vicinity,
                    latitude = place.geometry.location.lat,
                    longitude = place.geometry.location.lng,
                    rating = place.rating,
                    userRatingsTotal = place.userRatingsTotal,
                    priceLevel = place.priceLevel,
                    photoReference = place.photos?.firstOrNull()?.photoReference,
                    isOpen = place.openingHours?.openNow
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get detailed information about a specific place using the Place Details API.
     */
    actual suspend fun getPlaceDetails(placeId: String): NearbyRestaurant? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=$placeId" +
                    "&fields=place_id,name,formatted_address,vicinity,geometry,rating,user_ratings_total,price_level,photos,opening_hours" +
                    "&key=$apiKey"

            val response: PlaceDetailsApiResponse = httpClient.get(url).body()

            val place = response.result ?: return@withContext null

            NearbyRestaurant(
                id = place.placeId,
                name = place.name,
                address = place.formattedAddress ?: place.vicinity,
                latitude = place.geometry.location.lat,
                longitude = place.geometry.location.lng,
                rating = place.rating,
                userRatingsTotal = place.userRatingsTotal,
                priceLevel = place.priceLevel,
                photoReference = place.photos?.firstOrNull()?.photoReference,
                isOpen = place.openingHours?.openNow
            )
        } catch (e: Exception) {
            null
        }
    }
}
