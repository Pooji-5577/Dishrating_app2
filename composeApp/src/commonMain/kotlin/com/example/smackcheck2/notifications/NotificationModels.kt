package com.example.smackcheck2.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SmackCheck - Notification Models
 *
 * Data classes for notifications throughout the app.
 */

// ─── Event Types ─────────────────────────────────────────────────

enum class NotificationEventType(val value: String) {
    REVIEW_LIKED("review_liked"),
    DISH_COMMENT("dish_comment"),
    POINTS_EARNED("points_earned"),
    CHALLENGE_COMPLETED("challenge_completed"),
    TRENDING_DISH("trending_dish"),
    NEW_POST("new_post"),
    RATING_SUBMITTED("rating_submitted"),
    WELCOME("welcome"),
    FIRST_DISH("first_dish"),
    INACTIVITY_REMINDER("inactivity_reminder"),
    ADMIN_BROADCAST("admin_broadcast");

    companion object {
        fun fromValue(value: String): NotificationEventType? =
            entries.find { it.value == value }
    }
}

// ─── Database Model ──────────────────────────────────────────────

@Serializable
data class NotificationRecord(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("event_type") val eventType: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    val data: Map<String, String> = emptyMap()
)

// ─── Insert Payload ──────────────────────────────────────────────

@Serializable
data class NotificationInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
    @SerialName("event_type") val eventType: String,
    val data: Map<String, String> = emptyMap()
)

// ─── Results ─────────────────────────────────────────────────────

data class TriggerResult(
    val success: Boolean,
    val notificationId: String? = null,
    val error: String? = null
)

data class PushTokenResult(
    val success: Boolean,
    val token: String? = null,
    val error: String? = null
)

// ─── Notification Navigation Data ────────────────────────────────

data class NotificationNavigationTarget(
    val screen: String,
    val params: Map<String, String> = emptyMap()
)

/**
 * Maps notification data to a navigation target screen.
 */
fun getNavigationTarget(data: Map<String, String>): NotificationNavigationTarget? {
    val screen = data["screen"] ?: return null
    return when (screen) {
        "DishDetail" -> NotificationNavigationTarget(
            screen = "DishDetail",
            params = mapOf(
                "dishId" to (data["dishId"] ?: ""),
                "reviewId" to (data["reviewId"] ?: "")
            )
        )
        "GameScreen" -> NotificationNavigationTarget(
            screen = "GameScreen",
            params = mapOf("challengeId" to (data["challengeId"] ?: ""))
        )
        "SocialFeed" -> NotificationNavigationTarget(
            screen = "SocialFeed",
            params = emptyMap()
        )
        else -> NotificationNavigationTarget(screen = screen)
    }
}
