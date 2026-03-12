package com.example.smackcheck2.util

/**
 * Cross-platform image processing result.
 *
 * On Android: contains the EXIF-corrected JPEG bytes + the corrected file URI.
 * On iOS: will contain the correctly-oriented image bytes (stub for now).
 *
 * @property imageBytes  JPEG byte array of the correctly-oriented image
 * @property correctedUri  URI/path to the corrected image file (for display)
 * @property error  Error message if processing failed
 */
data class ProcessedImage(
    val imageBytes: ByteArray? = null,
    val correctedUri: String? = null,
    val error: String? = null
)

/**
 * Expect declaration for processing a captured/selected image.
 *
 * On Android: reads EXIF orientation, rotates the bitmap, saves the corrected
 *             version, and returns JPEG bytes ready for upload.
 * On iOS:     stub (returns the original URI as-is).
 *
 * @param imageUri  The URI string of the captured/selected image
 * @return ProcessedImage with corrected bytes and URI
 */
expect suspend fun processImageOrientation(imageUri: String): ProcessedImage
