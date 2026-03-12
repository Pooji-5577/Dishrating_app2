package com.example.smackcheck2.util

/**
 * Cross-platform interface for camera capture and gallery picking.
 *
 * On Android: uses ActivityResultContracts (TakePicture + PickVisualMedia)
 *             with EXIF orientation correction before returning the URI.
 * On iOS: stub (returns simulated data for now).
 *
 * The results are returned via callbacks with the corrected image URI.
 */

/**
 * Expect function to create a temporary photo file URI for camera capture.
 * On Android this creates a FileProvider URI; on iOS it returns a dummy.
 */
expect fun createTempPhotoUri(): String?

/**
 * Expect function to check if real camera is available on this platform.
 */
expect fun isCameraAvailable(): Boolean
