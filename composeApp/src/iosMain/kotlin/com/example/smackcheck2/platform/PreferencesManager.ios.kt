package com.example.smackcheck2.platform

import com.example.smackcheck2.model.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

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
        const val PERMISSIONS_ONBOARDING_SEEN = "permissions_onboarding_seen"
        const val BOOKMARKS = "bookmarked_ratings"
        const val PENDING_RATINGS = "pending_ratings"
    }

    actual suspend fun saveThemePreference(theme: ThemePreference) {
        userDefaults.setObject(theme.name, forKey = THEME_PREF)
        userDefaults.synchronize()
    }

    actual suspend fun getThemePreference(): ThemePreference {
        val themeString = userDefaults.stringForKey(THEME_PREF) ?: ThemePreference.LIGHT.name
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

    actual suspend fun hasSeenPermissionsOnboarding(): Boolean {
        return userDefaults.boolForKey(PERMISSIONS_ONBOARDING_SEEN)
    }

    actual suspend fun setPermissionsOnboardingSeen() {
        userDefaults.setBool(true, forKey = PERMISSIONS_ONBOARDING_SEEN)
        userDefaults.synchronize()
    }

    actual suspend fun hasDismissedProfileSetup(userId: String): Boolean {
        if (userId.isBlank()) return false
        return userDefaults.boolForKey(profileSetupDismissedKey(userId))
    }

    actual suspend fun setProfileSetupDismissed(userId: String) {
        if (userId.isBlank()) return
        userDefaults.setBool(true, forKey = profileSetupDismissedKey(userId))
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

    actual suspend fun savePendingRatings(ratings: List<PendingRating>) {
        val pendingJson = json.encodeToString(ratings)
        userDefaults.setObject(pendingJson, forKey = PENDING_RATINGS)
        userDefaults.synchronize()
    }

    actual suspend fun getPendingRatings(): List<PendingRating> {
        val pendingJson = userDefaults.stringForKey(PENDING_RATINGS) ?: return emptyList()
        return try {
            json.decodeFromString(pendingJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun savePendingRatingImage(localId: String, imageBytes: ByteArray): String? {
        return try {
            val dir = pendingImagesDirectory() ?: return null
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = dir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            val path = "$dir/$localId.jpg"
            val data = imageBytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
            }
            if (data.writeToFile(path, atomically = true)) path else null
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun readPendingRatingImage(imagePath: String): ByteArray? {
        return try {
            val data = NSData.dataWithContentsOfFile(imagePath) ?: return null
            val length = data.length.toInt()
            if (length == 0) return null
            val bytes = ByteArray(length)
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
            bytes
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun deletePendingRatingImage(imagePath: String) {
        try {
            NSFileManager.defaultManager.removeItemAtPath(imagePath, error = null)
        } catch (_: Exception) {
        }
    }

    actual suspend fun clearAll() {
        userDefaults.removePersistentDomainForName(userDefaults.dictionaryRepresentation().toString())
        userDefaults.synchronize()
    }

    private fun profileSetupDismissedKey(userId: String): String =
        "profile_setup_dismissed_$userId"

    private fun pendingImagesDirectory(): String? {
        val paths = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        )
        val documents = paths.firstOrNull() as? String ?: return null
        return "$documents/pending_rating_images"
    }
}
