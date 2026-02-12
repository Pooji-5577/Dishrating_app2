package com.example.smackcheck2.platform

import androidx.compose.runtime.compositionLocalOf

/**
 * Composition Local for providing LocationService
 * Allows accessing the platform-specific LocationService from composables
 */
val LocalLocationService = compositionLocalOf<LocationService?> { null }
