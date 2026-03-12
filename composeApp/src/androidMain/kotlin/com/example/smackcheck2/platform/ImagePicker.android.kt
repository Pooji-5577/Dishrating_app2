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
                            val bytes = readBytesFromUri(resultUri)
                            val mimeType = context.contentResolver.getType(resultUri) ?: "image/jpeg"
                            Log.d(TAG, "Image captured: ${bytes.size} bytes, mimeType=$mimeType")
                            continuation.resume(ImageResult(resultUri.toString(), bytes, mimeType))
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
                            val bytes = readBytesFromUri(uri)
                            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                            Log.d(TAG, "Image picked: ${bytes.size} bytes, mimeType=$mimeType")
                            continuation.resume(ImageResult(uri.toString(), bytes, mimeType))
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
                                    val bytes = readBytesFromUri(uri)
                                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                    Log.d(TAG, "Image picked: ${bytes.size} bytes, mimeType=$mimeType")
                                    ImageResult(uri.toString(), bytes, mimeType)
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
}
