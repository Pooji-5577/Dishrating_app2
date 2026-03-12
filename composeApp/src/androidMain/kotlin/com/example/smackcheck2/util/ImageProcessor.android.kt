package com.example.smackcheck2.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android actual implementation — processes image EXIF orientation.
 *
 * Steps:
 *   1. Parse the URI
 *   2. Read EXIF orientation via [ImageOrientationHelper]
 *   3. Rotate the bitmap to match the correct orientation
 *   4. Save the corrected bitmap to cache as JPEG
 *   5. Return the corrected URI + bytes for display and upload
 *
 * Runs on [Dispatchers.IO] to avoid blocking the main thread.
 */

/**
 * Singleton to hold the application context for image processing.
 * Initialized from MainActivity.onCreate().
 */
object ImageProcessorContext {
    var appContext: Context? = null
}

actual suspend fun processImageOrientation(imageUri: String): ProcessedImage =
    withContext(Dispatchers.IO) {
        val context = ImageProcessorContext.appContext
            ?: return@withContext ProcessedImage(error = "App context not initialized")

        try {
            val uri = Uri.parse(imageUri)

            // Step 1: Load and rotate bitmap based on EXIF orientation
            val correctedBitmap = ImageOrientationHelper.rotateImageIfRequired(context, uri)
                ?: return@withContext ProcessedImage(error = "Could not decode image")

            // Step 2: Save the corrected bitmap to a temp file in cache
            val fileName = "corrected_${System.currentTimeMillis()}.jpg"
            val savedFile = ImageOrientationHelper.saveBitmapToCache(context, correctedBitmap, fileName)
                ?: return@withContext ProcessedImage(error = "Could not save corrected image")

            // Step 3: Convert to JPEG bytes (for upload to Supabase Storage)
            val jpegBytes = ImageOrientationHelper.bitmapToJpegBytes(correctedBitmap)

            // Step 4: Build the content URI for the corrected file
            val correctedUri = Uri.fromFile(savedFile).toString()

            ProcessedImage(
                imageBytes = jpegBytes,
                correctedUri = correctedUri
            )
        } catch (e: Exception) {
            println("processImageOrientation error: ${e.message}")
            ProcessedImage(error = e.message ?: "Unknown error processing image")
        }
    }
