package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Repository for file storage operations via the custom backend API.
 * All uploads go through ApiClient.uploadImage() → POST /api/storage/upload.
 * All deletes go through DELETE /api/storage/delete with a JSON body.
 */
class StorageRepository {

    companion object {
        const val BUCKET_DISH_IMAGES = "dish"
        const val BUCKET_PROFILE_IMAGES = "profile"
        const val BUCKET_RESTAURANT_IMAGES = "restaurant"
        const val BUCKET_STORY_IMAGES = "story"
        const val BUCKET_RECEIPT_IMAGES = "receipt"
    }

    @Serializable
    private data class StorageDeleteRequest(val bucket: String, val path: String)

    /**
     * Upload a dish image.
     * @param userId User ID for organizing files
     * @param imageBytes Image data as ByteArray
     * @param fileName Original file name (for extension)
     * @return Public URL of the uploaded image
     */
    suspend fun uploadDishImage(
        userId: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return uploadImage(BUCKET_DISH_IMAGES, userId, imageBytes, fileName)
    }

    /**
     * Upload a profile image.
     * @param userId User ID
     * @param imageBytes Image data as ByteArray
     * @param fileName Original file name (for extension)
     * @return Public URL of the uploaded image
     */
    suspend fun uploadProfileImage(
        userId: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return uploadImage(BUCKET_PROFILE_IMAGES, userId, imageBytes, fileName)
    }

    /**
     * Upload a restaurant image.
     * @param restaurantId Restaurant ID for organizing files
     * @param imageBytes Image data as ByteArray
     * @param fileName Original file name (for extension)
     * @return Public URL of the uploaded image
     */
    suspend fun uploadRestaurantImage(
        restaurantId: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return uploadImage(BUCKET_RESTAURANT_IMAGES, restaurantId, imageBytes, fileName)
    }

    suspend fun uploadStoryImage(
        userId: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return uploadImage(BUCKET_STORY_IMAGES, userId, imageBytes, fileName)
    }

    suspend fun uploadReceiptImage(
        userId: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return uploadImage(BUCKET_RECEIPT_IMAGES, userId, imageBytes, fileName)
    }

    /**
     * Delete an image from storage via the backend.
     * @param bucketName Bucket name (use the BUCKET_* constants)
     * @param path Full path to the file within the bucket
     */
    suspend fun deleteImage(bucketName: String, path: String): Result<Unit> {
        return try {
            ApiClient.deleteWithBody(
                "storage/delete",
                StorageDeleteRequest(bucket = bucketName, path = path)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("not found", ignoreCase = true) == true ->
                    "Image not found."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                    "You don't have permission to delete this image."
                else -> "Failed to delete image. Please try again later."
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Returns a constructed public URL for an already-uploaded file.
     * This is a local helper; the authoritative URL is always the one
     * returned by uploadImage / ApiClient.uploadImage.
     */
    fun getPublicUrl(bucketName: String, path: String): String {
        return "${com.example.smackcheck2.data.BackendConfig.BACKEND_URL}/api/storage/$bucketName/$path"
    }

    private suspend fun uploadImage(
        bucketName: String,
        folderName: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return try {
            // Validate image size (max 10MB)
            if (imageBytes.size > 10 * 1024 * 1024) {
                return Result.failure(Exception("Image is too large. Maximum size is 10MB."))
            }

            val extension = fileName.substringAfterLast(".", "jpg")
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val storageName = "$folderName/$timestamp.$extension"

            val mimeType = when (extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }

            val response = ApiClient.uploadImage(
                bucket = bucketName,
                imageBytes = imageBytes,
                fileName = storageName,
                mimeType = mimeType
            )

            Result.success(response.url)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("size", ignoreCase = true) == true ->
                    "Image is too large. Maximum size is 10MB."
                e.message?.contains("type", ignoreCase = true) == true ||
                e.message?.contains("format", ignoreCase = true) == true ->
                    "Invalid image format. Please use JPG, PNG, or WebP."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("quota", ignoreCase = true) == true ->
                    "Storage limit reached. Please contact support."
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                    "Session expired. Please sign in again."
                else -> "Failed to upload image. Please try again later."
            }
            Result.failure(Exception(message))
        }
    }
}
