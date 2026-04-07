package com.example.smackcheck2.notifications

import com.example.smackcheck2.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

/**
 * SmackCheck - Notification Repository
 *
 * Kotlin port of notificationTriggers.ts.
 * Handles inserting notifications into Supabase and fetching them.
 * Each insert triggers the database webhook → Edge Function → Push API.
 *
 * Deduplication is handled at the database level via the unique index
 * on (user_id, event_type, data->>'source_id').
 */
object NotificationRepository {

    private val client get() = SupabaseClient.client

    // ─── Generic Insert ──────────────────────────────────────────

    private suspend fun insertNotification(payload: NotificationInsert): TriggerResult {
        return try {
            val result = client.postgrest["notifications"]
                .insert(payload) {
                    select()
                }
                .decodeSingleOrNull<NotificationRecord>()

            TriggerResult(
                success = true,
                notificationId = result?.id
            )
        } catch (e: Exception) {
            val message = e.message ?: "Unknown error"
            // Unique constraint violation = duplicate notification, not an error
            if (message.contains("23505") || message.contains("duplicate")) {
                println("Duplicate notification skipped: ${payload.eventType}")
                TriggerResult(success = true, notificationId = null)
            } else {
                println("Failed to insert notification: $message")
                TriggerResult(success = false, error = message)
            }
        }
    }

    // ─── Event-Specific Triggers ─────────────────────────────────

    /**
     * Notify a user that someone liked their dish review.
     */
    suspend fun notifyReviewLiked(
        reviewOwnerId: String,
        likerName: String,
        dishName: String,
        reviewId: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = reviewOwnerId,
                title = "❤️ Review Liked!",
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
    }

    /**
     * Notify a user that someone commented on a dish they reviewed.
     */
    suspend fun notifyDishComment(
        reviewOwnerId: String,
        commenterName: String,
        dishName: String,
        dishId: String,
        commentId: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = reviewOwnerId,
                title = "💬 New Comment",
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
    }

    /**
     * Notify a user that they earned gamification points.
     */
    suspend fun notifyPointsEarned(
        userId: String,
        points: Int,
        reason: String,
        actionId: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = userId,
                title = "🏆 Points Earned!",
                body = "You earned $points points for $reason!",
                eventType = NotificationEventType.POINTS_EARNED.value,
                data = mapOf(
                    "source_id" to "points_$actionId",
                    "screen" to "GameScreen",
                    "points" to points.toString()
                )
            )
        )
    }

    /**
     * Notify a user that they completed a challenge.
     */
    suspend fun notifyChallengeCompleted(
        userId: String,
        challengeTitle: String,
        xpReward: Int,
        challengeId: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = userId,
                title = "🎯 Challenge Complete!",
                body = "You completed \"$challengeTitle\" and earned $xpReward XP!",
                eventType = NotificationEventType.CHALLENGE_COMPLETED.value,
                data = mapOf(
                    "source_id" to "challenge_$challengeId",
                    "screen" to "GameScreen",
                    "challengeId" to challengeId,
                    "xpReward" to xpReward.toString()
                )
            )
        )
    }

    /**
     * Notify a user about trending dishes near their location.
     */
    suspend fun notifyTrendingDish(
        userId: String,
        dishName: String,
        restaurantName: String,
        dishId: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = userId,
                title = "🔥 Trending Near You",
                body = "$dishName at $restaurantName is trending!",
                eventType = NotificationEventType.TRENDING_DISH.value,
                data = mapOf(
                    "source_id" to "trending_$dishId",
                    "screen" to "DishDetail",
                    "dishId" to dishId,
                    "restaurantName" to restaurantName
                )
            )
        )
    }

    /**
     * Notify all followers of a user that they posted a new dish rating.
     */
    suspend fun notifyNewPost(
        posterId: String,
        posterName: String,
        dishName: String,
        restaurantName: String,
        ratingId: String
    ): TriggerResult {
        return try {
            val followers = client.postgrest["followers"]
                .select { filter { eq("following_id", posterId) } }
                .decodeList<FollowerIdDto>()

            if (followers.isEmpty()) return TriggerResult(success = true)

            followers.forEach { follower ->
                insertNotification(
                    NotificationInsert(
                        userId = follower.followerId,
                        title = "🍽️ New Review!",
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
            println("Failed to notify new post: ${e.message}")
            TriggerResult(success = false, error = e.message)
        }
    }

    /**
     * Notify the rater that their dish rating was successfully submitted (self-confirmation).
     */
    suspend fun notifyRatingSubmitted(
        userId: String,
        dishName: String,
        ratingId: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = userId,
                title = "✅ Rating Submitted!",
                body = "Your review of $dishName was posted successfully.",
                eventType = NotificationEventType.RATING_SUBMITTED.value,
                data = mapOf(
                    "source_id" to "rating_confirm_$ratingId",
                    "screen" to "SocialFeed",
                    "ratingId" to ratingId
                )
            )
        )
    }

    /**
     * Notify the owner of a rating that someone commented on it.
     * Fetches rating details internally to resolve the owner.
     */
    suspend fun notifyCommentOnRating(ratingId: String, commenterId: String, commenterName: String = ""): TriggerResult {
        return try {
            val rating = client.postgrest["ratings"]
                .select { filter { eq("id", ratingId) } }
                .decodeSingleOrNull<RatingBasicDto>()
                ?: return TriggerResult(success = false, error = "Rating not found")

            if (rating.userId == commenterId) return TriggerResult(success = true)

            // Fetch commenter name if not provided
            val name = commenterName.ifBlank {
                try {
                    client.postgrest["profiles"]
                        .select { filter { eq("id", commenterId) } }
                        .decodeSingleOrNull<ProfileBasicDto>()?.name ?: "Someone"
                } catch (_: Exception) { "Someone" }
            }

            insertNotification(
                NotificationInsert(
                    userId = rating.userId,
                    title = "💬 New Comment",
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
            println("Failed to notify comment: ${e.message}")
            TriggerResult(success = false, error = e.message)
        }
    }

    /**
     * Send a welcome notification to a newly signed-up user.
     */
    suspend fun notifyWelcome(
        userId: String,
        userName: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = userId,
                title = "Welcome to SmackCheck!",
                body = "Hey $userName! Start by rating your first dish and earn XP!",
                eventType = NotificationEventType.WELCOME.value,
                data = mapOf(
                    "source_id" to "welcome_$userId",
                    "screen" to "Home"
                )
            )
        )
    }

    /**
     * Congratulate a user on uploading their first dish rating.
     */
    suspend fun notifyFirstDish(
        userId: String,
        dishName: String
    ): TriggerResult {
        return insertNotification(
            NotificationInsert(
                userId = userId,
                title = "First Review Posted!",
                body = "Awesome! Your review of $dishName is live. Keep rating to level up!",
                eventType = NotificationEventType.FIRST_DISH.value,
                data = mapOf(
                    "source_id" to "first_dish_$userId",
                    "screen" to "SocialFeed"
                )
            )
        )
    }

    // ─── Fetch User's Notifications ──────────────────────────────

    /**
     * Fetch paginated notifications for the current user.
     */
    suspend fun fetchNotifications(
        limit: Int = 20,
        offset: Int = 0,
        unreadOnly: Boolean = false
    ): Result<List<NotificationRecord>> {
        return try {
            val result = client.postgrest["notifications"]
                .select {
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                    if (unreadOnly) {
                        filter { eq("is_read", false) }
                    }
                }
                .decodeList<NotificationRecord>()

            Result.success(result)
        } catch (e: Exception) {
            println("Failed to fetch notifications: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Mark a notification as read.
     */
    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            client.postgrest["notifications"]
                .update({ set("is_read", true) }) {
                    filter { eq("id", notificationId) }
                }
        } catch (e: Exception) {
            println("Failed to mark notification as read: ${e.message}")
        }
    }

    /**
     * Mark all notifications as read for the current user.
     */
    suspend fun markAllNotificationsAsRead() {
        try {
            val user = client.auth.currentUserOrNull()
            if (user != null) {
                client.postgrest["notifications"]
                    .update({ set("is_read", true) }) {
                        filter {
                            eq("user_id", user.id)
                            eq("is_read", false)
                        }
                    }
            }
        } catch (e: Exception) {
            println("Failed to mark all notifications as read: ${e.message}")
        }
    }

    /**
     * Get the count of unread notifications.
     * Uses a simple select + list count since postgrest-kt count API
     * varies by version.
     */
    suspend fun getUnreadNotificationCount(): Int {
        return try {
            val result = client.postgrest["notifications"]
                .select {
                    filter { eq("is_read", false) }
                }
                .decodeList<NotificationRecord>()
            result.size
        } catch (e: Exception) {
            println("Failed to get unread count: ${e.message}")
            0
        }
    }

    // ─── Save / Remove Push Token ────────────────────────────────

    /**
     * Save the push token (FCM or APNs) to the user's profile in Supabase.
     * Only updates if the token has changed to avoid unnecessary writes.
     */
    suspend fun savePushTokenToSupabase(token: String): Result<Unit> {
        return try {
            val user = client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("User not authenticated. Cannot save push token."))

            // Check if token has changed before updating
            val profile = client.postgrest["profiles"]
                .select {
                    filter { eq("id", user.id) }
                }
                .decodeSingleOrNull<ProfilePushToken>()

            if (profile?.pushToken == token) {
                println("Push token unchanged, skipping update")
                return Result.success(Unit)
            }

            // Upsert the push token
            client.postgrest["profiles"]
                .update({ set("push_token", token) }) {
                    filter { eq("id", user.id) }
                }

            println("Push token saved to Supabase for user: ${user.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("Failed to save push token: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove the push token from the user's profile on logout.
     * Prevents notifications being sent to a logged-out device.
     */
    suspend fun removePushToken() {
        try {
            val user = client.auth.currentUserOrNull()
            if (user != null) {
                client.postgrest["profiles"]
                    .update({ set("push_token", value = null as String?) }) {
                        filter { eq("id", user.id) }
                    }
                println("Push token removed for user: ${user.id}")
            }
        } catch (e: Exception) {
            println("Failed to remove push token: ${e.message}")
        }
    }
}

/**
 * Helper model to read just the push token from profiles.
 */
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
