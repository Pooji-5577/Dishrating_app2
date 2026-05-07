package com.example.smackcheck2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.navigation.SmackCheckNavHost
import com.example.smackcheck2.platform.GeofencingService
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.LocalGeofencingService
import com.example.smackcheck2.platform.LocalImagePicker
import com.example.smackcheck2.platform.LocalLocationService
import com.example.smackcheck2.platform.LocalPlacesService
import com.example.smackcheck2.platform.LocalShareService
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.PreferencesManager
import com.example.smackcheck2.platform.ShareService
import com.example.smackcheck2.ui.theme.LightThemeColors
import com.example.smackcheck2.ui.theme.LocalThemeState
import com.example.smackcheck2.ui.theme.SmackCheckTheme
import com.example.smackcheck2.ui.theme.ThemeState
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.httpUrlFetcher
import io.kamel.core.config.stringMapper
import io.kamel.core.config.urlMapper
import io.kamel.core.config.uriMapper
import io.kamel.core.config.fileFetcher
import io.kamel.core.config.fileUrlFetcher
import io.kamel.image.config.LocalKamelConfig
import io.kamel.image.config.imageBitmapDecoder

/**
 * Main App composable
 * Sets up the theme and navigation with support for light/dark mode toggle
 */
@Composable
fun App(
    preferencesManager: PreferencesManager,
    locationService: LocationService? = null,
    imagePicker: ImagePicker? = null,
    placesService: PlacesService? = null,
    shareService: ShareService? = null,
    geofencingService: GeofencingService? = null
) {
    val preferencesRepository = remember { PreferencesRepository(preferencesManager) }
    val scope = rememberCoroutineScope()
    val themeState = remember {
        ThemeState(
            initialDarkMode = false,
            preferencesRepository = preferencesRepository,
            coroutineScope = scope
        )
    }

    val kamelConfig = remember {
        KamelConfig {
            imageBitmapCacheSize = 100
            imageVectorCacheSize = 100
            svgCacheSize = 100
            animatedImageCacheSize = 100
            stringMapper()
            urlMapper()
            uriMapper()
            fileFetcher()
            fileUrlFetcher()
            httpUrlFetcher {
                httpCache(10 * 1024 * 1024)
            }
            imageBitmapDecoder()
        }
    }

    CompositionLocalProvider(
        LocalKamelConfig provides kamelConfig,
        LocalThemeState provides themeState,
        LocalLocationService provides locationService,
        LocalImagePicker provides imagePicker,
        LocalPlacesService provides placesService,
        LocalShareService provides shareService,
        LocalGeofencingService provides geofencingService
    ) {
        SmackCheckTheme(darkTheme = false) {
            val backgroundColor = LightThemeColors.Background

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                SmackCheckNavHost(preferencesRepository = preferencesRepository)
            }
        }
    }
}