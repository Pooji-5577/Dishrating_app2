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
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val usernameError: String? = null,
    val isCheckingUsername: Boolean = false,
    val usernameAvailable: Boolean? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Profile setup onboarding UI state
 */
data class ProfileSetupUiState(
    val username: String = "",
    val usernameError: String? = null,
    val isCheckingUsername: Boolean = false,
    val usernameAvailable: Boolean? = null,
    val profilePhotoUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Profile UI state
 */
data class ProfileUiState(
    val user: User? = null,
    val userRatings: List<FeedItem> = emptyList(),
    val isRatingsLoading: Boolean = false,
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
    val tags: List<String> = emptyList(),
    val price: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val xpEarned: Int? = null,
    val showXpNotification: Boolean = false,
    val submittedRatingId: String? = null
)

/**
 * Search UI state
 */
data class SearchUiState(
    val query: String = "",
    val selectedCuisines: Set<String> = emptySet(),
    val selectedRating: Float? = null,
    val selectedCity: String? = null,
    val restaurantsAndCafesOnly: Boolean = false,
    val results: List<Restaurant> = emptyList(),
    val availableCuisines: List<String> = emptyList(),
    val availableCities: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val locationError: String? = null,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null
)

/**
 * Restaurant detail UI state
 */
data class RestaurantDetailUiState(
    val restaurant: Restaurant? = null,
    val dishes: List<Dish> = emptyList(),
    val topDishes: List<Dish> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Dish detail UI state
 */
data class DishDetailUiState(
    val dish: Dish? = null,
    val restaurant: Restaurant? = null,
    val reviews: List<Review> = emptyList(),
    val relatedDishes: List<Dish> = emptyList(),
    val featuredReview: Review? = null,
    val isFavorite: Boolean = false,
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
 * Manual dish entry UI state (AI fallback)
 */
data class ManualDishEntryUiState(
    val dishName: String = "",
    val restaurantName: String = "",
    val description: String = "",
    val rating: Float = 0f,
    val imageUri: String = "",
    val dishNameError: String? = null,
    val restaurantNameError: String? = null,
    val isSubmitting: Boolean = false,
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
    val username: String = "",
    val bio: String = "",
    val location: String = "",
    val email: String = "",
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
 * Social feed UI state with filtering
 */
data class SocialFeedUiState(
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreItems: Boolean = true,
    val currentOffset: Int = 0,
    val filter: FeedFilter = FeedFilter.FOLLOWING,
    val errorMessage: String? = null,
    val scrollToRatingId: String? = null,
    val scrollToIndex: Int? = null,
    val storyUsers: List<UserSummary> = emptyList(),
    val topDishes: List<FeedItem> = emptyList(),
    val nearbyRestaurantCount: Int = 0
)

enum class FeedFilter { FOLLOWING, TRENDING, NEARBY, MY_RATINGS }

/**
 * Comments UI state
 */
data class CommentsUiState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val replyingTo: Comment? = null,
    val errorMessage: String? = null
)

/**
 * Notifications UI state
 */
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * User profile (other user) UI state
 */
data class UserProfileUiState(
    val user: User? = null,
    val ratings: List<FeedItem> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val isFollowLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Followers/Following list UI state
 */
data class FollowListUiState(
    val users: List<UserSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Discover users UI state (Find Friends)
 */
data class DiscoverUsersUiState(
    val users: List<UserSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val togglingUserIds: Set<String> = emptySet()
)

/**
 * Dish capture UI state for camera capture and AI detection
 */
data class DishCaptureUiState(
    val imageUri: String? = null,
    val imageBytes: ByteArray? = null,
    // Support for multiple images
    val additionalImages: List<CapturedImage> = emptyList(),
    val selectedImageIndex: Int = 0, // Which image is currently displayed/being analyzed
    // Per-image detection results keyed by image URI
    val perImageDetections: Map<String, ImageDetectionResult> = emptyMap(),
    val isAnalyzing: Boolean = false,
    val detectedDishName: String? = null,
    val detectedCuisine: String? = null,
    val detectionConfidence: Float = 0f,
    val alternatives: List<String> = emptyList(),
    val isAIDetected: Boolean = false,
    val itemType: String = "unknown", // "food", "beverage", or "unknown"
    val detectedRestaurantChain: String? = null,
    val detectedRestaurantType: String? = null,
    val isEditingName: Boolean = false,
    val editedName: String = "",
    val errorMessage: String? = null,
    val debugInfo: String? = null,
    val showConfirmation: Boolean = false,
    val showNotDishError: Boolean = false
) {
    /** Total number of images (primary + additional) */
    val totalImages: Int get() = if (imageUri != null) 1 + additionalImages.size else additionalImages.size

    /** Get all images as a list */
    val allImages: List<CapturedImage> get() = buildList {
        if (imageUri != null && imageBytes != null) {
            add(CapturedImage(imageUri, imageBytes))
        }
        addAll(additionalImages)
    }

    /** Get detection result for the currently selected image */
    val currentDetection: ImageDetectionResult? get() {
        val selectedImage = allImages.getOrNull(selectedImageIndex)
        return selectedImage?.let { perImageDetections[it.uri] }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DishCaptureUiState

        if (imageUri != other.imageUri) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (additionalImages != other.additionalImages) return false
        if (selectedImageIndex != other.selectedImageIndex) return false
        if (perImageDetections != other.perImageDetections) return false
        if (isAnalyzing != other.isAnalyzing) return false
        if (detectedDishName != other.detectedDishName) return false
        if (detectedCuisine != other.detectedCuisine) return false
        if (detectionConfidence != other.detectionConfidence) return false
        if (alternatives != other.alternatives) return false
        if (isAIDetected != other.isAIDetected) return false
        if (itemType != other.itemType) return false
        if (detectedRestaurantChain != other.detectedRestaurantChain) return false
        if (detectedRestaurantType != other.detectedRestaurantType) return false
        if (isEditingName != other.isEditingName) return false
        if (editedName != other.editedName) return false
        if (errorMessage != other.errorMessage) return false
        if (debugInfo != other.debugInfo) return false
        if (showConfirmation != other.showConfirmation) return false
        if (showNotDishError != other.showNotDishError) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageUri?.hashCode() ?: 0
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + additionalImages.hashCode()
        result = 31 * result + selectedImageIndex
        result = 31 * result + perImageDetections.hashCode()
        result = 31 * result + isAnalyzing.hashCode()
        result = 31 * result + (detectedDishName?.hashCode() ?: 0)
        result = 31 * result + (detectedCuisine?.hashCode() ?: 0)
        result = 31 * result + detectionConfidence.hashCode()
        result = 31 * result + alternatives.hashCode()
        result = 31 * result + isAIDetected.hashCode()
        result = 31 * result + itemType.hashCode()
        result = 31 * result + (detectedRestaurantChain?.hashCode() ?: 0)
        result = 31 * result + (detectedRestaurantType?.hashCode() ?: 0)
        result = 31 * result + isEditingName.hashCode()
        result = 31 * result + editedName.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (debugInfo?.hashCode() ?: 0)
        result = 31 * result + showConfirmation.hashCode()
        result = 31 * result + showNotDishError.hashCode()
        return result
    }
}

/**
 * Represents a captured image with its URI and bytes
 */
data class CapturedImage(
    val uri: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CapturedImage
        if (uri != other.uri) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * Per-image AI detection result
 */
data class ImageDetectionResult(
    val dishName: String? = null,
    val cuisine: String? = null,
    val confidence: Float = 0f,
    val alternatives: List<String> = emptyList(),
    val isAIDetected: Boolean = false,
    val itemType: String = "unknown",
    val restaurantChain: String? = null,
    val restaurantType: String? = null,
    val debugInfo: String? = null,
    val isAnalyzing: Boolean = false,
    val editedName: String = "",
    val showNotDishError: Boolean = false,
    val isOutage: Boolean = false
)
