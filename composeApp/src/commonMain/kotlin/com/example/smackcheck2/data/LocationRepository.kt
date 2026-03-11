package com.example.smackcheck2.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class for updating user location in the profiles table.
 * Matches the columns in your Supabase 'profiles' table.
 */
@Serializable
data class LocationUpdate(
    @SerialName("last_location")
    val lastLocation: String,
    @SerialName("current_latitude")
    val currentLatitude: Double? = null,
    @SerialName("current_longitude")
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
    
    private val client = SupabaseClient.client
    
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
            client.from("profiles")
                .update(LocationUpdate(
                    lastLocation = city,
                    currentLatitude = latitude,
                    currentLongitude = longitude
                )) {
                    filter {
                        eq("id", userId)
                    }
                }
            println("✅ Location updated in Supabase: $city ($latitude, $longitude)")
            true
        } catch (e: Exception) {
            println("❌ Failed to update location in Supabase: ${e.message}")
            false
        }
    }
    
    /**
     * Get the current authenticated user's ID.
     * Returns null if not authenticated.
     */
    suspend fun getCurrentUserId(): String? {
        return try {
            client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            println("❌ Failed to get current user: ${e.message}")
            null
        }
    }
    
    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            println("❌ Failed to get user profile: ${e.message}")
            null
        }
    }
    
    /**
     * Get all restaurants from a specific city
     */
    suspend fun getRestaurantsByCity(city: String): List<SupabaseRestaurant> {
        return try {
            client.from("restaurants")
                .select {
                    filter {
                        eq("city", city)
                    }
                }
                .decodeList<SupabaseRestaurant>()
        } catch (e: Exception) {
            println("❌ Failed to get restaurants: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get all dishes (optionally filtered by restaurant)
     */
    suspend fun getDishes(restaurantId: String? = null): List<SupabaseDish> {
        return try {
            if (restaurantId != null) {
                client.from("dishes")
                    .select {
                        filter {
                            eq("restaurant_id", restaurantId)
                        }
                    }
                    .decodeList<SupabaseDish>()
            } else {
                client.from("dishes")
                    .select()
                    .decodeList<SupabaseDish>()
            }
        } catch (e: Exception) {
            println("❌ Failed to get dishes: ${e.message}")
            emptyList()
        }
    }
}
