package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.data.ImageDelivery
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.*
import com.example.smackcheck2.util.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Local request/response DTOs used only within this file ───────────────────

@Serializable
private data class CreateRestaurantRequest(
    val name: String,
    val city: String,
    val cuisine: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("google_place_id") val googlePlaceId: String? = null,
    @SerialName("image_urls") val imageUrls: List<String>? = null,
    @SerialName("photo_urls") val photoUrls: List<String>? = null,
    val category: String? = null
)

@Serializable
private data class CreateDishRequest(
    val name: String,
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
private data class SubmitRatingRequest(
    @SerialName("dish_id") val dishId: String,
    @SerialName("restaurant_id") val restaurantId: String,
    val rating: Float,
    val comment: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price: Double? = null,
    @SerialName("currency_code") val currencyCode: String? = null
)

@Serializable
private data class ValidateReceiptRequest(
    @SerialName("rating_id") val ratingId: String,
    @SerialName("receipt_image_url") val receiptImageUrl: String,
    val source: String
)

@Serializable
private data class ValidateReceiptResponse(
    val id: String = "",
    @SerialName("validation_status") val validationStatus: String = "pending"
)

@Serializable
private data class ToggleLikeRequest(
    @SerialName("rating_id") val ratingId: String
)

@Serializable
private data class ToggleLikeResponse(
    val liked: Boolean
)

@Serializable
private data class LikeCheckResponse(
    val liked: Boolean
)

@Serializable
private data class ToggleSaveRequest(
    @SerialName("restaurant_id") val restaurantId: String
)

@Serializable
private data class ToggleSaveResponse(
    val saved: Boolean
)

@Serializable
private data class AwardBadgeRequest(
    @SerialName("badge_id") val badgeId: String
)

@Serializable
private data class BadgeHasResponse(
    val has: Boolean
)

@Serializable
private data class RecordActionRequest(
    @SerialName("action_type") val actionType: String,
    @SerialName("points_earned") val pointsEarned: Int,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
private data class RecordActionResponse(
    val success: Boolean = true,
    val xp: Int? = null,
    val level: Int? = null,
    @SerialName("streak_count") val streakCount: Int? = null
)

@Serializable
private data class LeaderboardEntryDto(
    val id: String,
    val name: String,
    val email: String = "",
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    val level: Int = 1,
    val xp: Int = 0,
    @SerialName("streak_count") val streakCount: Int = 0,
    @SerialName("last_location") val lastLocation: String? = null,
    val bio: String? = null,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    val username: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class CountTodayResponse(
    val count: Int = 0
)

@Serializable
private data class UniqueRestaurantsResponse(
    val count: Int = 0
)

@Serializable
private data class WithPhotosCountResponse(
    val count: Int = 0
)

@Serializable
private data class GroupedReviewItemResponse(
    val id: String = "",
    @SerialName("group_id")
    val groupId: String = "",
    @SerialName("rating_id")
    val ratingId: String? = null,
    @SerialName("dish_id")
    val dishId: String? = null,
    @SerialName("dish_name")
    val dishName: String = "",
    @SerialName("image_url")
    val imageUrl: String? = null,
    val price: Double? = null,
    @SerialName("currency_code")
    val currencyCode: String? = null
)

@Serializable
private data class DishDetailResponse(
    val id: String = "",
    val name: String = "",
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("average_rating")
    val averageRating: Float = 0f,
    @SerialName("review_count")
    val reviewCount: Int = 0,
    @SerialName("restaurant_id")
    val restaurantId: String = "",
    val restaurants: DishDetailRestaurantResponse? = null
)

@Serializable
private data class DishDetailRestaurantResponse(
    val name: String = "",
    val city: String? = null
)

@Serializable
private data class CuisinesResponse(
    val cuisines: List<String> = emptyList()
)

@Serializable
private data class CitiesResponse(
    val cities: List<String> = emptyList()
)

/**
 * Repository for database operations using the SmackCheck custom backend API.
 */
class DatabaseRepository(
    private val schemaAdapter: SupabaseSchemaAdapter = SupabaseSchemaAdapter(),
    private val feedAssembler: FeedAssembler = FeedAssembler()
) {

    // ==================== RESTAURANTS ====================

    /**
     * Get all restaurants
     */
    suspend fun getRestaurants(): Result<List<Restaurant>> {
        return try {
            val restaurants = ApiClient.get<List<RestaurantDto>>("restaurants")
                .map { it.toRestaurant() }
            Result.success(restaurants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search restaurants by query, cuisine, city, minimum rating, and category filter.
     * Query searches across name, cuisine, and city fields (case-insensitive).
     */
    suspend fun searchRestaurants(
        query: String? = null,
        cuisines: Set<String> = emptySet(),
        city: String? = null,
        minRating: Float? = null,
        restaurantsAndCafesOnly: Boolean = false
    ): Result<List<Restaurant>> {
        return try {
            Logger.d("DatabaseRepository", "Searching with query='$query', cuisines=$cuisines, city=$city, minRating=$minRating, restaurantsAndCafesOnly=$restaurantsAndCafesOnly")
            val params = buildMap<String, String?> {
                if (!query.isNullOrBlank()) put("query", query)
                if (cuisines.isNotEmpty()) put("cuisines", cuisines.joinToString(","))
                if (!city.isNullOrBlank()) put("city", city)
                if (minRating != null) put("minRating", minRating.toString())
                if (restaurantsAndCafesOnly) put("restaurantsOnly", "true")
            }
            val restaurants = ApiClient.get<List<RestaurantDto>>("restaurants", params)
                .map { it.toRestaurant() }
            Logger.d("DatabaseRepository", "Found ${restaurants.size} restaurants")
            Result.success(restaurants)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Search error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get restaurant by ID
     */
    suspend fun getRestaurantById(id: String): Result<Restaurant?> {
        return try {
            val restaurant = try {
                ApiClient.get<RestaurantDto>("restaurants/$id").toRestaurant()
            } catch (_: Exception) { null }
            Result.success(restaurant)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get restaurant by linked Google Place ID
     */
    suspend fun getRestaurantByGooglePlaceId(placeId: String): Result<Restaurant?> {
        return try {
            val restaurant = try {
                ApiClient.get<RestaurantDto>(
                    "restaurants/by-place-id",
                    mapOf("placeId" to placeId)
                ).toRestaurant()
            } catch (_: Exception) { null }
            Result.success(restaurant)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ensure a restaurant exists in the DB (e.g. when selected from Google Places).
     * If the restaurant is not found, inserts it. Optionally adds a dish photo as the restaurant image.
     */
    suspend fun ensureRestaurantExists(restaurant: Restaurant, dishImageUrl: String? = null): Result<Restaurant> {
        return try {
            val existing = findExistingRestaurant(restaurant)

            if (existing != null) {
                if (existing.imageUrls.isNullOrEmpty() && dishImageUrl != null) {
                    schemaAdapter.ensureRestaurantImage(existing.id ?: restaurant.id, dishImageUrl)
                    Logger.d("DatabaseRepository", "Updated restaurant ${restaurant.id} with dish image")
                }
                Result.success(existing.toRestaurant())
            } else {
                val imageUrls = when {
                    dishImageUrl != null -> listOf(dishImageUrl)
                    restaurant.imageUrls.isNotEmpty() -> restaurant.imageUrls
                    restaurant.photoUrl != null -> listOf(restaurant.photoUrl)
                    else -> emptyList()
                }
                val request = CreateRestaurantRequest(
                    name = restaurant.name,
                    city = restaurant.city,
                    cuisine = restaurant.cuisine,
                    latitude = restaurant.latitude,
                    longitude = restaurant.longitude,
                    googlePlaceId = restaurant.googlePlaceId,
                    imageUrls = imageUrls.ifEmpty { null },
                    photoUrls = restaurant.photoUrl?.let { listOf(it) },
                    category = restaurant.category.ifBlank { null }
                )
                val created = ApiClient.post<CreateRestaurantRequest, RestaurantDto>("restaurants", request)
                Logger.d("DatabaseRepository", "Created new restaurant from Places: ${created.id} — ${created.name}")
                Result.success(created.toRestaurant())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun findExistingRestaurant(restaurant: Restaurant): RestaurantDto? {
        if (restaurant.id.isNotBlank()) {
            try {
                val byId = ApiClient.get<RestaurantDto>("restaurants/${restaurant.id}")
                return byId
            } catch (_: Exception) { /* not found */ }
        }

        val placeId = restaurant.googlePlaceId
        if (!placeId.isNullOrBlank()) {
            try {
                val byPlaceId = ApiClient.get<RestaurantDto>(
                    "restaurants/by-place-id",
                    mapOf("placeId" to placeId)
                )
                return byPlaceId
            } catch (_: Exception) { /* not found */ }
        }

        return null
    }

    /**
     * Create a new restaurant
     */
    suspend fun createRestaurant(restaurant: Restaurant): Result<Restaurant> {
        return try {
            val request = CreateRestaurantRequest(
                name = restaurant.name,
                city = restaurant.city,
                cuisine = restaurant.cuisine,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                imageUrls = restaurant.imageUrls.ifEmpty { null }
            )
            val created = ApiClient.post<CreateRestaurantRequest, RestaurantDto>("restaurants", request)
            Result.success(created.toRestaurant())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get distinct cuisines across all restaurants
     */
    suspend fun getDistinctCuisines(): Result<List<String>> {
        return try {
            val response = ApiClient.get<CuisinesResponse>("restaurants/cuisines")
            Result.success(response.cuisines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get distinct cities across all restaurants
     */
    suspend fun getDistinctCities(): Result<List<String>> {
        return try {
            val response = ApiClient.get<CitiesResponse>("restaurants/cities")
            Result.success(response.cities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get restaurants by city (case-insensitive partial match)
     */
    suspend fun getRestaurantsByCity(city: String): Result<List<Restaurant>> {
        return try {
            Logger.d("DatabaseRepository", "Searching for restaurants in city: $city")
            val restaurants = try {
                ApiClient.get<List<RestaurantDto>>(
                    "restaurants/by-city",
                    mapOf("city" to city)
                ).map { it.toRestaurant() }
            } catch (_: Exception) { emptyList() }

            Logger.d("DatabaseRepository", "Found ${restaurants.size} restaurants for city: $city")

            Result.success(restaurants)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error searching restaurants: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get restaurant IDs saved by a user.
     * The userId parameter is kept for API compatibility; the backend resolves
     * the current user from the JWT token.
     */
    suspend fun getSavedRestaurantIds(userId: String): Result<Set<String>> {
        return try {
            val restaurantIds = ApiClient.get<List<String>>("saves")
            Result.success(restaurantIds.toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle save/unsave for a restaurant.
     *
     * @return true if restaurant is now saved, false if it was removed.
     */
    suspend fun toggleRestaurantSave(userId: String, restaurantId: String): Result<Boolean> {
        return try {
            val response = ApiClient.post<ToggleSaveRequest, ToggleSaveResponse>(
                "saves/toggle",
                ToggleSaveRequest(restaurantId = restaurantId)
            )
            Result.success(response.saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DISHES ====================

    /**
     * Get a single dish by ID, including its average rating and restaurant info
     */
    suspend fun getDishById(dishId: String): Result<Dish?> {
        return try {
            val dish = try {
                ApiClient.get<DishDetailResponse>("dishes/$dishId").toDish()
            } catch (_: Exception) { null }
            Result.success(dish)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error fetching dish $dishId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch a single rating by its ID and return the associated dish_id,
     * useful when a FeedItem only carries a rating ID (dishId blank).
     */
    suspend fun getDishIdFromRating(ratingId: String): String? {
        return try {
            val rating = ApiClient.get<RatingDto>("ratings/$ratingId")
            rating.dishId.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getGroupedReviewDishesForRating(ratingId: String): Result<List<GroupedReviewDish>> {
        return try {
            val rows = ApiClient.get<List<GroupedReviewItemResponse>>("ratings/$ratingId/group-items")
            Result.success(rows.map { row ->
                GroupedReviewDish(
                    ratingId = row.ratingId.orEmpty(),
                    dishId = row.dishId.orEmpty(),
                    dishName = row.dishName,
                    imageUrl = ImageDelivery.feed(row.imageUrl),
                    price = row.price,
                    currencyCode = row.currencyCode
                )
            })
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error fetching grouped review dishes for rating $ratingId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all ratings for a dish, optionally restricted to a specific restaurant
     * (for location-specific review filtering).
     *
     * @param dishId the dish to query reviews for
     * @param restaurantId when non-null, only reviews posted at this restaurant are returned
     */
    suspend fun getRatingsForDish(
        dishId: String,
        restaurantId: String? = null
    ): Result<List<Review>> {
        return try {
            val params = buildMap<String, String?> {
                put("dishId", dishId)
                if (!restaurantId.isNullOrBlank()) put("restaurantId", restaurantId)
            }
            val ratings = ApiClient.get<List<RatingDto>>("ratings", params)

            val reviews = ratings.map { rating ->
                Review(
                    id = rating.id ?: "",
                    userId = rating.userId,
                    userName = "",
                    userProfileUrl = null,
                    dishId = rating.dishId,
                    dishName = "",
                    dishImageUrl = rating.imageUrl,
                    restaurantName = "",
                    rating = rating.rating,
                    comment = rating.comment,
                    likesCount = rating.likesCount,
                    createdAt = 0L,
                    price = rating.price,
                    currencyCode = rating.currencyCode
                )
            }

            Result.success(reviews)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error fetching ratings for dish $dishId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get top-rated dishes across all restaurants.
     */
    suspend fun getTopRatedDishes(limit: Int = 10): Result<List<Dish>> {
        return try {
            val dishes = ApiClient.get<List<DishDto>>(
                "dishes/top",
                mapOf("limit" to limit.toString())
            ).map { it.toDish() }
            Logger.d("DatabaseRepository", "getTopRatedDishes → ${dishes.size} dishes")
            Result.success(dishes)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error fetching top rated dishes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get top-rated dishes restricted to the given restaurant IDs (location-based).
     * Falls back to global top dishes when restaurantIds is empty.
     */
    suspend fun getTopRatedDishesForRestaurants(restaurantIds: List<String>, limit: Int = 10): Result<List<Dish>> {
        if (restaurantIds.isEmpty()) return getTopRatedDishes(limit)
        return try {
            val dishes = ApiClient.get<List<DishDto>>(
                "dishes/top-for-restaurants",
                mapOf(
                    "ids" to restaurantIds.joinToString(","),
                    "limit" to limit.toString()
                )
            ).map { it.toDish() }
            Logger.d("DatabaseRepository", "getTopRatedDishesForRestaurants → ${dishes.size} dishes")

            if (dishes.isEmpty()) {
                Logger.d("DatabaseRepository", "No location dishes, falling back to global")
                return getTopRatedDishes(limit)
            }

            Result.success(dishes)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error fetching location top dishes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get dishes for a restaurant
     */
    suspend fun getDishesForRestaurant(restaurantId: String): Result<List<Dish>> {
        return try {
            val dishes = ApiClient.get<List<DishDto>>(
                "dishes",
                mapOf("restaurantId" to restaurantId)
            ).map { it.toDish() }
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
            val dishes = ApiClient.get<List<DishDto>>(
                "dishes",
                mapOf("restaurantIds" to restaurantIds.joinToString(","))
            ).map { it.toDish() }
            Result.success(dishes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create or get existing dish
     */
    suspend fun createOrGetDish(
        name: String,
        restaurantId: String,
        imageUrl: String? = null,
        restaurantName: String? = null
    ): Result<Dish> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Dish name cannot be blank"))
        }
        return try {
            // Try to find existing dish by fetching dishes for this restaurant
            val existing = try {
                ApiClient.get<List<DishDto>>(
                    "dishes",
                    mapOf("restaurantId" to restaurantId)
                ).firstOrNull { it.name.equals(name, ignoreCase = true) }
            } catch (_: Exception) { null }

            if (existing != null) {
                val needsImageUpdate = existing.imageUrl == null && imageUrl != null
                if (needsImageUpdate) {
                    schemaAdapter.backfillDishFields(
                        dishId = existing.id!!,
                        imageUrl = imageUrl
                    )
                    return Result.success(
                        existing.toDish().copy(
                            imageUrl = imageUrl,
                            restaurantName = restaurantName ?: existing.restaurantName ?: ""
                        )
                    )
                }
                return Result.success(
                    existing.toDish().copy(
                        restaurantName = existing.restaurantName ?: restaurantName ?: ""
                    )
                )
            }

            val request = CreateDishRequest(
                name = name,
                restaurantId = restaurantId,
                imageUrl = imageUrl
            )
            val created = ApiClient.post<CreateDishRequest, DishDto>("dishes", request)
            Result.success(
                created.toDish().copy(
                    restaurantName = schemaAdapter.resolveRestaurantName(created, restaurantName)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch-fetch all ratings for a list of dish IDs (used to compute avg rating/price per dish)
     */
    suspend fun getRatingsByDishIds(dishIds: List<String>): List<RatingDto> {
        val uniqueDishIds = dishIds.filter { it.isNotBlank() }.distinct()
        if (uniqueDishIds.isEmpty()) return emptyList()
        return try {
            ApiClient.get<List<RatingDto>>(
                "ratings",
                mapOf(
                    "dishIds" to uniqueDishIds.joinToString(","),
                    "limit" to (uniqueDishIds.size * 20).coerceIn(20, 200).toString()
                )
            )
        } catch (_: Exception) { emptyList() }
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
        imageUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        price: Double? = null,
        currencyCode: String? = null
    ): Result<String> {
        if (rating <= 0f) {
            return Result.failure(IllegalArgumentException("Star rating is required"))
        }
        return try {
            val request = SubmitRatingRequest(
                dishId = dishId,
                restaurantId = restaurantId,
                rating = rating,
                comment = comment,
                imageUrl = imageUrl,
                latitude = latitude,
                longitude = longitude,
                price = price,
                currencyCode = currencyCode
            )
            val created = ApiClient.post<SubmitRatingRequest, RatingDto>("ratings", request)
            Result.success(created.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateRatingReceipt(
        ratingId: String,
        receiptImageUrl: String,
        source: String
    ): Result<Unit> {
        if (ratingId.isBlank()) return Result.failure(IllegalArgumentException("ratingId required"))
        if (receiptImageUrl.isBlank()) return Result.failure(IllegalArgumentException("receipt image required"))
        return try {
            ApiClient.post<ValidateReceiptRequest, ValidateReceiptResponse>(
                "ratings/receipt",
                ValidateReceiptRequest(
                    ratingId = ratingId,
                    receiptImageUrl = receiptImageUrl,
                    source = source
                )
            )
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
            val ratings = ApiClient.get<List<RatingDto>>(
                "ratings",
                mapOf("restaurantId" to restaurantId)
            )

            val reviews = ratings.map { rating ->
                Review(
                    id = rating.id ?: "",
                    userId = rating.userId,
                    userName = "",
                    userProfileUrl = null,
                    dishId = rating.dishId,
                    dishName = "",
                    dishImageUrl = rating.imageUrl,
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
    suspend fun getFeed(
        limit: Int = 20,
        offset: Int = 0,
        currentUserId: String? = null
    ): Result<List<FeedItem>> {
        return try {
            val ratings = ApiClient.get<List<RatingDto>>(
                "ratings/feed",
                mapOf(
                    "limit" to limit.toString(),
                    "offset" to offset.toString()
                )
            )
            Result.success(feedAssembler.mapRatingsToFeedItems(ratings, currentUserId))
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
            val response = ApiClient.post<ToggleLikeRequest, ToggleLikeResponse>(
                "likes/toggle",
                ToggleLikeRequest(ratingId = ratingId)
            )
            Result.success(response.liked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user has liked a rating
     */
    suspend fun hasUserLiked(userId: String, ratingId: String): Boolean {
        return try {
            val response = ApiClient.get<LikeCheckResponse>(
                "likes/check",
                mapOf("ratingId" to ratingId)
            )
            response.liked
        } catch (e: Exception) {
            false
        }
    }

    // ==================== USER PROGRESS ====================

    /**
     * Add XP to user via gamification record-action endpoint
     */
    suspend fun addXpToUser(userId: String, xpAmount: Int): Result<Unit> {
        return try {
            ApiClient.post<RecordActionRequest, RecordActionResponse>(
                "gamification/record-action",
                RecordActionRequest(
                    actionType = "xp_award",
                    pointsEarned = xpAmount,
                    metadata = mapOf("userId" to userId)
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user streak (simple update)
     */
    suspend fun updateStreak(userId: String, streakCount: Int): Result<Unit> {
        return try {
            ApiClient.post<RecordActionRequest, RecordActionResponse>(
                "gamification/record-action",
                RecordActionRequest(
                    actionType = "streak_update",
                    pointsEarned = 0,
                    metadata = mapOf(
                        "userId" to userId,
                        "streakCount" to streakCount.toString()
                    )
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user's rating streak.
     * Increments streak if rating on consecutive days, resets if gap > 48 hours.
     */
    suspend fun updateUserStreak(userId: String): Result<Int> {
        return try {
            val response = ApiClient.post<RecordActionRequest, RecordActionResponse>(
                "gamification/record-action",
                RecordActionRequest(
                    actionType = "rating_submitted",
                    pointsEarned = 10,
                    metadata = mapOf("userId" to userId)
                )
            )
            val newStreak = response.streakCount ?: 1
            Result.success(newStreak)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error updating streak: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== LEADERBOARD ====================

    /**
     * Get leaderboard with top users by XP
     */
    suspend fun getLeaderboard(limit: Int = 50): Result<List<ProfileDto>> {
        return try {
            val entries = ApiClient.get<List<LeaderboardEntryDto>>(
                "gamification/leaderboard",
                mapOf("limit" to limit.toString())
            )
            // Map LeaderboardEntryDto → ProfileDto for backward compat with callers
            val profiles = entries.map { entry ->
                ProfileDto(
                    id = entry.id,
                    name = entry.name,
                    email = entry.email,
                    profilePhotoUrl = entry.profilePhotoUrl,
                    level = entry.level,
                    xp = entry.xp,
                    streakCount = entry.streakCount,
                    lastLocation = entry.lastLocation,
                    bio = entry.bio,
                    followersCount = entry.followersCount,
                    followingCount = entry.followingCount,
                    username = entry.username,
                    createdAt = entry.createdAt,
                    updatedAt = entry.updatedAt
                )
            }
            Result.success(profiles)
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
            val badges = ApiClient.get<List<BadgeDto>>("badges")
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
            val badges = ApiClient.get<List<BadgeDto>>(
                "badges/user",
                mapOf("userId" to userId)
            ).map { it.toBadge(isEarned = true) }
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
            ApiClient.post<AwardBadgeRequest, RecordActionResponse>(
                "badges/award",
                AwardBadgeRequest(badgeId = badgeId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user has a specific badge
     */
    suspend fun hasUserBadge(userId: String, badgeId: String): Result<Boolean> {
        return try {
            val response = ApiClient.get<BadgeHasResponse>(
                "badges/has",
                mapOf("badgeId" to badgeId)
            )
            Result.success(response.has)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== STATS TRACKING ====================

    /**
     * Get count of ratings submitted today
     */
    suspend fun getRatingsCountToday(userId: String): Result<Int> {
        return try {
            val response = ApiClient.get<CountTodayResponse>("ratings/count-today")
            Result.success(response.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of unique restaurants the user has rated
     */
    suspend fun getUniqueRestaurantsRated(userId: String): Result<Int> {
        return try {
            val response = ApiClient.get<UniqueRestaurantsResponse>("ratings/unique-restaurants")
            Result.success(response.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of ratings with photos
     */
    suspend fun getRatingsWithPhotosCount(userId: String): Result<Int> {
        return try {
            val response = ApiClient.get<WithPhotosCountResponse>("ratings/with-photos-count")
            Result.success(response.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get unique cuisines the user has tried
     */
    suspend fun getUniqueCuisinesTried(userId: String): Result<Set<String>> {
        return try {
            // Fetch all ratings for this user, then resolve their restaurant cuisines
            val ratings = ApiClient.get<List<RatingDto>>(
                "ratings",
                mapOf("restaurantId" to "")  // endpoint returns user's own ratings via JWT
            )
            val restaurantIds = ratings.map { it.restaurantId }.toSet()

            val cuisines = mutableSetOf<String>()
            for (restaurantId in restaurantIds) {
                try {
                    val restaurant = ApiClient.get<RestaurantDto>("restaurants/$restaurantId")
                    if (restaurant.cuisine.isNotBlank()) {
                        cuisines.add(restaurant.cuisine)
                    }
                } catch (_: Exception) { /* skip individual failures */ }
            }

            Result.success(cuisines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PRIVATE HELPERS ====================

    suspend fun refreshRestaurantRating(restaurantId: String): Result<Unit> {
        // Restaurant ratings are maintained server-side on every POST /api/ratings
        return Result.success(Unit)
    }

    /**
     * Get the total number of ratings submitted by a user.
     */
    suspend fun getUserRatingCount(userId: String): Int {
        return try {
            val ratings = ApiClient.get<List<RatingDto>>(
                "ratings",
                mapOf("limit" to "1000")
            )
            ratings.size
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Failed to get user rating count: ${e.message}", e)
            0
        }
    }

    // ==================== EXTENSION FUNCTIONS ====================

    private fun RestaurantDto.toRestaurant(): Restaurant {
        return Restaurant(
            id = id ?: "",
            name = name,
            city = city,
            cuisine = cuisine,
            imageUrls = imageUrls ?: emptyList(),
            averageRating = averageRating,
            reviewCount = reviewCount,
            latitude = latitude,
            longitude = longitude,
            googlePlaceId = googlePlaceId,
            photoUrl = photoUrls?.firstOrNull()
        )
    }

    private fun DishDto.toDish(): Dish {
        return Dish(
            id = id ?: "",
            name = name,
            imageUrl = imageUrl,
            restaurantId = restaurantId,
            restaurantName = restaurantName ?: ""
        )
    }

    private fun DishDetailResponse.toDish(): Dish {
        return Dish(
            id = id,
            name = name,
            imageUrl = imageUrl,
            rating = averageRating,
            ratingCount = reviewCount,
            restaurantId = restaurantId,
            restaurantName = restaurants?.name.orEmpty(),
            restaurantCity = restaurants?.city.orEmpty()
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
