package com.example.smackcheck2.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.platform.LocationErrorReason
import com.example.smackcheck2.platform.LocationOperationResult
import com.example.smackcheck2.platform.LocationResult
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.NearbyRestaurant
import com.example.smackcheck2.ui.screens.Achievement
import com.example.smackcheck2.ui.screens.Challenge
import com.example.smackcheck2.ui.screens.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for Location-based Home Screen
 */
data class LocationHomeUiState(
    val isLoading: Boolean = false,
    val isDetectingLocation: Boolean = false,
    val selectedLocation: String? = null,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val topRestaurants: List<Restaurant> = emptyList(),
    val topDishes: List<Dish> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val nearbyRestaurants: List<NearbyRestaurant> = emptyList(),
    val searchResults: List<LocationResult> = emptyList(),
    val error: String? = null,
    val locationError: String? = null
)

/**
 * ViewModel for Location-based Home Screen
 */
class LocationHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LocationHomeUiState())
    val uiState: StateFlow<LocationHomeUiState> = _uiState.asStateFlow()

    private var locationService: LocationService? = null
    private var placesService: PlacesService? = null
    private val authRepository = AuthRepository()

    init {
        // Load saved location from user profile or use default
        loadSavedLocation()
    }

    private fun loadSavedLocation() {
        viewModelScope.launch {
            try {
                // Small delay to ensure auth session is loaded
                kotlinx.coroutines.delay(500)

                val savedLocation = authRepository.getLastLocation()
                if (savedLocation != null) {
                    println("LocationHomeViewModel: Loaded saved location from profile: $savedLocation")
                    _uiState.update { it.copy(selectedLocation = savedLocation) }
                    loadDataForLocation(savedLocation)
                } else {
                    println("LocationHomeViewModel: No saved location found, using default: New York")
                    selectLocation("New York")
                }
            } catch (e: Exception) {
                println("LocationHomeViewModel: Error loading saved location: ${e.message}")
                e.printStackTrace()
                // Use default location on error
                selectLocation("New York")
            }
        }
    }

    fun setLocationService(service: LocationService?) {
        locationService = service
    }

    fun setPlacesService(service: PlacesService?) {
        placesService = service
    }

    fun selectLocation(location: String) {
        _uiState.update { it.copy(selectedLocation = location, isLoading = true, locationError = null) }

        // Save the selected location to user profile
        viewModelScope.launch {
            try {
                val result = authRepository.updateLastLocation(location)
                result.fold(
                    onSuccess = {
                        println("LocationHomeViewModel: ✓ Successfully saved location to profile: $location")
                    },
                    onFailure = { error ->
                        println("LocationHomeViewModel: ✗ Failed to save location: ${error.message}")
                        error.printStackTrace()
                    }
                )
            } catch (e: Exception) {
                println("LocationHomeViewModel: ✗ Exception saving location: ${e.message}")
                e.printStackTrace()
            }
        }

        loadDataForLocation(location)
    }

    fun useCurrentLocation() {
        val service = locationService
        if (service == null) {
            _uiState.update { it.copy(locationError = "Location service not available") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingLocation = true, locationError = null) }

            when (val result = service.getCurrentLocationWithDetails()) {
                is LocationOperationResult.Success -> {
                    val location = result.location
                    if (location.cityName != null) {
                        _uiState.update {
                            it.copy(
                                isDetectingLocation = false,
                                currentLatitude = location.latitude,
                                currentLongitude = location.longitude
                            )
                        }
                        selectLocation(location.cityName)
                    } else {
                        _uiState.update {
                            it.copy(
                                isDetectingLocation = false,
                                locationError = "Could not determine city name. Please select manually."
                            )
                        }
                    }
                }
                is LocationOperationResult.Error -> {
                    val errorMessage = getErrorMessage(result.reason, result.isEmulator)
                    _uiState.update {
                        it.copy(
                            isDetectingLocation = false,
                            locationError = errorMessage
                        )
                    }
                }
            }
        }
    }

    private fun getErrorMessage(reason: LocationErrorReason, isEmulator: Boolean): String {
        return when (reason) {
            LocationErrorReason.PERMISSION_DENIED ->
                "Location permission denied. Please grant location access in Settings."

            LocationErrorReason.LOCATION_SERVICES_DISABLED ->
                "Location services are disabled. Please enable GPS in Settings."

            LocationErrorReason.NO_LOCATION_AVAILABLE -> {
                if (isEmulator) {
                    "No location available. On emulator, use Extended Controls > Location to set a simulated location."
                } else {
                    "Could not get location. Please ensure GPS is enabled and try again outdoors."
                }
            }

            LocationErrorReason.TIMEOUT ->
                "Location request timed out. Please try again."

            LocationErrorReason.UNKNOWN ->
                "An unknown error occurred while getting location. Please try again."
        }
    }

    fun searchLocations(query: String) {
        val service = locationService ?: return

        viewModelScope.launch {
            val results = service.searchPlaces(query)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun clearSearchResults() {
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun clearLocationError() {
        _uiState.update { it.copy(locationError = null) }
    }

    private val databaseRepository = DatabaseRepository()

    private fun loadDataForLocation(location: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Fetch restaurants for the selected city from Supabase
                val restaurantsResult = databaseRepository.getRestaurantsByCity(location)

                // Fetch nearby restaurants from Google Places API if location is available
                val nearbyRestaurants = if (placesService != null && locationService != null) {
                    try {
                        val currentLoc = locationService?.getCurrentLocation()
                        if (currentLoc != null) {
                            // Update GPS coordinates in state
                            _uiState.update {
                                it.copy(
                                    currentLatitude = currentLoc.latitude,
                                    currentLongitude = currentLoc.longitude
                                )
                            }
                            placesService?.findNearbyRestaurants(
                                latitude = currentLoc.latitude,
                                longitude = currentLoc.longitude,
                                radiusInMeters = 5000
                            ) ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        println("LocationHomeViewModel: Failed to load nearby restaurants: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                restaurantsResult.fold(
                    onSuccess = { restaurants ->
                        // Get top 5 restaurants by rating (from database)
                        val topRestaurants = restaurants
                            .sortedByDescending { it.averageRating }
                            .take(5)

                        // Fetch dishes for these restaurants
                        val restaurantIds = restaurants.map { it.id }
                        val dishesResult = databaseRepository.getDishesForRestaurants(restaurantIds)

                        val topDishes = dishesResult.getOrNull()
                            ?.sortedByDescending { it.rating }
                            ?.take(6)
                            ?: emptyList()

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                topRestaurants = topRestaurants,
                                topDishes = topDishes,
                                allRestaurants = restaurants,
                                nearbyRestaurants = nearbyRestaurants,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        // Even if database fails, show nearby restaurants if available
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = if (nearbyRestaurants.isEmpty())
                                    "Failed to load restaurants: ${error.message}"
                                else null,
                                topRestaurants = emptyList(),
                                topDishes = emptyList(),
                                allRestaurants = emptyList(),
                                nearbyRestaurants = nearbyRestaurants
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading data: ${e.message}",
                        topRestaurants = emptyList(),
                        topDishes = emptyList(),
                        allRestaurants = emptyList(),
                        nearbyRestaurants = emptyList()
                    )
                }
            }
        }
    }
}

/**
 * UI State for Game Screen
 */
data class GameUiState(
    val isLoading: Boolean = false,
    val totalXp: Int = 0,
    val level: Int = 1,
    val rank: Int = 0,
    val streakDays: Int = 0,
    val dailyChallenges: List<Challenge> = emptyList(),
    val weeklyChallenges: List<Challenge> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

/**
 * ViewModel for Game Screen
 */
class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    
    init {
        loadGameData()
    }

    private fun loadGameData() {
        // TODO: Load game data from database/API
        val dailyChallenges = listOf(
            Challenge(
                id = "dc1",
                title = "Daily Explorer",
                description = "Rate 3 dishes today",
                icon = Icons.Filled.Restaurant,
                xpReward = 50,
                progress = 0.66f,
                isCompleted = false
            ),
            Challenge(
                id = "dc2",
                title = "Photo Pro",
                description = "Upload 5 dish photos",
                icon = Icons.Filled.CameraAlt,
                xpReward = 30,
                progress = 0.8f,
                isCompleted = false
            ),
            Challenge(
                id = "dc3",
                title = "Taste Tester",
                description = "Try a new cuisine today",
                icon = Icons.Filled.Fastfood,
                xpReward = 40,
                progress = 1f,
                isCompleted = true
            )
        )

        val weeklyChallenges = listOf(
            Challenge(
                id = "wc1",
                title = "Restaurant Hopper",
                description = "Visit 5 different restaurants",
                icon = Icons.Filled.Explore,
                xpReward = 200,
                progress = 0.4f,
                isCompleted = false
            ),
            Challenge(
                id = "wc2",
                title = "Review Master",
                description = "Write 10 detailed reviews",
                icon = Icons.Filled.RateReview,
                xpReward = 250,
                progress = 0.3f,
                isCompleted = false
            ),
            Challenge(
                id = "wc3",
                title = "Social Foodie",
                description = "Share 5 dishes on social media",
                icon = Icons.Filled.Share,
                xpReward = 150,
                progress = 0.6f,
                isCompleted = false
            ),
            Challenge(
                id = "wc4",
                title = "Streak Champion",
                description = "Maintain a 7-day rating streak",
                icon = Icons.Filled.LocalFireDepartment,
                xpReward = 300,
                progress = 1f,
                isCompleted = true
            )
        )

        val leaderboard = listOf(
            LeaderboardEntry("u1", "FoodMaster", 15420, 25),
            LeaderboardEntry("u2", "TasteHunter", 14850, 24),
            LeaderboardEntry("u3", "GourmetKing", 13200, 22),
            LeaderboardEntry("u4", "DishExplorer", 12800, 21),
            LeaderboardEntry("u5", "FlavorSeeker", 11500, 20),
            LeaderboardEntry("u6", "CuisinePro", 10200, 18),
            LeaderboardEntry("u7", "BiteReviewer", 9800, 17),
            LeaderboardEntry("u8", "FoodNinja", 8500, 15),
            LeaderboardEntry("u9", "TastyTraveler", 7200, 13),
            LeaderboardEntry("u10", "PlateRater", 6800, 12)
        )

        val achievements = listOf(
            Achievement(
                id = "a1",
                title = "First Bite",
                description = "Rate your first dish",
                icon = Icons.Filled.Star,
                isUnlocked = true
            ),
            Achievement(
                id = "a2",
                title = "Foodie Explorer",
                description = "Try 10 different restaurants",
                icon = Icons.Filled.Explore,
                isUnlocked = true
            ),
            Achievement(
                id = "a3",
                title = "Rating Streak",
                description = "Maintain a 7-day streak",
                icon = Icons.Filled.LocalFireDepartment,
                isUnlocked = true
            ),
            Achievement(
                id = "a4",
                title = "Cuisine Master",
                description = "Try 15 different cuisines",
                icon = Icons.Filled.Fastfood,
                isUnlocked = false
            ),
            Achievement(
                id = "a5",
                title = "Top Reviewer",
                description = "Be in the top 10 on leaderboard",
                icon = Icons.Filled.EmojiEvents,
                isUnlocked = false
            ),
            Achievement(
                id = "a6",
                title = "Trending Taste",
                description = "Have a review go viral",
                icon = Icons.Filled.TrendingUp,
                isUnlocked = false
            )
        )

        _uiState.update {
            it.copy(
                dailyChallenges = dailyChallenges,
                weeklyChallenges = weeklyChallenges,
                leaderboard = leaderboard,
                achievements = achievements
            )
        }
    }

    fun completeChallenge(challengeId: String) {
        // Mark challenge as completed and award XP
        _uiState.update { state ->
            val updatedDaily = state.dailyChallenges.map {
                if (it.id == challengeId) it.copy(isCompleted = true, progress = 1f) else it
            }
            val updatedWeekly = state.weeklyChallenges.map {
                if (it.id == challengeId) it.copy(isCompleted = true, progress = 1f) else it
            }

            val xpGained = (state.dailyChallenges + state.weeklyChallenges)
                .find { it.id == challengeId }?.xpReward ?: 0

            state.copy(
                dailyChallenges = updatedDaily,
                weeklyChallenges = updatedWeekly,
                totalXp = state.totalXp + xpGained
            )
        }
    }
}
