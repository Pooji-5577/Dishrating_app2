package com.example.smackcheck2.platform

import com.example.smackcheck2.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

actual class PreferencesManager {

    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val THEME_PREF = "theme_pref"
        const val NOTIFICATION_SETTINGS = "notification_settings"
        const val PRIVACY_SETTINGS = "privacy_settings"
        const val LANGUAGE = "language"
        const val FIRST_OPEN_TIMESTAMP = "first_open_timestamp"
        const val DAY1_RETENTION_TRACKED = "day1_retention_tracked"
        const val BOOKMARKS = "bookmarked_ratings"
    }

    actual suspend fun saveThemePreference(theme: ThemePreference) {
        userDefaults.setObject(theme.name, forKey = THEME_PREF)
        userDefaults.synchronize()
    }

    actual suspend fun getThemePreference(): ThemePreference {
        val themeString = userDefaults.stringForKey(THEME_PREF) ?: ThemePreference.SYSTEM.name
        return ThemePreference.valueOf(themeString)
    }

    actual suspend fun saveNotificationSettings(settings: NotificationSettings) {
        val settingsJson = json.encodeToString(settings)
        userDefaults.setObject(settingsJson, forKey = NOTIFICATION_SETTINGS)
        userDefaults.synchronize()
    }

    actual suspend fun getNotificationSettings(): NotificationSettings {
        val settingsJson = userDefaults.stringForKey(NOTIFICATION_SETTINGS) ?: return NotificationSettings()
        return try {
            json.decodeFromString(settingsJson)
        } catch (e: Exception) {
            NotificationSettings()
        }
    }

    actual suspend fun savePrivacySettings(settings: PrivacySettings) {
        val settingsJson = json.encodeToString(settings)
        userDefaults.setObject(settingsJson, forKey = PRIVACY_SETTINGS)
        userDefaults.synchronize()
    }

    actual suspend fun getPrivacySettings(): PrivacySettings {
        val settingsJson = userDefaults.stringForKey(PRIVACY_SETTINGS) ?: return PrivacySettings()
        return try {
            json.decodeFromString(settingsJson)
        } catch (e: Exception) {
            PrivacySettings()
        }
    }

    actual suspend fun saveLanguage(language: String) {
        userDefaults.setObject(language, forKey = LANGUAGE)
        userDefaults.synchronize()
    }

    actual suspend fun getLanguage(): String {
        return userDefaults.stringForKey(LANGUAGE) ?: "en"
    }

    actual suspend fun getFirstOpenTimestamp(): Long {
        val storedValue = userDefaults.objectForKey(FIRST_OPEN_TIMESTAMP) as? NSNumber
        return storedValue?.longLongValue ?: 0L
    }

    actual suspend fun saveFirstOpenTimestamp(timestamp: Long) {
        userDefaults.setObject(NSNumber(longLong = timestamp), forKey = FIRST_OPEN_TIMESTAMP)
        userDefaults.synchronize()
    }

    actual suspend fun isDay1RetentionTracked(): Boolean {
        return userDefaults.boolForKey(DAY1_RETENTION_TRACKED)
    }

    actual suspend fun setDay1RetentionTracked() {
        userDefaults.setBool(true, forKey = DAY1_RETENTION_TRACKED)
        userDefaults.synchronize()
    }

    actual suspend fun saveBookmarks(bookmarkIds: Set<String>) {
        val bookmarksJson = json.encodeToString(bookmarkIds.toList())
        userDefaults.setObject(bookmarksJson, forKey = BOOKMARKS)
        userDefaults.synchronize()
    }

    actual suspend fun getBookmarks(): Set<String> {
        val bookmarksJson = userDefaults.stringForKey(BOOKMARKS) ?: return emptySet()
        return try {
            json.decodeFromString<List<String>>(bookmarksJson).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    actual suspend fun clearAll() {
        userDefaults.removePersistentDomainForName(userDefaults.dictionaryRepresentation().toString())
        userDefaults.synchronize()
    }
}
