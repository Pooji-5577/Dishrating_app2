package com.example.smackcheck2.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault

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
    val bio: String? = null,
    @SerialName("followers_count")
    val followersCount: Int = 0,
    @SerialName("following_count")
    val followingCount: Int = 0,
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
    // Legacy columns — kept nullable so deserialization doesn't fail
    val icon: String? = null,
    val category: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null
)

/**
 * User Badge relationship DTO
 * Maps to 'user_badges' table
 */
@Serializable
data class UserBadgeDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("badge_id")
    val badgeId: String,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("earned_at")
    val earnedAt: String? = null
)

/**
 * Restaurant DTO for Supabase
 * Maps to 'restaurants' table
 */
@Serializable
data class RestaurantDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
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
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
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
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
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
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Like DTO for Supabase
 * Maps to 'likes' table
 */
@Serializable
data class LikeDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("rating_id")
    val ratingId: String,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
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

/**
 * Follower relationship DTO
 * Maps to 'followers' table
 */
@Serializable
data class FollowerDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("follower_id")
    val followerId: String,
    @SerialName("following_id")
    val followingId: String,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Comment DTO for Supabase
 * Maps to 'comments' table
 */
@Serializable
data class CommentDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("rating_id")
    val ratingId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    val content: String,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Notification DTO for Supabase
 * Maps to 'notifications' table
 */
@Serializable
data class NotificationDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: String = "{}",
    @SerialName("is_read")
    val isRead: Boolean = false,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Restaurant visit DTO for geofencing
 * Maps to 'restaurant_visits' table
 */
@Serializable
data class RestaurantVisitDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("restaurant_id")
    val restaurantId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("entered_at")
    val enteredAt: String? = null,
    @SerialName("exited_at")
    val exitedAt: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null
)

/**
 * Rating image DTO for multiple photos per dish
 * Maps to 'rating_images' table
 */
@Serializable
data class RatingImageDto(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("rating_id")
    val ratingId: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null
)
