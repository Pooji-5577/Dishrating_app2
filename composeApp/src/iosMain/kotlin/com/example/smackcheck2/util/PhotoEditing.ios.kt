package com.example.smackcheck2.util

actual suspend fun renderEditedPhoto(
    imageBytes: ByteArray,
    editState: PhotoEditState
): ByteArray = imageBytes
