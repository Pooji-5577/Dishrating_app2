package com.example.smackcheck2.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

/**
 * Android actual implementation of [ImagePickerEffect].
 *
 * Registers two activity result launchers:
 *   1. TakePicture — launches the camera, saves photo to a FileProvider URI
 *   2. PickVisualMedia — opens the system photo picker
 *
 * After an image is captured/selected, it:
 *   1. Reads the EXIF orientation
 *   2. Rotates the bitmap if needed
 *   3. Saves the corrected image to cache
 *   4. Returns the corrected URI via the callback
 *
 * This ensures portrait photos remain portrait throughout the app.
 */
@Composable
actual fun ImagePickerEffect(
    onImageReady: (correctedUri: String) -> Unit
): ImagePickerActions {
    val context = ImageProcessorContext.appContext
        ?: return ImagePickerActions(launchCamera = {}, launchGallery = {})

    val scope = rememberCoroutineScope()

    // ── Track the temp URI for camera capture ──
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // ── Camera launcher: TakePicture ──
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            val capturedUri = pendingCameraUri!!
            scope.launch {
                // Apply EXIF orientation correction
                val processed = processImageOrientation(capturedUri.toString())
                val resultUri = processed.correctedUri ?: capturedUri.toString()
                onImageReady(resultUri)
            }
        }
    }

    // ── Gallery launcher: PickVisualMedia ──
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                // Apply EXIF orientation correction
                val processed = processImageOrientation(uri.toString())
                val resultUri = processed.correctedUri ?: uri.toString()
                onImageReady(resultUri)
            }
        }
    }

    return ImagePickerActions(
        launchCamera = {
            try {
                // Create a temp file for the camera to write to
                val photoDir = File(context.cacheDir, "dish_photos")
                photoDir.mkdirs()
                val photoFile = File(photoDir, "capture_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                println("launchCamera error: ${e.message}")
            }
        },
        launchGallery = {
            try {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (e: Exception) {
                println("launchGallery error: ${e.message}")
            }
        }
    )
}
