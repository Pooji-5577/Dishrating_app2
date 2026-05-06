package com.example.smackcheck2.platform

import com.example.smackcheck2.model.NotificationSettings
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

    suspend fun saveBookmarks(bookmarkIds: Set<String>)
    suspend fun getBookmarks(): Set<String>

    suspend fun clearAll()
}
