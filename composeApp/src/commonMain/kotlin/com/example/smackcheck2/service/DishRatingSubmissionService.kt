package com.example.smackcheck2.service

import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.PendingRating
import com.example.smackcheck2.model.PendingRatingSyncStatus
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.data.repository.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import com.example.smackcheck2.util.Logger

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
    val longitude: Double? = null,
    val receiptAttachment: ReceiptAttachment? = null
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
            longitude == other.longitude &&
            receiptAttachment == other.receiptAttachment
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
        result = 31 * result + (receiptAttachment?.hashCode() ?: 0)
        return result
    }
}

data class ReceiptAttachment(
    val imageBytes: ByteArray,
    val source: ReceiptSource
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReceiptAttachment) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false
        return source == other.source
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + source.hashCode()
        return result
    }
}

enum class ReceiptSource {
    CAMERA,
    GALLERY,
    SCREENSHOT
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

internal data class SubmissionRestaurant(
    val id: String,
    val name: String,
    val city: String = ""
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
    private val notificationService: NotificationService = NotificationService(),
    private val socialRepository: SocialRepository = SocialRepository(),
    private val preferencesRepository: PreferencesRepository? = null,
    private val sideEffectScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

        val restaurant = resolveRestaurant(request, imageUrl)
            .getOrElse { return Result.failure(it) }

        val dish = createOrGetDish(request, restaurant, imageUrl)
            .getOrElse { return Result.failure(it) }

        val ratingId = insertRating(request, dish.id, restaurant.id, userId, imageUrl)
            .getOrElse { return Result.failure(it) }

        val receiptResult = uploadAndValidateReceiptIfPresent(
            request = request,
            userId = userId,
            ratingId = ratingId
        ).getOrElse { return Result.failure(it) }

        val xpEarned = calculateXp(request, imageUrl, receiptResult != null)
        trackAnalytics(request, imageUrl, xpEarned)
        runPostSubmissionSideEffects(
            userId = userId,
            userEmail = user.email ?: "",
            request = request,
            ratingId = ratingId,
            restaurant = restaurant,
            imageUrl = imageUrl,
            xpEarned = xpEarned
        )

        return Result.success(
            DishRatingSubmissionResult(
                ratingId = ratingId,
                xpEarned = xpEarned,
                imageUrl = imageUrl
            )
        )
    }

    suspend fun enqueueForBackgroundSubmission(
        request: DishRatingSubmissionRequest
    ): Result<PendingRating> {
        val store = preferencesRepository
            ?: return Result.failure(IllegalStateException("Pending rating storage is not configured"))

        val validationError = validate(request)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val user = authRepository.getCurrentUser()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        val now = Clock.System.now().toEpochMilliseconds()
        val localId = "pending_rating_$now"
        val imagePath = request.imageBytes?.let { bytes ->
            store.savePendingRatingImage(localId, bytes)
        }
        val restaurant = request.selectedRestaurant

        val pendingRating = PendingRating(
            localId = localId,
            userId = user.id,
            userName = user.name.ifBlank { user.email.substringBefore("@").ifBlank { "You" } },
            userProfilePhotoUrl = user.profilePhotoUrl,
            dishName = request.dishName,
            rating = request.rating,
            comment = request.comment,
            tags = request.tags,
            price = request.price,
            imagePath = imagePath,
            restaurantId = request.restaurantId,
            restaurantName = restaurant?.name ?: "",
            restaurantCity = restaurant?.city ?: "",
            restaurantCuisine = restaurant?.cuisine ?: "",
            restaurantLatitude = restaurant?.latitude,
            restaurantLongitude = restaurant?.longitude,
            restaurantGooglePlaceId = restaurant?.googlePlaceId,
            restaurantPhotoUrl = restaurant?.photoUrl,
            latitude = request.latitude,
            longitude = request.longitude,
            createdAt = now
        )

        store.upsertPendingRating(pendingRating)
        sideEffectScope.launch {
            processPendingSubmissions()
        }
        return Result.success(pendingRating)
    }

    fun kickPendingSubmissionSync() {
        if (preferencesRepository == null) return
        sideEffectScope.launch {
            processPendingSubmissions()
        }
    }

    suspend fun processPendingSubmissions() {
        val store = preferencesRepository ?: return
        val pending = store.getPendingRatings()
        pending.forEach { item ->
            val syncing = item.copy(status = PendingRatingSyncStatus.SYNCING, lastError = null)
            store.upsertPendingRating(syncing)

            val imageBytes = syncing.imagePath?.let { store.readPendingRatingImage(it) }
            val result = submit(syncing.toSubmissionRequest(imageBytes))
            result.fold(
                onSuccess = {
                    store.removePendingRating(syncing.localId)
                    syncing.imagePath?.let { path -> store.deletePendingRatingImage(path) }
                },
                onFailure = { error ->
                    store.upsertPendingRating(
                        syncing.copy(
                            status = PendingRatingSyncStatus.FAILED,
                            lastError = error.message ?: "Sync failed"
                        )
                    )
                }
            )
        }
    }

    private fun validate(request: DishRatingSubmissionRequest): String? {
        if (request.dishName.isBlank()) return "Dish name cannot be empty"
        if (request.rating == 0f) return "Please provide a rating"
        if (request.restaurantId.isBlank() && request.selectedRestaurant == null) return "Please select a restaurant"
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

    private suspend fun resolveRestaurant(
        request: DishRatingSubmissionRequest,
        imageUrl: String?
    ): Result<SubmissionRestaurant> {
        val selected = request.selectedRestaurant
        if (selected != null) {
            return databaseRepository.ensureRestaurantExists(selected, dishImageUrl = imageUrl)
                .map { restaurant ->
                    SubmissionRestaurant(
                        id = restaurant.id,
                        name = restaurant.name,
                        city = restaurant.city
                    )
                }
        }

        return databaseRepository.getRestaurantById(request.restaurantId)
            .mapCatching { restaurant ->
                val existing = restaurant
                    ?: throw IllegalArgumentException("Selected restaurant no longer exists")
                SubmissionRestaurant(
                    id = existing.id,
                    name = existing.name,
                    city = existing.city
                )
            }
    }

    private suspend fun createOrGetDish(
        request: DishRatingSubmissionRequest,
        restaurant: SubmissionRestaurant,
        imageUrl: String?
    ): Result<Dish> {
        return databaseRepository.createOrGetDish(
            name = request.dishName,
            restaurantId = restaurant.id,
            imageUrl = imageUrl,
            restaurantName = restaurant.name
        )
    }

    private suspend fun insertRating(
        request: DishRatingSubmissionRequest,
        dishId: String,
        restaurantId: String,
        userId: String,
        imageUrl: String?
    ): Result<String> {
        return databaseRepository.submitRating(
            userId = userId,
            dishId = dishId,
            restaurantId = restaurantId,
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
        imageUrl: String?,
        hasValidatedReceipt: Boolean
    ): Int {
        val baseXp = 10
        val photoBonus = if (imageUrl != null) 5 else 0
        val commentBonus = if (request.comment.length > 50) 10 else 0
        val tagBonus = request.tags.size * 2
        val receiptBonus = if (hasValidatedReceipt) 15 else 0
        return baseXp + photoBonus + commentBonus + tagBonus + receiptBonus
    }

    private suspend fun uploadAndValidateReceiptIfPresent(
        request: DishRatingSubmissionRequest,
        userId: String,
        ratingId: String
    ): Result<Unit?> {
        val receipt = request.receiptAttachment ?: return Result.success(null)
        val uploadResult = storageRepository.uploadReceiptImage(
            userId = userId,
            imageBytes = receipt.imageBytes,
            fileName = "receipt_$ratingId.jpg"
        )
        val receiptUrl = uploadResult.getOrElse { return Result.failure(it) }

        return databaseRepository.validateRatingReceipt(
            ratingId = ratingId,
            receiptImageUrl = receiptUrl,
            source = receipt.source.name.lowercase()
        ).map { Unit }
    }

    private suspend fun awardXp(userId: String, xpAmount: Int) {
        databaseRepository.addXpToUser(userId, xpAmount)
            .onFailure { Logger.e("DishRatingSubmissionService", "Failed to award XP: ${it.message}", it) }
    }

    private suspend fun updateUserStreak(userId: String) {
        databaseRepository.updateUserStreak(userId)
            .onFailure { Logger.e("DishRatingSubmissionService", "Failed to update streak: ${it.message}", it) }
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

    private fun runPostSubmissionSideEffects(
        userId: String,
        userEmail: String,
        request: DishRatingSubmissionRequest,
        ratingId: String,
        restaurant: SubmissionRestaurant,
        imageUrl: String?,
        xpEarned: Int
    ) {
        sideEffectScope.launch {
            databaseRepository.refreshRestaurantRating(restaurant.id)
                .onFailure { Logger.e("DishRatingSubmissionService", "Failed to refresh restaurant rating: ${it.message}", it) }

            if (imageUrl != null) {
                socialRepository.uploadStory(
                    userId = userId,
                    imageUrl = imageUrl,
                    dishName = request.dishName,
                    rating = request.rating,
                    city = restaurant.city.ifBlank { request.selectedRestaurant?.city ?: "" }
                )
                    .onSuccess { Logger.d("DishRatingSubmissionService", "Auto-published story for rating $ratingId") }
                    .onFailure { Logger.e("DishRatingSubmissionService", "Failed to auto-publish story: ${it.message}", it) }
            }

            awardXp(userId, xpEarned)
            updateUserStreak(userId)
            val achievements = checkAchievements(userId)

            sendPostSubmissionNotifications(
                userId = userId,
                userEmail = userEmail,
                request = request,
                ratingId = ratingId,
                restaurant = restaurant,
                achievements = achievements
            )
        }
    }

    private suspend fun sendPostSubmissionNotifications(
        userId: String,
        userEmail: String,
        request: DishRatingSubmissionRequest,
        ratingId: String,
        restaurant: SubmissionRestaurant,
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
            restaurantName = restaurant.name,
            ratingId = ratingId
        )

        val ratingCount = databaseRepository.getUserRatingCount(userId)
        if (ratingCount == 1) {
            notificationService.notifyFirstDish(userId, request.dishName)
        }
    }
}

private fun PendingRating.toSubmissionRequest(imageBytes: ByteArray?): DishRatingSubmissionRequest {
    val restaurant = Restaurant(
        id = restaurantId,
        name = restaurantName,
        city = restaurantCity,
        cuisine = restaurantCuisine,
        latitude = restaurantLatitude,
        longitude = restaurantLongitude,
        googlePlaceId = restaurantGooglePlaceId,
        photoUrl = restaurantPhotoUrl
    )

    return DishRatingSubmissionRequest(
        dishName = dishName,
        rating = rating,
        comment = comment,
        tags = tags,
        price = price,
        imageBytes = imageBytes,
        restaurantId = restaurantId,
        selectedRestaurant = restaurant.takeIf { it.name.isNotBlank() },
        latitude = latitude,
        longitude = longitude
    )
}
