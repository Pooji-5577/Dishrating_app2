package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.DishDto
import com.example.smackcheck2.data.dto.RestaurantDto
import io.github.jan.supabase.postgrest.postgrest

/**
 * Supabase schema adapter that owns all schema compatibility and migration logic.
 *
 * Responsibilities:
 * - Knows which columns are stable vs optional across schema versions
 * - Handles fallback insert strategies when columns are missing
 * - Back-fills denormalized fields when safe
 * - Returns stable domain-model-friendly DTOs to callers
 *
 * The rest of the app should never need to know about column history,
 * retry-without-field patterns, or nullable DTOs for old columns.
 */
class SupabaseSchemaAdapter(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client
) {

    private val postgrest = client.postgrest

    /**
     * Insert a dish with schema-safe fallback.
     *
     * If the `restaurant_name` column is missing on the server, retries
     * without it. Callers always receive a complete DishDto back.
     */
    suspend fun insertDish(dto: DishDto): Result<DishDto> {
        return try {
            val created = postgrest["dishes"].insert(dto) { select() }.decodeSingle<DishDto>()
            Result.success(created)
        } catch (e: Exception) {
            if (isMissingColumnError(e, "restaurant_name")) {
                try {
                    val minimalDto = DishDto(
                        id = dto.id,
                        name = dto.name,
                        restaurantId = dto.restaurantId,
                        imageUrl = dto.imageUrl,
                        restaurantName = null
                    )
                    val created = postgrest["dishes"].insert(minimalDto) { select() }.decodeSingle<DishDto>()
                    Result.success(created)
                } catch (e2: Exception) {
                    Result.failure(e2)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Back-fill optional fields on an existing dish row.
     *
     * Attempts to update both image_url and restaurant_name together.
     * If restaurant_name column is missing, falls back to image_url only.
     * All failures are silently swallowed — these are non-critical denormalization hints.
     */
    suspend fun backfillDishFields(
        dishId: String,
        imageUrl: String? = null,
        restaurantName: String? = null
    ) {
        if (imageUrl == null && restaurantName == null) return
        try {
            postgrest["dishes"].update({
                imageUrl?.let { set("image_url", it) }
                restaurantName?.let { set("restaurant_name", it) }
            }) {
                filter { eq("id", dishId) }
            }
        } catch (_: Exception) {
            if (restaurantName != null) {
                imageUrl?.let {
                    try {
                        postgrest["dishes"].update({ set("image_url", it) }) {
                            filter { eq("id", dishId) }
                        }
                    } catch (_: Exception) { /* swallow */ }
                }
            }
        }
    }

    /**
     * Insert or update a restaurant with schema-safe image handling.
     *
     * If the restaurant already exists but has no images, back-fills
     * the image_urls column. If the column is missing, the update is
     * silently skipped.
     */
    suspend fun ensureRestaurantImage(restaurantId: String, imageUrl: String) {
        try {
            postgrest["restaurants"].update({
                set("image_urls", listOf(imageUrl))
            }) {
                filter { eq("id", restaurantId) }
            }
        } catch (_: Exception) { /* swallow — image_urls column may not exist */ }
    }

    /**
     * Insert a new restaurant row.
     */
    suspend fun insertRestaurant(dto: RestaurantDto): Result<RestaurantDto> {
        return try {
            val created = postgrest["restaurants"].insert(dto) { select() }.decodeSingle<RestaurantDto>()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if an exception indicates a missing column on the server.
     */
    fun isMissingColumnError(e: Exception, columnName: String): Boolean {
        val message = e.message ?: return false
        return message.contains(columnName, ignoreCase = true) ||
            message.contains("column", ignoreCase = true) ||
            message.contains("42703") // PostgreSQL undefined_column
    }

    /**
     * Resolve a stable restaurant name from a DTO, using fallbacks
     * when the denormalized restaurant_name field is null.
     */
    fun resolveRestaurantName(
        dish: DishDto,
        providedName: String? = null
    ): String =
        dish.restaurantName
            ?: providedName
            ?: ""

    /**
     * Resolve stable image URLs from a restaurant DTO.
     */
    fun resolveRestaurantImages(restaurant: RestaurantDto): List<String> =
        restaurant.imageUrls ?: restaurant.photoUrls ?: emptyList()
}
