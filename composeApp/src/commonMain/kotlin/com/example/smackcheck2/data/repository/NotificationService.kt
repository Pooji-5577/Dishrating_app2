package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.NotificationDto
import com.example.smackcheck2.model.Notification
import com.example.smackcheck2.notifications.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Unified notification service that owns all notification behavior:
 * - Event-trigger inserts (like, comment, post, welcome, etc.)
 * - CRUD and query operations (fetch, mark read, unread count)
 * - Realtime subscriptions
 * - Push token management
 *
 * All callers should use this single module instead of direct table
 * access or fragmented notification helpers.
 */
class NotificationService(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client
) {

    private val postgrest = client.postgrest
    private val realtime = client.realtime
    private var notificationChannel: RealtimeChannel? = null

    // ─── Generic Insert ──────────────────────────────────────────

    private suspend fun insertNotification(payload: NotificationInsert): TriggerResult {
        return try {
            val result = postgrest["notifications"]
                .insert(payload) { select() }
                .decodeSingleOrNull<NotificationRecord>()

            TriggerResult(success = true, notificationId = result?.id)
        } catch (e: Exception) {
            val message = e.message ?: "Unknown error"
            if (message.contains("23505") || message.contains("duplicate")) {
                TriggerResult(success = true, notificationId = null)
            } else {
                TriggerResult(success = false, error = message)
            }
        }
    }

    // ─── Event-Specific Triggers ─────────────────────────────────

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
            val followers = postgrest["followers"]
                .select { filter { eq("following_id", posterId) } }
                .decodeList<FollowerIdDto>()

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
            val rating = postgrest["ratings"]
                .select { filter { eq("id", ratingId) } }
                .decodeSingleOrNull<RatingBasicDto>()
                ?: return TriggerResult(success = false, error = "Rating not found")

            if (rating.userId == commenterId) return TriggerResult(success = true)

            val name = commenterName.ifBlank {
                try {
                    postgrest["profiles"]
                        .select { filter { eq("id", commenterId) } }
                        .decodeSingleOrNull<ProfileBasicDto>()?.name ?: "Someone"
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

    // ─── CRUD / Query Operations ─────────────────────────────────

    suspend fun getNotifications(userId: String): List<Notification> {
        val result = postgrest["notifications"]
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }
            .decodeList<NotificationDto>()
        return result.map { toNotification(it) }
    }

    suspend fun fetchNotifications(
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
        unreadOnly: Boolean = false
    ): Result<List<NotificationRecord>> {
        return try {
            val result = postgrest["notifications"]
                .select {
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                    if (unreadOnly) filter { eq("is_read", false) }
                }
                .decodeList<NotificationRecord>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            postgrest["notifications"]
                .update({ set("is_read", true) }) {
                    filter { eq("id", notificationId) }
                }
        } catch (_: Exception) { }
    }

    suspend fun markAllAsRead(userId: String) {
        try {
            postgrest["notifications"]
                .update({ set("is_read", true) }) {
                    filter { eq("user_id", userId); eq("is_read", false) }
                }
        } catch (_: Exception) { }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            postgrest["notifications"]
                .select { filter { eq("user_id", userId); eq("is_read", false) } }
                .decodeList<NotificationDto>()
                .size
        } catch (_: Exception) { 0 }
    }

    // ─── Realtime Subscriptions ──────────────────────────────────

    suspend fun subscribeToNotifications(userId: String): Flow<Notification> {
        notificationChannel?.let { realtime.removeChannel(it) }

        val channel = realtime.channel("notifications:$userId")
        notificationChannel = channel

        val flow: Flow<Notification> = channel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
                filter("user_id", FilterOperator.EQ, userId)
            }
            .map { action ->
                toNotification(Json.decodeFromJsonElement(NotificationDto.serializer(), action.record))
            }

        channel.subscribe()
        return flow
    }

    suspend fun unsubscribeFromNotifications() {
        notificationChannel?.let { realtime.removeChannel(it) }
        notificationChannel = null
    }

    // ─── Push Token Management ───────────────────────────────────

    suspend fun savePushToken(token: String): Result<Unit> {
        return try {
            val user = client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated"))

            val profile = postgrest["profiles"]
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<ProfilePushToken>()

            if (profile?.pushToken == token) return Result.success(Unit)

            postgrest["profiles"]
                .update({ set("push_token", token) }) { filter { eq("id", user.id) } }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removePushToken() {
        try {
            val user = client.auth.currentUserOrNull() ?: return
            postgrest["profiles"]
                .update({ set("push_token", value = null as String?) }) {
                    filter { eq("id", user.id) }
                }
        } catch (_: Exception) { }
    }

    // ─── Private Helpers ─────────────────────────────────────────

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

// ─── Private DTOs ────────────────────────────────────────────────

@kotlinx.serialization.Serializable
private data class ProfilePushToken(
    @kotlinx.serialization.SerialName("push_token")
    val pushToken: String? = null
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
