package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

/**
 * iOS implementation of ByteArrayImage
 * Stub - requires native implementation
 */
@Composable
actual fun ByteArrayImage(
    imageBytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    // iOS implementation requires native code to convert bytes to UIImage
    // For now, show placeholder
    Box(modifier = modifier.background(Color.DarkGray))
}
