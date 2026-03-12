package com.example.smackcheck2.data

import com.example.smackcheck2.model.Restaurant
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    private val client = SupabaseClient.client

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
        restaurantsAndCafesOnly: Boolean = false
    ): List<Restaurant> {
        return try {
            val results = client.from("restaurants")
                .select() {
                    // Text search: match name, cuisine, or city
                    if (query.isNotBlank()) {
                        filter {
                            or {
                                ilike("name", "%$query%")
                                ilike("cuisine", "%$query%")
                                ilike("city", "%$query%")
                            }
                        }
                    }

                    // Cuisine filter
                    if (cuisines.isNotEmpty()) {
                        filter {
                            isIn("cuisine", cuisines.toList())
                        }
                    }

                    // Minimum rating filter
                    if (minRating != null) {
                        filter {
                            gte("average_rating", minRating)
                        }
                    }

                    // City filter
                    if (!city.isNullOrBlank()) {
                        filter {
                            ilike("city", "%$city%")
                        }
                    }

                    // Restaurants & Cafes Only filter
                    if (restaurantsAndCafesOnly) {
                        filter {
                            or {
                                ilike("category", "%restaurant%")
                                ilike("category", "%cafe%")
                                ilike("category", "%coffee%")
                            }
                        }
                    }
                }
                .decodeList<SupabaseRestaurantRow>()

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
            println("SearchRepository.searchRestaurants error: ${e.message}")
            emptyList()
        }
    }
}
