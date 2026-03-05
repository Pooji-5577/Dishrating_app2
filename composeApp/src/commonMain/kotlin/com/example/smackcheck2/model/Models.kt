package com.example.smackcheck2.model

/**
 * User data model
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profilePhotoUrl: String? = null,
    val level: Int = 1,
    val xp: Int = 0,
    val streakCount: Int = 0,
    val lastLocation: String? = null,
    val bio: String? = null,
    val badges: List<Badge> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

/**
 * Badge data model
 */
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String? = null,
    val isEarned: Boolean = false,
    val earnedDate: Long? = null
)

/**
 * Dish data model
 */
data class Dish(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val rating: Float = 0f,
    val comment: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val userId: String = "",
    val createdAt: Long = 0L
)

/**
 * Restaurant data model
 */
data class Restaurant(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val cuisine: String = "",
    val imageUrls: List<String> = emptyList(),
    val averageRating: Float = 0f,
    val reviewCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Review data model for social feed
 */
data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileUrl: String? = null,
    val dishId: String = "",
    val dishName: String = "",
    val dishImageUrl: String? = null,
    val restaurantName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: Long = 0L
)

/**
 * Feed item for social feed display
 */
data class FeedItem(
    val id: String,
    val userId: String = "",
    val userProfileImageUrl: String?,
    val userName: String,
    val dishImageUrl: String?,
    val dishName: String,
    val restaurantName: String,
    val rating: Float,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val timestamp: Long,
    val comment: String = "",
    val imageUrls: List<String> = emptyList()
)

/**
 * Comment data model for rating comments
 */
data class Comment(
    val id: String = "",
    val ratingId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileUrl: String? = null,
    val parentCommentId: String? = null,
    val content: String = "",
    val replies: List<Comment> = emptyList(),
    val createdAt: Long = 0L
)

/**
 * Notification data model
 */
data class Notification(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val data: Map<String, String> = emptyMap()
)

/**
 * Restaurant visit data model for geofencing
 */
data class RestaurantVisit(
    val id: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val enteredAt: Long = 0L,
    val exitedAt: Long? = null,
    val durationMinutes: Int? = null
)

/**
 * Simple user info for followers/following lists
 */
data class UserSummary(
    val id: String = "",
    val name: String = "",
    val profilePhotoUrl: String? = null,
    val isFollowing: Boolean = false
)
