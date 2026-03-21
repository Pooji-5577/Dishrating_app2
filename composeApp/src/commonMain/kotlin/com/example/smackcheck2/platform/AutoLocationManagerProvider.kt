package com.example.smackcheck2.platform

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for AutoLocationManager
 * Provides access to the automatic location detection manager across the app
 */
val LocalAutoLocationManager = compositionLocalOf<AutoLocationManager?> { null }
