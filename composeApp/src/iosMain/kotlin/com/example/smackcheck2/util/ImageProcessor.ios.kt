package com.example.smackcheck2.util

/**
 * iOS stub implementation for image orientation processing.
 *
 * iOS handles EXIF orientation automatically in most cases via UIImage.
 * This stub returns the original URI unchanged.
 * To be enhanced when iOS camera integration is built out.
 */
actual suspend fun processImageOrientation(imageUri: String): ProcessedImage {
    // iOS UIImage typically auto-corrects orientation,
    // so we return the original URI as-is for now.
    return ProcessedImage(
        correctedUri = imageUri,
        error = null
    )
}
