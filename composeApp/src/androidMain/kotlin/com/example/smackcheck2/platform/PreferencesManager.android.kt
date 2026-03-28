package com.example.smackcheck2.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.smackcheck2.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

actual class PreferencesManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        val THEME_PREF = stringPreferencesKey("theme_pref")
        val NOTIFICATION_SETTINGS = stringPreferencesKey("notification_settings")
        val PRIVACY_SETTINGS = stringPreferencesKey("privacy_settings")
        val LANGUAGE = stringPreferencesKey("language")
        val FIRST_OPEN_TIMESTAMP = longPreferencesKey("first_open_timestamp")
        val DAY1_RETENTION_TRACKED = booleanPreferencesKey("day1_retention_tracked")
    }

    actual suspend fun saveThemePreference(theme: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[THEME_PREF] = theme.name
        }
    }

    actual suspend fun getThemePreference(): ThemePreference {
        val prefs = context.dataStore.data.first()
        val themeString = prefs[THEME_PREF] ?: ThemePreference.SYSTEM.name
        return ThemePreference.valueOf(themeString)
    }

    actual suspend fun saveNotificationSettings(settings: NotificationSettings) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATION_SETTINGS] = json.encodeToString(settings)
        }
    }

    actual suspend fun getNotificationSettings(): NotificationSettings {
        val prefs = context.dataStore.data.first()
        val settingsJson = prefs[NOTIFICATION_SETTINGS] ?: return NotificationSettings()
        return try {
            json.decodeFromString(settingsJson)
        } catch (e: Exception) {
            NotificationSettings()
        }
    }

    actual suspend fun savePrivacySettings(settings: PrivacySettings) {
        context.dataStore.edit { prefs ->
            prefs[PRIVACY_SETTINGS] = json.encodeToString(settings)
        }
    }

    actual suspend fun getPrivacySettings(): PrivacySettings {
        val prefs = context.dataStore.data.first()
        val settingsJson = prefs[PRIVACY_SETTINGS] ?: return PrivacySettings()
        return try {
            json.decodeFromString(settingsJson)
        } catch (e: Exception) {
            PrivacySettings()
        }
    }

    actual suspend fun saveLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE] = language
        }
    }

    actual suspend fun getLanguage(): String {
        val prefs = context.dataStore.data.first()
        return prefs[LANGUAGE] ?: "en"
    }

    actual suspend fun getFirstOpenTimestamp(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[FIRST_OPEN_TIMESTAMP] ?: 0L
    }

    actual suspend fun saveFirstOpenTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[FIRST_OPEN_TIMESTAMP] = timestamp
        }
    }

    actual suspend fun isDay1RetentionTracked(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[DAY1_RETENTION_TRACKED] ?: false
    }

    actual suspend fun setDay1RetentionTracked() {
        context.dataStore.edit { prefs ->
            prefs[DAY1_RETENTION_TRACKED] = true
        }
    }

    actual suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
