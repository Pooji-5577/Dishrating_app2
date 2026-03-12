package com.example.smackcheck2.util

import androidx.compose.runtime.Composable

/**
 * Expect declaration for a platform-specific image picker composable.
 *
 * On Android: provides real camera capture (TakePicture) and gallery picking
 *             (PickVisualMedia), applies EXIF orientation correction, and
 *             returns the corrected image URI via callbacks.
 * On iOS: shows placeholder buttons (stub).
 *
 * @param onCaptureResult Called with the corrected URI after camera capture
 * @param onGalleryResult Called with the corrected URI after gallery pick
 * @param onCaptureClick  Trigger to request camera capture
 * @param onGalleryClick  Trigger to request gallery pick
 */
@Composable
expect fun ImagePickerEffect(
    onImageReady: (correctedUri: String) -> Unit
): ImagePickerActions

/**
 * Platform actions returned from ImagePickerEffect.
 * UI code calls these to trigger camera / gallery.
 */
data class ImagePickerActions(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)
