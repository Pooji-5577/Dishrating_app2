package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.model.DishRatingUiState
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

    private val _uiState = MutableStateFlow(DishRatingUiState())
    val uiState: StateFlow<DishRatingUiState> = _uiState.asStateFlow()

    private var restaurantId: String = ""
    private var imageBytes: ByteArray? = null

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
        this.imageBytes = bytes
    }

    fun setRestaurantId(id: String) {
        this.restaurantId = id
    }

    fun onRatingChange(rating: Float) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun onCommentChange(comment: String) {
        _uiState.update { it.copy(comment = comment) }
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
                imageBytes?.let { bytes ->
                    println("DishRatingViewModel: Uploading image (${bytes.size} bytes)...")
                    val uploadResult = storageRepository.uploadDishImage(
                        userId = userId,
                        imageBytes = bytes,
                        fileName = "${currentState.dishName}.jpg"
                    )
                    imageUrl = uploadResult.getOrNull()
                    println("DishRatingViewModel: Image upload result: ${imageUrl ?: "failed"}")
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
                        // Award XP for submitting a rating
                        println("DishRatingViewModel: Awarding 10 XP...")
                        databaseRepository.addXpToUser(userId, 10)

                        _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
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
}
