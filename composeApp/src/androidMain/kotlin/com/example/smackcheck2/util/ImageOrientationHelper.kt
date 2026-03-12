package com.example.smackcheck2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * Helper utility for fixing image orientation issues on Android.
 *
 * Problem: Many phone cameras (especially Samsung, Pixel, etc.) save portrait
 * photos with EXIF orientation metadata rather than physically rotating the pixels.
 * When the app reads the raw bitmap, it appears rotated (portrait shows as landscape).
 *
 * Solution: Read the EXIF orientation tag and rotate the bitmap accordingly
 * before displaying or uploading.
 *
 * Usage:
 *   val fixedBitmap = ImageOrientationHelper.rotateImageIfRequired(context, uri)
 */
object ImageOrientationHelper {

    /**
     * Load a bitmap from a URI and rotate it to match its EXIF orientation.
     *
     * This is the primary entry point — use this for both camera captures
     * and gallery picks.
     *
     * @param context  Android context for ContentResolver access
     * @param imageUri URI of the image (content:// or file://)
     * @return Correctly oriented bitmap, or null if the image can't be read
     */
    fun rotateImageIfRequired(context: Context, imageUri: Uri): Bitmap? {
        return try {
            // Step 1: Read the EXIF orientation tag from the image metadata
            val orientation = getExifOrientation(context, imageUri)

            // Step 2: Decode the bitmap from the URI
            val bitmap = decodeBitmapFromUri(context, imageUri) ?: return null

            // Step 3: Apply rotation based on EXIF orientation
            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            println("ImageOrientationHelper: Error processing image: ${e.message}")
            // Fallback: try to return the unrotated bitmap
            try {
                decodeBitmapFromUri(context, imageUri)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Rotate a bitmap from a file path based on its EXIF data.
     *
     * @param filePath Absolute path to the image file
     * @return Correctly oriented bitmap, or null on failure
     */
    fun rotateImageIfRequired(filePath: String): Bitmap? {
        return try {
            val orientation = getExifOrientation(filePath)
            val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            println("ImageOrientationHelper: Error processing file: ${e.message}")
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EXIF ORIENTATION READING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Read the EXIF orientation tag from an image URI.
     *
     * Uses [ExifInterface] which supports content:// URIs via InputStream.
     *
     * @return One of the ExifInterface.ORIENTATION_* constants
     */
    private fun getExifOrientation(context: Context, imageUri: Uri): Int {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(imageUri)
                ?: return ExifInterface.ORIENTATION_NORMAL

            inputStream.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            println("ImageOrientationHelper: Could not read EXIF from URI: ${e.message}")
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Read the EXIF orientation tag from a file path.
     */
    private fun getExifOrientation(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            println("ImageOrientationHelper: Could not read EXIF from file: ${e.message}")
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BITMAP ROTATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rotate a bitmap based on the EXIF orientation constant.
     *
     * Handles all standard orientations:
     *   - ROTATE_90  (portrait photo taken with phone held normally)
     *   - ROTATE_180 (upside-down)
     *   - ROTATE_270 (landscape rotated the other way)
     *   - FLIP variants (mirrored selfies, etc.)
     *
     * @param bitmap      The original (potentially mis-oriented) bitmap
     * @param orientation EXIF orientation constant
     * @return A new, correctly-oriented bitmap (or the original if no rotation needed)
     */
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            // ── No rotation needed ──
            ExifInterface.ORIENTATION_NORMAL ->
                return bitmap

            // ── Horizontal flip ──
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                matrix.setScale(-1f, 1f)

            // ── 180° rotation ──
            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.setRotate(180f)

            // ── Vertical flip ──
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            // ── Transposed (flipped + 270° rotation) ──
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            // ── 90° clockwise rotation (most common for portrait photos) ──
            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.setRotate(90f)

            // ── Transverse (flipped + 90° rotation) ──
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            // ── 270° clockwise rotation ──
            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.setRotate(-90f)

            // ── Unknown orientation — return as-is ──
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height,
                matrix, true
            )
            // Recycle the original if a new bitmap was created
            if (rotated !== bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            println("ImageOrientationHelper: OOM during rotation, returning original")
            bitmap
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BITMAP DECODING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Decode a bitmap from a content URI with downsampling for large images.
     *
     * First pass: read dimensions only (inJustDecodeBounds = true).
     * Second pass: decode with calculated sample size to avoid OOM.
     *
     * @param context  Android context
     * @param uri      Image URI
     * @param maxDimension Maximum width or height in pixels (default 2048)
     * @return Decoded bitmap, or null on failure
     */
    private fun decodeBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? {
        // First pass: get image dimensions without loading pixels
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // Calculate sample size to stay within maxDimension
        options.inSampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight, maxDimension
        )
        options.inJustDecodeBounds = false

        // Second pass: decode the actual bitmap
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    /**
     * Calculate the largest power-of-2 sample size that keeps both
     * dimensions within the max.
     */
    private fun calculateInSampleSize(
        rawWidth: Int, rawHeight: Int, maxDimension: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > maxDimension || rawWidth > maxDimension) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while (halfHeight / inSampleSize >= maxDimension &&
                halfWidth / inSampleSize >= maxDimension
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONVERSION UTILITIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compress a bitmap to JPEG bytes for uploading to Supabase Storage.
     *
     * @param bitmap  The correctly-oriented bitmap
     * @param quality JPEG compression quality (0–100, default 85)
     * @return JPEG byte array
     */
    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Save a correctly-oriented bitmap to a file in the app's cache directory.
     * Useful for getting a file path to upload or share.
     *
     * @param context  Android context
     * @param bitmap   The bitmap to save
     * @param fileName Name for the saved file
     * @return The saved File, or null on failure
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "dish_photos")
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file
        } catch (e: Exception) {
            println("ImageOrientationHelper: Error saving bitmap: ${e.message}")
            null
        }
    }
}
