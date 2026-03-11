package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AIDetectionRepository
import com.example.smackcheck2.model.DishCaptureUiState
import com.example.smackcheck2.platform.ImageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Capture screen with AI detection
 */
class DishCaptureViewModel : ViewModel() {

    private val aiDetectionRepository = AIDetectionRepository()

    private val _uiState = MutableStateFlow(DishCaptureUiState())
    val uiState: StateFlow<DishCaptureUiState> = _uiState.asStateFlow()

    /**
     * Called when user captures or picks an image
     * Triggers AI detection automatically
     */
    fun onImageCaptured(imageResult: ImageResult) {
        _uiState.update {
            it.copy(
                imageUri = imageResult.uri,
                imageBytes = imageResult.bytes,
                isAnalyzing = true,
                detectedDishName = null,
                showConfirmation = false,
                errorMessage = null
            )
        }

        // Start AI detection
        viewModelScope.launch {
            try {
                val result = aiDetectionRepository.detectDish(
                    imageBytes = imageResult.bytes,
                    mimeType = imageResult.mimeType
                )

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        detectedDishName = result.dishName,
                        detectedCuisine = result.cuisine,
                        detectionConfidence = result.confidence,
                        alternatives = result.alternatives,
                        isAIDetected = result.isAIDetected,
                        itemType = result.itemType,
                        editedName = result.dishName,
                        debugInfo = result.debugInfo,
                        showConfirmation = true,
                        // Only show "Not a dish" modal when itemType is unknown (truly unrecognised)
                        showNotDishError = result.itemType == "unknown"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        detectedDishName = "Unknown",
                        isAIDetected = false,
                        itemType = "unknown",
                        editedName = "Unknown",
                        showConfirmation = true,
                        showNotDishError = true,
                        errorMessage = "AI detection failed. Please enter dish name manually.",
                        debugInfo = "ViewModel Exception: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Called when user taps edit button to change dish name
     */
    fun onEditClick() {
        _uiState.update { it.copy(isEditingName = true) }
    }

    /**
     * Called when user changes the dish name in edit mode
     */
    fun onDishNameEdited(name: String) {
        _uiState.update { it.copy(editedName = name) }
    }

    /**
     * Called when user confirms the edited dish name
     */
    fun confirmDishName() {
        _uiState.update {
            it.copy(
                isEditingName = false,
                detectedDishName = it.editedName
            )
        }
    }

    /**
     * Called when user cancels editing
     */
    fun cancelEdit() {
        _uiState.update {
            it.copy(
                isEditingName = false,
                editedName = it.detectedDishName ?: ""
            )
        }
    }

    /**
     * Called when user wants to retake/pick another image
     */
    fun retake() {
        _uiState.update {
            DishCaptureUiState()
        }
    }

    /**
     * Get the final dish name to use (edited or detected)
     */
    fun getFinalDishName(): String {
        val state = _uiState.value
        return state.detectedDishName ?: state.editedName
    }

    /**
     * Get the image bytes for uploading
     */
    fun getImageBytes(): ByteArray? {
        return _uiState.value.imageBytes
    }

    /**
     * Get the image URI for display
     */
    fun getImageUri(): String? {
        return _uiState.value.imageUri
    }

    /**
     * Called when user dismisses the "not a dish" error modal.
     * Resets state so the user can retake/pick a new photo.
     */
    fun dismissNotDishError() {
        _uiState.update { DishCaptureUiState() }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
