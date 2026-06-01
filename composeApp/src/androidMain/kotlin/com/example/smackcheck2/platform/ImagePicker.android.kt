package com.example.smackcheck2.platform

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.smackcheck2.util.ImageOrientationHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

private const val TAG = "ImagePicker"

/**
 * Android implementation of ImagePicker using Activity Result APIs
 */
actual class ImagePicker(
    private val activity: ComponentActivity
) {
    private val context: Context get() = activity

    private var captureResultCallback: ((Uri?) -> Unit)? = null
    private var galleryResultCallback: ((Uri?) -> Unit)? = null
    private var multipleGalleryResultCallback: ((List<Uri>) -> Unit)? = null
    private var pendingCameraUri: Uri? = null

    private val takePictureLauncher: ActivityResultLauncher<Uri> =
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            Log.d(TAG, "Camera capture result: success=$success, uri=$pendingCameraUri")
            val callback = captureResultCallback
            captureResultCallback = null
            if (success && pendingCameraUri != null) {
                callback?.invoke(pendingCameraUri)
            } else {
                callback?.invoke(null)
            }
        }

    private val pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            Log.d(TAG, "Gallery pick result: uri=$uri")
            val callback = galleryResultCallback
            galleryResultCallback = null
            callback?.invoke(uri)
        }
    
    private val pickMultipleMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        activity.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            Log.d(TAG, "Multiple gallery pick result: ${uris.size} images selected")
            val callback = multipleGalleryResultCallback
            multipleGalleryResultCallback = null
            callback?.invoke(uris)
        }

    actual suspend fun captureImage(): ImageResult? {
        Log.d(TAG, "captureImage called")

        if (!hasCameraPermission()) {
            Log.e(TAG, "Camera permission not granted")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                // Create temporary file for camera output
                val imageFile = createImageFile()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                pendingCameraUri = uri
                Log.d(TAG, "Created camera URI: $uri")

                captureResultCallback = { resultUri ->
                    if (resultUri != null) {
                        try {
                            val normalized = normalizeImageResult(resultUri, forcePortrait = true)
                            val bytes = normalized.bytes
                            val mimeType = normalized.mimeType
                            Log.d(TAG, "Image captured+normalized: ${bytes.size} bytes, mimeType=$mimeType")
                            continuation.resume(normalized)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading captured image", e)
                            continuation.resume(null)
                        }
                    } else {
                        Log.d(TAG, "Camera capture cancelled or failed")
                        continuation.resume(null)
                    }
                }

                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Error launching camera", e)
                continuation.resume(null)
            }
        }
    }

    actual suspend fun pickFromGallery(): ImageResult? {
        Log.d(TAG, "pickFromGallery called")

        return suspendCancellableCoroutine { continuation ->
            try {
                galleryResultCallback = { uri ->
                    if (uri != null) {
                        try {
                            val normalized = normalizeImageResult(uri, forcePortrait = false)
                            val bytes = normalized.bytes
                            val mimeType = normalized.mimeType
                            Log.d(TAG, "Image picked+normalized: ${bytes.size} bytes, mimeType=$mimeType")
                            continuation.resume(normalized)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading picked image", e)
                            continuation.resume(null)
                        }
                    } else {
                        Log.d(TAG, "Gallery pick cancelled")
                        continuation.resume(null)
                    }
                }

                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error launching gallery picker", e)
                continuation.resume(null)
            }
        }
    }
    
    actual suspend fun pickMultipleFromGallery(maxImages: Int): List<ImageResult> {
        Log.d(TAG, "pickMultipleFromGallery called, maxImages=$maxImages")

        return suspendCancellableCoroutine { continuation ->
            try {
                multipleGalleryResultCallback = { uris ->
                    if (uris.isNotEmpty()) {
                        try {
                            val results = uris.take(maxImages).mapNotNull { uri ->
                                try {
                                    val normalized = normalizeImageResult(uri, forcePortrait = false)
                                    Log.d(
                                        TAG,
                                        "Image picked+normalized: ${normalized.bytes.size} bytes, aiBytes=${normalized.aiBytes.size}, mimeType=${normalized.mimeType}"
                                    )
                                    normalized
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error reading picked image: $uri", e)
                                    null
                                }
                            }
                            Log.d(TAG, "Multiple images picked: ${results.size} images")
                            continuation.resume(results)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing picked images", e)
                            continuation.resume(emptyList())
                        }
                    } else {
                        Log.d(TAG, "Multiple gallery pick cancelled or no selection")
                        continuation.resume(emptyList())
                    }
                }

                pickMultipleMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error launching multiple gallery picker", e)
                continuation.resume(emptyList())
            }
        }
    }

    actual fun hasCameraPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "hasCameraPermission: $hasPermission")
        return hasPermission
    }

    private fun createImageFile(): File {
        val imagesDir = File(context.cacheDir, "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "captured_${System.currentTimeMillis()}.jpg")
        Log.d(TAG, "Created image file: ${imageFile.absolutePath}")
        return imageFile
    }

    private fun readBytesFromUri(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw Exception("Failed to read from URI: $uri")
    }

    /**
     * Normalize image orientation and return a JPEG-backed ImageResult.
     * Falls back to original bytes when EXIF/bitmap decode fails.
     */
    private fun normalizeImageResult(uri: Uri, forcePortrait: Boolean): ImageResult {
        val rotatedBitmap = ImageOrientationHelper.rotateImageIfRequired(context, uri)
        if (rotatedBitmap != null) {
            val normalizedBitmap = if (forcePortrait) {
                ImageOrientationHelper.forcePortrait(rotatedBitmap)
            } else {
                rotatedBitmap
            }
            var aiBitmap: android.graphics.Bitmap? = null
            try {
                val bytes = ImageOrientationHelper.bitmapToJpegBytes(normalizedBitmap)
                aiBitmap = ImageOrientationHelper.scaleToMaxDimension(normalizedBitmap, 512)
                val aiBytes = ImageOrientationHelper.bitmapToJpegBytes(aiBitmap, quality = 58)
                val normalizedFile = File(context.cacheDir, "images/normalized_${System.currentTimeMillis()}.jpg")
                normalizedFile.parentFile?.mkdirs()
                normalizedFile.writeBytes(bytes)
                return ImageResult(
                    uri = Uri.fromFile(normalizedFile).toString(),
                    bytes = bytes,
                    mimeType = "image/jpeg",
                    aiBytes = aiBytes,
                    aiMimeType = "image/jpeg"
                )
            } finally {
                aiBitmap?.let {
                    if (it !== normalizedBitmap && !it.isRecycled) it.recycle()
                }
                if (normalizedBitmap !== rotatedBitmap && !normalizedBitmap.isRecycled) {
                    normalizedBitmap.recycle()
                }
                if (!rotatedBitmap.isRecycled) rotatedBitmap.recycle()
            }
        }

        val bytes = readBytesFromUri(uri)
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        return ImageResult(uri.toString(), bytes, mimeType)
    }
}
