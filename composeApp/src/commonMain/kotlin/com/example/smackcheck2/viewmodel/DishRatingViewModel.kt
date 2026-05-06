package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.notifications.NotificationRepository
import com.example.smackcheck2.model.DishRatingUiState
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.service.AchievementService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Rating screen
 */
class DishRatingViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()
    private val storageRepository = StorageRepository()
    private val authRepository = AuthRepository()
    private val achievementService = AchievementService()

    private val _uiState = MutableStateFlow(DishRatingUiState())
    val uiState: StateFlow<DishRatingUiState> = _uiState.asStateFlow()

    private var restaurantId: String = ""
    private var selectedRestaurant: Restaurant? = null
    private var ratingLatitude: Double? = null
    private var ratingLongitude: Double? = null

    fun initialize(dishName: String, imageUri: String, restaurantId: String = "") {
        this.restaurantId = restaurantId
        _uiState.update {
            it.copy(
                dishName = dishName,
                imageUri = imageUri
            )
        }
    }

    fun setImageBytes(bytes: ByteArray) {
        _uiState.update { it.copy(imageBytes = bytes) }
    }

    fun setRestaurantId(id: String) {
        this.restaurantId = id
    }

    fun setRestaurant(restaurant: Restaurant) {
        this.selectedRestaurant = restaurant
        this.restaurantId = restaurant.id
    }

    fun setRatingLocation(lat: Double?, lng: Double?) {
        ratingLatitude = lat
        ratingLongitude = lng
    }

    fun onRatingChange(rating: Float) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun onCommentChange(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }

    fun onTagsChange(tags: List<String>) {
        _uiState.update { it.copy(tags = tags) }
    }

    fun onPriceChange(price: String) {
        // Only allow digits and a single decimal point
        val filtered = price.filter { it.isDigit() || it == '.' }
            .let { if (it.count { c -> c == '.' } > 1) it.dropLast(1) else it }
        _uiState.update { it.copy(price = filtered) }
    }

    fun submitRating(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value

        println("DishRatingViewModel: Starting submitRating - dishName='${currentState.dishName}', rating=${currentState.rating}, restaurantId=$restaurantId")

        if (currentState.dishName.isBlank()) {
            println("DishRatingViewModel: ✗ Dish name is blank")
            _uiState.update { it.copy(errorMessage = "Dish name cannot be empty") }
            return
        }

        if (currentState.rating == 0f) {
            println("DishRatingViewModel: ✗ No rating provided")
            _uiState.update { it.copy(errorMessage = "Please provide a rating") }
            return
        }

        if (restaurantId.isBlank()) {
            println("DishRatingViewModel: ✗ No restaurant selected")
            _uiState.update { it.copy(errorMessage = "Please select a restaurant") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            
            // Ensure user profile exists before attempting to rate
            // This handles users who signed up but profile creation failed or was skipped
            println("DishRatingViewModel: Ensuring user profile exists...")
            try {
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    println("DishRatingViewModel: ✗ Failed to get/create user profile")
                    _uiState.update { 
                        it.copy(
                            isSubmitting = false, 
                            errorMessage = "Failed to verify user profile. Please try signing out and back in."
                        ) 
                    }
                    return@launch
                }
                println("DishRatingViewModel: ✓ User profile verified: ${user.id}")
            } catch (e: Exception) {
                println("DishRatingViewModel: ✗ Error ensuring profile: ${e.message}")
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Could not verify your profile. Please try again."
                    )
                }
                return@launch
            }
            
            println("DishRatingViewModel: Validation passed, starting submission...")

            // Ensure user profile exists in database
            val user = authRepository.getCurrentUser()
            if (user == null) {
                println("DishRatingViewModel: ✗ User not signed in")
                _uiState.update { 
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Please sign in to submit a rating"
                    ) 
                }
                return@launch
            }
            val userId = user.id
            println("DishRatingViewModel: ✓ User profile confirmed - userId=$userId")

            try {
                // Upload image if available
                var imageUrl: String? = null
                if (currentState.imageBytes == null) {
                    println("DishRatingViewModel: ⚠ No imageBytes — photo will NOT be uploaded (imageUri=${currentState.imageUri})")
                } else {
                    val bytes = currentState.imageBytes
                    println("DishRatingViewModel: Uploading image (${bytes.size} bytes)...")
                    val uploadResult = storageRepository.uploadDishImage(
                        userId = userId,
                        imageBytes = bytes,
                        fileName = "${currentState.dishName}.jpg"
                    )
                    uploadResult.fold(
                        onSuccess = { url ->
                            imageUrl = url
                            println("DishRatingViewModel: ✓ Image uploaded: $url")
                        },
                        onFailure = { error ->
                            println("DishRatingViewModel: ✗ Image upload FAILED: ${error.message}")
                            // Don't block the rating — proceed without photo
                        }
                    )
                }

                // Ensure the restaurant exists in DB (e.g. if selected from Google Places)
                val restaurant = selectedRestaurant
                if (restaurant != null) {
                    println("DishRatingViewModel: Ensuring restaurant '${restaurant.name}' (${restaurant.id}) exists in DB...")
                    val ensureResult = databaseRepository.ensureRestaurantExists(restaurant, dishImageUrl = imageUrl)
                    ensureResult.fold(
                        onSuccess = { dbRestaurant ->
                            restaurantId = dbRestaurant.id
                            println("DishRatingViewModel: ✓ Restaurant ensured: ${dbRestaurant.id} — ${dbRestaurant.name}")
                        },
                        onFailure = { error ->
                            println("DishRatingViewModel: ✗ Failed to ensure restaurant: ${error.message}")
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    errorMessage = "Could not save restaurant. Please try again."
                                )
                            }
                            return@launch
                        }
                    )
                }

                // Create or get dish
                println("DishRatingViewModel: Creating/getting dish '${currentState.dishName}' for restaurant $restaurantId...")
                val dishResult = databaseRepository.createOrGetDish(
                    name = currentState.dishName,
                    restaurantId = restaurantId,
                    imageUrl = imageUrl,
                    restaurantName = selectedRestaurant?.name
                )

                val dish = dishResult.getOrElse { error ->
                    println("DishRatingViewModel: ✗ Failed to create dish: ${error.message}")
                    error.printStackTrace()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Could not save dish. Please try again."
                        )
                    }
                    return@launch
                }
                println("DishRatingViewModel: ✓ Dish created/retrieved: ${dish.id}")

                // Submit the rating
                println("DishRatingViewModel: Submitting rating...")
                val ratingResult = databaseRepository.submitRating(
                    userId = userId,
                    dishId = dish.id,
                    restaurantId = restaurantId,
                    rating = currentState.rating,
                    comment = currentState.comment,
                    imageUrl = imageUrl,
                    latitude = ratingLatitude,
                    longitude = ratingLongitude,
                    price = currentState.price.toDoubleOrNull()
                )

                ratingResult.fold(
                    onSuccess = { ratingId ->
                        println("DishRatingViewModel: ✓ Rating submitted successfully! ratingId=$ratingId")

                        // Calculate variable XP rewards
                        // Unified formula: base 10 + photo 5 + comment(>50 chars) 10 + tags * 2
                        val baseXp = 10
                        val photoBonus = if (imageUrl != null) 5 else 0
                        val commentBonus = if (currentState.comment.length > 50) 10 else 0
                        val tagBonus = currentState.tags.size * 2
                        val totalXp = baseXp + photoBonus + commentBonus + tagBonus

                        println("DishRatingViewModel: Awarding $totalXp XP (base: $baseXp, photo: $photoBonus, comment: $commentBonus, tags: $tagBonus)...")

                        // PROPERLY AWAIT the XP award
                        val xpResult = databaseRepository.addXpToUser(userId, totalXp)
                        xpResult.fold(
                            onSuccess = {
                                println("DishRatingViewModel: ✓ XP awarded successfully!")
                            },
                            onFailure = { error ->
                                println("DishRatingViewModel: ✗ Failed to award XP: ${error.message}")
                                error.printStackTrace()
                                // Log error but don't block success flow
                            }
                        )

                        // Update user's streak
                        viewModelScope.launch {
                            val streakResult = databaseRepository.updateUserStreak(userId)
                            streakResult.fold(
                                onSuccess = { newStreak ->
                                    println("DishRatingViewModel: ✓ Streak updated to $newStreak days")
                                },
                                onFailure = { error ->
                                    println("DishRatingViewModel: ✗ Failed to update streak: ${error.message}")
                                }
                            )

                            // Check and award achievements
                            val achievementResult = achievementService.checkAndAwardAchievements(userId)
                            achievementResult.fold(
                                onSuccess = { newAchievements ->
                                    if (newAchievements.isNotEmpty()) {
                                        println("DishRatingViewModel: ✓ Unlocked ${newAchievements.size} achievements: $newAchievements")
                                    }
                                },
                                onFailure = { error ->
                                    println("DishRatingViewModel: ✗ Failed to check achievements: ${error.message}")
                                }
                            )
                        }

                        _uiState.update { it.copy(isSubmitting = false, isSuccess = true, xpEarned = totalXp, showXpNotification = true, submittedRatingId = ratingId) }

                        Analytics.track("post_created", mapOf(
                            "rating" to currentState.rating,
                            "has_photo" to (imageUrl != null),
                            "has_comment" to currentState.comment.isNotEmpty(),
                            "xp_earned" to totalXp,
                            "tags_count" to currentState.tags.size
                        ))

                        println("DishRatingViewModel: ✓ Complete! Calling onSuccess callback with ratingId=$ratingId")
                        onSuccess(ratingId)

                        // Notify rater of successful submission
                        viewModelScope.launch {
                            NotificationRepository.notifyRatingSubmitted(
                                userId = userId,
                                dishName = currentState.dishName,
                                ratingId = ratingId
                            )
                        }
                        // Notify followers of new post
                        viewModelScope.launch {
                            NotificationRepository.notifyNewPost(
                                posterId = userId,
                                posterName = user.email ?: "",
                                dishName = currentState.dishName,
                                restaurantName = selectedRestaurant?.name ?: "",
                                ratingId = ratingId
                            )
                        }
                        // Check if this is the user's first dish and send congratulatory notification
                        viewModelScope.launch {
                            try {
                                val ratingCount = databaseRepository.getUserRatingCount(userId)
                                if (ratingCount == 1) {
                                    NotificationRepository.notifyFirstDish(userId, currentState.dishName)
                                }
                            } catch (e: Exception) {
                                println("DishRatingViewModel: First dish check failed: ${e.message}")
                            }
                        }
                    },
                    onFailure = { error ->
                        println("DishRatingViewModel: ✗ Rating submission failed: ${error.message}")
                        error.printStackTrace()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = "Failed to submit rating. Please try again."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                println("DishRatingViewModel: ✗ Unexpected error: ${e.message}")
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearXpNotification() {
        _uiState.update { it.copy(showXpNotification = false) }
    }

    fun resetForm() {
        _uiState.value = DishRatingUiState()
        restaurantId = ""
        selectedRestaurant = null
        ratingLatitude = null
        ratingLongitude = null
    }
}
