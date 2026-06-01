package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.model.DishRatingUiState
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.service.DishRatingSubmissionRequest
import com.example.smackcheck2.service.DishRatingSubmissionService
import com.example.smackcheck2.service.ReceiptAttachment
import com.example.smackcheck2.service.ReceiptSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Rating screen
 *
 * Delegates submission orchestration to [DishRatingSubmissionService],
 * keeping only UI state management and input handling.
 */
class DishRatingViewModel(
    private val preferencesRepository: PreferencesRepository? = null
) : ViewModel() {

    private val submissionService = DishRatingSubmissionService(
        preferencesRepository = preferencesRepository
    )

    private val _uiState = MutableStateFlow(DishRatingUiState())
    val uiState: StateFlow<DishRatingUiState> = _uiState.asStateFlow()

    private var restaurantId: String = ""
    private var selectedRestaurant: Restaurant? = null
    private var ratingLatitude: Double? = null
    private var ratingLongitude: Double? = null
    private var initializedImageUri: String? = null
    private var hasUserEditedDishName = false

    fun initialize(dishName: String, imageUri: String, restaurantId: String = "") {
        this.restaurantId = restaurantId
        if (initializedImageUri != imageUri) {
            initializedImageUri = imageUri
            hasUserEditedDishName = false
        }
        val currentDishName = _uiState.value.dishName
        val shouldApplyName = currentDishName.isBlank() ||
            currentDishName == "Identifying..." ||
            (!hasUserEditedDishName && dishName != "Identifying...")
        _uiState.update {
            it.copy(
                dishName = if (shouldApplyName) dishName else it.dishName,
                imageUri = imageUri
            )
        }
    }

    fun onDishNameChange(name: String) {
        hasUserEditedDishName = true
        _uiState.update { it.copy(dishName = name) }
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
        val filtered = price.filter { it.isDigit() || it == '.' }
            .let { if (it.count { c -> c == '.' } > 1) it.dropLast(1) else it }
        _uiState.update { it.copy(price = filtered) }
    }

    fun submitRating(onSuccess: (String) -> Unit) {
        submitRating(receiptBytes = null, receiptSource = null, onSuccess = onSuccess)
    }

    fun submitRating(
        receiptBytes: ByteArray?,
        receiptSource: ReceiptSource?,
        onSuccess: (String) -> Unit
    ) {
        val currentState = _uiState.value

        if (currentState.dishName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Dish name cannot be empty") }
            return
        }

        if (currentState.rating == 0f) {
            _uiState.update { it.copy(errorMessage = "Please provide a rating") }
            return
        }

        if (restaurantId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select a restaurant") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val request = DishRatingSubmissionRequest(
                dishName = currentState.dishName,
                rating = currentState.rating,
                comment = currentState.comment,
                tags = currentState.tags,
                price = currentState.price.toDoubleOrNull(),
                imageBytes = currentState.imageBytes,
                restaurantId = this@DishRatingViewModel.restaurantId,
                selectedRestaurant = this@DishRatingViewModel.selectedRestaurant,
                latitude = this@DishRatingViewModel.ratingLatitude,
                longitude = this@DishRatingViewModel.ratingLongitude,
                receiptAttachment = if (receiptBytes != null && receiptSource != null) {
                    ReceiptAttachment(receiptBytes, receiptSource)
                } else {
                    null
                }
            )

            val result = if (preferencesRepository != null) {
                submissionService.enqueueForBackgroundSubmission(request)
                    .map { pending -> pending.localId }
            } else {
                submissionService.submit(request)
                    .map { submission -> submission.ratingId }
            }

            result.fold(
                onSuccess = { ratingId ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSuccess = preferencesRepository == null,
                            xpEarned = null,
                            showXpNotification = false,
                            submittedRatingId = ratingId
                        )
                    }
                    onSuccess(ratingId)
                },
                onFailure = { error ->
                    val message = when (error) {
                        is IllegalStateException -> error.message
                        is IllegalArgumentException -> error.message
                        else -> "Failed to submit rating. Please try again."
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = message
                        )
                    }
                }
            )
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
        initializedImageUri = null
        hasUserEditedDishName = false
    }
}
