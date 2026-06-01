package com.example.smackcheck2.platform

import com.example.smackcheck2.model.NotificationSettings
import com.example.smackcheck2.model.PendingRating
import com.example.smackcheck2.model.PrivacySettings
import com.example.smackcheck2.model.ThemePreference

/**
 * Platform-specific preferences storage
 * Android: DataStore
 * iOS: NSUserDefaults
 */
expect class PreferencesManager {
    suspend fun saveThemePreference(theme: ThemePreference)
    suspend fun getThemePreference(): ThemePreference

    suspend fun saveNotificationSettings(settings: NotificationSettings)
    suspend fun getNotificationSettings(): NotificationSettings

    suspend fun savePrivacySettings(settings: PrivacySettings)
    suspend fun getPrivacySettings(): PrivacySettings

    suspend fun saveLanguage(language: String)
    suspend fun getLanguage(): String

    suspend fun getFirstOpenTimestamp(): Long
    suspend fun saveFirstOpenTimestamp(timestamp: Long)
    suspend fun isDay1RetentionTracked(): Boolean
    suspend fun setDay1RetentionTracked()

    suspend fun hasSeenPermissionsOnboarding(): Boolean
    suspend fun setPermissionsOnboardingSeen()

    suspend fun hasDismissedProfileSetup(userId: String): Boolean
    suspend fun setProfileSetupDismissed(userId: String)

    suspend fun saveBookmarks(bookmarkIds: Set<String>)
    suspend fun getBookmarks(): Set<String>

    suspend fun savePendingRatings(ratings: List<PendingRating>)
    suspend fun getPendingRatings(): List<PendingRating>
    suspend fun savePendingRatingImage(localId: String, imageBytes: ByteArray): String?
    suspend fun readPendingRatingImage(imagePath: String): ByteArray?
    suspend fun deletePendingRatingImage(imagePath: String)

    suspend fun clearAll()
}
