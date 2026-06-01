package com.example.smackcheck2.data

import com.example.smackcheck2.model.Restaurant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.smackcheck2.util.Logger

/**
 * Supabase response model for restaurant search
 */
@Serializable
data class SupabaseRestaurantRow(
    val id: String,
    val name: String,
    val city: String? = null,
    val cuisine: String? = null,
    val category: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("average_rating")
    val averageRating: Double? = null,
    @SerialName("rating_count")
    val ratingCount: Int? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("google_place_id")
    val googlePlaceId: String? = null,
    @SerialName("photo_urls")
    val photoUrls: List<String>? = null
)

/**
 * Repository for searching restaurants and dishes from Supabase.
 * Supports "Restaurants & Cafes Only" filter.
 */
class SearchRepository {

    /**
     * Search restaurants from Supabase with optional filters.
     *
     * @param query        Search text (matches name, cuisine, city)
     * @param cuisines     Set of selected cuisine filters
     * @param minRating    Minimum average rating filter
     * @param city         City filter
     * @param restaurantsAndCafesOnly  When true, only return restaurants/cafes
     * @return List of matching Restaurant objects
     */
    suspend fun searchRestaurants(
        query: String,
        cuisines: Set<String> = emptySet(),
        minRating: Float? = null,
        city: String? = null,
        restaurantsAndCafesOnly: Boolean = false,
        limit: Int = 50,
        offset: Int = 0
    ): List<Restaurant> {
        return try {
            val results = ApiClient.get<List<SupabaseRestaurantRow>>(
                "restaurants",
                buildMap {
                    if (query.isNotBlank()) put("query", query)
                    if (cuisines.isNotEmpty()) put("cuisines", cuisines.joinToString(","))
                    if (minRating != null) put("minRating", minRating.toString())
                    if (!city.isNullOrBlank()) put("city", city)
                    if (restaurantsAndCafesOnly) put("restaurantsOnly", "true")
                    put("limit", limit.toString())
                    put("offset", offset.toString())
                }
            )

            // Map Supabase rows to app model
            results.map { row ->
                // Build image URLs: prefer photo_urls, fallback to image_url
                val images = when {
                    !row.photoUrls.isNullOrEmpty() -> row.photoUrls
                    !row.imageUrl.isNullOrBlank() -> listOf(row.imageUrl)
                    else -> emptyList()
                }

                Restaurant(
                    id = row.id,
                    name = row.name,
                    city = row.city ?: "",
                    cuisine = row.cuisine ?: "",
                    category = row.category ?: "",
                    averageRating = row.averageRating?.toFloat() ?: 0f,
                    reviewCount = row.ratingCount ?: 0,
                    latitude = row.latitude,
                    longitude = row.longitude,
                    imageUrls = images,
                    googlePlaceId = row.googlePlaceId
                )
            }
        } catch (e: Exception) {
            Logger.e("SearchRepository", "SearchRepository.searchRestaurants error: ${e.message}", e)
            emptyList()
        }
    }
}
