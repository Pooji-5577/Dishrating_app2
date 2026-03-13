package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AIDetectionRepository
import com.example.smackcheck2.model.CapturedImage
import com.example.smackcheck2.model.DishCaptureUiState
import com.example.smackcheck2.model.ImageDetectionResult
import com.example.smackcheck2.platform.ImageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Dish Capture screen with AI detection
 * Supports multiple image capture for dish ratings
 * Each image gets its own independent AI detection
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
                selectedImageIndex = 0,
                perImageDetections = mapOf(
                    imageResult.uri to ImageDetectionResult(isAnalyzing = true)
                )
            )
        }

        // Start AI detection for this image
        runAIDetection(imageResult.uri, imageResult.bytes, imageResult.mimeType)
    }

    /**
     * Called when user adds additional images from gallery
     * Triggers AI detection for each new image
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
        val imagesToAdd = images.take(remainingSlots)
        val capturedImages = imagesToAdd.map { result ->
            CapturedImage(uri = result.uri, bytes = result.bytes)
        }

        // Mark new images as analyzing in per-image detections
        val newDetections = imagesToAdd.associate { result ->
            result.uri to ImageDetectionResult(isAnalyzing = true)
        }

        _uiState.update {
            it.copy(
                additionalImages = it.additionalImages + capturedImages,
                perImageDetections = it.perImageDetections + newDetections,
                errorMessage = null
            )
        }

        // Run AI detection for each new image
        for (imageResult in imagesToAdd) {
            runAIDetection(imageResult.uri, imageResult.bytes, imageResult.mimeType)
        }
    }

    /**
     * Called when user captures an additional image with camera
     * Triggers AI detection for the new image
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
                perImageDetections = it.perImageDetections + (imageResult.uri to ImageDetectionResult(isAnalyzing = true)),
                errorMessage = null
            )
        }

        // Run AI detection for the new image
        runAIDetection(imageResult.uri, imageResult.bytes, imageResult.mimeType)
    }

    /**
     * Select an image by index for viewing
     * Updates the displayed detection result to match the selected image
     */
    fun selectImage(index: Int) {
        val currentState = _uiState.value
        val allImages = currentState.allImages
        if (index in allImages.indices) {
            val selectedUri = allImages[index].uri
            val detection = currentState.perImageDetections[selectedUri]

            _uiState.update {
                it.copy(
                    selectedImageIndex = index,
                    // If outage, keep editing mode so user can enter name manually
                    isEditingName = detection?.isOutage ?: false,
                    // Update displayed detection from per-image results
                    isAnalyzing = detection?.isAnalyzing ?: false,
                    detectedDishName = detection?.dishName,
                    detectedCuisine = detection?.cuisine,
                    detectionConfidence = detection?.confidence ?: 0f,
                    alternatives = detection?.alternatives ?: emptyList(),
                    isAIDetected = detection?.isAIDetected ?: false,
                    itemType = detection?.itemType ?: "unknown",
                    editedName = detection?.editedName ?: (detection?.dishName ?: ""),
                    debugInfo = detection?.debugInfo,
                    showConfirmation = detection != null && !(detection.isAnalyzing),
                    showNotDishError = detection?.showNotDishError ?: false,
                    errorMessage = if (detection?.isOutage == true) "AI service unavailable. Please enter dish name manually." else null
                )
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

        // Remove detection result for this image
        val removedUri = allImages[index].uri
        val updatedDetections = currentState.perImageDetections - removedUri

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
                        selectedImageIndex = 0,
                        perImageDetections = updatedDetections
                    )
                }
                // Update displayed detection for new selected image
                selectImage(0)
            } else {
                // No more images, reset to capture state
                _uiState.update { DishCaptureUiState() }
            }
        } else {
            // Removing an additional image
            val adjustedIndex = if (currentState.imageUri != null) index - 1 else index
            val newSelectedIndex = minOf(currentState.selectedImageIndex, currentState.totalImages - 2)
            _uiState.update {
                it.copy(
                    additionalImages = it.additionalImages.filterIndexed { i, _ -> i != adjustedIndex },
                    selectedImageIndex = newSelectedIndex,
                    perImageDetections = updatedDetections
                )
            }
            // Update displayed detection for the new selected image
            selectImage(newSelectedIndex.coerceAtLeast(0))
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

    /**
     * Run AI detection for a specific image identified by URI
     */
    private fun runAIDetection(imageUri: String, imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            try {
                val result = aiDetectionRepository.detectDish(
                    imageBytes = imageBytes,
                    mimeType = mimeType
                )

                // On outage: allow manual entry (no "Not a Dish" error)
                // On real "not food": show the error modal
                val isOutage = result.isOutage
                val showNotDish = !isOutage && result.itemType == "unknown"

                val detectionResult = ImageDetectionResult(
                    dishName = if (isOutage) "" else result.dishName,
                    cuisine = result.cuisine,
                    confidence = result.confidence,
                    alternatives = result.alternatives,
                    isAIDetected = result.isAIDetected,
                    itemType = if (isOutage) "food" else result.itemType,
                    debugInfo = result.debugInfo,
                    isAnalyzing = false,
                    editedName = if (isOutage) "" else result.dishName,
                    showNotDishError = showNotDish,
                    isOutage = isOutage
                )

                _uiState.update {
                    val updatedDetections = it.perImageDetections + (imageUri to detectionResult)

                    // If this is the currently selected image, update displayed fields too
                    val selectedImage = it.allImages.getOrNull(it.selectedImageIndex)
                    if (selectedImage?.uri == imageUri) {
                        it.copy(
                            perImageDetections = updatedDetections,
                            isAnalyzing = false,
                            detectedDishName = if (isOutage) null else result.dishName,
                            detectedCuisine = result.cuisine,
                            detectionConfidence = result.confidence,
                            alternatives = result.alternatives,
                            isAIDetected = result.isAIDetected,
                            itemType = if (isOutage) "food" else result.itemType,
                            editedName = if (isOutage) "" else result.dishName,
                            debugInfo = result.debugInfo,
                            showConfirmation = true,
                            showNotDishError = showNotDish,
                            // On outage, go straight to editing mode so user can type dish name
                            isEditingName = isOutage,
                            errorMessage = if (isOutage) "AI service unavailable. Please enter dish name manually." else null
                        )
                    } else {
                        it.copy(perImageDetections = updatedDetections)
                    }
                }
            } catch (e: Exception) {
                // Network/ViewModel exceptions are treated as outages - allow manual entry
                val detectionResult = ImageDetectionResult(
                    dishName = null,
                    isAIDetected = false,
                    itemType = "food",
                    isAnalyzing = false,
                    editedName = "",
                    showNotDishError = false,
                    isOutage = true,
                    debugInfo = "ViewModel Exception: ${e.message}"
                )

                _uiState.update {
                    val updatedDetections = it.perImageDetections + (imageUri to detectionResult)

                    val selectedImage = it.allImages.getOrNull(it.selectedImageIndex)
                    if (selectedImage?.uri == imageUri) {
                        it.copy(
                            perImageDetections = updatedDetections,
                            isAnalyzing = false,
                            detectedDishName = null,
                            isAIDetected = false,
                            itemType = "food",
                            editedName = "",
                            showConfirmation = true,
                            showNotDishError = false,
                            isEditingName = true,
                            errorMessage = "AI service unavailable. Please enter dish name manually.",
                            debugInfo = "ViewModel Exception: ${e.message}"
                        )
                    } else {
                        it.copy(perImageDetections = updatedDetections)
                    }
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
     * Saves the edited name to per-image detection results
     */
    fun confirmDishName() {
        _uiState.update { state ->
            val selectedImage = state.allImages.getOrNull(state.selectedImageIndex)
            val updatedDetections = if (selectedImage != null) {
                val existing = state.perImageDetections[selectedImage.uri]
                if (existing != null) {
                    state.perImageDetections + (selectedImage.uri to existing.copy(
                        dishName = state.editedName,
                        editedName = state.editedName
                    ))
                } else {
                    state.perImageDetections
                }
            } else {
                state.perImageDetections
            }

            state.copy(
                isEditingName = false,
                detectedDishName = state.editedName,
                perImageDetections = updatedDetections
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
     * Get the final dish name for the currently selected image
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
     * Get all per-image detection results (for passing to rating screen)
     */
    fun getAllDetectionResults(): Map<String, ImageDetectionResult> {
        return _uiState.value.perImageDetections
    }

    /**
     * Called when user dismisses the "not a dish" error modal.
     * Removes the current image that's not a dish, or resets if it's the only one.
     */
    fun dismissNotDishError() {
        val currentState = _uiState.value
        if (currentState.totalImages <= 1) {
            _uiState.update { DishCaptureUiState() }
        } else {
            // Remove the current non-dish image and select another
            removeImage(currentState.selectedImageIndex)
            _uiState.update { it.copy(showNotDishError = false) }
        }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
