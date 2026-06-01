package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.NotificationDto
import com.example.smackcheck2.model.Notification
import com.example.smackcheck2.notifications.NotificationEventType
import com.example.smackcheck2.notifications.NotificationInsert
import com.example.smackcheck2.notifications.NotificationRecord
import com.example.smackcheck2.notifications.TriggerResult
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Unified notification service that owns all notification behavior:
 * - Event-trigger inserts (like, comment, post, welcome, etc.)
 * - CRUD and query operations (fetch, mark read, unread count)
 * - Polling-based subscription (replaces Realtime; swap for WebSocket/SSE when available)
 * - Push token management
 *
 * All callers should use this single module instead of direct table
 * access or fragmented notification helpers.
 */
class NotificationService {

    // ─── Generic Insert (via POST /api/notifications) ────────────────────────

    private suspend fun insertNotification(payload: NotificationInsert): TriggerResult {
        return try {
            val body = NotificationCreateRequest(
                userId = payload.userId,
                eventType = payload.eventType,
                title = payload.title,
                body = payload.body,
                data = payload.data
            )
            val result: NotificationCreateResponse = ApiClient.post("notifications", body)
            TriggerResult(success = true, notificationId = result.id)
        } catch (e: Exception) {
            val message = e.message ?: "Unknown error"
            if (message.contains("23505") || message.contains("duplicate")) {
                TriggerResult(success = true, notificationId = null)
            } else {
                TriggerResult(success = false, error = message)
            }
        }
    }

    // ─── Event-Specific Triggers ──────────────────────────────────────────────

    suspend fun notifyReviewLiked(
        reviewOwnerId: String,
        likerName: String,
        dishName: String,
        reviewId: String
    ): TriggerResult = insertNotification(
        NotificationInsert(
            userId = reviewOwnerId,
            title = "Review Liked",
            body = "$likerName liked your review of $dishName",
            eventType = NotificationEventType.REVIEW_LIKED.value,
            data = mapOf(
                "source_id" to "like_${reviewId}_$likerName",
                "screen" to "DishDetail",
                "reviewId" to reviewId,
                "dishName" to dishName
            )
        )
    )

    suspend fun notifyDishComment(
        reviewOwnerId: String,
        commenterName: String,
        dishName: String,
        dishId: String,
        commentId: String
    ): TriggerResult = insertNotification(
        NotificationInsert(
            userId = reviewOwnerId,
            title = "New Comment",
            body = "$commenterName commented on $dishName",
            eventType = NotificationEventType.DISH_COMMENT.value,
            data = mapOf(
                "source_id" to "comment_$commentId",
                "screen" to "DishDetail",
                "dishId" to dishId,
                "dishName" to dishName
            )
        )
    )

    suspend fun notifyPointsEarned(
        userId: String,
        points: Int,
        reason: String,
        actionId: String
    ): TriggerResult = insertNotification(
        NotificationInsert(
            userId = userId,
            title = "Points Earned",
            body = "You earned $points points for $reason.",
            eventType = NotificationEventType.POINTS_EARNED.value,
            data = mapOf(
                "source_id" to "points_$actionId",
                "screen" to "GameScreen",
                "points" to points.toString()
            )
        )
    )

    suspend fun notifyChallengeCompleted(
        userId: String,
        challengeTitle: String,
        xpReward: Int,
        challengeId: String
    ): TriggerResult = insertNotification(
        NotificationInsert(
            userId = userId,
            title = "Challenge Complete",
            body = "You completed \"$challengeTitle\" and earned $xpReward XP.",
            eventType = NotificationEventType.CHALLENGE_COMPLETED.value,
            data = mapOf(
                "source_id" to "challenge_$challengeId",
                "screen" to "GameScreen",
                "challengeId" to challengeId,
                "xpReward" to xpReward.toString()
            )
        )
    )

    suspend fun notifyTrendingDish(
        userId: String,
        dishName: String,
        restaurantName: String,
        dishId: String
    ): TriggerResult = insertNotification(
        NotificationInsert(
            userId = userId,
            title = "Trending Near You",
            body = "$dishName at $restaurantName is trending.",
            eventType = NotificationEventType.TRENDING_DISH.value,
            data = mapOf(
                "source_id" to "trending_$dishId",
                "screen" to "DishDetail",
                "dishId" to dishId,
                "restaurantName" to restaurantName
            )
        )
    )

    suspend fun notifyNewPost(
        posterId: String,
        posterName: String,
        dishName: String,
        restaurantName: String,
        ratingId: String
    ): TriggerResult {
        return try {
            val followers: List<FollowerIdDto> = ApiClient.get(
                "followers",
                mapOf("userId" to posterId)
            )

            if (followers.isEmpty()) return TriggerResult(success = true)

            followers.forEach { follower ->
                insertNotification(
                    NotificationInsert(
                        userId = follower.followerId,
                        title = "New Review",
                        body = "$posterName reviewed $dishName at $restaurantName",
                        eventType = NotificationEventType.NEW_POST.value,
                        data = mapOf(
                            "source_id" to "post_${ratingId}_${follower.followerId}",
                            "screen" to "SocialFeed",
                            "ratingId" to ratingId
                        )
                    )
                )
            }
            TriggerResult(success = true)
        } catch (e: Exception) {
            TriggerResult(success = false, error = e.message)
        }
    }

    suspend fun notifyRatingSubmitted(
        userId: String,
        dishName: String,
        ratingId: String
    ): TriggerResult = insertNotification(
        NotificationInsert(
            userId = userId,
            title = "Rating Submitted",
            body = "Your review of $dishName was posted successfully.",
            eventType = NotificationEventType.RATING_SUBMITTED.value,
            data = mapOf(
                "source_id" to "rating_confirm_$ratingId",
                "screen" to "SocialFeed",
                "ratingId" to ratingId
            )
        )
    )

    suspend fun notifyCommentOnRating(
        ratingId: String,
        commenterId: String,
        commenterName: String = ""
    ): TriggerResult {
        return try {
            val rating: RatingBasicDto = ApiClient.get("ratings/$ratingId")

            if (rating.userId == commenterId) return TriggerResult(success = true)

            val name = commenterName.ifBlank {
                try {
                    val profile: ProfileBasicDto = ApiClient.get("profiles/$commenterId")
                    profile.name.ifBlank { "Someone" }
                } catch (_: Exception) { "Someone" }
            }

            insertNotification(
                NotificationInsert(
                    userId = rating.userId,
                    title = "New Comment",
                    body = "$name commented on your review",
                    eventType = NotificationEventType.DISH_COMMENT.value,
                    data = mapOf(
                        "source_id" to "comment_${ratingId}_$commenterId",
                        "screen" to "SocialFeed",
                        "ratingId" to ratingId
                    )
                )
            )
        } catch (e: Exception) {
            TriggerResult(success = false, error = e.message)
        }
    }

    suspend fun notifyWelcome(userId: String, userName: String): TriggerResult =
        insertNotification(
            NotificationInsert(
                userId = userId,
                title = "Welcome to SmackCheck",
                body = "Hey $userName, start by rating your first dish and earn XP.",
                eventType = NotificationEventType.WELCOME.value,
                data = mapOf("source_id" to "welcome_$userId", "screen" to "Home")
            )
        )

    suspend fun notifyFirstDish(userId: String, dishName: String): TriggerResult =
        insertNotification(
            NotificationInsert(
                userId = userId,
                title = "First Review Posted",
                body = "Nice — your review of $dishName is live. Keep rating to level up.",
                eventType = NotificationEventType.FIRST_DISH.value,
                data = mapOf("source_id" to "first_dish_$userId", "screen" to "SocialFeed")
            )
        )

    // ─── CRUD / Query Operations ──────────────────────────────────────────────

    suspend fun getNotifications(userId: String): List<Notification> {
        val result: List<NotificationDto> = ApiClient.get(
            "notifications",
            mapOf("limit" to "50", "offset" to "0", "unreadOnly" to "false")
        )
        return result.map { toNotification(it) }
    }

    suspend fun fetchNotifications(
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
        unreadOnly: Boolean = false
    ): Result<List<NotificationRecord>> {
        return try {
            val result: List<NotificationRecord> = ApiClient.get(
                "notifications",
                mapOf(
                    "limit" to limit.toString(),
                    "offset" to offset.toString(),
                    "unreadOnly" to unreadOnly.toString()
                )
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            ApiClient.patch<Unit, SuccessResponse>("notifications/$notificationId/read", Unit)
        } catch (_: Exception) { }
    }

    suspend fun markAllAsRead(userId: String) {
        try {
            ApiClient.postEmpty<SuccessResponse>("notifications/read-all")
        } catch (_: Exception) { }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val response: UnreadCountResponse = ApiClient.get("notifications/unread-count")
            response.count
        } catch (_: Exception) { 0 }
    }

    // ─── Polling Subscription ─────────────────────────────────────────────────
    // TODO: Replace with a WebSocket or SSE connection when the backend supports it.
    // Currently polls GET /api/notifications?unreadOnly=true every 5 seconds and
    // emits any new unread notifications as they appear.

    fun subscribeToNotifications(userId: String): Flow<Notification> = flow {
        val seen = mutableSetOf<String>()
        while (true) {
            try {
                val items: List<NotificationRecord> = ApiClient.get(
                    "notifications",
                    mapOf("limit" to "50", "offset" to "0", "unreadOnly" to "true")
                )
                for (record in items) {
                    if (record.id.isNotEmpty() && !seen.contains(record.id)) {
                        seen.add(record.id)
                        emit(
                            Notification(
                                id = record.id,
                                type = record.eventType,
                                title = record.title,
                                body = record.body,
                                data = runCatching {
                                    record.data.mapValues { it.value.jsonPrimitive.content }
                                }.getOrDefault(emptyMap()),
                                isRead = record.isRead,
                                createdAt = runCatching {
                                    Instant.parse(record.createdAt).toEpochMilliseconds()
                                }.getOrDefault(0L)
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
            delay(5_000)
        }
    }

    suspend fun unsubscribeFromNotifications() {
        // No-op: the polling Flow above is cancelled automatically when the
        // collecting coroutine scope is cancelled. No persistent channel to clean up.
    }

    // ─── Push Token Management ────────────────────────────────────────────────

    suspend fun savePushToken(token: String): Result<Unit> {
        return try {
            SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))

            ApiClient.put<PushTokenRequest, SuccessResponse>(
                "auth/push-token",
                PushTokenRequest(token = token)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removePushToken() {
        try {
            SupabaseClientProvider.client.auth.currentUserOrNull() ?: return
            ApiClient.put<PushTokenRequest, SuccessResponse>(
                "auth/push-token",
                PushTokenRequest(token = null)
            )
        } catch (_: Exception) { }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private fun toNotification(dto: NotificationDto): Notification = Notification(
        id = dto.id ?: "",
        type = dto.type,
        title = dto.title,
        body = dto.body,
        data = runCatching {
            dto.data?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
        }.getOrDefault(emptyMap()),
        isRead = dto.isRead,
        createdAt = runCatching {
            dto.createdAt?.let { Instant.parse(it).toEpochMilliseconds() } ?: 0L
        }.getOrDefault(0L)
    )
}

// ─── Private Request / Response DTOs ─────────────────────────────────────────

@kotlinx.serialization.Serializable
private data class NotificationCreateRequest(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("event_type") val eventType: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)

@kotlinx.serialization.Serializable
private data class NotificationCreateResponse(
    val id: String? = null,
    val success: Boolean = true
)

@kotlinx.serialization.Serializable
private data class SuccessResponse(
    val success: Boolean = true
)

@kotlinx.serialization.Serializable
private data class UnreadCountResponse(
    val count: Int = 0
)

@kotlinx.serialization.Serializable
private data class PushTokenRequest(
    val token: String?
)

@kotlinx.serialization.Serializable
private data class FollowerIdDto(
    @kotlinx.serialization.SerialName("follower_id")
    val followerId: String
)

@kotlinx.serialization.Serializable
private data class RatingBasicDto(
    val id: String = "",
    @kotlinx.serialization.SerialName("user_id") val userId: String = "",
    @kotlinx.serialization.SerialName("dish_id") val dishId: String = ""
)

@kotlinx.serialization.Serializable
private data class ProfileBasicDto(
    val id: String = "",
    val name: String = ""
)
