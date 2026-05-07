package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Clock

/**
 * Repository for file storage operations using Supabase Storage
 */
class StorageRepository {

    private val client = SupabaseClientProvider.client
    private val storage = client.storage

    companion object {
        const val BUCKET_DISH_IMAGES = "dish-images"
        const val BUCKET_PROFILE_IMAGES = "profile-images"
        const val BUCKET_RESTAURANT_IMAGES = "restaurant-images"
        const val BUCKET_STORY_IMAGES = "story-images"
    }

    /**
     * Upload a dish image
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
     * Upload a profile image
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
     * Upload a restaurant image
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

    /**
     * Upload a story image
     * @param userId User ID for organizing files
     * @param imageBytes Image data as ByteArray
     * @param fileName Original file name (for extension)
     * @return Public URL of the uploaded image
     */
    suspend fun uploadStoryImage(
        userId: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<String> {
        return uploadImage(BUCKET_STORY_IMAGES, userId, imageBytes, fileName)
    }

    /**
     * Delete an image from storage
     * @param bucketName Bucket name
     * @param path Full path to the file
     */
    suspend fun deleteImage(bucketName: String, path: String): Result<Unit> {
        return try {
            storage.from(bucketName).delete(path)
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
     * Get public URL for a file
     * @param bucketName Bucket name
     * @param path Path to the file within the bucket
     */
    fun getPublicUrl(bucketName: String, path: String): String {
        return storage.from(bucketName).publicUrl(path)
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
            val path = "$folderName/${timestamp}.$extension"

            storage.from(bucketName).upload(path, imageBytes) {
                upsert = true
            }

            val publicUrl = storage.from(bucketName).publicUrl(path)
            Result.success(publicUrl)
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
