package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.DishDto
import com.example.smackcheck2.data.dto.RestaurantDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DishInsertRow(
    val id: String? = null,
    val name: String,
    @SerialName("restaurant_id")
    val restaurantId: String,
    @SerialName("image_url")
    val imageUrl: String? = null
)

internal fun DishDto.toDishInsertRow(): DishInsertRow =
    DishInsertRow(
        id = id,
        name = name,
        restaurantId = restaurantId,
        imageUrl = imageUrl
    )

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

    private val postgrest
        get() = client.postgrest

    /**
     * Insert a dish using the stable live schema.
     *
     * Restaurant names are resolved through the restaurants table. Sending
     * denormalized dish columns from older schemas causes PostgREST PGRST204
     * errors before the real insert can run.
     */
    suspend fun insertDish(dto: DishDto): Result<DishDto> {
        return try {
            val created = postgrest["dishes"].insert(dto.toDishInsertRow()) { select() }.decodeSingle<DishDto>()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Back-fill optional fields on an existing dish row.
     *
     * Updates the dish image when a later submission supplies one.
     * Restaurant names are intentionally not written to dishes; they belong
     * to the referenced restaurant row.
     */
    suspend fun backfillDishFields(
        dishId: String,
        imageUrl: String? = null,
        restaurantName: String? = null
    ) {
        if (imageUrl == null) return
        try {
            postgrest["dishes"].update({
                set("image_url", imageUrl)
            }) {
                filter { eq("id", dishId) }
            }
        } catch (_: Exception) { /* swallow - this is a non-critical denormalization hint */ }
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
