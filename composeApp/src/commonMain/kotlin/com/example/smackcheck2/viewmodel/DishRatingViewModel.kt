package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.model.DishRatingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Rating screen
 */
class DishRatingViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(DishRatingUiState())
    val uiState: StateFlow<DishRatingUiState> = _uiState.asStateFlow()
    
    fun initialize(dishName: String, imageUri: String) {
        _uiState.update {
            it.copy(
                dishName = dishName,
                imageUri = imageUri
            )
        }
    }
    
    fun onRatingChange(rating: Float) {
        _uiState.update { it.copy(rating = rating) }
    }
    
    fun onCommentChange(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }
    
    fun submitRating(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        
        if (currentState.rating == 0f) {
            _uiState.update { it.copy(errorMessage = "Please provide a rating") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            
            try {
                // Simulate API call
                delay(1500)
                _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
                onSuccess()
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
