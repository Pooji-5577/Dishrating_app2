package com.example.smackcheck2.model

import kotlinx.serialization.Serializable

/**
 * Notification settings
 */
@Serializable
data class NotificationSettings(
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val newFollowerNotif: Boolean = true,
    val newLikeNotif: Boolean = true,
    val newCommentNotif: Boolean = true,
    val weeklyDigest: Boolean = true,
    val achievementNotif: Boolean = true
)

/**
 * Privacy settings
 */
@Serializable
data class PrivacySettings(
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    val showEmail: Boolean = false,
    val showLocation: Boolean = true,
    val allowTagging: Boolean = true,
    val dataCollection: Boolean = true
)

enum class ProfileVisibility {
    PUBLIC, FRIENDS_ONLY, PRIVATE
}

/**
 * Theme preference
 */
enum class ThemePreference {
    LIGHT, DARK, SYSTEM
}

/**
 * App preferences (aggregates all settings)
 */
@Serializable
data class AppPreferences(
    val theme: String = ThemePreference.SYSTEM.name,
    val language: String = "en",
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val privacySettings: PrivacySettings = PrivacySettings()
)
