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
            Result.failure(e)
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
            val extension = fileName.substringAfterLast(".", "jpg")
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val path = "$folderName/${timestamp}.$extension"

            storage.from(bucketName).upload(path, imageBytes) {
                upsert = true
            }

            val publicUrl = storage.from(bucketName).publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
