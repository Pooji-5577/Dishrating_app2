package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.*
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import com.example.smackcheck2.util.Logger

class SocialRepository(
    private val feedAssembler: FeedAssembler = FeedAssembler()
) {
    companion object {
        private const val MAX_COMMENT_LENGTH = 500
    }

    private val client get() = SupabaseClientProvider.client

    // ==================== SERIALIZABLE REQUEST / RESPONSE BODIES ====================

    @Serializable
    private data class PublicUserProfileDto(
        val id: String,
        val name: String = "",
        val username: String? = null,
        @SerialName("profile_photo_url")
        val profilePhotoUrl: String? = null,
        val bio: String? = null,
        val location: String? = null
    )

    @Serializable
    private data class FollowRequest(
        @SerialName("target_user_id") val targetUserId: String
    )

    @Serializable
    private data class NotificationRequest(
        @SerialName("user_id") val userId: String,
        @SerialName("event_type") val eventType: String,
        val title: String,
        val body: String,
        val data: String = "{}"
    )

    @Serializable
    private data class CommentRequest(
        @SerialName("rating_id") val ratingId: String,
        val content: String,
        @SerialName("parent_comment_id") val parentCommentId: String? = null
    )

    @Serializable
    private data class StoryRequest(
        @SerialName("image_url") val imageUrl: String,
        @SerialName("dish_name") val dishName: String? = null,
        val rating: Float? = null,
        val city: String? = null
    )

    @Serializable
    private data class ReportRequest(
        @SerialName("target_type") val targetType: String,
        @SerialName("target_id") val targetId: String,
        val reason: String,
        val details: String? = null
    )

    @Serializable
    private data class BlockRequest(
        @SerialName("blocked_user_id") val blockedUserId: String,
        val reason: String? = null
    )

    @Serializable
    private data class ReadAllRequest(
        @SerialName("mark_all") val markAll: Boolean = true
    )

    // API response wrappers
    @Serializable
    private data class IsFollowingResponse(
        @SerialName("is_following") val isFollowing: Boolean = false
    )

    @Serializable
    private data class UnreadCountResponse(
        val count: Int = 0
    )

    @Serializable
    private data class CommentCountResponse(
        val count: Int = 0
    )

    @Serializable
    private data class CommentResponse(
        val id: String = "",
        @SerialName("rating_id") val ratingId: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("parent_comment_id") val parentCommentId: String? = null,
        val content: String = "",
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("user_name") val userName: String? = null,
        @SerialName("user_profile_url") val userProfileUrl: String? = null
    )

    @Serializable
    private data class StoryResponse(
        val id: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("image_url") val imageUrl: String = "",
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("user_name") val userName: String? = null,
        @SerialName("user_profile_url") val userProfileUrl: String? = null,
        @SerialName("dish_name") val dishName: String? = null,
        val rating: Float? = null,
        val city: String? = null
    )

    @Serializable
    private data class NotificationResponse(
        val id: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("event_type") val type: String = "",
        val title: String = "",
        val body: String = "",
        @SerialName("is_read") val isRead: Boolean = false,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    private data class UserProfileResponse(
        val id: String = "",
        val name: String = "",
        val email: String? = null,
        val username: String? = null,
        @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
        val level: Int = 1,
        val xp: Int = 0,
        @SerialName("streak_count") val streakCount: Int = 0,
        val bio: String? = null,
        @SerialName("followers_count") val followersCount: Int = 0,
        @SerialName("following_count") val followingCount: Int = 0
    )

    @Serializable
    private data class FollowerUserResponse(
        val id: String = "",
        val name: String = "",
        @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
        val username: String? = null,
        val bio: String? = null,
        val location: String? = null,
        @SerialName("is_following") val isFollowing: Boolean = false
    )

    @Serializable
    private data class DiscoverUserResponse(
        val id: String = "",
        val name: String = "",
        val username: String? = null,
        @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
        val bio: String? = null,
        val location: String? = null,
        @SerialName("is_following") val isFollowing: Boolean = false
    )

    @Serializable
    private data class FeedItemResponse(
        val id: String = "",
        @SerialName("user_id") val userId: String = "",
        @SerialName("user_name") val userName: String = "",
        @SerialName("user_profile_url") val userProfileUrl: String? = null,
        @SerialName("dish_name") val dishName: String = "",
        @SerialName("dish_id") val dishId: String = "",
        @SerialName("dish_image_url") val dishImageUrl: String? = null,
        @SerialName("restaurant_name") val restaurantName: String = "",
        @SerialName("restaurant_city") val restaurantCity: String = "",
        val rating: Float = 0f,
        @SerialName("likes_count") val likesCount: Int = 0,
        @SerialName("comments_count") val commentsCount: Int = 0,
        @SerialName("is_liked") val isLiked: Boolean = false,
        val comment: String = "",
        @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
        val price: Double? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    private data class FindByUsernameResponse(
        @SerialName("user_id")
        val id: String? = null
    )

    @Serializable
    private data class SearchUserResponse(
        val id: String = "",
        val name: String = "",
        val username: String? = null,
        @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
        val bio: String? = null,
        val location: String? = null
    )

    // ==================== FOLLOW / UNFOLLOW ====================

    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            ApiClient.post<FollowRequest, Unit>(
                "followers",
                FollowRequest(targetUserId = targetUserId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            ApiClient.delete("followers/$targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val response = ApiClient.get<IsFollowingResponse>(
                "followers/is-following",
                mapOf("targetId" to targetUserId)
            )
            response.isFollowing
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowers(userId: String): Result<List<UserSummary>> {
        return try {
            val users = ApiClient.get<List<FollowerUserResponse>>(
                "followers",
                mapOf("userId" to userId)
            )
            Result.success(users.map { u ->
                UserSummary(
                    id = u.id,
                    name = u.name,
                    username = u.username,
                    profilePhotoUrl = u.profilePhotoUrl,
                    bio = u.bio,
                    location = u.location,
                    isFollowing = u.isFollowing
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find a user id by username/handle (supports input with or without @).
     */
    suspend fun findUserIdByUsername(usernameOrHandle: String): Result<String?> {
        return try {
            val normalized = usernameOrHandle.trim().removePrefix("@")
            if (normalized.isBlank()) return Result.success(null)

            val response = ApiClient.get<FindByUsernameResponse>(
                "social/find-by-username",
                mapOf("username" to normalized)
            )
            Result.success(response.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Suggest public profiles by username prefix.
     */
    suspend fun searchUserSuggestions(
        usernameOrHandle: String,
        currentUserId: String? = null,
        limit: Int = 6
    ): Result<List<UserSummary>> {
        return try {
            val normalized = usernameOrHandle
                .trim()
                .removePrefix("@")
                .lowercase()
                .filter { it.isLetterOrDigit() || it == '_' }
                .take(20)

            if (normalized.length < 2) return Result.success(emptyList())

            val users = ApiClient.get<List<SearchUserResponse>>(
                "social/search",
                mapOf("q" to normalized, "limit" to limit.toString())
            )
            Result.success(
                users
                    .filter { it.id != currentUserId }
                    .map { u ->
                        UserSummary(
                            id = u.id,
                            name = u.name,
                            username = u.username,
                            profilePhotoUrl = u.profilePhotoUrl,
                            bio = u.bio,
                            location = u.location
                        )
                    }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowing(userId: String): Result<List<UserSummary>> {
        return try {
            val users = ApiClient.get<List<FollowerUserResponse>>(
                "followers/following",
                mapOf("userId" to userId)
            )
            Result.success(users.map { u ->
                UserSummary(
                    id = u.id,
                    name = u.name,
                    username = u.username,
                    profilePhotoUrl = u.profilePhotoUrl,
                    bio = u.bio,
                    location = u.location,
                    isFollowing = true
                )
            })
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
            val items = ApiClient.get<List<FeedItemResponse>>(
                "social/feed/following",
                mapOf("limit" to limit.toString(), "offset" to offset.toString())
            )
            Result.success(items.map { it.toFeedItem() })
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
            val items = ApiClient.get<List<FeedItemResponse>>(
                "social/discover",
                mapOf("limit" to limit.toString(), "offset" to offset.toString())
            )
            Result.success(items.map { it.toFeedItem() })
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
            val params = mutableMapOf(
                "limit" to limit.toString(),
                "offset" to offset.toString(),
                "sort" to "rating"
            )
            if (userLat != null) params["lat"] = userLat.toString()
            if (userLon != null) params["lon"] = userLon.toString()
            params["radiusKm"] = radiusKm.toString()

            val items = ApiClient.get<List<FeedItemResponse>>("social/discover", params)
            Result.success(items.map { it.toFeedItem() })
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
            val params = mutableMapOf(
                "lat" to userLat.toString(),
                "lon" to userLon.toString(),
                "radiusKm" to radiusKm.toString(),
                "limit" to limit.toString(),
                "offset" to offset.toString(),
                "sort" to "nearby"
            )
            if (userCity != null) params["city"] = userCity

            val items = ApiClient.get<List<FeedItemResponse>>("social/discover", params)
            Logger.d("SocialRepository", "SocialRepository[Nearby]: returning=${items.size}")
            Result.success(items.map { it.toFeedItem() })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendingFeed(
        limit: Int = 20,
        offset: Int = 0,
        currentUserId: String? = null
    ): Result<List<FeedItem>> {
        return try {
            val items = ApiClient.get<List<FeedItemResponse>>(
                "social/discover",
                mapOf("limit" to limit.toString(), "offset" to offset.toString(), "sort" to "trending")
            )
            Result.success(items.map { it.toFeedItem() })
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
            val users = ApiClient.get<List<DiscoverUserResponse>>("social/discover")
            Result.success(
                users
                    .filter { it.id != currentUserId }
                    .map { u ->
                        UserSummary(
                            id = u.id,
                            name = u.name.ifBlank { u.username ?: "User" },
                            username = u.username,
                            profilePhotoUrl = u.profilePhotoUrl,
                            bio = u.bio,
                            location = u.location,
                            isFollowing = u.isFollowing
                        )
                    }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== TRUST / SAFETY ====================

    suspend fun reportContent(
        targetType: String,
        targetId: String,
        reason: String,
        details: String? = null
    ): Result<Unit> {
        return try {
            ApiClient.post<ReportRequest, Unit>(
                "moderation/report",
                ReportRequest(
                    targetType = targetType,
                    targetId = targetId,
                    reason = reason,
                    details = details
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockUser(blockedUserId: String, reason: String? = null): Result<Unit> {
        return try {
            ApiClient.post<BlockRequest, Unit>(
                "moderation/block",
                BlockRequest(blockedUserId = blockedUserId, reason = reason)
            )
            Result.success(Unit)
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
            val authUserId = client.auth.currentUserOrNull()?.id
            if (!authUserId.isNullOrBlank() && authUserId != userId) {
                return Result.failure(Exception("Unauthorized comment operation"))
            }

            val normalizedContent = normalizeCommentContent(content)
                ?: return Result.failure(Exception("Comment must be 1-$MAX_COMMENT_LENGTH characters"))

            val response = ApiClient.post<CommentRequest, CommentResponse>(
                "comments",
                CommentRequest(
                    ratingId = ratingId,
                    content = normalizedContent,
                    parentCommentId = parentCommentId
                )
            )

            Result.success(
                Comment(
                    id = response.id,
                    ratingId = ratingId,
                    userId = userId,
                    userName = response.userName ?: "Unknown",
                    userProfileUrl = response.userProfileUrl,
                    parentCommentId = parentCommentId,
                    content = normalizedContent,
                    createdAt = parseTimestamp(response.createdAt)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCommentAsCurrentUser(
        ratingId: String,
        content: String,
        parentCommentId: String? = null
    ): Result<Comment> {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Please sign in to comment"))
        return addComment(
            ratingId = ratingId,
            userId = userId,
            content = content,
            parentCommentId = parentCommentId
        )
    }

    suspend fun getCommentsForRating(ratingId: String): Result<List<Comment>> {
        return try {
            val responses = ApiClient.get<List<CommentResponse>>(
                "comments",
                mapOf("ratingId" to ratingId)
            )

            val allComments = responses.map { dto ->
                Comment(
                    id = dto.id,
                    ratingId = dto.ratingId,
                    userId = dto.userId,
                    userName = dto.userName ?: "Unknown",
                    userProfileUrl = dto.userProfileUrl,
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
            val response = ApiClient.get<CommentCountResponse>(
                "comments/count",
                mapOf("ratingId" to ratingId)
            )
            response.count
        } catch (e: Exception) {
            0
        }
    }

    suspend fun deleteComment(commentId: String, userId: String): Result<Unit> {
        return try {
            ApiClient.delete("comments/$commentId")
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
            ApiClient.post<NotificationRequest, Unit>(
                "notifications",
                NotificationRequest(
                    userId = userId,
                    eventType = type,
                    title = title,
                    body = body,
                    data = data
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(userId: String, limit: Int = 50): Result<List<Notification>> {
        return try {
            val responses = ApiClient.get<List<NotificationResponse>>(
                "notifications",
                mapOf("limit" to limit.toString(), "offset" to "0", "unreadOnly" to "false")
            )
            Result.success(responses.map { dto ->
                Notification(
                    id = dto.id,
                    type = dto.type,
                    title = dto.title,
                    body = dto.body,
                    isRead = dto.isRead,
                    createdAt = parseTimestamp(dto.createdAt)
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            ApiClient.patch<Unit, Unit>("notifications/$notificationId/read", Unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            ApiClient.post<ReadAllRequest, Unit>("notifications/read-all", ReadAllRequest())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadNotificationCount(userId: String): Int {
        return try {
            val response = ApiClient.get<UnreadCountResponse>("notifications/unread-count")
            response.count
        } catch (e: Exception) {
            0
        }
    }

    // ==================== STORIES ====================

    suspend fun uploadStory(
        userId: String,
        imageUrl: String,
        dishName: String? = null,
        rating: Float? = null,
        city: String? = null
    ): Result<Story> {
        return try {
            val response = ApiClient.post<StoryRequest, StoryResponse>(
                "stories",
                StoryRequest(imageUrl = imageUrl, dishName = dishName, rating = rating, city = city)
            )
            Result.success(
                Story(
                    id = response.id,
                    userId = response.userId,
                    userName = response.userName ?: "Unknown",
                    userProfileUrl = response.userProfileUrl,
                    imageUrl = response.imageUrl,
                    dishName = response.dishName,
                    rating = response.rating,
                    city = response.city,
                    createdAt = parseTimestamp(response.createdAt),
                    expiresAt = parseTimestamp(response.expiresAt)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStories(): Result<List<Story>> {
        return try {
            val responses = ApiClient.get<List<StoryResponse>>("stories")
            Result.success(responses.map { dto ->
                Story(
                    id = dto.id,
                    userId = dto.userId,
                    userName = dto.userName ?: "Unknown",
                    userProfileUrl = dto.userProfileUrl,
                    imageUrl = dto.imageUrl,
                    dishName = dto.dishName,
                    rating = dto.rating,
                    city = dto.city,
                    createdAt = parseTimestamp(dto.createdAt),
                    expiresAt = parseTimestamp(dto.expiresAt)
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String, userId: String): Result<Unit> {
        return try {
            ApiClient.delete("stories/$storyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== USER PROFILE (OTHER USER) ====================

    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val profile = ApiClient.get<UserProfileResponse>("social/profile/$userId")
            Result.success(
                User(
                    id = profile.id,
                    name = profile.name,
                    email = profile.email ?: "",
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
            val items = ApiClient.get<List<FeedItemResponse>>(
                "social/ratings/$userId",
                mapOf("limit" to limit.toString(), "offset" to offset.toString())
            )
            Result.success(items.map { it.toFeedItem() })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRatingsByIds(
        ids: List<String>,
        currentUserId: String? = null
    ): Result<List<FeedItem>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return try {
            val items = ApiClient.get<List<FeedItemResponse>>(
                "social/ratings-by-ids",
                mapOf("ids" to ids.joinToString(","))
            )
            Result.success(items.map { it.toFeedItem() })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private fun FeedItemResponse.toFeedItem() = FeedItem(
        id = id,
        userId = userId,
        userProfileImageUrl = userProfileUrl,
        userName = userName,
        dishImageUrl = dishImageUrl,
        dishName = dishName,
        dishId = dishId,
        restaurantName = restaurantName,
        restaurantCity = restaurantCity,
        rating = rating,
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLiked = isLiked,
        timestamp = parseTimestamp(createdAt),
        comment = comment,
        imageUrls = imageUrls,
        price = price
    )

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return 0L
        return try {
            Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }

    private fun normalizeCommentContent(content: String): String? {
        val normalized = content.trim().replace(Regex("\\s{2,}"), " ")
        if (normalized.isBlank()) return null
        if (normalized.length > MAX_COMMENT_LENGTH) return null
        return normalized
    }
}
