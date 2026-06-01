package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ImageDelivery
import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.GroupedFeedDish
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class FeedReadRepository {

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
            val rows = ApiClient.get<List<FeedPageRow>>(
                "social/feed-page",
                mapOf(
                    "filter" to filter.name,
                    "limit" to limit.toString(),
                    "cursorCreatedAt" to cursorCreatedAt,
                    "cursorId" to cursorId,
                    "cursorRating" to cursorRating?.toString(),
                    "userLat" to userLat?.toString(),
                    "userLon" to userLon?.toString(),
                    "userCity" to userCity,
                    "radiusKm" to radiusKm.toString()
                )
            )

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

        val grouped = groupedDishes.map {
            GroupedFeedDish(
                dishId = it.dishId,
                dishName = it.dishName,
                imageUrl = ImageDelivery.feed(it.imageUrl),
                price = it.price,
                currencyCode = it.currencyCode,
                ratingId = it.ratingId
            )
        }

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
            price = price,
            currencyCode = currencyCode,
            isGrouped = isGrouped,
            groupId = groupId,
            groupedDishes = grouped
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
    val price: Double? = null,
    @SerialName("currency_code")
    val currencyCode: String? = null,
    @SerialName("is_grouped")
    val isGrouped: Boolean = false,
    @SerialName("group_id")
    val groupId: String? = null,
    @SerialName("grouped_dishes")
    val groupedDishes: List<GroupedFeedDishRow> = emptyList()
)

@Serializable
private data class GroupedFeedDishRow(
    @SerialName("dish_id")
    val dishId: String,
    @SerialName("dish_name")
    val dishName: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val price: Double? = null,
    @SerialName("currency_code")
    val currencyCode: String? = null,
    @SerialName("rating_id")
    val ratingId: String? = null
)
