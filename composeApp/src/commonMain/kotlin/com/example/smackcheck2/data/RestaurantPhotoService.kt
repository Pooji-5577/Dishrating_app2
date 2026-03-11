package com.example.smackcheck2.data

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * Service for fetching restaurant images.
 *
 * Strategy:
 * 1. First check if restaurant already has cached photo_urls in Supabase.
 * 2. If not, call the Supabase Edge Function "fetch-restaurant-photos"
 *    which uses Google Places API to find the place and fetch photos.
 * 3. The Edge Function caches the photo URLs back into the restaurants table
 *    so subsequent requests are fast (no repeated Google API calls).
 *
 * Photo URL format from Google Places:
 *   https://maps.googleapis.com/maps/api/place/photo
 *     ?maxwidth=600
 *     &photo_reference=PHOTO_REFERENCE
 *     &key=YOUR_API_KEY
 *
 * The Edge Function handles the API key securely on the server side and
 * returns direct image URLs.
 */
class RestaurantPhotoService {

    private val client = SupabaseClient.client

    /**
     * Get photo URLs for a restaurant.
     * Returns cached URLs from Supabase, or fetches from Google Places API
     * via Edge Function if not cached.
     *
     * @param restaurantId  UUID of the restaurant in Supabase
     * @param restaurantName  Name for Google Places search fallback
     * @param city  City for Google Places search fallback
     * @return List of photo URLs, or empty list if none found
     */
    suspend fun getRestaurantPhotos(
        restaurantId: String,
        restaurantName: String = "",
        city: String = ""
    ): List<String> {
        return try {
            // Step 1: Check if photos are already cached in Supabase
            val cached = getCachedPhotos(restaurantId)
            if (cached.isNotEmpty()) {
                return cached
            }

            // Step 2: Call Edge Function to fetch from Google Places API
            val fetched = fetchPhotosFromEdgeFunction(restaurantId, restaurantName, city)
            if (fetched.isNotEmpty()) {
                return fetched
            }

            // Step 3: Fallback — return empty (UI will show placeholder)
            emptyList()
        } catch (e: Exception) {
            println("RestaurantPhotoService error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if restaurant already has cached photo URLs in Supabase.
     */
    private suspend fun getCachedPhotos(restaurantId: String): List<String> {
        return try {
            val result = client.from("restaurants")
                .select {
                    filter { eq("id", restaurantId) }
                }
                .decodeSingleOrNull<RestaurantPhotoRow>()

            // Return photo_urls if available, or image_url as single item
            when {
                !result?.photoUrls.isNullOrEmpty() -> result!!.photoUrls!!
                !result?.imageUrl.isNullOrBlank() -> listOf(result!!.imageUrl!!)
                else -> emptyList()
            }
        } catch (e: Exception) {
            println("getCachedPhotos error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Call the Supabase Edge Function to fetch photos from Google Places API.
     * The Edge Function will:
     *   1. Search Google Places by restaurant name + city
     *   2. Get the place_id
     *   3. Fetch photo references
     *   4. Build photo URLs
     *   5. Cache them in the restaurants table
     *   6. Return the URLs
     */
    private suspend fun fetchPhotosFromEdgeFunction(
        restaurantId: String,
        restaurantName: String,
        city: String
    ): List<String> {
        return try {
            // Call the Edge Function via Supabase Functions
            // Note: This requires deploying the "fetch-restaurant-photos" Edge Function
            // For now, we return empty — the Edge Function will populate photo_urls
            // when deployed, and getCachedPhotos will return them on next call.
            emptyList()
        } catch (e: Exception) {
            println("fetchPhotosFromEdgeFunction error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Update restaurant photo URLs in Supabase (for caching).
     */
    suspend fun cachePhotoUrls(restaurantId: String, photoUrls: List<String>) {
        try {
            @Serializable
            data class PhotoUpdate(
                @SerialName("photo_urls") val photoUrls: List<String>
            )

            client.from("restaurants")
                .update(PhotoUpdate(photoUrls = photoUrls)) {
                    filter { eq("id", restaurantId) }
                }
        } catch (e: Exception) {
            println("cachePhotoUrls error: ${e.message}")
        }
    }
}
