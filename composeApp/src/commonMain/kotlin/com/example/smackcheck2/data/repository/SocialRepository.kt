package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SocialRepository(
    private val feedAssembler: FeedAssembler = FeedAssembler()
) {

    private val client = SupabaseClientProvider.client
    private val postgrest = client.postgrest

    // ==================== FOLLOW / UNFOLLOW ====================

    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val dto = FollowerDto(
                followerId = currentUserId,
                followingId = targetUserId
            )
            postgrest["followers"].insert(dto)

            // Create notification for the target user
            createNotification(
                userId = targetUserId,
                type = "follow",
                title = "New Follower",
                body = "Someone started following you.",
                data = """{"follower_id": "$currentUserId"}"""
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            postgrest["followers"].delete {
                filter {
                    eq("follower_id", currentUserId)
                    eq("following_id", targetUserId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val existing = postgrest["followers"]
                .select {
                    filter {
                        eq("follower_id", currentUserId)
                        eq("following_id", targetUserId)
                    }
                }
                .decodeSingleOrNull<FollowerDto>()
            existing != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowers(userId: String): Result<List<UserSummary>> {
        return try {
            val followers = postgrest["followers"]
                .select {
                    filter {
                        eq("following_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<FollowerDto>()

            val users = followers.mapNotNull { follower ->
                try {
                    val profile = postgrest["profiles"]
                        .select {
                            filter { eq("id", follower.followerId) }
                        }
                        .decodeSingleOrNull<ProfileDto>()

                    profile?.let {
                        UserSummary(
                            id = it.id,
                            name = it.name,
                            profilePhotoUrl = it.profilePhotoUrl
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowing(userId: String): Result<List<UserSummary>> {
        return try {
            val following = postgrest["followers"]
                .select {
                    filter {
                        eq("follower_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<FollowerDto>()

            val users = following.mapNotNull { follow ->
                try {
                    val profile = postgrest["profiles"]
                        .select {
                            filter { eq("id", follow.followingId) }
                        }
                        .decodeSingleOrNull<ProfileDto>()

                    profile?.let {
                        UserSummary(
                            id = it.id,
                            name = it.name,
                            profilePhotoUrl = it.profilePhotoUrl,
                            isFollowing = true
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowingFeed(
        userId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<FeedItem>> {
        return try {
            // Get list of users the current user follows
            val following = postgrest["followers"]
                .select {
                    filter { eq("follower_id", userId) }
                }
                .decodeList<FollowerDto>()

            val followingIds = following.map { it.followingId }
            if (followingIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Get ratings from followed users
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        isIn("user_id", followingIds)
                    }
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = feedAssembler.mapRatingsToFeedItems(ratings, userId)
            Result.success(feedItems)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeedAll(
        limit: Int = 20,
        offset: Int = 0,
        currentUserId: String? = null
    ): Result<List<FeedItem>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = feedAssembler.mapRatingsToFeedItems(ratings, currentUserId)
            Result.success(feedItems)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHighestRatedFeed(
        limit: Int = 20,
        offset: Int = 0,
        currentUserId: String? = null,
        userLat: Double? = null,
        userLon: Double? = null,
        radiusKm: Double = 25.0
    ): Result<List<FeedItem>> {
        return try {
            // If location is available, scope to nearby restaurants first
            val nearbyRestaurantIds: List<String>? = if (userLat != null && userLon != null) {
                try {
                    val restaurants = postgrest["restaurants"]
                        .select()
                        .decodeList<RestaurantDto>()
                    restaurants
                        .filter { r ->
                            val lat = r.latitude ?: return@filter false
                            val lon = r.longitude ?: return@filter false
                            haversineDistanceKm(userLat, userLon, lat, lon) <= radiusKm
                        }
                        .mapNotNull { it.id }
                        .takeIf { it.isNotEmpty() }
                } catch (_: Exception) { null }
            } else null

            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        gte("rating", 4.0)
                        lte("rating", 5.0)
                        if (nearbyRestaurantIds != null) {
                            isIn("restaurant_id", nearbyRestaurantIds)
                        }
                    }
                    order("rating", Order.DESCENDING)
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<RatingDto>()
            val feedItems = feedAssembler.mapRatingsToFeedItems(ratings, currentUserId)
            Result.success(feedItems)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNearbyFeed(
        userLat: Double,
        userLon: Double,
        radiusKm: Double = 25.0,
        limit: Int = 20,
        offset: Int = 0,
        currentUserId: String? = null,
        userCity: String? = null
    ): Result<List<FeedItem>> {
        return try {
            // A rating is "nearby" if:
            //   (a) its own lat/lon, OR the lat/lon of its restaurant, is
            //       within radiusKm of the user (haversine), OR
            //   (b) its restaurant is in the same city as the user
            //       (fallback for rows lacking coords).
            // Ratings with no usable coords AND no matching city are excluded.
            val fetchSize = (limit * 5).coerceAtLeast(limit)
            val candidateRatings = postgrest["ratings"]
                .select {
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + fetchSize - 1).toLong())
                }
                .decodeList<RatingDto>()

            println("SocialRepository[Nearby]: user=($userLat,$userLon) city=$userCity radius=${radiusKm}km candidates=${candidateRatings.size}")
            if (candidateRatings.isEmpty()) return Result.success(emptyList())

            val restaurantIds = candidateRatings
                .map { it.restaurantId }
                .distinct()
                .filter { it.isNotBlank() }

            val restaurantsById: Map<String, RestaurantDto> = try {
                if (restaurantIds.isNotEmpty()) {
                    postgrest["restaurants"]
                        .select { filter { isIn("id", restaurantIds) } }
                        .decodeList<RestaurantDto>()
                        .mapNotNull { r -> r.id?.let { it to r } }
                        .toMap()
                } else emptyMap()
            } catch (_: Exception) { emptyMap() }

            val normalizedUserCity = userCity?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

            var matchedByCoords = 0
            var matchedByCity = 0
            var skippedNoData = 0
            val nearbyRatings = candidateRatings.filter { rating ->
                val restaurant = restaurantsById[rating.restaurantId]
                val lat = rating.latitude ?: restaurant?.latitude
                val lon = rating.longitude ?: restaurant?.longitude
                if (lat != null && lon != null) {
                    val d = haversineDistanceKm(userLat, userLon, lat, lon)
                    if (d <= radiusKm) {
                        matchedByCoords++
                        return@filter true
                    }
                }
                // City fallback: match restaurant city to user city
                val restaurantCity = restaurant?.city?.trim()?.lowercase()
                if (normalizedUserCity != null && restaurantCity != null && restaurantCity == normalizedUserCity) {
                    matchedByCity++
                    return@filter true
                }
                skippedNoData++
                false
            }.take(limit)

            println("SocialRepository[Nearby]: matchedCoords=$matchedByCoords matchedCity=$matchedByCity skipped=$skippedNoData returning=${nearbyRatings.size}")
            if (nearbyRatings.isEmpty()) return Result.success(emptyList())

            val feedItems = feedAssembler.mapRatingsToFeedItems(nearbyRatings, currentUserId)
            Result.success(feedItems)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2).let { it * it }
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }

    suspend fun getTrendingFeed(
        limit: Int = 20,
        offset: Int = 0,
        currentUserId: String? = null
    ): Result<List<FeedItem>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    order("likes_count", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = feedAssembler.mapRatingsToFeedItems(ratings, currentUserId)
            Result.success(feedItems)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DISCOVER USERS ====================

    /**
     * Fetch all profiles (minus the current user) annotated with isFollowing.
     * Used by the "Find Friends" screen.
     */
    suspend fun getDiscoverableUsers(currentUserId: String?): Result<List<UserSummary>> {
        return try {
            val profiles = postgrest["profiles"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ProfileDto>()

            val followingIds: Set<String> = if (!currentUserId.isNullOrBlank()) {
                try {
                    postgrest["followers"]
                        .select { filter { eq("follower_id", currentUserId) } }
                        .decodeList<FollowerDto>()
                        .map { it.followingId }
                        .toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }

            val users = profiles
                .filter { it.id != currentUserId }
                .distinctBy { it.id }
                .map { profile ->
                    UserSummary(
                        id = profile.id,
                        name = profile.name.ifBlank { profile.username ?: "User" },
                        username = profile.username,
                        profilePhotoUrl = profile.profilePhotoUrl,
                        bio = profile.bio,
                        location = profile.lastLocation,
                        isFollowing = followingIds.contains(profile.id)
                    )
                }
            Result.success(users)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== COMMENTS ====================

    suspend fun addComment(
        ratingId: String,
        userId: String,
        content: String,
        parentCommentId: String? = null
    ): Result<Comment> {
        return try {
            val dto = CommentDto(
                ratingId = ratingId,
                userId = userId,
                content = content,
                parentCommentId = parentCommentId
            )
            val created = postgrest["comments"]
                .insert(dto) { select() }
                .decodeSingle<CommentDto>()

            val profile = postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()

            // Notification is handled by CommentsViewModel via NotificationRepository

            Result.success(
                Comment(
                    id = created.id ?: "",
                    ratingId = ratingId,
                    userId = userId,
                    userName = profile?.name ?: "Unknown",
                    userProfileUrl = profile?.profilePhotoUrl,
                    parentCommentId = parentCommentId,
                    content = content,
                    createdAt = parseTimestamp(created.createdAt)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsForRating(ratingId: String): Result<List<Comment>> {
        return try {
            val commentDtos = postgrest["comments"]
                .select {
                    filter { eq("rating_id", ratingId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<CommentDto>()

            // Fetch profiles for all comment authors
            val userIds = commentDtos.map { it.userId }.distinct()
            val profiles = mutableMapOf<String, ProfileDto>()
            for (uid in userIds) {
                try {
                    val profile = postgrest["profiles"]
                        .select { filter { eq("id", uid) } }
                        .decodeSingleOrNull<ProfileDto>()
                    if (profile != null) profiles[uid] = profile
                } catch (_: Exception) {}
            }

            // Map to domain models
            val allComments = commentDtos.map { dto ->
                Comment(
                    id = dto.id ?: "",
                    ratingId = dto.ratingId,
                    userId = dto.userId,
                    userName = profiles[dto.userId]?.name ?: "Unknown",
                    userProfileUrl = profiles[dto.userId]?.profilePhotoUrl,
                    parentCommentId = dto.parentCommentId,
                    content = dto.content,
                    createdAt = parseTimestamp(dto.createdAt)
                )
            }

            // Build nested comment tree
            val topLevel = allComments.filter { it.parentCommentId == null }
            val byParent = allComments.filter { it.parentCommentId != null }
                .groupBy { it.parentCommentId }

            val nested = topLevel.map { comment ->
                comment.copy(replies = byParent[comment.id] ?: emptyList())
            }

            Result.success(nested)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsCount(ratingId: String): Int {
        return try {
            val comments = postgrest["comments"]
                .select {
                    filter { eq("rating_id", ratingId) }
                }
                .decodeList<CommentDto>()
            comments.size
        } catch (e: Exception) {
            0
        }
    }

    suspend fun deleteComment(commentId: String, userId: String): Result<Unit> {
        return try {
            postgrest["comments"].delete {
                filter {
                    eq("id", commentId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== NOTIFICATIONS ====================

    suspend fun createNotification(
        userId: String,
        type: String,
        title: String,
        body: String,
        data: String = "{}"
    ): Result<Unit> {
        return try {
            val jsonData = kotlinx.serialization.json.Json.parseToJsonElement(data)
            val dto = NotificationDto(
                userId = userId,
                type = type,
                title = title,
                body = body,
                data = jsonData
            )
            postgrest["notifications"].insert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(userId: String, limit: Int = 50): Result<List<Notification>> {
        return try {
            val dtos = postgrest["notifications"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<NotificationDto>()

            val notifications = dtos.map { dto ->
                Notification(
                    id = dto.id ?: "",
                    type = dto.type,
                    title = dto.title,
                    body = dto.body,
                    isRead = dto.isRead,
                    createdAt = parseTimestamp(dto.createdAt)
                )
            }
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            postgrest["notifications"]
                .update(mapOf("is_read" to true)) {
                    filter { eq("id", notificationId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            postgrest["notifications"]
                .update(mapOf("is_read" to true)) {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadNotificationCount(userId: String): Int {
        return try {
            val dtos = postgrest["notifications"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
                .decodeList<NotificationDto>()
            dtos.size
        } catch (e: Exception) {
            0
        }
    }

    // ==================== STORIES ====================

    suspend fun uploadStory(userId: String, imageUrl: String): Result<Story> {
        return try {
            val profile = postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()

            val now = Clock.System.now()
            val expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + 24 * 60 * 60 * 1000)

            val storyDto = StoryDto(
                userId = userId,
                imageUrl = imageUrl,
                expiresAt = expiresAt.toString()
            )

            val created = postgrest["stories"]
                .insert(storyDto) { select() }
                .decodeSingleOrNull<StoryDto>()
                ?: throw Exception("Failed to create story")

            Result.success(
                Story(
                    id = created.id ?: "",
                    userId = created.userId,
                    userName = profile?.name ?: "Unknown",
                    userProfileUrl = profile?.profilePhotoUrl,
                    imageUrl = created.imageUrl,
                    createdAt = parseTimestamp(created.createdAt),
                    expiresAt = parseTimestamp(created.expiresAt)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStories(): Result<List<Story>> {
        return try {
            val now = Clock.System.now().toString()
            val storyDtos = postgrest["stories"]
                .select {
                    filter {
                        gt("expires_at", now)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<StoryDto>()

            val userIds = storyDtos.map { it.userId }.distinct()
            val profiles = mutableMapOf<String, ProfileDto>()
            for (uid in userIds) {
                try {
                    val profile = postgrest["profiles"]
                        .select { filter { eq("id", uid) } }
                        .decodeSingleOrNull<ProfileDto>()
                    if (profile != null) profiles[uid] = profile
                } catch (_: Exception) {}
            }

            val stories = storyDtos.map { dto ->
                Story(
                    id = dto.id ?: "",
                    userId = dto.userId,
                    userName = profiles[dto.userId]?.name ?: "Unknown",
                    userProfileUrl = profiles[dto.userId]?.profilePhotoUrl,
                    imageUrl = dto.imageUrl,
                    createdAt = parseTimestamp(dto.createdAt),
                    expiresAt = parseTimestamp(dto.expiresAt)
                )
            }
            Result.success(stories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String, userId: String): Result<Unit> {
        return try {
            postgrest["stories"].delete {
                filter {
                    eq("id", storyId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== USER PROFILE (OTHER USER) ====================

    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val profile = postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()
                ?: return Result.failure(Exception("User not found"))

            Result.success(
                User(
                    id = profile.id,
                    name = profile.name,
                    email = profile.email,
                    profilePhotoUrl = profile.profilePhotoUrl,
                    level = profile.level,
                    xp = profile.xp,
                    streakCount = profile.streakCount,
                    bio = profile.bio,
                    followersCount = profile.followersCount,
                    followingCount = profile.followingCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRatings(
        userId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<FeedItem>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = feedAssembler.mapRatingsToFeedItems(ratings, userId)
            Result.success(feedItems)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== STORIES ====================

    suspend fun uploadStory(userId: String, imageUrl: String): Result<Unit> {
        return try {
            val dto = StoryDto(
                userId = userId,
                imageUrl = imageUrl
            )
            postgrest["stories"].insert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String, userId: String): Result<Unit> {
        return try {
            postgrest["stories"].delete {
                filter {
                    eq("id", storyId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStories(): Result<List<Story>> {
        return try {
            val dtos = postgrest["stories"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<StoryDto>()

            val stories = dtos.map { dto ->
                Story(
                    id = dto.id ?: "",
                    userId = dto.userId,
                    imageUrl = dto.imageUrl,
                    createdAt = parseTimestamp(dto.createdAt)
                )
            }
            Result.success(stories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return 0L
        return try {
            Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }
}
