package com.example.smackcheck2.data

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
 * Service for fetching restaurant images via the Supabase Edge Function.
 *
 * Strategy:
 * 1. First check if restaurant already has cached photo_urls in Supabase.
 * 2. If not, call the deployed "google-places" Edge Function which uses
 *    the GOOGLE_PLACES_API_KEY secret (stored in Supabase) to search
 *    Google Places and return photo URLs.
 * 3. Cache the returned URLs in Supabase for future requests.
 *
 * The API key never leaves the server — it's only accessed by the Edge Function.
 */
class RestaurantPhotoService {

    private val supabaseClient = SupabaseClient.client

    private val json = Json { ignoreUnknownKeys = true }

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
            // Step 1: Check if photos are already cached in Supabase
            val cached = getCachedPhotos(restaurantId)
            if (cached.isNotEmpty()) {
                return cached
            }

            // Step 2: Call Edge Function to fetch from Google Places API
            val fetched = fetchPhotosFromEdgeFunction(restaurantName, city, placeId)
            if (fetched.isNotEmpty()) {
                // Step 3: Cache URLs in Supabase for future requests
                cachePhotoUrls(restaurantId, fetched)
                return fetched
            }

            // Step 4: Fallback — return empty (UI will show placeholder)
            emptyList()
        } catch (e: Exception) {
            println("RestaurantPhotoService error: ${e.message}")
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

    /**
     * Check if restaurant already has cached photo URLs in Supabase.
     */
    private suspend fun getCachedPhotos(restaurantId: String): List<String> {
        return try {
            val result = supabaseClient.from("restaurants")
                .select {
                    filter { eq("id", restaurantId) }
                }
                .decodeSingleOrNull<RestaurantPhotoRow>()

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
     * Call the deployed Supabase "google-places" Edge Function.
     *
     * The Edge Function:
     *   1. Searches Google Places by restaurant name + city (or uses placeId)
     *   2. Fetches photo references from Place Details
     *   3. Returns up to 5 direct photo URLs (with API key baked in server-side)
     *
     * Request:  { "restaurantName": "...", "city": "...", "placeId": "..." }
     * Response: { "photos": ["url1", "url2", ...] }
     */
    private suspend fun fetchPhotosFromEdgeFunction(
        restaurantName: String,
        city: String,
        placeId: String? = null
    ): List<String> {
        return try {
            // Build request body
            val requestBody = PhotoSearchRequest(
                restaurantName = restaurantName,
                city = city,
                placeId = placeId
            )

            // Use Supabase functions client which handles auth headers automatically
            val response = supabaseClient.functions.invoke(
                function = "google-places",
                body = requestBody
            )

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                println("Edge Function error (${response.status.value}): $errorText")
                return emptyList()
            }

            val responseText = response.body<String>()
            println("Edge Function response: $responseText")

            // Parse the JSON response: { "photos": ["url1", "url2", ...] }
            val jsonElement = json.parseToJsonElement(responseText)
            val photosArray = jsonElement.jsonObject["photos"]?.jsonArray ?: return emptyList()

            photosArray.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            println("fetchPhotosFromEdgeFunction error: ${e.message}")
            emptyList()
        }
    }

    @Serializable
    private data class PhotoSearchRequest(
        val restaurantName: String,
        val city: String,
        val placeId: String? = null
    )

    /**
     * Cache photo URLs in the restaurants table for future lookups.
     */
    suspend fun cachePhotoUrls(restaurantId: String, photoUrls: List<String>) {
        try {
            @Serializable
            data class PhotoUpdate(
                @SerialName("photo_urls") val photoUrls: List<String>
            )

            supabaseClient.from("restaurants")
                .update(PhotoUpdate(photoUrls = photoUrls)) {
                    filter { eq("id", restaurantId) }
                }
        } catch (e: Exception) {
            println("cachePhotoUrls error: ${e.message}")
        }
    }
}
