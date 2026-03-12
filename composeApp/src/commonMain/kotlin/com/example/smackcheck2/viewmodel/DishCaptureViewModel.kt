package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AIDetectionRepository
import com.example.smackcheck2.model.CapturedImage
import com.example.smackcheck2.model.DishCaptureUiState
import com.example.smackcheck2.platform.ImageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Capture screen with AI detection
 * Supports multiple image capture for dish ratings
 */
class DishCaptureViewModel : ViewModel() {

    private val aiDetectionRepository = AIDetectionRepository()

    private val _uiState = MutableStateFlow(DishCaptureUiState())
    val uiState: StateFlow<DishCaptureUiState> = _uiState.asStateFlow()
    
    companion object {
        const val MAX_IMAGES = 5
    }

    /**
     * Called when user captures or picks an image
     * Triggers AI detection automatically for the first image
     */
    fun onImageCaptured(imageResult: ImageResult) {
        _uiState.update {
            it.copy(
                imageUri = imageResult.uri,
                imageBytes = imageResult.bytes,
                isAnalyzing = true,
                detectedDishName = null,
                showConfirmation = false,
                errorMessage = null,
                selectedImageIndex = 0
            )
        }

        // Start AI detection
        runAIDetection(imageResult)
    }
    
    /**
     * Called when user adds additional images from gallery
     */
    fun onAdditionalImagesSelected(images: List<ImageResult>) {
        if (images.isEmpty()) return
        
        val currentState = _uiState.value
        val currentAdditionalCount = currentState.additionalImages.size
        val hasPrimaryImage = currentState.imageUri != null
        
        // Calculate how many more images we can add
        val totalCurrentImages = if (hasPrimaryImage) 1 + currentAdditionalCount else currentAdditionalCount
        val remainingSlots = MAX_IMAGES - totalCurrentImages
        
        if (remainingSlots <= 0) {
            _uiState.update {
                it.copy(errorMessage = "Maximum $MAX_IMAGES images allowed")
            }
            return
        }
        
        // Take only as many as we have room for
        val imagesToAdd = images.take(remainingSlots).map { result ->
            CapturedImage(uri = result.uri, bytes = result.bytes)
        }
        
        _uiState.update {
            it.copy(
                additionalImages = it.additionalImages + imagesToAdd,
                errorMessage = null
            )
        }
    }
    
    /**
     * Called when user captures an additional image with camera
     */
    fun onAdditionalImageCaptured(imageResult: ImageResult) {
        val currentState = _uiState.value
        val currentAdditionalCount = currentState.additionalImages.size
        val hasPrimaryImage = currentState.imageUri != null
        
        val totalCurrentImages = if (hasPrimaryImage) 1 + currentAdditionalCount else currentAdditionalCount
        
        if (totalCurrentImages >= MAX_IMAGES) {
            _uiState.update {
                it.copy(errorMessage = "Maximum $MAX_IMAGES images allowed")
            }
            return
        }
        
        val newImage = CapturedImage(uri = imageResult.uri, bytes = imageResult.bytes)
        
        _uiState.update {
            it.copy(
                additionalImages = it.additionalImages + newImage,
                errorMessage = null
            )
        }
    }
    
    /**
     * Select an image by index for viewing
     */
    fun selectImage(index: Int) {
        val allImages = _uiState.value.allImages
        if (index in allImages.indices) {
            _uiState.update {
                it.copy(selectedImageIndex = index)
            }
        }
    }
    
    /**
     * Remove an image at the specified index
     */
    fun removeImage(index: Int) {
        val currentState = _uiState.value
        val allImages = currentState.allImages
        
        if (index !in allImages.indices) return
        
        if (index == 0 && currentState.imageUri != null) {
            // Removing the primary image
            if (currentState.additionalImages.isNotEmpty()) {
                // Promote first additional image to primary
                val newPrimary = currentState.additionalImages.first()
                _uiState.update {
                    it.copy(
                        imageUri = newPrimary.uri,
                        imageBytes = newPrimary.bytes,
                        additionalImages = it.additionalImages.drop(1),
                        selectedImageIndex = 0
                    )
                }
            } else {
                // No more images, reset to capture state
                _uiState.update { DishCaptureUiState() }
            }
        } else {
            // Removing an additional image
            val adjustedIndex = if (currentState.imageUri != null) index - 1 else index
            _uiState.update {
                it.copy(
                    additionalImages = it.additionalImages.filterIndexed { i, _ -> i != adjustedIndex },
                    selectedImageIndex = minOf(it.selectedImageIndex, it.totalImages - 2)
                )
            }
        }
    }
    
    /**
     * Check if more images can be added
     */
    fun canAddMoreImages(): Boolean {
        return _uiState.value.totalImages < MAX_IMAGES
    }
    
    /**
     * Get remaining image slots available
     */
    fun remainingImageSlots(): Int {
        return MAX_IMAGES - _uiState.value.totalImages
    }
    
    private fun runAIDetection(imageResult: ImageResult) {
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
     * Get the primary image bytes for uploading
     */
    fun getImageBytes(): ByteArray? {
        return _uiState.value.imageBytes
    }

    /**
     * Get the primary image URI for display
     */
    fun getImageUri(): String? {
        return _uiState.value.imageUri
    }
    
    /**
     * Get all captured images (primary + additional)
     */
    fun getAllImages(): List<CapturedImage> {
        return _uiState.value.allImages
    }
    
    /**
     * Get all image bytes for uploading
     */
    fun getAllImageBytes(): List<ByteArray> {
        return _uiState.value.allImages.map { it.bytes }
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
