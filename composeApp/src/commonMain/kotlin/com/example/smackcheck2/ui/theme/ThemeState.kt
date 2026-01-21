package com.example.smackcheck2.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme state manager for toggling between light and dark themes
 */
class ThemeState(initialDarkMode: Boolean = true) {
    var isDarkMode by mutableStateOf(initialDarkMode)
        private set
    
    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }
    
    fun updateDarkMode(enabled: Boolean) {
        isDarkMode = enabled
    }
}

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
    val Divider: Color
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
            Divider = DarkThemeColors.Divider
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
            Divider = LightThemeColors.Divider
        )
    }
}

/**
 * Check if current theme is dark mode
 */
@Composable
fun isDarkMode(): Boolean = LocalThemeState.current.isDarkMode
