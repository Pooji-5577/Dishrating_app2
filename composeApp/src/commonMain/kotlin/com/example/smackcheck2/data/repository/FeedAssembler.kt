package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.ImageDelivery
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.FeedItem
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant

/**
 * Single authoritative feed assembler that maps raw ratings into FeedItems.
 *
 * Both [SocialRepository] and [RealtimeFeedRepository] delegate to this
 * class instead of maintaining their own mapping logic. This ensures:
 * - One source of truth for feed shape and fallback behavior
 * - Consistent batch-fetching strategy (no N+1 queries)
 * - Easier testing and evolution of feed policy
 */
class FeedAssembler(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client
) {

    private val postgrest
        get() = client.postgrest

    /**
     * Map a batch of ratings into FeedItems using bulk queries.
     * All related data (profiles, dishes, restaurants, comments, likes, images)
     * is fetched in 6 bulk queries instead of N*6 individual ones.
     */
    suspend fun mapRatingsToFeedItems(
        ratings: List<RatingDto>,
        currentUserId: String?
    ): List<FeedItem> {
        if (ratings.isEmpty()) return emptyList()

        val userIds = ratings.map { it.userId }.distinct()
        val dishIds = ratings.map { it.dishId }.distinct()
        val restaurantIds = ratings.map { it.restaurantId }.distinct()
        val ratingIds = ratings.mapNotNull { it.id }

        val feedData = coroutineScope {
            val profilesDeferred = async { batchFetchProfiles(userIds) }
            val dishesDeferred = async { batchFetchDishes(dishIds) }
            val commentsDeferred = async { batchFetchCommentsCount(ratingIds) }
            val likesDeferred = async { batchFetchLikedRatingIds(currentUserId, ratingIds) }
            val additionalImagesDeferred = async { batchFetchAdditionalImages(ratingIds) }

            val profilesMap = profilesDeferred.await()
            val dishesMap = dishesDeferred.await()

            val dishRestaurantIds = dishesMap.values.map { it.restaurantId }.distinct()
            val validRestaurantIds = (restaurantIds + dishRestaurantIds)
                .distinct()
                .filter { it.isNotBlank() }
            val restaurantsMap = batchFetchRestaurants(validRestaurantIds)
            val missingRestaurantIds = validRestaurantIds.filter { it !in restaurantsMap }
            val fallbackRestaurantNames = batchFetchFallbackRestaurantNames(missingRestaurantIds)

            FeedData(
                profilesMap = profilesMap,
                dishesMap = dishesMap,
                restaurantsMap = restaurantsMap,
                commentsCountMap = commentsDeferred.await(),
                likedRatingIds = likesDeferred.await(),
                additionalImagesMap = additionalImagesDeferred.await(),
                fallbackRestaurantNames = fallbackRestaurantNames
            )
        }

        return ratings.mapNotNull { rating ->
            val ratingId = rating.id ?: return@mapNotNull null
            val profile = feedData.profilesMap[rating.userId]
            val dish = feedData.dishesMap[rating.dishId]
            val restaurant = feedData.restaurantsMap[rating.restaurantId]
            val dishRestaurant = if (restaurant == null && dish != null) feedData.restaurantsMap[dish.restaurantId] else null

            val restaurantName = resolveRestaurantName(
                restaurant, dishRestaurant, dish,
                rating.restaurantId, feedData.fallbackRestaurantNames
            )

            val additionalImages = feedData.additionalImagesMap[ratingId] ?: emptyList()
            val allImages = buildImageList(rating.imageUrl, dish?.imageUrl, additionalImages)

            FeedItem(
                id = ratingId,
                userId = rating.userId,
                userProfileImageUrl = ImageDelivery.avatar(profile?.profilePhotoUrl),
                userName = profile?.name ?: "Unknown",
                dishImageUrl = ImageDelivery.feed(allImages.firstOrNull()),
                dishName = dish?.name ?: "Unknown Dish",
                dishId = rating.dishId,
                restaurantName = restaurantName,
                restaurantCity = (restaurant ?: dishRestaurant)?.city ?: "",
                rating = rating.rating,
                likesCount = rating.likesCount,
                commentsCount = feedData.commentsCountMap[ratingId] ?: 0,
                isLiked = feedData.likedRatingIds.contains(ratingId),
                timestamp = parseTimestamp(rating.createdAt),
                comment = rating.comment,
                imageUrls = allImages.mapNotNull { ImageDelivery.feed(it) },
                price = rating.price
            )
        }
    }

    private suspend fun batchFetchProfiles(userIds: List<String>): Map<String, ProfileDto> =
        try {
            if (userIds.isEmpty()) emptyMap()
            else postgrest["profiles"]
                .select { filter { isIn("id", userIds) } }
                .decodeList<ProfileDto>()
                .associateBy { it.id }
        } catch (_: Exception) { emptyMap() }

    private suspend fun batchFetchDishes(dishIds: List<String>): Map<String, DishDto> =
        try {
            if (dishIds.isEmpty()) emptyMap()
            else postgrest["dishes"]
                .select { filter { isIn("id", dishIds) } }
                .decodeList<DishDto>()
                .associateBy { it.id ?: "" }
        } catch (_: Exception) { emptyMap() }

    private suspend fun batchFetchRestaurants(restaurantIds: List<String>): Map<String, RestaurantDto> {
        if (restaurantIds.isEmpty()) return emptyMap()
        return try {
            val results = postgrest["restaurants"]
                .select { filter { isIn("id", restaurantIds) } }
                .decodeList<RestaurantDto>()
            results.associateBy { it.id ?: "" }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun batchFetchCommentsCount(ratingIds: List<String>): Map<String, Int> =
        try {
            if (ratingIds.isEmpty()) emptyMap()
            else postgrest["comments"]
                .select { filter { isIn("rating_id", ratingIds) } }
                .decodeList<CommentDto>()
                .groupBy { it.ratingId }
                .mapValues { it.value.size }
        } catch (_: Exception) { emptyMap() }

    private suspend fun batchFetchLikedRatingIds(
        currentUserId: String?,
        ratingIds: List<String>
    ): Set<String> {
        if (currentUserId == null || ratingIds.isEmpty()) return emptySet()
        return try {
            postgrest["likes"]
                .select {
                    filter {
                        eq("user_id", currentUserId)
                        isIn("rating_id", ratingIds)
                    }
                }
                .decodeList<LikeDto>()
                .map { it.ratingId }
                .toSet()
        } catch (_: Exception) { emptySet() }
    }

    private suspend fun batchFetchAdditionalImages(ratingIds: List<String>): Map<String, List<String>> =
        try {
            if (ratingIds.isEmpty()) emptyMap()
            else postgrest["rating_images"]
                .select {
                    filter { isIn("rating_id", ratingIds) }
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<RatingImageDto>()
                .groupBy { it.ratingId }
                .mapValues { entry -> entry.value.map { it.imageUrl } }
        } catch (_: Exception) { emptyMap() }

    private suspend fun batchFetchFallbackRestaurantNames(
        missingIds: List<String>
    ): Map<String, String> {
        val names = mutableMapOf<String, String>()
        for (missingId in missingIds.filter { it.isNotBlank() }) {
            try {
                val dto = postgrest["restaurants"]
                    .select { filter { eq("id", missingId) } }
                    .decodeSingleOrNull<RestaurantDto>()
                if (dto != null) {
                    names[missingId] = dto.name
                    dto.id?.let { names[it] = dto.name }
                }
            } catch (e: Exception) {
                // Skip individual failures
            }
        }
        return names
    }

    private fun resolveRestaurantName(
        restaurant: RestaurantDto?,
        dishRestaurant: RestaurantDto?,
        dish: DishDto?,
        ratingRestaurantId: String,
        fallbackNames: Map<String, String>
    ): String =
        restaurant?.name
            ?: dishRestaurant?.name
            ?: dish?.restaurantName
            ?: fallbackNames[ratingRestaurantId]
            ?: fallbackNames[dish?.restaurantId ?: ""]
            ?: "Unknown Restaurant"

    private fun buildImageList(
        ratingImageUrl: String?,
        dishImageUrl: String?,
        additionalImages: List<String>
    ): List<String> = buildList {
        ratingImageUrl?.let { add(it) }
        dishImageUrl?.let { if (ratingImageUrl == null) add(it) }
        addAll(additionalImages)
    }

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return 0L
        return try {
            Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }

    private data class FeedData(
        val profilesMap: Map<String, ProfileDto>,
        val dishesMap: Map<String, DishDto>,
        val restaurantsMap: Map<String, RestaurantDto>,
        val commentsCountMap: Map<String, Int>,
        val likedRatingIds: Set<String>,
        val additionalImagesMap: Map<String, List<String>>,
        val fallbackRestaurantNames: Map<String, String>
    )
}
