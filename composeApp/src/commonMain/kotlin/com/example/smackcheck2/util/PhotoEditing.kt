package com.example.smackcheck2.util

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

enum class PhotoCropMode(val label: String) {
    ORIGINAL("Original"),
    SQUARE("Square"),
    STORY("Story"),
    WIDE("Wide")
}

enum class PhotoFilterPreset(
    val label: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float
) {
    NATURAL("Natural", 0f, 1f, 1f),
    VIVID("Vivid", 0.04f, 1.18f, 1.25f),
    WARM("Warm", 0.03f, 1.08f, 1.08f),
    CRISP("Crisp", 0.02f, 1.28f, 0.95f),
    MONO("Mono", 0.02f, 1.12f, 0f)
}

data class PhotoEditState(
    val cropMode: PhotoCropMode = PhotoCropMode.ORIGINAL,
    val filterPreset: PhotoFilterPreset = PhotoFilterPreset.NATURAL,
    val rotationDegrees: Int = 0,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f
) {
    val hasManualCrop: Boolean
        get() = cropScale != 1f || cropOffsetX != 0f || cropOffsetY != 0f
}

fun PhotoEditState.toColorFilter(): ColorFilter =
    filterPreset.toColorFilter()

fun PhotoFilterPreset.toColorFilter(): ColorFilter {
    val brightnessMatrix = ColorMatrix().apply {
        val b = brightness * 255f
        set(0, 4, b)
        set(1, 4, b)
        set(2, 4, b)
    }

    val contrastMatrix = ColorMatrix().apply {
        val c = contrast
        val t = (1f - c) / 2f * 255f
        set(0, 0, c)
        set(1, 1, c)
        set(2, 2, c)
        set(0, 4, t)
        set(1, 4, t)
        set(2, 4, t)
    }

    val saturationMatrix = ColorMatrix().apply {
        setToSaturation(saturation)
    }

    brightnessMatrix.timesAssign(contrastMatrix)
    brightnessMatrix.timesAssign(saturationMatrix)
    return ColorFilter.colorMatrix(brightnessMatrix)
}

expect suspend fun renderEditedPhoto(
    imageBytes: ByteArray,
    editState: PhotoEditState
): ByteArray
