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
import com.example.smackcheck2.data.repository.ChallengeRepository
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
    val isLoading: Boolean = true,
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
    val locationError: String? = null,
    val noRestaurantsFound: Boolean = false
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

    /**
     * Select a location with full GPS data (called after auto-detection via SharedLocationState)
     *
     * @param city     Reverse-geocoded city name
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     */
    fun selectLocationWithCoordinates(city: String, latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                selectedLocation = city,
                currentLatitude = latitude,
                currentLongitude = longitude,
                isLoading = true
            )
        }
        loadDataForLocation(city)
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
            _uiState.update { it.copy(isLoading = true, error = null, noRestaurantsFound = false) }
            println("LocationHomeViewModel: Loading data for location: $location")

            try {
                // Fetch restaurants for the selected city from Supabase
                println("LocationHomeViewModel: Fetching restaurants from database for: $location")
                val restaurantsResult = databaseRepository.getRestaurantsByCity(location)

                // Get coordinates for the selected location
                // Strategy: Try Android geocoder first, then fall back to Google Places geocoding
                println("LocationHomeViewModel: Getting coordinates for: $location")
                var coordinates: LocationResult? = null
                
                // Step 1: Try Android/iOS native geocoder
                if (locationService != null) {
                    try {
                        val geocodedLocation = locationService?.getCoordinatesForCity(location)
                        if (geocodedLocation != null) {
                            println("LocationHomeViewModel: Native geocoder success for $location: ${geocodedLocation.latitude}, ${geocodedLocation.longitude}")
                            coordinates = geocodedLocation
                        } else {
                            println("LocationHomeViewModel: Native geocoder returned null for $location")
                        }
                    } catch (e: Exception) {
                        println("LocationHomeViewModel: Native geocoder error for $location: ${e.message}")
                    }
                }
                
                // Step 2: If native geocoder failed, try Google Places geocoding via Edge Function
                if (coordinates == null && placesService != null) {
                    println("LocationHomeViewModel: Trying Google Places geocoding for: $location")
                    try {
                        val geocodedCity = placesService?.geocodeCity(location)
                        if (geocodedCity != null) {
                            println("LocationHomeViewModel: Google Places geocoded $location to: ${geocodedCity.latitude}, ${geocodedCity.longitude}")
                            coordinates = LocationResult(
                                latitude = geocodedCity.latitude,
                                longitude = geocodedCity.longitude,
                                cityName = location,
                                fullAddress = geocodedCity.formattedAddress
                            )
                        } else {
                            println("LocationHomeViewModel: Google Places geocoding returned null for $location")
                        }
                    } catch (e: Exception) {
                        println("LocationHomeViewModel: Google Places geocoding error: ${e.message}")
                    }
                }
                
                if (coordinates == null) {
                    println("LocationHomeViewModel: All geocoding methods failed for $location - cannot fetch nearby restaurants")
                }

                // Fetch nearby restaurants from Google Places API using the geocoded coordinates
                println("LocationHomeViewModel: placesService=${placesService != null}, coordinates=${coordinates != null}")
                val nearbyRestaurants = if (placesService != null && coordinates != null) {
                    try {
                        // Update coordinates in state
                        _uiState.update {
                            it.copy(
                                currentLatitude = coordinates.latitude,
                                currentLongitude = coordinates.longitude
                            )
                        }
                        println("LocationHomeViewModel: Calling Google Places API at: ${coordinates.latitude}, ${coordinates.longitude}")
                        val nearby = placesService?.findNearbyRestaurants(
                            latitude = coordinates.latitude,
                            longitude = coordinates.longitude,
                            radiusInMeters = 5000
                        ) ?: emptyList()
                        println("LocationHomeViewModel: Google Places returned ${nearby.size} restaurants")
                        nearby
                    } catch (e: Exception) {
                        println("LocationHomeViewModel: Failed to load nearby restaurants: ${e.message}")
                        e.printStackTrace()
                        emptyList()
                    }
                } else {
                    if (coordinates == null) {
                        println("LocationHomeViewModel: Skipping Google Places - geocoding failed for '$location'")
                    } else {
                        println("LocationHomeViewModel: Skipping Google Places - placesService is null")
                    }
                    emptyList()
                }

                restaurantsResult.fold(
                    onSuccess = { restaurants ->
                        println("LocationHomeViewModel: Database returned ${restaurants.size} restaurants")
                        // Get top 5 restaurants by rating (from database)
                        val topRestaurants = restaurants
                            .sortedByDescending { it.averageRating }
                            .take(5)

                        // Fetch top-rated dishes across all users
                        val topDishesResult = databaseRepository.getTopRatedDishes(limit = 10)
                        val topDishes = topDishesResult.getOrElse {
                            println("LocationHomeViewModel: Failed to load top dishes: ${it.message}")
                            emptyList()
                        }

                        // Check if we found any restaurants at all
                        val totalRestaurants = restaurants.size + nearbyRestaurants.size
                        val noResults = totalRestaurants == 0

                        println("LocationHomeViewModel: Final state - topRestaurants=${topRestaurants.size}, allRestaurants=${restaurants.size}, nearbyRestaurants=${nearbyRestaurants.size}, topDishes=${topDishes.size}, noRestaurantsFound=$noResults")
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                topRestaurants = topRestaurants,
                                topDishes = topDishes,
                                allRestaurants = restaurants,
                                nearbyRestaurants = nearbyRestaurants,
                                error = null,
                                noRestaurantsFound = noResults
                            )
                        }
                    },
                    onFailure = { error ->
                        println("LocationHomeViewModel: Database query failed: ${error.message}")
                        // Even if database fails, show nearby restaurants if available
                        val noResults = nearbyRestaurants.isEmpty()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = if (noResults)
                                    "Failed to load restaurants: ${error.message}"
                                else null,
                                topRestaurants = emptyList(),
                                topDishes = emptyList(),
                                allRestaurants = emptyList(),
                                nearbyRestaurants = nearbyRestaurants,
                                noRestaurantsFound = noResults
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                println("LocationHomeViewModel: Critical error: ${e.message}")
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading data: ${e.message}",
                        topRestaurants = emptyList(),
                        topDishes = emptyList(),
                        allRestaurants = emptyList(),
                        nearbyRestaurants = emptyList(),
                        noRestaurantsFound = true
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
    val achievements: List<Achievement> = emptyList(),
    val showLevelUpAnimation: Boolean = false,
    val newLevel: Int? = null,
    val showAchievementUnlock: Boolean = false,
    val newAchievement: Achievement? = null
)

/**
 * ViewModel for Game Screen
 */
class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val authRepository = AuthRepository()
    private val databaseRepository = DatabaseRepository()
    private val challengeRepository = ChallengeRepository()

    init {
        loadGameData()
    }

    fun loadGameData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val user = authRepository.getCurrentUser()

                // Load real challenges
                val (dailyChallenges, weeklyChallenges) = challengeRepository.getUserChallenges(userId)
                    .getOrDefault(Pair(emptyList(), emptyList()))

                // Load real leaderboard
                val leaderboard = loadLeaderboard()

                // Load real achievements
                val achievements = loadAchievements()

                // Calculate user's rank
                val userRank = leaderboard.indexOfFirst { it.userId == userId } + 1

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalXp = user?.xp ?: 0,
                        level = user?.level ?: 1,
                        rank = userRank,
                        streakDays = user?.streakCount ?: 0,
                        dailyChallenges = dailyChallenges,
                        weeklyChallenges = weeklyChallenges,
                        leaderboard = leaderboard,
                        achievements = achievements
                    )
                }
            } catch (e: Exception) {
                println("GameViewModel: Error loading game data: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadLeaderboard(): List<LeaderboardEntry> {
        return try {
            val result = databaseRepository.getLeaderboard(50)
            val profiles = result.getOrDefault(emptyList())

            profiles.map { profile ->
                LeaderboardEntry(
                    userId = profile.id,
                    userName = profile.name,
                    xp = profile.xp,
                    level = profile.level
                )
            }
        } catch (e: Exception) {
            println("GameViewModel: Error loading leaderboard: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadAchievements(): List<Achievement> {
        return try {
            val userId = authRepository.getCurrentUserId() ?: return getDefaultAchievements()

            // Get all available badges
            val allBadges = databaseRepository.getAllBadges().getOrDefault(emptyList())

            // Get user's earned badges
            val earnedBadges = databaseRepository.getUserBadges(userId).getOrDefault(emptyList())
            val earnedBadgeIds = earnedBadges.map { it.id }.toSet()

            // If no badges exist in database, return defaults
            if (allBadges.isEmpty()) {
                return getDefaultAchievements()
            }

            // Map badges to achievements
            allBadges.map { badge ->
                Achievement(
                    id = badge.id,
                    title = badge.name,
                    description = badge.description,
                    icon = getIconForBadge(badge.id),
                    isUnlocked = earnedBadgeIds.contains(badge.id)
                )
            }
        } catch (e: Exception) {
            println("GameViewModel: Error loading achievements: ${e.message}")
            getDefaultAchievements()
        }
    }

    private fun getIconForBadge(badgeId: String): androidx.compose.ui.graphics.vector.ImageVector {
        return when (badgeId) {
            "first_bite" -> Icons.Filled.Star
            "foodie_explorer", "restaurant_hopper" -> Icons.Filled.Explore
            "rating_streak" -> Icons.Filled.LocalFireDepartment
            "cuisine_master" -> Icons.Filled.Fastfood
            "photo_pro" -> Icons.Filled.CameraAlt
            else -> Icons.Filled.Star
        }
    }

    private fun getDefaultAchievements(): List<Achievement> {
        // Fallback default achievements if database is empty
        return listOf(
            Achievement(
                id = "first_bite",
                title = "First Bite",
                description = "Rate your first dish",
                icon = Icons.Filled.Star,
                isUnlocked = false
            ),
            Achievement(
                id = "foodie_explorer",
                title = "Foodie Explorer",
                description = "Try 10 different restaurants",
                icon = Icons.Filled.Explore,
                isUnlocked = false
            ),
            Achievement(
                id = "rating_streak",
                title = "Rating Streak",
                description = "Maintain a 7-day streak",
                icon = Icons.Filled.LocalFireDepartment,
                isUnlocked = false
            ),
            Achievement(
                id = "cuisine_master",
                title = "Cuisine Master",
                description = "Try 15 different cuisines",
                icon = Icons.Filled.Fastfood,
                isUnlocked = false
            ),
            Achievement(
                id = "photo_pro",
                title = "Photo Pro",
                description = "Upload 20 photos with reviews",
                icon = Icons.Filled.CameraAlt,
                isUnlocked = false
            ),
            Achievement(
                id = "restaurant_hopper",
                title = "Restaurant Hopper",
                description = "Visit 5 different restaurants",
                icon = Icons.Filled.Restaurant,
                isUnlocked = false
            )
        )
    }

    /**
     * Manually complete a challenge and award XP
     */
    fun completeChallenge(challengeId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val challenge = (_uiState.value.dailyChallenges + _uiState.value.weeklyChallenges)
                .find { it.id == challengeId } ?: return@launch

            // Award XP through ChallengeRepository
            val result = challengeRepository.markChallengeCompleted(userId, challengeId, challenge.xpReward)
            result.fold(
                onSuccess = {
                    println("GameViewModel: ✓ Challenge completed: ${challenge.title}")
                    // Reload game data to refresh challenges and XP
                    loadGameData()
                },
                onFailure = { error ->
                    println("GameViewModel: ✗ Failed to complete challenge: ${error.message}")
                }
            )
        }
    }

    fun clearLevelUpAnimation() {
        _uiState.update { it.copy(showLevelUpAnimation = false, newLevel = null) }
    }

    fun clearAchievementUnlock() {
        _uiState.update { it.copy(showAchievementUnlock = false, newAchievement = null) }
    }

    /**
     * Check if user leveled up and trigger animation
     */
    fun checkForLevelUp(oldLevel: Int, newLevel: Int) {
        if (newLevel > oldLevel) {
            _uiState.update {
                it.copy(
                    showLevelUpAnimation = true,
                    newLevel = newLevel
                )
            }
        }
    }

    /**
     * Show achievement unlock animation
     */
    fun showAchievementUnlock(achievement: Achievement) {
        _uiState.update {
            it.copy(
                showAchievementUnlock = true,
                newAchievement = achievement
            )
        }
    }
}
