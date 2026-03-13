package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.StorageRepository
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

    fun onRatingChange(rating: Float) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun onCommentChange(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }

    fun onTagsChange(tags: List<String>) {
        _uiState.update { it.copy(tags = tags) }
    }

    fun submitRating(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val userId = authRepository.getCurrentUserId()

        println("DishRatingViewModel: Starting submitRating - userId=$userId, rating=${currentState.rating}, restaurantId=$restaurantId")

        if (userId == null) {
            println("DishRatingViewModel: ✗ User not signed in")
            _uiState.update { it.copy(errorMessage = "Please sign in to submit a rating") }
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
            println("DishRatingViewModel: Validation passed, starting submission...")

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
                                    errorMessage = "Failed to save restaurant: ${error.message}"
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
                    imageUrl = imageUrl
                )

                val dish = dishResult.getOrElse { error ->
                    println("DishRatingViewModel: ✗ Failed to create dish: ${error.message}")
                    error.printStackTrace()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Failed to create dish: ${error.message}"
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
                    imageUrl = imageUrl
                )

                ratingResult.fold(
                    onSuccess = {
                        println("DishRatingViewModel: ✓ Rating submitted successfully!")

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

                        _uiState.update { it.copy(isSubmitting = false, isSuccess = true, xpEarned = totalXp, showXpNotification = true) }
                        println("DishRatingViewModel: ✓ Complete! Calling onSuccess callback")
                        onSuccess()
                    },
                    onFailure = { error ->
                        println("DishRatingViewModel: ✗ Rating submission failed: ${error.message}")
                        error.printStackTrace()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = error.message ?: "Failed to submit rating"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to submit rating"
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
}
