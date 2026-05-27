package com.example.smackcheck2.data.repository

import com.example.smackcheck2.model.*
import com.example.smackcheck2.platform.PreferencesManager

class PreferencesRepository(private val preferencesManager: PreferencesManager) {

    suspend fun getAppPreferences(): AppPreferences {
        return AppPreferences(
            theme = preferencesManager.getThemePreference().name,
            language = preferencesManager.getLanguage(),
            notificationSettings = preferencesManager.getNotificationSettings(),
            privacySettings = preferencesManager.getPrivacySettings()
        )
    }

    suspend fun getThemePreference(): ThemePreference {
        return preferencesManager.getThemePreference()
    }

    suspend fun saveThemePreference(theme: ThemePreference): Result<Unit> {
        return try {
            preferencesManager.saveThemePreference(theme)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save theme preference. Changes may not persist."))
        }
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> {
        return try {
            preferencesManager.saveNotificationSettings(settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save notification settings. Changes may not persist."))
        }
    }

    suspend fun savePrivacySettings(settings: PrivacySettings): Result<Unit> {
        return try {
            preferencesManager.savePrivacySettings(settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save privacy settings. Changes may not persist."))
        }
    }

    suspend fun getFirstOpenTimestamp(): Long = preferencesManager.getFirstOpenTimestamp()
    suspend fun saveFirstOpenTimestamp(timestamp: Long) = preferencesManager.saveFirstOpenTimestamp(timestamp)
    suspend fun isDay1RetentionTracked(): Boolean = preferencesManager.isDay1RetentionTracked()
    suspend fun setDay1RetentionTracked() = preferencesManager.setDay1RetentionTracked()

    suspend fun hasSeenPermissionsOnboarding(): Boolean = preferencesManager.hasSeenPermissionsOnboarding()
    suspend fun setPermissionsOnboardingSeen() = preferencesManager.setPermissionsOnboardingSeen()

    suspend fun hasDismissedProfileSetup(userId: String): Boolean =
        preferencesManager.hasDismissedProfileSetup(userId)

    suspend fun setProfileSetupDismissed(userId: String) =
        preferencesManager.setProfileSetupDismissed(userId)

    suspend fun getBookmarks(): Set<String> {
        return try {
            preferencesManager.getBookmarks()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun toggleBookmark(ratingId: String): Boolean {
        val current = getBookmarks().toMutableSet()
        val isNowBookmarked = if (current.contains(ratingId)) {
            current.remove(ratingId)
            false
        } else {
            current.add(ratingId)
            true
        }
        preferencesManager.saveBookmarks(current)
        return isNowBookmarked
    }

    suspend fun isBookmarked(ratingId: String): Boolean {
        return getBookmarks().contains(ratingId)
    }

    suspend fun getPendingRatings(): List<PendingRating> {
        return try {
            preferencesManager.getPendingRatings()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun savePendingRatings(ratings: List<PendingRating>) {
        preferencesManager.savePendingRatings(ratings)
    }

    suspend fun upsertPendingRating(rating: PendingRating) {
        val ratings = getPendingRatings()
            .filterNot { it.localId == rating.localId } + rating
        savePendingRatings(ratings.sortedByDescending { it.createdAt })
    }

    suspend fun removePendingRating(localId: String) {
        savePendingRatings(getPendingRatings().filterNot { it.localId == localId })
    }

    suspend fun savePendingRatingImage(localId: String, imageBytes: ByteArray): String? =
        preferencesManager.savePendingRatingImage(localId, imageBytes)

    suspend fun readPendingRatingImage(imagePath: String): ByteArray? =
        preferencesManager.readPendingRatingImage(imagePath)

    suspend fun deletePendingRatingImage(imagePath: String) =
        preferencesManager.deletePendingRatingImage(imagePath)

    suspend fun clearAllSettings(): Result<Unit> {
        return try {
            preferencesManager.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to clear settings. Please try again."))
        }
    }
}
