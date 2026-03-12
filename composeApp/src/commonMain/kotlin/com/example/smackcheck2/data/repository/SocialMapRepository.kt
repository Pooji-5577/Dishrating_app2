package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.model.MapUserMarker
import com.example.smackcheck2.model.UserMapProfile
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository for Social Map features - fetches nearby users with dish posts
 */
class SocialMapRepository {

    private val client = SupabaseClientProvider.client
    private val postgrest = client.postgrest

    /**
     * Get nearby users who have posted dishes recently
     * Uses the Supabase RPC function get_nearby_users_with_dishes
     */
    suspend fun getNearbyUsersWithDishes(
        userLat: Double,
        userLng: Double,
        radiusMeters: Int = 3000,
        hoursAgo: Int = 168 // 7 days
    ): Result<List<MapUserMarker>> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
            
            val result = postgrest.rpc(
                function = "get_nearby_users_with_dishes",
                parameters = NearbyUsersParams(
                    p_user_lat = userLat,
                    p_user_lng = userLng,
                    p_radius_meters = radiusMeters,
                    p_hours_ago = hoursAgo
                )
            ).decodeList<NearbyUserDto>()

            val markers = result.map { dto ->
                MapUserMarker(
                    userId = dto.user_id,
                    username = dto.username ?: "Unknown",
                    avatarUrl = dto.avatar_url,
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0,
                    distanceMeters = dto.distance_meters ?: 0.0,
                    latestRatingId = dto.latest_rating_id,
                    latestDishId = dto.latest_dish_id,
                    latestDishName = dto.latest_dish_name,
                    latestDishImage = dto.latest_dish_image,
                    latestRating = dto.latest_rating,
                    latestRestaurantId = dto.latest_restaurant_id,
                    latestRestaurantName = dto.latest_restaurant_name,
                    latestPostTime = dto.latest_post_time?.let { parseTimestamp(it) },
                    isCurrentUser = dto.user_id == currentUserId
                )
            }

            Result.success(markers)
        } catch (e: Exception) {
            println("SocialMapRepository: Error fetching nearby users: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get current user's map profile
     */
    suspend fun getCurrentUserMapProfile(): Result<UserMapProfile?> {
        return try {
            val result = postgrest.rpc(
                function = "get_current_user_map_profile"
            ).decodeList<UserMapProfileDto>()

            val profile = result.firstOrNull()?.let { dto ->
                UserMapProfile(
                    userId = dto.user_id,
                    username = dto.username ?: "Unknown",
                    avatarUrl = dto.avatar_url,
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    locationSharingEnabled = dto.location_sharing_enabled ?: true,
                    totalRatings = dto.total_ratings?.toInt() ?: 0,
                    latestRatingId = dto.latest_rating_id,
                    latestDishName = dto.latest_dish_name,
                    latestDishImage = dto.latest_dish_image
                )
            }

            Result.success(profile)
        } catch (e: Exception) {
            println("SocialMapRepository: Error fetching user profile: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update user's current location
     */
    suspend fun updateUserLocation(latitude: Double, longitude: Double): Result<Unit> {
        return try {
            postgrest.rpc(
                function = "update_user_location",
                parameters = UpdateLocationParams(
                    p_latitude = latitude,
                    p_longitude = longitude
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            println("SocialMapRepository: Error updating location: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Toggle location sharing on/off
     */
    suspend fun toggleLocationSharing(enabled: Boolean): Result<Unit> {
        return try {
            postgrest.rpc(
                function = "toggle_location_sharing",
                parameters = ToggleLocationParams(p_enabled = enabled)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            println("SocialMapRepository: Error toggling location sharing: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get nearby dish posts using PostGIS (if available)
     */
    suspend fun getNearbyDishPostsPostGIS(
        userLat: Double,
        userLng: Double,
        radiusMeters: Int = 3000,
        limit: Int = 50
    ): Result<List<MapUserMarker>> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
            
            val result = postgrest.rpc(
                function = "get_nearby_dish_posts_postgis",
                parameters = NearbyPostsPostGISParams(
                    p_user_lat = userLat,
                    p_user_lng = userLng,
                    p_radius_meters = radiusMeters,
                    p_limit = limit
                )
            ).decodeList<NearbyDishPostDto>()

            // Group by user and take the latest post per user
            val markersByUser = result.groupBy { it.user_id }
                .map { (_, posts) -> posts.first() }
                .map { dto ->
                    MapUserMarker(
                        userId = dto.user_id,
                        username = dto.username ?: "Unknown",
                        avatarUrl = dto.avatar_url,
                        latitude = dto.latitude ?: 0.0,
                        longitude = dto.longitude ?: 0.0,
                        latestRatingId = dto.rating_id,
                        latestDishId = dto.dish_id,
                        latestDishName = dto.dish_name,
                        latestDishImage = dto.dish_image,
                        latestRating = dto.rating,
                        latestRestaurantId = dto.restaurant_id,
                        latestRestaurantName = dto.restaurant_name,
                        latestPostTime = dto.posted_at?.let { parseTimestamp(it) },
                        isCurrentUser = dto.user_id == currentUserId
                    )
                }

            Result.success(markersByUser)
        } catch (e: Exception) {
            // If PostGIS function fails, fall back to non-PostGIS version
            println("SocialMapRepository: PostGIS query failed, trying fallback: ${e.message}")
            getNearbyUsersWithDishes(userLat, userLng, radiusMeters)
        }
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            kotlinx.datetime.Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DTOs for Supabase RPC calls
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
private data class NearbyUsersParams(
    val p_user_lat: Double,
    val p_user_lng: Double,
    val p_radius_meters: Int,
    val p_hours_ago: Int
)

@Serializable
private data class UpdateLocationParams(
    val p_latitude: Double,
    val p_longitude: Double
)

@Serializable
private data class ToggleLocationParams(
    val p_enabled: Boolean
)

@Serializable
private data class NearbyPostsPostGISParams(
    val p_user_lat: Double,
    val p_user_lng: Double,
    val p_radius_meters: Int,
    val p_limit: Int
)

@Serializable
private data class NearbyUserDto(
    val user_id: String,
    val username: String? = null,
    val avatar_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distance_meters: Double? = null,
    val latest_rating_id: String? = null,
    val latest_dish_id: String? = null,
    val latest_dish_name: String? = null,
    val latest_dish_image: String? = null,
    val latest_rating: Float? = null,
    val latest_restaurant_id: String? = null,
    val latest_restaurant_name: String? = null,
    val latest_post_time: String? = null
)

@Serializable
private data class UserMapProfileDto(
    val user_id: String,
    val username: String? = null,
    val avatar_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_sharing_enabled: Boolean? = null,
    val total_ratings: Long? = null,
    val latest_rating_id: String? = null,
    val latest_dish_name: String? = null,
    val latest_dish_image: String? = null
)

@Serializable
private data class NearbyDishPostDto(
    val user_id: String,
    val username: String? = null,
    val avatar_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rating_id: String? = null,
    val dish_id: String? = null,
    val dish_name: String? = null,
    val dish_image: String? = null,
    val rating: Float? = null,
    val restaurant_id: String? = null,
    val restaurant_name: String? = null,
    val posted_at: String? = null
)
