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
    val rating: Float = 0f,
    val comment: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Restaurant detail UI state
 */
data class RestaurantDetailUiState(
    val restaurant: Restaurant? = null,
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
    val errorMessage: String? = null
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
