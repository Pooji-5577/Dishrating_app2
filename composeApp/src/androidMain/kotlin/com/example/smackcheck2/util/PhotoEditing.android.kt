package com.example.smackcheck2.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

actual suspend fun renderEditedPhoto(
    imageBytes: ByteArray,
    editState: PhotoEditState
): ByteArray {
    val source = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return imageBytes
    var working: Bitmap = source
    try {
        working = rotateBitmap(source, editState.rotationDegrees)
        val cropped = cropBitmap(working, editState)
        if (cropped !== working && working !== source && !working.isRecycled) working.recycle()
        working = cropped
        val filtered = applyFilter(working, editState.filterPreset)
        if (filtered !== working && working !== source && !working.isRecycled) working.recycle()
        working = filtered

        val out = ByteArrayOutputStream()
        working.compress(Bitmap.CompressFormat.JPEG, 82, out)
        return out.toByteArray()
    } finally {
        if (working !== source && !working.isRecycled) working.recycle()
        if (!source.isRecycled) source.recycle()
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    val normalized = ((degrees % 360) + 360) % 360
    if (normalized == 0) return bitmap

    val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun cropBitmap(bitmap: Bitmap, editState: PhotoEditState): Bitmap {
    val targetAspect = when (editState.cropMode) {
        PhotoCropMode.ORIGINAL -> if (editState.hasManualCrop) 1f else return bitmap
        PhotoCropMode.SQUARE -> 1f
        PhotoCropMode.STORY -> 9f / 16f
        PhotoCropMode.WIDE -> 16f / 9f
    }

    val currentAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val cropWidth: Int
    val cropHeight: Int
    if (currentAspect > targetAspect) {
        cropHeight = bitmap.height
        cropWidth = (cropHeight * targetAspect).roundToInt().coerceAtLeast(1)
    } else {
        cropWidth = bitmap.width
        cropHeight = (cropWidth / targetAspect).roundToInt().coerceAtLeast(1)
    }

    val scale = editState.cropScale.coerceIn(1f, 4f)
    val manualCropWidth = (cropWidth / scale).roundToInt().coerceIn(1, bitmap.width)
    val manualCropHeight = (cropHeight / scale).roundToInt().coerceIn(1, bitmap.height)
    val maxOffsetX = ((cropWidth - manualCropWidth) / 2f).coerceAtLeast(0f)
    val maxOffsetY = ((cropHeight - manualCropHeight) / 2f).coerceAtLeast(0f)
    val offsetX = (-editState.cropOffsetX * cropWidth).coerceIn(-maxOffsetX, maxOffsetX).roundToInt()
    val offsetY = (-editState.cropOffsetY * cropHeight).coerceIn(-maxOffsetY, maxOffsetY).roundToInt()

    val x = ((bitmap.width - manualCropWidth) / 2 + offsetX)
        .coerceIn(0, bitmap.width - manualCropWidth)
    val y = ((bitmap.height - manualCropHeight) / 2 + offsetY)
        .coerceIn(0, bitmap.height - manualCropHeight)
    return Bitmap.createBitmap(bitmap, x, y, manualCropWidth, manualCropHeight)
}

private fun applyFilter(bitmap: Bitmap, preset: PhotoFilterPreset): Bitmap {
    if (preset == PhotoFilterPreset.NATURAL) return bitmap

    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(buildColorMatrix(preset))
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

private fun buildColorMatrix(preset: PhotoFilterPreset): ColorMatrix {
    val brightness = ColorMatrix().apply {
        val b = preset.brightness * 255f
        set(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val c = preset.contrast
    val t = (1f - c) / 2f * 255f
    val contrast = ColorMatrix(floatArrayOf(
        c, 0f, 0f, 0f, t,
        0f, c, 0f, 0f, t,
        0f, 0f, c, 0f, t,
        0f, 0f, 0f, 1f, 0f
    ))
    val saturation = ColorMatrix().apply { setSaturation(preset.saturation) }
    brightness.postConcat(contrast)
    brightness.postConcat(saturation)
    return brightness
}
