package com.example.smackcheck2.data

import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.smackcheck2.util.Logger

/**
 * Data class for updating user location in the profiles table.
 * Matches the columns in your Supabase 'profiles' table.
 */
@Serializable
data class LocationUpdate(
    @SerialName("last_location")
    val lastLocation: String,
    @SerialName("latitude")
    val currentLatitude: Double? = null,
    @SerialName("longitude")
    val currentLongitude: Double? = null
)

/**
 * Data class for fetching user profile from Supabase.
 */
@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    @SerialName("profile_photo_url")
    val profilePhotoUrl: String? = null,
    val xp: Int? = null,
    val level: Int? = null,
    @SerialName("streak_count")
    val streakCount: Int? = null,
    @SerialName("last_location")
    val lastLocation: String? = null,
    val bio: String? = null
)

/**
 * Data class for dishes from Supabase
 */
@Serializable
data class SupabaseDish(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("restaurant_id")
    val restaurantId: String? = null
)

/**
 * Data class for restaurants from Supabase
 */
@Serializable
data class SupabaseRestaurant(
    val id: String,
    val name: String,
    val city: String? = null,
    val cuisine: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("average_rating")
    val averageRating: Double? = null,
    @SerialName("rating_count")
    val ratingCount: Int? = null
)

/**
 * Repository for location-related database operations.
 * Handles syncing user location to Supabase.
 */
class LocationRepository {
    
    private val auth get() = SupabaseClient.client.auth
    
    /**
     * Update the current user's location in the profiles table.
     * 
     * @param userId The user's UUID from Supabase Auth
     * @param city The detected city name
     * @param latitude GPS latitude (optional)
     * @param longitude GPS longitude (optional)
     * @return true if update was successful, false otherwise
     */
    suspend fun updateUserLocation(
        userId: String, 
        city: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Boolean {
        return try {
            ApiClient.put<LocationUpdate, UserProfile>(
                "auth/profile",
                LocationUpdate(
                    lastLocation = city,
                    currentLatitude = latitude,
                    currentLongitude = longitude
                )
            )
            Logger.d("LocationRepository", "Location updated via backend: $city ($latitude, $longitude)")
            true
        } catch (e: Exception) {
            Logger.e("LocationRepository", "Failed to update location via backend: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get the current authenticated user's ID.
     * Returns null if not authenticated.
     */
    suspend fun getCurrentUserId(): String? {
        return try {
            auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Logger.e("LocationRepository", "Failed to get current user: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            ApiClient.get<UserProfile>("auth/users/$userId")
        } catch (e: Exception) {
            Logger.e("LocationRepository", "Failed to get user profile: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get all restaurants from a specific city
     */
    suspend fun getRestaurantsByCity(city: String): List<SupabaseRestaurant> {
        return try {
            ApiClient.get<List<SupabaseRestaurant>>(
                "restaurants/by-city",
                mapOf("city" to city)
            )
        } catch (e: Exception) {
            Logger.e("LocationRepository", "Failed to get restaurants: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get all dishes (optionally filtered by restaurant)
     */
    suspend fun getDishes(restaurantId: String? = null): List<SupabaseDish> {
        return try {
            ApiClient.get<List<SupabaseDish>>(
                "dishes",
                mapOf("restaurantId" to restaurantId)
            )
        } catch (e: Exception) {
            Logger.e("LocationRepository", "Failed to get dishes: ${e.message}", e)
            emptyList()
        }
    }
}
