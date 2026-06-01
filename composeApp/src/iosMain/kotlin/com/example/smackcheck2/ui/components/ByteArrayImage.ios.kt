package com.example.smackcheck2.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.skia.Image

/**
 * iOS implementation of ByteArrayImage
 */
@Composable
actual fun ByteArrayImage(
    imageBytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    colorFilter: ColorFilter?
) {
    val imageBitmap = remember(imageBytes) {
        runCatching {
            Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            colorFilter = colorFilter
        )
    }
}
