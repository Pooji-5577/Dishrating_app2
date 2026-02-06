package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * Repository for database operations using Supabase Postgrest
 */
class DatabaseRepository {

    private val client = SupabaseClientProvider.client
    private val postgrest = client.postgrest

    // ==================== RESTAURANTS ====================

    /**
     * Get all restaurants
     */
    suspend fun getRestaurants(): Result<List<Restaurant>> {
        return try {
            val restaurants = postgrest["restaurants"]
                .select()
                .decodeList<RestaurantDto>()
                .map { it.toRestaurant() }
            Result.success(restaurants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search restaurants by query, cuisine, city, and minimum rating
     */
    suspend fun searchRestaurants(
        query: String? = null,
        cuisines: Set<String> = emptySet(),
        city: String? = null,
        minRating: Float? = null
    ): Result<List<Restaurant>> {
        return try {
            val restaurants = postgrest["restaurants"]
                .select {
                    filter {
                        if (!query.isNullOrBlank()) {
                            ilike("name", "%$query%")
                        }
                        if (cuisines.isNotEmpty()) {
                            isIn("cuisine", cuisines.toList())
                        }
                        if (!city.isNullOrBlank()) {
                            eq("city", city)
                        }
                        if (minRating != null) {
                            gte("average_rating", minRating)
                        }
                    }
                    order("average_rating", Order.DESCENDING)
                }
                .decodeList<RestaurantDto>()
                .map { it.toRestaurant() }
            Result.success(restaurants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get restaurant by ID
     */
    suspend fun getRestaurantById(id: String): Result<Restaurant?> {
        return try {
            val restaurant = postgrest["restaurants"]
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingleOrNull<RestaurantDto>()
                ?.toRestaurant()
            Result.success(restaurant)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new restaurant
     */
    suspend fun createRestaurant(restaurant: Restaurant): Result<Restaurant> {
        return try {
            val dto = RestaurantDto(
                name = restaurant.name,
                city = restaurant.city,
                cuisine = restaurant.cuisine,
                imageUrls = restaurant.imageUrls,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude
            )
            val created = postgrest["restaurants"]
                .insert(dto) {
                    select()
                }
                .decodeSingle<RestaurantDto>()
            Result.success(created.toRestaurant())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get restaurants by city
     */
    suspend fun getRestaurantsByCity(city: String): Result<List<Restaurant>> {
        return try {
            val restaurants = postgrest["restaurants"]
                .select {
                    filter {
                        eq("city", city)
                    }
                    order("average_rating", Order.DESCENDING)
                }
                .decodeList<RestaurantDto>()
                .map { it.toRestaurant() }
            Result.success(restaurants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DISHES ====================

    /**
     * Get dishes for a restaurant
     */
    suspend fun getDishesForRestaurant(restaurantId: String): Result<List<Dish>> {
        return try {
            val dishes = postgrest["dishes"]
                .select {
                    filter {
                        eq("restaurant_id", restaurantId)
                    }
                }
                .decodeList<DishDto>()
                .map { it.toDish() }
            Result.success(dishes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get dishes for multiple restaurants (for city-based top dishes)
     */
    suspend fun getDishesForRestaurants(restaurantIds: List<String>): Result<List<Dish>> {
        if (restaurantIds.isEmpty()) return Result.success(emptyList())
        return try {
            val dishes = postgrest["dishes"]
                .select {
                    filter {
                        isIn("restaurant_id", restaurantIds)
                    }
                }
                .decodeList<DishDto>()
                .map { it.toDish() }
            Result.success(dishes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create or get existing dish
     */
    suspend fun createOrGetDish(name: String, restaurantId: String, imageUrl: String? = null): Result<Dish> {
        return try {
            // Check if dish already exists
            val existing = postgrest["dishes"]
                .select {
                    filter {
                        eq("name", name)
                        eq("restaurant_id", restaurantId)
                    }
                }
                .decodeSingleOrNull<DishDto>()

            if (existing != null) {
                return Result.success(existing.toDish())
            }

            // Create new dish
            val dto = DishDto(
                name = name,
                restaurantId = restaurantId,
                imageUrl = imageUrl
            )
            val created = postgrest["dishes"]
                .insert(dto) {
                    select()
                }
                .decodeSingle<DishDto>()
            Result.success(created.toDish())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== RATINGS ====================

    /**
     * Submit a new rating
     */
    suspend fun submitRating(
        userId: String,
        dishId: String,
        restaurantId: String,
        rating: Float,
        comment: String,
        imageUrl: String? = null
    ): Result<Unit> {
        return try {
            val dto = RatingDto(
                userId = userId,
                dishId = dishId,
                restaurantId = restaurantId,
                rating = rating,
                comment = comment,
                imageUrl = imageUrl
            )
            postgrest["ratings"].insert(dto)

            // Update restaurant average rating
            updateRestaurantRating(restaurantId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get ratings for a restaurant
     */
    suspend fun getRatingsForRestaurant(restaurantId: String): Result<List<Review>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        eq("restaurant_id", restaurantId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<RatingDto>()

            // Fetch user profiles for each rating
            val reviews = ratings.map { rating ->
                val profile = postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", rating.userId)
                        }
                    }
                    .decodeSingleOrNull<ProfileDto>()

                val dish = postgrest["dishes"]
                    .select {
                        filter {
                            eq("id", rating.dishId)
                        }
                    }
                    .decodeSingleOrNull<DishDto>()

                Review(
                    id = rating.id ?: "",
                    userId = rating.userId,
                    userName = profile?.name ?: "Unknown",
                    userProfileUrl = profile?.profilePhotoUrl,
                    dishId = rating.dishId,
                    dishName = dish?.name ?: "Unknown Dish",
                    dishImageUrl = rating.imageUrl ?: dish?.imageUrl,
                    restaurantName = "",
                    rating = rating.rating,
                    comment = rating.comment,
                    likesCount = rating.likesCount,
                    createdAt = 0L
                )
            }

            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== FEED ====================

    /**
     * Get social feed items
     */
    suspend fun getFeed(limit: Int = 20, offset: Int = 0): Result<List<FeedItem>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<RatingDto>()

            val feedItems = ratings.mapNotNull { rating ->
                try {
                    val profile = postgrest["profiles"]
                        .select {
                            filter {
                                eq("id", rating.userId)
                            }
                        }
                        .decodeSingleOrNull<ProfileDto>()

                    val dish = postgrest["dishes"]
                        .select {
                            filter {
                                eq("id", rating.dishId)
                            }
                        }
                        .decodeSingleOrNull<DishDto>()

                    val restaurant = postgrest["restaurants"]
                        .select {
                            filter {
                                eq("id", rating.restaurantId)
                            }
                        }
                        .decodeSingleOrNull<RestaurantDto>()

                    FeedItem(
                        id = rating.id ?: return@mapNotNull null,
                        userProfileImageUrl = profile?.profilePhotoUrl,
                        userName = profile?.name ?: "Unknown",
                        dishImageUrl = rating.imageUrl ?: dish?.imageUrl,
                        dishName = dish?.name ?: "Unknown Dish",
                        restaurantName = restaurant?.name ?: "Unknown Restaurant",
                        rating = rating.rating,
                        likesCount = rating.likesCount,
                        commentsCount = 0,
                        isLiked = false,
                        timestamp = 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(feedItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== LIKES ====================

    /**
     * Toggle like on a rating
     */
    suspend fun toggleLike(userId: String, ratingId: String): Result<Boolean> {
        return try {
            // Check if already liked
            val existing = postgrest["likes"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("rating_id", ratingId)
                    }
                }
                .decodeSingleOrNull<LikeDto>()

            if (existing != null) {
                // Unlike
                postgrest["likes"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("rating_id", ratingId)
                    }
                }
                // Decrement likes count
                updateLikesCount(ratingId, -1)
                Result.success(false)
            } else {
                // Like
                val like = LikeDto(userId = userId, ratingId = ratingId)
                postgrest["likes"].insert(like)
                // Increment likes count
                updateLikesCount(ratingId, 1)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user has liked a rating
     */
    suspend fun hasUserLiked(userId: String, ratingId: String): Boolean {
        return try {
            val existing = postgrest["likes"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("rating_id", ratingId)
                    }
                }
                .decodeSingleOrNull<LikeDto>()
            existing != null
        } catch (e: Exception) {
            false
        }
    }

    // ==================== USER PROGRESS ====================

    /**
     * Add XP to user
     */
    suspend fun addXpToUser(userId: String, xpAmount: Int): Result<Unit> {
        return try {
            val profile = postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()
                ?: return Result.failure(Exception("Profile not found"))

            val newXp = profile.xp + xpAmount
            val newLevel = calculateLevel(newXp)

            postgrest["profiles"]
                .update(
                    mapOf(
                        "xp" to newXp,
                        "level" to newLevel
                    )
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user streak
     */
    suspend fun updateStreak(userId: String, streakCount: Int): Result<Unit> {
        return try {
            postgrest["profiles"]
                .update(mapOf("streak_count" to streakCount)) {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== BADGES ====================

    /**
     * Get all badges
     */
    suspend fun getAllBadges(): Result<List<Badge>> {
        return try {
            val badges = postgrest["badges"]
                .select()
                .decodeList<BadgeDto>()
                .map { it.toBadge() }
            Result.success(badges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's earned badges
     */
    suspend fun getUserBadges(userId: String): Result<List<Badge>> {
        return try {
            val userBadges = postgrest["user_badges"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<UserBadgeDto>()

            val badges = userBadges.mapNotNull { ub ->
                postgrest["badges"]
                    .select {
                        filter {
                            eq("id", ub.badgeId)
                        }
                    }
                    .decodeSingleOrNull<BadgeDto>()
                    ?.toBadge(isEarned = true)
            }
            Result.success(badges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Award badge to user
     */
    suspend fun awardBadge(userId: String, badgeId: String): Result<Unit> {
        return try {
            val userBadge = UserBadgeDto(
                userId = userId,
                badgeId = badgeId
            )
            postgrest["user_badges"].insert(userBadge)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private suspend fun updateRestaurantRating(restaurantId: String) {
        try {
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        eq("restaurant_id", restaurantId)
                    }
                }
                .decodeList<RatingDto>()

            val avgRating = if (ratings.isNotEmpty()) {
                ratings.map { it.rating }.average().toFloat()
            } else 0f

            postgrest["restaurants"]
                .update(
                    mapOf(
                        "average_rating" to avgRating,
                        "review_count" to ratings.size
                    )
                ) {
                    filter {
                        eq("id", restaurantId)
                    }
                }
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    private suspend fun updateLikesCount(ratingId: String, delta: Int) {
        try {
            val rating = postgrest["ratings"]
                .select {
                    filter {
                        eq("id", ratingId)
                    }
                }
                .decodeSingleOrNull<RatingDto>()

            if (rating != null) {
                val newCount = (rating.likesCount + delta).coerceAtLeast(0)
                postgrest["ratings"]
                    .update(mapOf("likes_count" to newCount)) {
                        filter {
                            eq("id", ratingId)
                        }
                    }
            }
        } catch (e: Exception) {
            // Log error but don't fail the main operation
        }
    }

    private fun calculateLevel(xp: Int): Int {
        // Simple level calculation: every 100 XP = 1 level
        return (xp / 100) + 1
    }

    // ==================== EXTENSION FUNCTIONS ====================

    private fun RestaurantDto.toRestaurant(): Restaurant {
        return Restaurant(
            id = id ?: "",
            name = name,
            city = city,
            cuisine = cuisine,
            imageUrls = imageUrls,
            averageRating = averageRating,
            reviewCount = reviewCount,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun DishDto.toDish(): Dish {
        return Dish(
            id = id ?: "",
            name = name,
            imageUrl = imageUrl,
            restaurantId = restaurantId
        )
    }

    private fun BadgeDto.toBadge(isEarned: Boolean = false): Badge {
        return Badge(
            id = id,
            name = name,
            description = description,
            iconUrl = iconUrl,
            isEarned = isEarned
        )
    }
}
