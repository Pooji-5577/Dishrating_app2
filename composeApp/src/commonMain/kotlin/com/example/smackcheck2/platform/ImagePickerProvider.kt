package com.example.smackcheck2.platform

import androidx.compose.runtime.compositionLocalOf

/**
 * Composition Local for providing ImagePicker
 * Allows accessing the platform-specific ImagePicker from composables
 */
val LocalImagePicker = compositionLocalOf<ImagePicker?> { null }
