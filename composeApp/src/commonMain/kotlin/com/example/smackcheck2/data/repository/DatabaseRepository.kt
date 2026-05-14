package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import com.example.smackcheck2.util.Logger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Repository for database operations using Supabase Postgrest
 */
class DatabaseRepository(
    private val schemaAdapter: SupabaseSchemaAdapter = SupabaseSchemaAdapter(),
    private val feedAssembler: FeedAssembler = FeedAssembler()
) {

    private val client get() = SupabaseClientProvider.client
    private val postgrest get() = client.postgrest

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
     * Search restaurants by query, cuisine, city, minimum rating, and category filter
     * Query searches across name, cuisine, and city fields (case-insensitive)
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
            val restaurants = postgrest["restaurants"]
                .select {
                    filter {
                        // Text search: match name, cuisine, or city (case-insensitive)
                        if (!query.isNullOrBlank()) {
                            or {
                                ilike("name", "%$query%")
                                ilike("cuisine", "%$query%")
                                ilike("city", "%$query%")
                            }
                        }
                        if (cuisines.isNotEmpty()) {
                            isIn("cuisine", cuisines.toList())
                        }
                        if (!city.isNullOrBlank()) {
                            ilike("city", "%$city%")
                        }
                        if (minRating != null) {
                            gte("average_rating", minRating)
                        }
                        // Restaurants & Cafes Only filter - filters by category field
                        if (restaurantsAndCafesOnly) {
                            or {
                                ilike("category", "%restaurant%")
                                ilike("category", "%cafe%")
                                ilike("category", "%coffee%")
                            }
                        }
                    }
                    order("average_rating", Order.DESCENDING)
                }
                .decodeList<RestaurantDto>()
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
     * Get restaurant by linked Google Place ID
     */
    suspend fun getRestaurantByGooglePlaceId(placeId: String): Result<Restaurant?> {
        return try {
            val restaurant = postgrest["restaurants"]
                .select {
                    filter {
                        eq("google_place_id", placeId)
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
     * Ensure a restaurant exists in the DB (e.g. when selected from Google Places).
     * If the restaurant ID is not found, inserts it. Optionally adds a dish photo as the restaurant image.
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
                val dto = RestaurantDto(
                    id = restaurant.id.takeIf { it.isNotBlank() },
                    name = restaurant.name,
                    city = restaurant.city,
                    cuisine = restaurant.cuisine,
                    imageUrls = imageUrls,
                    latitude = restaurant.latitude,
                    longitude = restaurant.longitude,
                    googlePlaceId = restaurant.googlePlaceId,
                    photoUrls = restaurant.photoUrl?.let { listOf(it) }
                )
                val created = schemaAdapter.insertRestaurant(dto).getOrThrow()
                Logger.d("DatabaseRepository", "Created new restaurant from Places: ${created.id} — ${created.name}")
                Result.success(created.toRestaurant())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun findExistingRestaurant(restaurant: Restaurant): RestaurantDto? {
        if (restaurant.id.isNotBlank()) {
            val byId = postgrest["restaurants"]
                .select {
                    filter { eq("id", restaurant.id) }
                    limit(1)
                }
                .decodeList<RestaurantDto>()
                .firstOrNull()
            if (byId != null) return byId
        }

        val placeId = restaurant.googlePlaceId
        if (!placeId.isNullOrBlank()) {
            val byPlaceId = postgrest["restaurants"]
                .select {
                    filter { eq("google_place_id", placeId) }
                    limit(1)
                }
                .decodeList<RestaurantDto>()
                .firstOrNull()
            if (byPlaceId != null) return byPlaceId
        }

        return null
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
     * Get restaurants by city (case-insensitive partial match)
     */
    suspend fun getDistinctCuisines(): Result<List<String>> {
        return try {
            val list = postgrest["restaurants"]
                .select(columns = Columns.list("cuisine")) {}
                .decodeList<Map<String, String>>()
                .mapNotNull { it["cuisine"] }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDistinctCities(): Result<List<String>> {
        return try {
            val list = postgrest["restaurants"]
                .select(columns = Columns.list("city")) {}
                .decodeList<Map<String, String>>()
                .mapNotNull { it["city"] }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRestaurantsByCity(city: String): Result<List<Restaurant>> {
        return try {
            Logger.d("DatabaseRepository", "Searching for restaurants in city: $city")
            var restaurants = postgrest["restaurants"]
                .select {
                    filter {
                        // Use case-insensitive pattern matching for better results
                        ilike("city", "%$city%")
                    }
                    order("average_rating", Order.DESCENDING)
                    limit(50)
                }
                .decodeList<RestaurantDto>()
                .map { it.toRestaurant() }
            Logger.d("DatabaseRepository", "Found ${restaurants.size} restaurants for city: $city")

            // If no results for the specific city, fetch all restaurants as fallback
            if (restaurants.isEmpty()) {
                Logger.d("DatabaseRepository", "No restaurants for '$city', fetching all restaurants")
                restaurants = postgrest["restaurants"]
                    .select {
                        order("average_rating", Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<RestaurantDto>()
                    .map { it.toRestaurant() }
                Logger.d("DatabaseRepository", "Fallback returned ${restaurants.size} restaurants")
            }

            Result.success(restaurants)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error searching restaurants: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get restaurant IDs saved by a user.
     */
    suspend fun getSavedRestaurantIds(userId: String): Result<Set<String>> {
        return try {
            val rows = postgrest["restaurant_saves"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<Map<String, String>>()
            val ids = rows.mapNotNull { it["restaurant_id"] }.toSet()
            Result.success(ids)
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
            val existing = postgrest["restaurant_saves"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("restaurant_id", restaurantId)
                    }
                    limit(1)
                }
                .decodeList<Map<String, String>>()
                .firstOrNull()

            if (existing != null) {
                postgrest["restaurant_saves"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("restaurant_id", restaurantId)
                    }
                }
                Result.success(false)
            } else {
                postgrest["restaurant_saves"].insert(
                    mapOf(
                        "user_id" to userId,
                        "restaurant_id" to restaurantId
                    )
                )
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DISHES ====================

    /**
     * Get a single dish by ID, including its average rating and restaurant name
     */
    suspend fun getDishById(dishId: String): Result<Dish?> {
        return try {
            val dishDto = postgrest["dishes"]
                .select {
                    filter { eq("id", dishId) }
                }
                .decodeSingleOrNull<DishDto>()

            if (dishDto == null) {
                return Result.success(null)
            }

            // Fetch restaurant name
            val restaurant = postgrest["restaurants"]
                .select {
                    filter { eq("id", dishDto.restaurantId) }
                }
                .decodeSingleOrNull<RestaurantDto>()

            // Calculate average rating from all ratings for this dish
            val ratings = postgrest["ratings"]
                .select {
                    filter { eq("dish_id", dishId) }
                }
                .decodeList<RatingDto>()

            val avgRating = if (ratings.isNotEmpty()) {
                ratings.map { it.rating }.average().toFloat()
            } else 0f

            // Fetch uploader (top-rated reviewer) profile
            val topRating = ratings.maxByOrNull { it.rating }
            val uploaderProfile = topRating?.let { r ->
                try {
                    postgrest["profiles"]
                        .select { filter { eq("id", r.userId) } }
                        .decodeSingleOrNull<ProfileDto>()
                } catch (_: Exception) { null }
            }

            val dish = dishDto.toDish().copy(
                rating = avgRating,
                ratingCount = ratings.size,
                restaurantName = restaurant?.name ?: "Unknown Restaurant",
                restaurantCity = restaurant?.city ?: "",
                uploaderName = uploaderProfile?.name ?: "",
                uploaderProfileUrl = uploaderProfile?.profilePhotoUrl
            )
            Result.success(dish)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error fetching dish $dishId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all ratings/reviews for a specific dish
     */
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
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        eq("dish_id", dishId)
                        if (!restaurantId.isNullOrBlank()) {
                            eq("restaurant_id", restaurantId)
                        }
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<RatingDto>()

            // Batch-fetch profiles and restaurants
            val userIds = ratings.map { it.userId }.distinct()
            val restIds = ratings.map { it.restaurantId }.distinct()

            val profilesMap = try {
                if (userIds.isNotEmpty()) {
                    postgrest["profiles"]
                        .select { filter { isIn("id", userIds) } }
                        .decodeList<ProfileDto>()
                        .associateBy { it.id }
                } else emptyMap()
            } catch (_: Exception) { emptyMap() }

            val restaurantsMap = try {
                if (restIds.isNotEmpty()) {
                    postgrest["restaurants"]
                        .select { filter { isIn("id", restIds) } }
                        .decodeList<RestaurantDto>()
                        .associateBy { it.id ?: "" }
                } else emptyMap()
            } catch (_: Exception) { emptyMap() }

            val reviews = ratings.map { rating ->
                val profile = profilesMap[rating.userId]
                val restaurant = restaurantsMap[rating.restaurantId]
                Review(
                    id = rating.id ?: "",
                    userId = rating.userId,
                    userName = profile?.name ?: "Unknown",
                    userProfileUrl = profile?.profilePhotoUrl,
                    dishId = rating.dishId,
                    dishName = "",
                    dishImageUrl = rating.imageUrl,
                    restaurantName = restaurant?.name ?: "",
                    rating = rating.rating,
                    comment = rating.comment,
                    likesCount = rating.likesCount,
                    createdAt = 0L
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
     * Falls back to querying the dishes table directly when the ratings table is empty.
     */
    suspend fun getTopRatedDishes(limit: Int = 10): Result<List<Dish>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    order("rating", Order.DESCENDING)
                    limit(100)
                }
                .decodeList<RatingDto>()

            Logger.d("DatabaseRepository", "getTopRatedDishes - found ${ratings.size} ratings globally")

            if (ratings.isNotEmpty()) {
                // Ratings exist — build dish list from them
                val dishRatingGroups = ratings.groupBy { it.dishId }
                val dishRatings = dishRatingGroups
                    .mapValues { (_, r) -> r.map { it.rating }.average().toFloat() }
                    .entries
                    .sortedByDescending { it.value }
                    .take(limit)

                val topDishes = dishRatings.mapNotNull { (dishId, avgRating) ->
                    try {
                        val dish = postgrest["dishes"]
                            .select { filter { eq("id", dishId) } }
                            .decodeSingleOrNull<DishDto>()
                        if (dish != null) {
                            val restaurant = postgrest["restaurants"]
                                .select { filter { eq("id", dish.restaurantId) } }
                                .decodeSingleOrNull<RestaurantDto>()
                            val topRating = dishRatingGroups[dishId]?.maxByOrNull { it.rating }
                            val uploaderProfile = topRating?.let { r ->
                                try { postgrest["profiles"].select { filter { eq("id", r.userId) } }.decodeSingleOrNull<ProfileDto>() } catch (_: Exception) { null }
                            }
                            // Prefer any rating photo over a blank dish image — users
                            // typically attach their dish photo to the rating, not the
                            // dishes row itself.
                            val ratingImageUrl = dishRatingGroups[dishId]
                                ?.firstOrNull { !it.imageUrl.isNullOrBlank() }
                                ?.imageUrl
                            dish.toDish().copy(
                                imageUrl = dish.imageUrl?.takeIf { it.isNotBlank() } ?: ratingImageUrl,
                                rating = avgRating,
                                ratingCount = dishRatingGroups[dishId]?.size ?: 0,
                                restaurantName = restaurant?.name ?: dish.restaurantName ?: "Unknown Restaurant",
                                restaurantCity = restaurant?.city ?: "",
                                uploaderName = uploaderProfile?.name ?: "",
                                uploaderProfileUrl = uploaderProfile?.profilePhotoUrl
                            )
                        } else null
                    } catch (e: Exception) {
                        Logger.e("DatabaseRepository", "Error fetching dish $dishId: ${e.message}", e)
                        null
                    }
                }
                Logger.d("DatabaseRepository", "getTopRatedDishes → ${topDishes.size} rated dishes")
                Result.success(topDishes)
            } else {
                // No ratings yet — fall back to most recently added dishes
                Logger.d("DatabaseRepository", "No ratings found, falling back to dishes table")
                val dishes = postgrest["dishes"]
                    .select { order("created_at", Order.DESCENDING); limit(limit.toLong()) }
                    .decodeList<DishDto>()

                // Batch-fetch restaurants for those dishes
                val restaurantIds = dishes.map { it.restaurantId }.distinct()
                val restaurantsMap = if (restaurantIds.isNotEmpty()) {
                    try {
                        postgrest["restaurants"]
                            .select { filter { isIn("id", restaurantIds) } }
                            .decodeList<RestaurantDto>()
                            .associateBy { it.id ?: "" }
                    } catch (_: Exception) { emptyMap() }
                } else emptyMap()

                val fallbackDishes = dishes.mapNotNull { dto ->
                    try {
                        val restaurant = restaurantsMap[dto.restaurantId]
                        dto.toDish().copy(
                            restaurantName = restaurant?.name ?: dto.restaurantName ?: "Unknown Restaurant",
                            restaurantCity = restaurant?.city ?: ""
                        )
                    } catch (_: Exception) { null }
                }
                Logger.d("DatabaseRepository", "getTopRatedDishes fallback → ${fallbackDishes.size} dishes")
                Result.success(fallbackDishes)
            }
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error fetching top rated dishes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get top-rated dishes restricted to the given restaurant IDs (location-based).
     * Falls back to querying the dishes table directly when ratings are absent.
     */
    suspend fun getTopRatedDishesForRestaurants(restaurantIds: List<String>, limit: Int = 10): Result<List<Dish>> {
        if (restaurantIds.isEmpty()) return getTopRatedDishes(limit)
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    filter { isIn("restaurant_id", restaurantIds) }
                    order("rating", Order.DESCENDING)
                    limit(100)
                }
                .decodeList<RatingDto>()

            Logger.d("DatabaseRepository", "getTopRatedDishesForRestaurants - ${restaurantIds.size} restaurants, ${ratings.size} ratings")

            if (ratings.isNotEmpty()) {
                // Ratings exist for this location — use them
                val dishRatingGroups = ratings.groupBy { it.dishId }
                val dishRatings = dishRatingGroups
                    .mapValues { (_, r) -> r.map { it.rating }.average().toFloat() }
                    .entries
                    .sortedByDescending { it.value }
                    .take(limit)

                val topDishes = dishRatings.mapNotNull { (dishId, avgRating) ->
                    try {
                        val dish = postgrest["dishes"]
                            .select { filter { eq("id", dishId) } }
                            .decodeSingleOrNull<DishDto>()
                        if (dish != null) {
                            val restaurant = postgrest["restaurants"]
                                .select { filter { eq("id", dish.restaurantId) } }
                                .decodeSingleOrNull<RestaurantDto>()
                            val topRating = dishRatingGroups[dishId]?.maxByOrNull { it.rating }
                            val uploaderProfile = topRating?.let { r ->
                                try { postgrest["profiles"].select { filter { eq("id", r.userId) } }.decodeSingleOrNull<ProfileDto>() } catch (_: Exception) { null }
                            }
                            val ratingImageUrl = dishRatingGroups[dishId]
                                ?.firstOrNull { !it.imageUrl.isNullOrBlank() }
                                ?.imageUrl
                            dish.toDish().copy(
                                imageUrl = dish.imageUrl?.takeIf { it.isNotBlank() } ?: ratingImageUrl,
                                rating = avgRating,
                                ratingCount = dishRatingGroups[dishId]?.size ?: 0,
                                restaurantName = restaurant?.name ?: dish.restaurantName ?: "Unknown Restaurant",
                                restaurantCity = restaurant?.city ?: "",
                                uploaderName = uploaderProfile?.name ?: "",
                                uploaderProfileUrl = uploaderProfile?.profilePhotoUrl
                            )
                        } else null
                    } catch (e: Exception) { null }
                }
                Logger.d("DatabaseRepository", "getTopRatedDishesForRestaurants → ${topDishes.size} rated dishes")
                Result.success(topDishes)
            } else {
                // No ratings for this location yet — query dishes table directly for these restaurants
                Logger.d("DatabaseRepository", "No local ratings, falling back to dishes table for ${restaurantIds.size} restaurants")
                val dishes = postgrest["dishes"]
                    .select {
                        filter { isIn("restaurant_id", restaurantIds) }
                        order("created_at", Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<DishDto>()

                Logger.d("DatabaseRepository", "Found ${dishes.size} dishes in dishes table for these restaurants")

                // Batch-fetch restaurants
                val restIds = dishes.map { it.restaurantId }.distinct()
                val restaurantsMap = if (restIds.isNotEmpty()) {
                    try {
                        postgrest["restaurants"]
                            .select { filter { isIn("id", restIds) } }
                            .decodeList<RestaurantDto>()
                            .associateBy { it.id ?: "" }
                    } catch (_: Exception) { emptyMap() }
                } else emptyMap()

                val fallbackDishes = dishes.mapNotNull { dto ->
                    try {
                        val restaurant = restaurantsMap[dto.restaurantId]
                        dto.toDish().copy(
                            restaurantName = restaurant?.name ?: dto.restaurantName ?: "Unknown Restaurant",
                            restaurantCity = restaurant?.city ?: ""
                        )
                    } catch (_: Exception) { null }
                }

                // If still empty (no dishes in DB for these restaurants), fall back globally
                if (fallbackDishes.isEmpty()) {
                    Logger.d("DatabaseRepository", "Location dishes table also empty, falling back to global")
                    return getTopRatedDishes(limit)
                }

                Logger.d("DatabaseRepository", "getTopRatedDishesForRestaurants fallback → ${fallbackDishes.size} dishes")
                Result.success(fallbackDishes)
            }
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
            val existing = postgrest["dishes"]
                .select {
                    filter {
                        eq("name", name)
                        eq("restaurant_id", restaurantId)
                    }
                }
                .decodeSingleOrNull<DishDto>()

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
                return Result.success(existing.toDish().copy(
                    restaurantName = existing.restaurantName ?: restaurantName ?: ""
                ))
            }

            @OptIn(ExperimentalUuidApi::class)
            val dishId = Uuid.random().toString()
            val dto = DishDto(
                id = dishId,
                name = name,
                restaurantId = restaurantId,
                imageUrl = imageUrl,
                restaurantName = restaurantName
            )
            val created = schemaAdapter.insertDish(dto).getOrThrow()
            Result.success(created.toDish().copy(restaurantName = schemaAdapter.resolveRestaurantName(created, restaurantName)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch-fetch all ratings for a list of dish IDs (used to compute avg rating/price per dish)
     */
    suspend fun getRatingsByDishIds(dishIds: List<String>): List<RatingDto> {
        if (dishIds.isEmpty()) return emptyList()
        return try {
            postgrest["ratings"]
                .select { filter { isIn("dish_id", dishIds) } }
                .decodeList<RatingDto>()
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
        price: Double? = null
    ): Result<String> {
        if (rating <= 0f) {
            return Result.failure(IllegalArgumentException("Star rating is required"))
        }
        return try {
            @OptIn(ExperimentalUuidApi::class)
            val ratingId = Uuid.random().toString()
            val dto = RatingDto(
                id = ratingId,
                userId = userId,
                dishId = dishId,
                restaurantId = restaurantId,
                rating = rating,
                comment = comment,
                imageUrl = imageUrl,
                latitude = latitude,
                longitude = longitude,
                price = price
            )
            postgrest["ratings"].insert(dto)

            Result.success(ratingId)
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
    suspend fun getFeed(
        limit: Int = 20,
        offset: Int = 0,
        currentUserId: String? = null
    ): Result<List<FeedItem>> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<RatingDto>()

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
                // Like — generate UUID for id since the DB column has no default
                @OptIn(ExperimentalUuidApi::class)
                val like = LikeDto(id = Uuid.random().toString(), userId = userId, ratingId = ratingId)
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
     * Update user streak (legacy - simple update)
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

    /**
     * Update user's rating streak
     * Increments streak if rating on consecutive days, resets if gap > 48 hours
     */
    suspend fun updateUserStreak(userId: String): Result<Int> {
        return try {
            // Get current profile
            val profile = postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()
                ?: return Result.failure(Exception("Profile not found"))

            // Get last 2 ratings to find previous rating time
            val ratings = postgrest["ratings"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    limit(2)
                }
                .decodeList<RatingDto>()

            val newStreak = if (ratings.size >= 2) {
                val lastRatingTime = parseTimestamp(ratings[1].createdAt ?: "")
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val hoursSinceLast = (currentTime - lastRatingTime) / (1000 * 60 * 60)

                when {
                    hoursSinceLast < 24 -> profile.streakCount // Same day
                    hoursSinceLast < 48 -> profile.streakCount + 1 // Next day
                    else -> 1 // Streak broken, restart
                }
            } else {
                1 // First rating
            }

            // Update streak in database
            postgrest["profiles"]
                .update(mapOf("streak_count" to newStreak)) {
                    filter { eq("id", userId) }
                }

            // Award streak bonus XP if maintaining/increasing streak
            if (newStreak > profile.streakCount) {
                addXpToUser(userId, 5)
            }

            Result.success(newStreak)
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Error updating streak: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse ISO timestamp to milliseconds
     * Handles Supabase/PostgreSQL timestamp formats: 2024-01-15T12:34:56.789Z
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            if (timestamp.isBlank()) return 0L

            // Parse ISO 8601 timestamp using kotlinx-datetime
            val instant = Instant.parse(timestamp)
            instant.toEpochMilliseconds()
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Failed to parse timestamp '$timestamp': ${e.message}", e)
            // Return 0 on parse failure (will be treated as very old timestamp)
            0L
        }
    }

    // ==================== LEADERBOARD ====================

    /**
     * Get leaderboard with top users by XP
     */
    suspend fun getLeaderboard(limit: Int = 50): Result<List<ProfileDto>> {
        return try {
            val profiles = postgrest["profiles"]
                .select {
                    order("xp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ProfileDto>()
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

    /**
     * Check if user has a specific badge
     */
    suspend fun hasUserBadge(userId: String, badgeId: String): Result<Boolean> {
        return try {
            val existing = postgrest["user_badges"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("badge_id", badgeId)
                    }
                }
                .decodeSingleOrNull<UserBadgeDto>()
            Result.success(existing != null)
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
            // Get ratings from last 24 hours
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<RatingDto>()

            val todayCount = ratings.count { rating ->
                val ratingTime = parseTimestamp(rating.createdAt ?: "")
                val hoursSince = (Clock.System.now().toEpochMilliseconds() - ratingTime) / (1000 * 60 * 60)
                hoursSince < 24
            }

            Result.success(todayCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of unique restaurants the user has rated
     */
    suspend fun getUniqueRestaurantsRated(userId: String): Result<Int> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<RatingDto>()

            val uniqueRestaurants = ratings.map { it.restaurantId }.toSet()
            Result.success(uniqueRestaurants.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of ratings with photos
     */
    suspend fun getRatingsWithPhotosCount(userId: String): Result<Int> {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<RatingDto>()

            val withPhotos = ratings.count { it.imageUrl != null }
            Result.success(withPhotos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get unique cuisines the user has tried
     */
    suspend fun getUniqueCuisinesTried(userId: String): Result<Set<String>> {
        return try {
            // Get all ratings by user
            val ratings = postgrest["ratings"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<RatingDto>()

            // Get unique restaurant IDs
            val restaurantIds = ratings.map { it.restaurantId }.toSet()

            // Get cuisines for those restaurants
            val cuisines = mutableSetOf<String>()
            for (restaurantId in restaurantIds) {
                val restaurant = postgrest["restaurants"]
                    .select {
                        filter {
                            eq("id", restaurantId)
                        }
                    }
                    .decodeSingleOrNull<RestaurantDto>()

                restaurant?.cuisine?.let { cuisines.add(it) }
            }

            Result.success(cuisines)
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

    suspend fun refreshRestaurantRating(restaurantId: String): Result<Unit> {
        return try {
            updateRestaurantRating(restaurantId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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

    /**
     * Get the total number of ratings submitted by a user.
     */
    suspend fun getUserRatingCount(userId: String): Int {
        return try {
            val ratings = postgrest["ratings"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<RatingDto>()
            ratings.size
        } catch (e: Exception) {
            Logger.e("DatabaseRepository", "Failed to get user rating count: ${e.message}", e)
            0
        }
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
