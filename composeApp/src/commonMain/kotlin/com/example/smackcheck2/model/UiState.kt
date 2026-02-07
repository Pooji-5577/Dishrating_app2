package com.example.smackcheck2.model

/**
 * Sealed class representing different UI states
 */
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * Authentication state
 */
sealed class AuthState {
    data object Unknown : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
}

/**
 * Login UI state
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Register UI state
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Profile UI state
 */
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Home feed UI state
 */
data class HomeFeedUiState(
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Dish rating UI state
 */
data class DishRatingUiState(
    val dishName: String = "",
    val imageUri: String = "",
    val imageBytes: ByteArray? = null,
    val rating: Float = 0f,
    val comment: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val xpEarned: Int? = null,
    val showXpNotification: Boolean = false
)

/**
 * Search UI state
 */
data class SearchUiState(
    val query: String = "",
    val selectedCuisines: Set<String> = emptySet(),
    val selectedRating: Float? = null,
    val selectedCity: String? = null,
    val results: List<Restaurant> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val locationError: String? = null
)

/**
 * Restaurant detail UI state
 */
data class RestaurantDetailUiState(
    val restaurant: Restaurant? = null,
    val dishes: List<Dish> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Manual restaurant entry UI state
 */
data class ManualRestaurantUiState(
    val restaurantName: String = "",
    val city: String = "",
    val cuisine: String = "",
    val restaurantNameError: String? = null,
    val cityError: String? = null,
    val cuisineError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User progress UI state
 */
data class UserProgressUiState(
    val currentXp: Int = 0,
    val maxXp: Int = 100,
    val level: Int = 1,
    val streakCount: Int = 0,
    val badges: List<Badge> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showLevelUpAnimation: Boolean = false,
    val newLevel: Int? = null
)

/**
 * Permission state
 */
sealed class PermissionState {
    data object NotRequested : PermissionState()
    data object Granted : PermissionState()
    data object Denied : PermissionState()
    data object PermanentlyDenied : PermissionState()
}

/**
 * Edit profile UI state
 */
data class EditProfileUiState(
    val name: String = "",
    val bio: String = "",
    val profilePhotoUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val nameError: String? = null
)

/**
 * Notification settings UI state
 */
data class NotificationSettingsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Account settings UI state
 */
data class AccountSettingsUiState(
    val email: String = "",
    val isChangingPassword: Boolean = false,
    val isChangingEmail: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * Privacy settings UI state
 */
data class PrivacySettingsUiState(
    val settings: PrivacySettings = PrivacySettings(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Dish capture UI state for camera capture and AI detection
 */
data class DishCaptureUiState(
    val imageUri: String? = null,
    val imageBytes: ByteArray? = null,
    val isAnalyzing: Boolean = false,
    val detectedDishName: String? = null,
    val detectedCuisine: String? = null,
    val detectionConfidence: Float = 0f,
    val alternatives: List<String> = emptyList(),
    val isAIDetected: Boolean = false,
    val isEditingName: Boolean = false,
    val editedName: String = "",
    val errorMessage: String? = null,
    val debugInfo: String? = null,
    val showConfirmation: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DishCaptureUiState

        if (imageUri != other.imageUri) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (isAnalyzing != other.isAnalyzing) return false
        if (detectedDishName != other.detectedDishName) return false
        if (detectedCuisine != other.detectedCuisine) return false
        if (detectionConfidence != other.detectionConfidence) return false
        if (alternatives != other.alternatives) return false
        if (isAIDetected != other.isAIDetected) return false
        if (isEditingName != other.isEditingName) return false
        if (editedName != other.editedName) return false
        if (errorMessage != other.errorMessage) return false
        if (debugInfo != other.debugInfo) return false
        if (showConfirmation != other.showConfirmation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageUri?.hashCode() ?: 0
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + isAnalyzing.hashCode()
        result = 31 * result + (detectedDishName?.hashCode() ?: 0)
        result = 31 * result + (detectedCuisine?.hashCode() ?: 0)
        result = 31 * result + detectionConfidence.hashCode()
        result = 31 * result + alternatives.hashCode()
        result = 31 * result + isAIDetected.hashCode()
        result = 31 * result + isEditingName.hashCode()
        result = 31 * result + editedName.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (debugInfo?.hashCode() ?: 0)
        result = 31 * result + showConfirmation.hashCode()
        return result
    }
}
