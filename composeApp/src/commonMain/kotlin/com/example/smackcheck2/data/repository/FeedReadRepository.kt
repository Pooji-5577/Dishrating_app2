package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ImageDelivery
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.FeedItem
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class FeedReadRepository {

    private val postgrest
        get() = SupabaseClientProvider.client.postgrest

    suspend fun getFeedPage(
        filter: FeedFilter,
        limit: Int,
        cursorCreatedAt: String? = null,
        cursorId: String? = null,
        cursorRating: Double? = null,
        userLat: Double? = null,
        userLon: Double? = null,
        userCity: String? = null,
        radiusKm: Double = 25.0,
        currentUserId: String? = null
    ): Result<List<FeedItem>> {
        return try {
            val rows = postgrest.rpc(
                function = "get_feed_page",
                parameters = FeedPageParams(
                    p_filter = filter.name,
                    p_limit = limit,
                    p_cursor_created_at = cursorCreatedAt,
                    p_cursor_id = cursorId,
                    p_cursor_rating = cursorRating,
                    p_user_lat = userLat,
                    p_user_lon = userLon,
                    p_user_city = userCity,
                    p_radius_km = radiusKm,
                    p_current_user_id = currentUserId
                )
            ).decodeList<FeedPageRow>()

            Result.success(rows.map { it.toFeedItem() })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FeedPageRow.toFeedItem(): FeedItem {
        val images = imageUrls
            .filter { it.isNotBlank() }
            .distinct()

        return FeedItem(
            id = id,
            userId = userId,
            userProfileImageUrl = ImageDelivery.avatar(userProfileImageUrl),
            userName = userName,
            dishImageUrl = ImageDelivery.feed(dishImageUrl ?: images.firstOrNull()),
            dishName = dishName,
            dishId = dishId,
            restaurantName = restaurantName,
            restaurantCity = restaurantCity.orEmpty(),
            rating = rating.toFloat(),
            likesCount = likesCount,
            commentsCount = commentsCount,
            isLiked = isLiked,
            timestamp = parseTimestamp(createdAt),
            comment = comment,
            imageUrls = images.mapNotNull { ImageDelivery.feed(it) },
            price = price
        )
    }

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return 0L
        return try {
            Instant.parse(timestamp).toEpochMilliseconds()
        } catch (_: Exception) {
            0L
        }
    }
}

@Serializable
private data class FeedPageParams(
    val p_filter: String,
    val p_limit: Int,
    val p_cursor_created_at: String? = null,
    val p_cursor_id: String? = null,
    val p_cursor_rating: Double? = null,
    val p_user_lat: Double? = null,
    val p_user_lon: Double? = null,
    val p_user_city: String? = null,
    val p_radius_km: Double = 25.0,
    val p_current_user_id: String? = null
)

@Serializable
private data class FeedPageRow(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_profile_image_url")
    val userProfileImageUrl: String? = null,
    @SerialName("user_name")
    val userName: String,
    @SerialName("dish_image_url")
    val dishImageUrl: String? = null,
    @SerialName("dish_name")
    val dishName: String,
    @SerialName("dish_id")
    val dishId: String,
    @SerialName("restaurant_name")
    val restaurantName: String,
    @SerialName("restaurant_city")
    val restaurantCity: String? = null,
    val rating: Double,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    @SerialName("is_liked")
    val isLiked: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    val comment: String = "",
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    val price: Double? = null
)
