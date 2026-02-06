package com.example.smackcheck2.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User profile DTO for Supabase
 * Maps to 'profiles' table
 */
@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("profile_photo_url")
    val profilePhotoUrl: String? = null,
    val level: Int = 1,
    val xp: Int = 0,
    @SerialName("streak_count")
    val streakCount: Int = 0,
    @SerialName("last_location")
    val lastLocation: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Badge DTO for Supabase
 * Maps to 'badges' table
 */
@Serializable
data class BadgeDto(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("icon_url")
    val iconUrl: String? = null
)

/**
 * User Badge relationship DTO
 * Maps to 'user_badges' table
 */
@Serializable
data class UserBadgeDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("badge_id")
    val badgeId: String,
    @SerialName("earned_at")
    val earnedAt: String? = null
)

/**
 * Restaurant DTO for Supabase
 * Maps to 'restaurants' table
 */
@Serializable
data class RestaurantDto(
    val id: String? = null,
    val name: String,
    val city: String,
    val cuisine: String,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("average_rating")
    val averageRating: Float = 0f,
    @SerialName("review_count")
    val reviewCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Dish DTO for Supabase
 * Maps to 'dishes' table
 */
@Serializable
data class DishDto(
    val id: String? = null,
    val name: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("restaurant_id")
    val restaurantId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Rating/Review DTO for Supabase
 * Maps to 'ratings' table
 */
@Serializable
data class RatingDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("dish_id")
    val dishId: String,
    @SerialName("restaurant_id")
    val restaurantId: String,
    val rating: Float,
    val comment: String = "",
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Like DTO for Supabase
 * Maps to 'likes' table
 */
@Serializable
data class LikeDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("rating_id")
    val ratingId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Feed item DTO - joined data for social feed
 */
@Serializable
data class FeedItemDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("user_profile_url")
    val userProfileUrl: String? = null,
    @SerialName("dish_name")
    val dishName: String,
    @SerialName("dish_image_url")
    val dishImageUrl: String? = null,
    @SerialName("restaurant_name")
    val restaurantName: String,
    val rating: Float,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    val comment: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)
