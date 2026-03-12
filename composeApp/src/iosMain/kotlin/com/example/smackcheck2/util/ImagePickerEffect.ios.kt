package com.example.smackcheck2.util

import androidx.compose.runtime.Composable

/**
 * iOS stub for [ImagePickerEffect].
 *
 * Returns no-op actions — iOS camera integration not yet implemented.
 * The capture screen will fall back to its simulated behavior.
 */
@Composable
actual fun ImagePickerEffect(
    onImageReady: (correctedUri: String) -> Unit
): ImagePickerActions {
    return ImagePickerActions(
        launchCamera = {
            // iOS stub — simulate a captured image
            onImageReady("captured_image_ios_${(0..999999).random()}")
        },
        launchGallery = {
            // iOS stub — simulate a gallery selection
            onImageReady("gallery_image_ios_${(0..999999).random()}")
        }
    )
}
