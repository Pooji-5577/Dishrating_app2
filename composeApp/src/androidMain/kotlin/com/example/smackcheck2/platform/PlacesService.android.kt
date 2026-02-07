package com.example.smackcheck2.platform

import android.content.Context
import android.util.Log
import com.example.smackcheck2.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "PlacesService"

@Serializable
data class PlacesApiResponse(
    val results: List<PlaceResult> = emptyList(),
    val status: String = "",
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class PlaceResult(
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
data class Geometry(
    val location: Location
)

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)

@Serializable
data class Photo(
    @SerialName("photo_reference") val photoReference: String
)

@Serializable
data class OpeningHours(
    @SerialName("open_now") val openNow: Boolean? = null
)

/**
 * Android implementation of Places API service for finding nearby restaurants
 * Uses HTTP API instead of SDK due to compatibility issues
 */
actual class PlacesService(private val context: Context) {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY

    /**
     * Find nearby restaurants based on current location using Places API (New) Nearby Search
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
        Log.d(TAG, "Finding nearby restaurants at ($latitude, $longitude) within ${radiusInMeters}m, keyword: $keyword, minRating: $minRating")

        return@withContext try {
            var url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=$latitude,$longitude" +
                    "&radius=$radiusInMeters" +
                    "&type=restaurant"

            // Add keyword for cuisine search if provided
            if (!keyword.isNullOrBlank()) {
                url += "&keyword=$keyword"
            }

            url += "&key=$apiKey"

            Log.d(TAG, "Requesting: $url")

            val response: PlacesApiResponse = httpClient.get(url).body()

            Log.d(TAG, "Places API status: ${response.status}")
            if (response.status != "OK" && response.status != "ZERO_RESULTS") {
                Log.e(TAG, "Places API error: ${response.errorMessage ?: response.status}")
                return@withContext emptyList()
            }

            Log.d(TAG, "Found ${response.results.size} restaurants")

            // Filter by minimum rating if specified
            val filteredResults = if (minRating != null) {
                response.results.filter { place ->
                    place.rating != null && place.rating >= minRating
                }
            } else {
                response.results
            }

            Log.d(TAG, "After rating filter: ${filteredResults.size} restaurants")

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
            Log.e(TAG, "Error finding nearby restaurants", e)
            emptyList()
        }
    }

    /**
     * Get detailed information about a specific place
     */
    actual suspend fun getPlaceDetails(placeId: String): NearbyRestaurant? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=$placeId" +
                    "&key=$apiKey"

            val response: PlacesApiResponse = httpClient.get(url).body()

            val place = response.results.firstOrNull() ?: return@withContext null

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
        } catch (e: Exception) {
            Log.e(TAG, "Error getting place details", e)
            null
        }
    }
}
