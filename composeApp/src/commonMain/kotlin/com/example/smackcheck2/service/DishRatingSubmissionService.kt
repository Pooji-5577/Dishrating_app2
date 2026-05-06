package com.example.smackcheck2.service

import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.data.repository.NotificationService

/**
 * Request object for submitting a dish rating.
 * Encapsulates all input needed for the full submission flow.
 */
data class DishRatingSubmissionRequest(
    val dishName: String,
    val rating: Float,
    val comment: String = "",
    val tags: List<String> = emptyList(),
    val price: Double? = null,
    val imageBytes: ByteArray? = null,
    val restaurantId: String,
    val selectedRestaurant: Restaurant? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DishRatingSubmissionRequest) return false
        return dishName == other.dishName &&
            rating == other.rating &&
            comment == other.comment &&
            tags == other.tags &&
            price == other.price &&
            imageBytes?.contentEquals(other.imageBytes) != false &&
            restaurantId == other.restaurantId &&
            selectedRestaurant == other.selectedRestaurant &&
            latitude == other.latitude &&
            longitude == other.longitude
    }

    override fun hashCode(): Int {
        var result = dishName.hashCode()
        result = 31 * result + rating.hashCode()
        result = 31 * result + comment.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + restaurantId.hashCode()
        result = 31 * result + (selectedRestaurant?.hashCode() ?: 0)
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        return result
    }
}

/**
 * Result of a dish rating submission.
 * Contains the rating ID, XP earned, and any newly unlocked achievements.
 */
data class DishRatingSubmissionResult(
    val ratingId: String,
    val xpEarned: Int,
    val newlyUnlockedAchievements: List<String> = emptyList(),
    val imageUrl: String? = null
)

/**
 * Deep module that owns the dish rating submission workflow end-to-end.
 *
 * Responsibilities:
 * - User profile verification
 * - Image upload
 * - Restaurant creation/verification
 * - Dish creation/deduplication
 * - Rating insertion
 * - XP rewards calculation and awarding
 * - Streak updates
 * - Achievement checking and awarding
 * - Analytics tracking
 * - Post-submission notifications
 *
 * Callers pass a [DishRatingSubmissionRequest] once and receive a
 * [DishRatingSubmissionResult] or an error. All internal steps and
 * side effects are managed within this service.
 */
class DishRatingSubmissionService(
    private val databaseRepository: DatabaseRepository = DatabaseRepository(),
    private val storageRepository: StorageRepository = StorageRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val achievementService: AchievementService = AchievementService(),
    private val notificationService: NotificationService = NotificationService()
) {

    /**
     * Submit a dish rating with all side effects handled internally.
     *
     * @return Result containing the submission result or an error
     */
    suspend fun submit(request: DishRatingSubmissionRequest): Result<DishRatingSubmissionResult> {
        val validationError = validate(request)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val user = authRepository.getCurrentUser()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        val userId = user.id

        val imageUrl = uploadImageIfPresent(request, userId)

        ensureRestaurantExists(request, imageUrl)

        val dish = createOrGetDish(request, imageUrl)
            .getOrElse { return Result.failure(it) }

        val ratingId = insertRating(request, dish.id, userId, imageUrl)
            .getOrElse { return Result.failure(it) }

        val xpEarned = calculateXp(request, imageUrl)
        awardXp(userId, xpEarned)

        val streakResult = updateUserStreak(userId)
        val achievements = checkAchievements(userId)

        trackAnalytics(request, imageUrl, xpEarned)

        sendPostSubmissionNotifications(
            userId = userId,
            userEmail = user.email ?: "",
            request = request,
            ratingId = ratingId,
            achievements = achievements
        )

        return Result.success(
            DishRatingSubmissionResult(
                ratingId = ratingId,
                xpEarned = xpEarned,
                newlyUnlockedAchievements = achievements,
                imageUrl = imageUrl
            )
        )
    }

    private fun validate(request: DishRatingSubmissionRequest): String? {
        if (request.dishName.isBlank()) return "Dish name cannot be empty"
        if (request.rating == 0f) return "Please provide a rating"
        if (request.restaurantId.isBlank()) return "Please select a restaurant"
        return null
    }

    private suspend fun uploadImageIfPresent(
        request: DishRatingSubmissionRequest,
        userId: String
    ): String? {
        val bytes = request.imageBytes ?: return null
        val uploadResult = storageRepository.uploadDishImage(
            userId = userId,
            imageBytes = bytes,
            fileName = "${request.dishName}.jpg"
        )
        return uploadResult.getOrNull()
    }

    private suspend fun ensureRestaurantExists(
        request: DishRatingSubmissionRequest,
        imageUrl: String?
    ) {
        val restaurant = request.selectedRestaurant ?: return
        val result = databaseRepository.ensureRestaurantExists(restaurant, dishImageUrl = imageUrl)
        result.getOrThrow()
    }

    private suspend fun createOrGetDish(
        request: DishRatingSubmissionRequest,
        imageUrl: String?
    ): Result<Dish> {
        return databaseRepository.createOrGetDish(
            name = request.dishName,
            restaurantId = request.restaurantId,
            imageUrl = imageUrl,
            restaurantName = request.selectedRestaurant?.name
        )
    }

    private suspend fun insertRating(
        request: DishRatingSubmissionRequest,
        dishId: String,
        userId: String,
        imageUrl: String?
    ): Result<String> {
        return databaseRepository.submitRating(
            userId = userId,
            dishId = dishId,
            restaurantId = request.restaurantId,
            rating = request.rating,
            comment = request.comment,
            imageUrl = imageUrl,
            latitude = request.latitude,
            longitude = request.longitude,
            price = request.price
        )
    }

    private fun calculateXp(
        request: DishRatingSubmissionRequest,
        imageUrl: String?
    ): Int {
        val baseXp = 10
        val photoBonus = if (imageUrl != null) 5 else 0
        val commentBonus = if (request.comment.length > 50) 10 else 0
        val tagBonus = request.tags.size * 2
        return baseXp + photoBonus + commentBonus + tagBonus
    }

    private suspend fun awardXp(userId: String, xpAmount: Int) {
        databaseRepository.addXpToUser(userId, xpAmount)
            .onFailure { println("DishRatingSubmissionService: Failed to award XP: ${it.message}") }
    }

    private suspend fun updateUserStreak(userId: String) {
        databaseRepository.updateUserStreak(userId)
            .onFailure { println("DishRatingSubmissionService: Failed to update streak: ${it.message}") }
    }

    private suspend fun checkAchievements(userId: String): List<String> {
        return achievementService.checkAndAwardAchievements(userId)
            .getOrDefault(emptyList())
    }

    private fun trackAnalytics(
        request: DishRatingSubmissionRequest,
        imageUrl: String?,
        xpEarned: Int
    ) {
        Analytics.track("post_created", mapOf(
            "rating" to request.rating,
            "has_photo" to (imageUrl != null),
            "has_comment" to request.comment.isNotEmpty(),
            "xp_earned" to xpEarned,
            "tags_count" to request.tags.size
        ))
    }

    private suspend fun sendPostSubmissionNotifications(
        userId: String,
        userEmail: String,
        request: DishRatingSubmissionRequest,
        ratingId: String,
        achievements: List<String>
    ) {
        notificationService.notifyRatingSubmitted(
            userId = userId,
            dishName = request.dishName,
            ratingId = ratingId
        )

        notificationService.notifyNewPost(
            posterId = userId,
            posterName = userEmail,
            dishName = request.dishName,
            restaurantName = request.selectedRestaurant?.name ?: "",
            ratingId = ratingId
        )

        val ratingCount = databaseRepository.getUserRatingCount(userId)
        if (ratingCount == 1) {
            notificationService.notifyFirstDish(userId, request.dishName)
        }
    }
}
