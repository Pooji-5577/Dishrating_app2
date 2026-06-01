package com.example.smackcheck2.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.smackcheck2.util.Logger

/**
 * Supabase response for restaurant photo data.
 */
@Serializable
data class RestaurantPhotoRow(
    val id: String,
    @SerialName("google_place_id")
    val googlePlaceId: String? = null,
    @SerialName("photo_urls")
    val photoUrls: List<String>? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)

/**
 * Service for fetching restaurant images via the SmackCheck backend.
 *
 * Strategy:
 * The backend owns Google Places access, Supabase cache reads/writes, and API
 * key handling. The app only asks for UI-ready photo URLs.
 *
 * The API key never leaves the server — it's only accessed by the Edge Function.
 */
class RestaurantPhotoService {

    /**
     * Get photo URLs for a restaurant.
     * Returns cached URLs from Supabase, or fetches from Google Places API
     * via Edge Function if not cached.
     *
     * @param restaurantId  UUID of the restaurant in Supabase
     * @param restaurantName  Name for Google Places search fallback
     * @param city  City for Google Places search fallback
     * @param placeId  Google Place ID if already known
     * @return List of photo URLs, or empty list if none found
     */
    suspend fun getRestaurantPhotos(
        restaurantId: String,
        restaurantName: String = "",
        city: String = "",
        placeId: String? = null
    ): List<String> {
        return try {
            val response = ApiClient.get<RestaurantPhotosResponse>(
                "places/restaurant-photos",
                mapOf(
                    "restaurantId" to restaurantId,
                    "restaurantName" to restaurantName,
                    "city" to city,
                    "placeId" to placeId
                )
            )
            if (response.photos.isNotEmpty()) {
                Logger.d("RestaurantPhotoService", "[DEBUG][PhotoService] Backend returned ${response.photos.size} photos for '$restaurantName'")
                return response.photos
            }
            Logger.d("RestaurantPhotoService", "[DEBUG][PhotoService] No photos found anywhere for '$restaurantName' (id=$restaurantId, placeId=$placeId)")
            emptyList()
        } catch (e: Exception) {
            Logger.e("RestaurantPhotoService", "[DEBUG][PhotoService] ERROR for '$restaurantName': ${e::class.simpleName} - ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get a single thumbnail URL (first photo) — efficient for search cards.
     */
    suspend fun getThumbnailUrl(
        restaurantId: String,
        restaurantName: String = "",
        city: String = "",
        placeId: String? = null
    ): String? {
        return getRestaurantPhotos(restaurantId, restaurantName, city, placeId).firstOrNull()
    }

    @Serializable
    private data class RestaurantPhotosResponse(
        val photos: List<String> = emptyList()
    )
}
