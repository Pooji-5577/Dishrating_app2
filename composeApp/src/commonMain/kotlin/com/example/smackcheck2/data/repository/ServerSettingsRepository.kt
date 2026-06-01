package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.model.NotificationSettings
import com.example.smackcheck2.model.PrivacySettings
import com.example.smackcheck2.model.ProfileVisibility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server-backed settings repository for privacy and notification preferences.
 *
 * Uses the custom backend:
 *   GET  /api/settings/privacy
 *   PUT  /api/settings/privacy
 *   GET  /api/settings/notifications
 *   PUT  /api/settings/notifications
 */
class ServerSettingsRepository(
    private val authRepository: AuthRepository = AuthRepository()
) {

    suspend fun getPrivacySettings(): Result<PrivacySettings> = runCatching {
        authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Please sign in to sync privacy settings")

        val dto: PrivacySettingsDto = ApiClient.get("settings/privacy")
        dto.toModel()
    }

    suspend fun savePrivacySettings(settings: PrivacySettings): Result<Unit> = runCatching {
        authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Please sign in to sync privacy settings")

        ApiClient.put<PrivacySettingsDto, Unit>(
            path = "settings/privacy",
            body = PrivacySettingsDto.from(settings)
        )
    }

    suspend fun getNotificationSettings(): Result<NotificationSettings> = runCatching {
        authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Please sign in to sync notification settings")

        val dto: NotificationSettingsDto = ApiClient.get("settings/notifications")
        dto.toModel()
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> = runCatching {
        authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Please sign in to sync notification settings")

        ApiClient.put<NotificationSettingsDto, Unit>(
            path = "settings/notifications",
            body = NotificationSettingsDto.from(settings)
        )
    }
}

@Serializable
private data class PrivacySettingsDto(
    @SerialName("profile_visibility")
    val profileVisibility: String = ProfileVisibility.PUBLIC.name,
    @SerialName("show_email")
    val showEmail: Boolean = false,
    @SerialName("show_location")
    val showLocation: Boolean = true,
    @SerialName("allow_tagging")
    val allowTagging: Boolean = true,
    @SerialName("data_collection")
    val dataCollection: Boolean = true,
    @SerialName("share_exact_location")
    val shareExactLocation: Boolean = false,
    @SerialName("share_approximate_location")
    val shareApproximateLocation: Boolean = true
) {
    fun toModel(): PrivacySettings = PrivacySettings(
        profileVisibility = runCatching { ProfileVisibility.valueOf(profileVisibility) }
            .getOrDefault(ProfileVisibility.PUBLIC),
        showEmail = showEmail,
        showLocation = showLocation,
        allowTagging = allowTagging,
        dataCollection = dataCollection,
        shareExactLocation = shareExactLocation,
        shareApproximateLocation = shareApproximateLocation
    )

    companion object {
        fun from(settings: PrivacySettings): PrivacySettingsDto =
            PrivacySettingsDto(
                profileVisibility = settings.profileVisibility.name,
                showEmail = settings.showEmail,
                showLocation = settings.showLocation,
                allowTagging = settings.allowTagging,
                dataCollection = settings.dataCollection,
                shareExactLocation = settings.shareExactLocation,
                shareApproximateLocation = settings.shareApproximateLocation
            )
    }
}

@Serializable
private data class NotificationSettingsDto(
    @SerialName("push_enabled")
    val pushEnabled: Boolean = true,
    @SerialName("email_enabled")
    val emailEnabled: Boolean = true,
    @SerialName("new_follower_notif")
    val newFollowerNotif: Boolean = true,
    @SerialName("new_like_notif")
    val newLikeNotif: Boolean = true,
    @SerialName("new_comment_notif")
    val newCommentNotif: Boolean = true,
    @SerialName("weekly_digest")
    val weeklyDigest: Boolean = true,
    @SerialName("achievement_notif")
    val achievementNotif: Boolean = true
) {
    fun toModel(): NotificationSettings = NotificationSettings(
        pushEnabled = pushEnabled,
        emailEnabled = emailEnabled,
        newFollowerNotif = newFollowerNotif,
        newLikeNotif = newLikeNotif,
        newCommentNotif = newCommentNotif,
        weeklyDigest = weeklyDigest,
        achievementNotif = achievementNotif
    )

    companion object {
        fun from(settings: NotificationSettings): NotificationSettingsDto =
            NotificationSettingsDto(
                pushEnabled = settings.pushEnabled,
                emailEnabled = settings.emailEnabled,
                newFollowerNotif = settings.newFollowerNotif,
                newLikeNotif = settings.newLikeNotif,
                newCommentNotif = settings.newCommentNotif,
                weeklyDigest = settings.weeklyDigest,
                achievementNotif = settings.achievementNotif
            )
    }
}
