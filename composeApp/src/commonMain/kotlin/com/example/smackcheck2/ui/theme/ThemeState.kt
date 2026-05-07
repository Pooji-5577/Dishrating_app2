package com.example.smackcheck2.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.model.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Theme state manager for toggling between light and dark themes
 */
class ThemeState(
    initialDarkMode: Boolean = false,
    private val preferencesRepository: PreferencesRepository? = null,
    private val coroutineScope: CoroutineScope? = null
) {
    var isDarkMode by mutableStateOf(initialDarkMode)
        private set

    init {
        loadThemePreference()
    }

    private fun loadThemePreference() {
        // Force light mode globally until internal dark mode is fully implemented.
        // This prevents OS-level dark mode from affecting the app appearance.
        isDarkMode = false

        // TODO: Restore dynamic theme loading once dark mode is ready
        // if (preferencesRepository == null || coroutineScope == null) return
        // coroutineScope.launch {
        //     try {
        //         val pref = preferencesRepository.getThemePreference()
        //         isDarkMode = when (pref) {
        //             ThemePreference.LIGHT -> false
        //             ThemePreference.DARK -> true
        //             ThemePreference.SYSTEM -> isSystemInDarkMode()
        //         }
        //     } catch (e: Exception) {
        //         println("ThemeState: Failed to load theme preference: ${e.message}")
        //     }
        // }
    }

    fun toggleTheme() {
        isDarkMode = !isDarkMode
        saveThemePreference()
    }

    fun updateDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        saveThemePreference()
    }

    private fun saveThemePreference() {
        if (preferencesRepository == null || coroutineScope == null) return

        coroutineScope.launch {
            try {
                val pref = if (isDarkMode) ThemePreference.DARK else ThemePreference.LIGHT
                preferencesRepository.saveThemePreference(pref)
            } catch (e: Exception) {
                println("ThemeState: Failed to save theme preference: ${e.message}")
            }
        }
    }
}

/**
 * Platform-specific function to detect system theme
 */
expect fun isSystemInDarkMode(): Boolean

/**
 * Local composition provider for theme state
 */
val LocalThemeState = staticCompositionLocalOf<ThemeState> { 
    error("ThemeState not provided") 
}

/**
 * Helper class to hold theme colors
 */
data class ThemeColors(
    val Background: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val CardBackground: Color,
    val Primary: Color,
    val PrimaryVariant: Color,
    val OnPrimary: Color,
    val Secondary: Color,
    val OnSecondary: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextTertiary: Color,
    val Success: Color,
    val Warning: Color,
    val Error: Color,
    val StarYellow: Color,
    val CalorieBadge: Color,
    val BestsellerBadge: Color,
    val AvailableBadge: Color,
    val ImageOverlay: Color,
    val Divider: Color,
    val PrimaryDark: Color,
    val PrimaryRed: Color,
    val MaroonAccent: Color
)

/**
 * Provides colors based on the current theme state
 */
@Composable
fun appColors(): ThemeColors {
    val isDark = LocalThemeState.current.isDarkMode
    return if (isDark) {
        ThemeColors(
            Background = DarkThemeColors.Background,
            Surface = DarkThemeColors.Surface,
            SurfaceVariant = DarkThemeColors.SurfaceVariant,
            CardBackground = DarkThemeColors.CardBackground,
            Primary = DarkThemeColors.Primary,
            PrimaryVariant = DarkThemeColors.PrimaryVariant,
            OnPrimary = DarkThemeColors.OnPrimary,
            Secondary = DarkThemeColors.Secondary,
            OnSecondary = DarkThemeColors.OnSecondary,
            TextPrimary = DarkThemeColors.TextPrimary,
            TextSecondary = DarkThemeColors.TextSecondary,
            TextTertiary = DarkThemeColors.TextTertiary,
            Success = DarkThemeColors.Success,
            Warning = DarkThemeColors.Warning,
            Error = DarkThemeColors.Error,
            StarYellow = DarkThemeColors.StarYellow,
            CalorieBadge = DarkThemeColors.CalorieBadge,
            BestsellerBadge = DarkThemeColors.BestsellerBadge,
            AvailableBadge = DarkThemeColors.AvailableBadge,
            ImageOverlay = DarkThemeColors.ImageOverlay,
            Divider = DarkThemeColors.Divider,
            PrimaryDark = DarkThemeColors.PrimaryDark,
            PrimaryRed = DarkThemeColors.PrimaryRed,
            MaroonAccent = DarkThemeColors.MaroonAccent
        )
    } else {
        ThemeColors(
            Background = LightThemeColors.Background,
            Surface = LightThemeColors.Surface,
            SurfaceVariant = LightThemeColors.SurfaceVariant,
            CardBackground = LightThemeColors.CardBackground,
            Primary = LightThemeColors.Primary,
            PrimaryVariant = LightThemeColors.PrimaryVariant,
            OnPrimary = LightThemeColors.OnPrimary,
            Secondary = LightThemeColors.Secondary,
            OnSecondary = LightThemeColors.OnSecondary,
            TextPrimary = LightThemeColors.TextPrimary,
            TextSecondary = LightThemeColors.TextSecondary,
            TextTertiary = LightThemeColors.TextTertiary,
            Success = LightThemeColors.Success,
            Warning = LightThemeColors.Warning,
            Error = LightThemeColors.Error,
            StarYellow = LightThemeColors.StarYellow,
            CalorieBadge = LightThemeColors.CalorieBadge,
            BestsellerBadge = LightThemeColors.BestsellerBadge,
            AvailableBadge = LightThemeColors.AvailableBadge,
            ImageOverlay = LightThemeColors.ImageOverlay,
            Divider = LightThemeColors.Divider,
            PrimaryDark = LightThemeColors.PrimaryDark,
            PrimaryRed = LightThemeColors.PrimaryRed,
            MaroonAccent = LightThemeColors.MaroonAccent
        )
    }
}

/**
 * Check if current theme is dark mode
 */
@Composable
fun isDarkMode(): Boolean = LocalThemeState.current.isDarkMode
