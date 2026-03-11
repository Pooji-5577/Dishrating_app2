package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.DishRepository
import com.example.smackcheck2.model.ManualDishEntryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Manual Dish Entry screen (AI fallback).
 *
 * Manages form state, validates user input, and saves the dish
 * entry to Supabase via [DishRepository].
 *
 * Flow:
 *   1. Screen initializes with captured image URI
 *   2. User fills in dish name, restaurant, description, rating
 *   3. On submit → validate → save to Supabase → report success/failure
 */
class ManualDishEntryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ManualDishEntryUiState())
    val uiState: StateFlow<ManualDishEntryUiState> = _uiState.asStateFlow()

    // ── Initialization ─────────────────────────────────────────────────────

    /**
     * Pre-fill the form with the captured image URI.
     * Called once when the screen is first composed.
     */
    fun initialize(imageUri: String) {
        _uiState.update { it.copy(imageUri = imageUri) }
    }

    // ── Field change handlers ──────────────────────────────────────────────

    /** Update the dish name and clear its validation error. */
    fun onDishNameChange(name: String) {
        _uiState.update { it.copy(dishName = name, dishNameError = null) }
    }

    /** Update the restaurant name and clear its validation error. */
    fun onRestaurantNameChange(name: String) {
        _uiState.update { it.copy(restaurantName = name, restaurantNameError = null) }
    }

    /** Update the optional description. */
    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    /** Update the star rating. */
    fun onRatingChange(rating: Float) {
        _uiState.update { it.copy(rating = rating) }
    }

    // ── Submit ─────────────────────────────────────────────────────────────

    /**
     * Validate inputs and save the dish entry to Supabase.
     *
     * @param onSuccess Callback invoked after a successful save; the caller
     *                  typically uses this to navigate away from the screen.
     */
    fun submitDish(onSuccess: () -> Unit) {
        val current = _uiState.value

        // ── Input validation ──
        var hasError = false
        var dishNameError: String? = null
        var restaurantNameError: String? = null

        if (current.dishName.isBlank()) {
            dishNameError = "Dish name is required"
            hasError = true
        }

        if (current.restaurantName.isBlank()) {
            restaurantNameError = "Restaurant name is required"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    dishNameError = dishNameError,
                    restaurantNameError = restaurantNameError
                )
            }
            return
        }

        // ── Save to Supabase via coroutine ──
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val success = DishRepository.saveDish(
                    dishName = current.dishName.trim(),
                    restaurantName = current.restaurantName.trim(),
                    description = current.description.trim().ifBlank { null },
                    rating = if (current.rating > 0f) current.rating else null,
                    imageUri = current.imageUri.ifBlank { null },
                    isManualEntry = true,   // Always true — this is the fallback path
                    aiConfidence = null      // No AI confidence for manual entries
                )

                if (success) {
                    _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Failed to save dish. Please try again."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────

    /** Dismiss the current error message. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
