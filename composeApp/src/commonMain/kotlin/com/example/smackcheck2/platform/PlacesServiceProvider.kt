package com.example.smackcheck2.platform

import androidx.compose.runtime.compositionLocalOf

/**
 * Composition local for PlacesService
 * Allows PlacesService to be accessed throughout the composable hierarchy
 */
val LocalPlacesService = compositionLocalOf<PlacesService?> { null }
