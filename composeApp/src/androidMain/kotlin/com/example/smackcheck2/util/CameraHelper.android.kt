package com.example.smackcheck2.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android actual implementation for camera helper utilities.
 */

actual fun createTempPhotoUri(): String? {
    val context = ImageProcessorContext.appContext ?: return null
    return try {
        val photoDir = File(context.cacheDir, "dish_photos")
        photoDir.mkdirs()
        val photoFile = File(photoDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        uri.toString()
    } catch (e: Exception) {
        println("createTempPhotoUri error: ${e.message}")
        null
    }
}

actual fun isCameraAvailable(): Boolean {
    val context = ImageProcessorContext.appContext ?: return false
    val pm = context.packageManager
    return pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
}
