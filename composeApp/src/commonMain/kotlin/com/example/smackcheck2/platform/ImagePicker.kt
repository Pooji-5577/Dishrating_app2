package com.example.smackcheck2.platform

/**
 * Result of an image pick operation
 */
data class ImageResult(
    val uri: String,
    val bytes: ByteArray,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageResult

        if (uri != other.uri) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

/**
 * Expected platform-specific image picker
 */
expect class ImagePicker {
    /**
     * Capture an image using the device camera
     * Returns null if capture is cancelled or fails
     */
    suspend fun captureImage(): ImageResult?

    /**
     * Pick an image from the device gallery
     * Returns null if selection is cancelled or fails
     */
    suspend fun pickFromGallery(): ImageResult?

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(): Boolean
}
