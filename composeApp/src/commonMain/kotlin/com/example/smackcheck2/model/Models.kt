package com.example.smackcheck2.model

/**
 * User data model
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
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
    val ratingCount: Int = 0,
    val comment: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantCity: String = "",
    val uploaderName: String = "",
    val uploaderProfileUrl: String? = null,
    val userId: String = "",
    val createdAt: Long = 0L,
    val price: Double? = null
)

/**
 * Restaurant data model
 */
data class Restaurant(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val cuisine: String = "",
    val category: String = "",
    val imageUrls: List<String> = emptyList(),
    val averageRating: Float = 0f,
    val reviewCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val googlePlaceId: String? = null,   // Google Places API place ID for fetching photos
    val photoUrl: String? = null,         // Direct Google Places photo URL
    val tagline: String? = null,
    val isOpenNow: Boolean? = null
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
    val createdAt: Long = 0L,
    val price: Double? = null
)

/**
 * Feed item for social feed display
 */
data class FeedItem(
    val id: String,
    val userId: String = "",
    val userProfileImageUrl: String?,
    val userName: String,
    val roleBadge: String? = null,
    val dishImageUrl: String?,
    val dishName: String,
    val dishId: String = "",
    val restaurantName: String,
    val restaurantCity: String = "",
    val rating: Float,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean = false,
    val timestamp: Long,
    val comment: String = "",
    val imageUrls: List<String> = emptyList(),
    val price: Double? = null
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
    val username: String? = null,
    val profilePhotoUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val isFollowing: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════════
// SOCIAL MAP MODELS (Snapchat-style map feature)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * User marker on the social map - represents a user with their latest dish post
 */
data class MapUserMarker(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double = 0.0,
    val latestRatingId: String? = null,
    val latestDishId: String? = null,
    val latestDishName: String? = null,
    val latestDishImage: String? = null,
    val latestRating: Float? = null,
    val latestRestaurantId: String? = null,
    val latestRestaurantName: String? = null,
    val latestPostTime: Long? = null,
    val isCurrentUser: Boolean = false
)

/**
 * User's map profile - for the current user's own marker
 */
data class UserMapProfile(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationSharingEnabled: Boolean = true,
    val totalRatings: Int = 0,
    val latestRatingId: String? = null,
    val latestDishName: String? = null,
    val latestDishImage: String? = null
)

/**
 * Story data model for ephemeral photo stories
 */
data class Story(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileUrl: String? = null,
    val imageUrl: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L
)

enum class MapMode { NEARBY, MY_RATINGS }

/**
 * UI state for the Social Map screen
 */
data class SocialMapUiState(
    val isLoading: Boolean = true,
    val currentUserProfile: UserMapProfile? = null,
    val nearbyUsers: List<MapUserMarker> = emptyList(),
    val myRatingMarkers: List<MapUserMarker> = emptyList(),
    val selectedUser: MapUserMarker? = null,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val radiusMeters: Int = 3000,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val lastRefreshTime: Long = 0L,
    val locationPermissionGranted: Boolean = false,
    val mapMode: MapMode = MapMode.NEARBY,
    val recenterTrigger: Int = 0
)


