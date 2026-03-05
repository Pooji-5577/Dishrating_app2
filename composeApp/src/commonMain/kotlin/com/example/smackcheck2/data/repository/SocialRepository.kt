package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Instant

class SocialRepository {

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
                body = "Someone started following you!",
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
                    limit(limit.toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = mapRatingsToFeedItems(ratings, userId)
            Result.success(feedItems)
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
                    limit(limit.toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = mapRatingsToFeedItems(ratings, currentUserId)
            Result.success(feedItems)
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

            // Create notification for rating owner
            val rating = postgrest["ratings"]
                .select { filter { eq("id", ratingId) } }
                .decodeSingleOrNull<RatingDto>()

            if (rating != null && rating.userId != userId) {
                createNotification(
                    userId = rating.userId,
                    type = "comment",
                    title = "New Comment",
                    body = "${profile?.name ?: "Someone"} commented on your rating",
                    data = """{"rating_id": "$ratingId", "comment_id": "${created.id}"}"""
                )
            }

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
            val dto = NotificationDto(
                userId = userId,
                type = type,
                title = title,
                body = body,
                data = data
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

    suspend fun getUserRatings(userId: String, limit: Int = 20): Result<List<FeedItem>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = mapRatingsToFeedItems(ratings, null)
            Result.success(feedItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private suspend fun mapRatingsToFeedItems(
        ratings: List<RatingDto>,
        currentUserId: String?
    ): List<FeedItem> {
        return ratings.mapNotNull { rating ->
            try {
                val profile = postgrest["profiles"]
                    .select { filter { eq("id", rating.userId) } }
                    .decodeSingleOrNull<ProfileDto>()

                val dish = postgrest["dishes"]
                    .select { filter { eq("id", rating.dishId) } }
                    .decodeSingleOrNull<DishDto>()

                val restaurant = postgrest["restaurants"]
                    .select { filter { eq("id", rating.restaurantId) } }
                    .decodeSingleOrNull<RestaurantDto>()

                val commentsCount = getCommentsCount(rating.id ?: "")

                val isLiked = if (currentUserId != null && rating.id != null) {
                    try {
                        val existing = postgrest["likes"]
                            .select {
                                filter {
                                    eq("user_id", currentUserId)
                                    eq("rating_id", rating.id)
                                }
                            }
                            .decodeSingleOrNull<LikeDto>()
                        existing != null
                    } catch (_: Exception) { false }
                } else false

                // Fetch additional images
                val additionalImages = try {
                    postgrest["rating_images"]
                        .select {
                            filter { eq("rating_id", rating.id ?: "") }
                            order("sort_order", Order.ASCENDING)
                        }
                        .decodeList<RatingImageDto>()
                        .map { it.imageUrl }
                } catch (_: Exception) { emptyList() }

                val allImages = buildList {
                    rating.imageUrl?.let { add(it) }
                    dish?.imageUrl?.let { if (rating.imageUrl == null) add(it) }
                    addAll(additionalImages)
                }

                FeedItem(
                    id = rating.id ?: return@mapNotNull null,
                    userId = rating.userId,
                    userProfileImageUrl = profile?.profilePhotoUrl,
                    userName = profile?.name ?: "Unknown",
                    dishImageUrl = allImages.firstOrNull(),
                    dishName = dish?.name ?: "Unknown Dish",
                    restaurantName = restaurant?.name ?: "Unknown Restaurant",
                    rating = rating.rating,
                    likesCount = rating.likesCount,
                    commentsCount = commentsCount,
                    isLiked = isLiked,
                    timestamp = parseTimestamp(rating.createdAt),
                    comment = rating.comment,
                    imageUrls = allImages
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return 0L
        return try {
            Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }
}
