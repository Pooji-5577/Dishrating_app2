package com.example.smackcheck2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.smackcheck2.navigation.SmackCheckNavHost
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.LocalImagePicker
import com.example.smackcheck2.platform.LocalLocationService
import com.example.smackcheck2.platform.LocalPlacesService
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.ui.theme.DarkThemeColors
import com.example.smackcheck2.ui.theme.LightThemeColors
import com.example.smackcheck2.ui.theme.LocalThemeState
import com.example.smackcheck2.ui.theme.SmackCheckTheme
import com.example.smackcheck2.ui.theme.ThemeState

/**
 * Main App composable
 * Sets up the theme and navigation with support for light/dark mode toggle
 */
@Composable
fun App(
    locationService: LocationService? = null,
    imagePicker: ImagePicker? = null,
    placesService: PlacesService? = null
) {
    val themeState = remember { ThemeState(initialDarkMode = true) }

    CompositionLocalProvider(
        LocalThemeState provides themeState,
        LocalLocationService provides locationService,
        LocalImagePicker provides imagePicker,
        LocalPlacesService provides placesService
    ) {
        SmackCheckTheme(darkTheme = themeState.isDarkMode) {
            val backgroundColor = if (themeState.isDarkMode)
                DarkThemeColors.Background else LightThemeColors.Background

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                SmackCheckNavHost()
            }
        }
    }
}