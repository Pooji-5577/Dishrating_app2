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
import com.example.smackcheck2.util.Logger
import kotlinx.coroutines.launch

/**
 * UI State for Location-based Home Screen
 */
data class LocationHomeUiState(
    val isLoading: Boolean = true,
    val isDetectingLocation: Boolean = false,
    val selectedLocation: String? = null,
    // Search-center coordinates (city geocode) — used for Google Places nearby lookup
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    // User's actual live GPS — used for distance display on restaurant cards
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val topRestaurants: List<Restaurant> = emptyList(),
    val topDishes: List<Dish> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val nearbyRestaurants: List<NearbyRestaurant> = emptyList(),
    val searchResults: List<LocationResult> = emptyList(),
    val savedRestaurantIds: Set<String> = emptySet(),
    val error: String? = null,
    val locationError: String? = null,
    val noRestaurantsFound: Boolean = false,
    val isManuallySelected: Boolean = false,
    // ISO 3166-1 alpha-2 country code derived from the user's last detected GPS
    val countryCode: String? = null,
    // Set to true once the first batch of DB data is loaded so Splash can navigate
    val isInitialDataLoaded: Boolean = false
)

private fun Restaurant.uniqueRestaurantKey(): String {
    val normalizedName = name.trim().lowercase()
    val normalizedCity = city.trim().lowercase()
    val placeKey = googlePlaceId?.trim()?.takeIf { it.isNotBlank() }
    return when {
        normalizedName.isNotBlank() -> "$normalizedName|$normalizedCity"
        placeKey != null -> "place|$placeKey"
        else -> "id|$id"
    }
}

private fun List<Restaurant>.distinctRestaurants(): List<Restaurant> =
    distinctBy { it.uniqueRestaurantKey() }

private fun List<Restaurant>.distinctRestaurantNames(): List<Restaurant> =
    distinctBy {
        it.name.trim().lowercase().ifBlank {
            it.googlePlaceId?.trim()?.takeIf { placeId -> placeId.isNotBlank() } ?: it.id
        }
    }

/**
 * ViewModel for Location-based Home Screen
 */
class LocationHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LocationHomeUiState())
    val uiState: StateFlow<LocationHomeUiState> = _uiState.asStateFlow()

    private var locationService: LocationService? = null
    private var placesService: PlacesService? = null
    private val authRepository = AuthRepository()
    private val databaseRepository = DatabaseRepository()
    private var hasStartedHomePreload = false

    init {
        // Only preload if the user is already signed in.
        // For first-time users the Splash screen will trigger this after login.
        if (authRepository.isSignedIn()) {
            preloadHomeFromSplash()
            loadSavedRestaurants()
        }
    }

    private fun loadSavedRestaurants() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            databaseRepository.getSavedRestaurantIds(userId)
                .onSuccess { ids ->
                    _uiState.update { it.copy(savedRestaurantIds = ids) }
                }
                .onFailure {
                    Logger.e("LocationHomeViewModel", "Failed to load saved restaurants: ${it.message}", it)
                }
        }
    }

    fun toggleRestaurantSaved(restaurantId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        val currentIds = _uiState.value.savedRestaurantIds
        val wasSaved = currentIds.contains(restaurantId)

        _uiState.update {
            it.copy(
                savedRestaurantIds = if (wasSaved) currentIds - restaurantId else currentIds + restaurantId
            )
        }

        viewModelScope.launch {
            databaseRepository.toggleRestaurantSave(userId, restaurantId)
                .onFailure {
                    // Rollback optimistic update
                    _uiState.update { state ->
                        val now = state.savedRestaurantIds
                        state.copy(
                            savedRestaurantIds = if (wasSaved) now + restaurantId else now - restaurantId
                        )
                    }
                }
        }
    }

    fun preloadHomeFromSplash() {
        if (hasStartedHomePreload) return
        if (!authRepository.isSignedIn()) {
            Logger.d("LocationHomeViewModel", "Skipping preload — user not authenticated yet")
            return
        }
        hasStartedHomePreload = true
        loadSavedLocation()
    }

    private fun loadSavedLocation() {
        viewModelScope.launch {
            try {
                val savedLocation = authRepository.getLastLocation()
                if (savedLocation != null) {
                    Logger.d("LocationHomeViewModel", "Loaded saved location from profile: $savedLocation")
                    _uiState.update { it.copy(selectedLocation = savedLocation) }
                    loadDataForLocation(savedLocation)
                } else {
                    Logger.d("LocationHomeViewModel", "No saved location found, waiting for current-location detection")
                    _uiState.update {
                        it.copy(
                            selectedLocation = null,
                            topRestaurants = emptyList(),
                            allRestaurants = emptyList(),
                            nearbyRestaurants = emptyList(),
                            isLoading = false,
                            isInitialDataLoaded = true,
                            noRestaurantsFound = false
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.e("LocationHomeViewModel", "Error loading saved location: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        selectedLocation = null,
                        topRestaurants = emptyList(),
                        allRestaurants = emptyList(),
                        nearbyRestaurants = emptyList(),
                        isLoading = false,
                        isInitialDataLoaded = true
                    )
                }
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
        _uiState.update { it.copy(selectedLocation = location, isLoading = true, locationError = null, isManuallySelected = true) }

        // Save the selected location to user profile
        viewModelScope.launch {
            try {
                val result = authRepository.updateLastLocation(location)
                result.fold(
                    onSuccess = {
                        Logger.d("LocationHomeViewModel", "Successfully saved location to profile: $location")
                    },
                    onFailure = { error ->
                        Logger.e("LocationHomeViewModel", "Failed to save location: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Logger.e("LocationHomeViewModel", "Exception saving location: ${e.message}", e)
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
    fun selectLocationWithCoordinates(city: String, latitude: Double, longitude: Double, isManual: Boolean = false, countryCode: String? = null) {
        _uiState.update {
            it.copy(
                selectedLocation = city,
                currentLatitude = latitude,
                currentLongitude = longitude,
                // When auto-detected via GPS, these coords are the user's real position.
                // When manually selected, leave user GPS untouched.
                userLatitude = if (!isManual) latitude else it.userLatitude,
                userLongitude = if (!isManual) longitude else it.userLongitude,
                isLoading = true,
                isManuallySelected = if (isManual) true else it.isManuallySelected,
                countryCode = countryCode ?: it.countryCode
            )
        }
        Logger.d("LocationHomeViewModel", "selectLocationWithCoordinates($city, $latitude, $longitude, isManual=$isManual)")
        loadDataForLocation(city, knownLatitude = latitude, knownLongitude = longitude)
    }

    /**
     * Fetch the user's live GPS and update [LocationHomeUiState.userLatitude]/[userLongitude]
     * without changing the selected city or reloading data. Used to keep the distance
     * shown on restaurant cards in sync with the user's real position.
     */
    fun refreshUserLocation() {
        val service = locationService ?: return
        viewModelScope.launch {
            when (val result = service.getCurrentLocationWithDetails()) {
                is LocationOperationResult.Success -> {
                    val loc = result.location
                    _uiState.update {
                        it.copy(
                            userLatitude = loc.latitude,
                            userLongitude = loc.longitude,
                            countryCode = loc.countryCode ?: it.countryCode
                        )
                    }
                    Logger.d("LocationHomeViewModel", "refreshUserLocation -> ${loc.latitude}, ${loc.longitude}, country=${loc.countryCode}")
                }
                is LocationOperationResult.Error -> {
                    Logger.w("LocationHomeViewModel", "refreshUserLocation failed: ${result.reason}")
                }
            }
        }
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
                                countryCode = location.countryCode ?: it.countryCode
                            )
                        }
                        selectLocationWithCoordinates(
                            city = location.cityName,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isManual = false
                        )
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

    /**
     * Refresh Home data from live database/API based on the current selected location.
     */
    fun fetchNearbyForCuisine(keyword: String?) {
        viewModelScope.launch {
            val state = _uiState.value
            val lat = state.currentLatitude ?: state.userLatitude ?: return@launch
            val lon = state.currentLongitude ?: state.userLongitude ?: return@launch
            try {
                val nearby = placesService?.findNearbyRestaurants(
                    latitude = lat,
                    longitude = lon,
                    radiusInMeters = 5000,
                    keyword = keyword
                ) ?: emptyList()
                _uiState.update { it.copy(nearbyRestaurants = nearby) }
            } catch (e: Exception) {
                Logger.e("LocationHomeViewModel", "fetchNearbyForCuisine failed: ${e.message}", e)
            }
        }
    }

    fun refreshHomeData() {
        val currentState = _uiState.value
        val selectedLocation = currentState.selectedLocation
        if (!selectedLocation.isNullOrBlank()) {
            loadDataForLocation(
                location = selectedLocation,
                knownLatitude = currentState.currentLatitude,
                knownLongitude = currentState.currentLongitude
            )
        } else {
            Logger.d("LocationHomeViewModel", "Skipping global home refresh until a city is selected")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isInitialDataLoaded = true,
                    topRestaurants = emptyList(),
                    allRestaurants = emptyList(),
                    nearbyRestaurants = emptyList()
                )
            }
        }
    }

    fun ensureHomeDataLoaded() {
        val state = _uiState.value
        val hasData = state.topRestaurants.isNotEmpty() ||
            state.topDishes.isNotEmpty() ||
            state.allRestaurants.isNotEmpty() ||
            state.nearbyRestaurants.isNotEmpty()
        if (state.isLoading || hasData) return
        refreshHomeData()
    }

    private fun loadAllRestaurants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val restaurantsResult = databaseRepository.getRestaurants()
                val topDishesResult = databaseRepository.getTopRatedDishes(limit = 10)
                var topDishes = topDishesResult.getOrElse {
                    Logger.e("LocationHomeViewModel", "Failed to load top dishes: ${it.message}")
                    emptyList()
                }
                Logger.d("LocationHomeViewModel", "loadAllRestaurants - topDishes=${topDishes.size}")

                restaurantsResult.onSuccess { restaurants ->
                    val distinctRestaurants = restaurants.distinctRestaurants()
                    _uiState.update {
                        it.copy(
                            allRestaurants = distinctRestaurants,
                            topDishes = topDishes,
                            isLoading = false,
                            isInitialDataLoaded = true,
                            noRestaurantsFound = distinctRestaurants.isEmpty()
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, isInitialDataLoaded = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadDataForLocation(location: String, knownLatitude: Double? = null, knownLongitude: Double? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, noRestaurantsFound = false) }
            Logger.d("LocationHomeViewModel", "Loading data for location: $location")

            // Wait briefly for placesService to be injected if it hasn't been yet
            if (placesService == null) {
                kotlinx.coroutines.delay(300)
            }

            try {
                // Phase 1 (fast paint): DB data first
                Logger.d("LocationHomeViewModel", "Fetching restaurants from database for: $location")
                val restaurantsResult = databaseRepository.getRestaurantsByCity(location)
                var locationRestaurants: List<Restaurant> = emptyList()

                restaurantsResult.fold(
                    onSuccess = { restaurants ->
                        val distinctRestaurants = restaurants.distinctRestaurants()
                        locationRestaurants = distinctRestaurants
                        Logger.d("LocationHomeViewModel", "Database returned ${restaurants.size} restaurants (${distinctRestaurants.size} distinct)")
                        val topRestaurants = distinctRestaurants
                            .distinctRestaurantNames()
                            .sortedByDescending { it.averageRating }
                            .take(5)

                        val locationRestaurantIds = distinctRestaurants.map { it.id }
                        val topDishesResult = databaseRepository.getTopRatedDishesForRestaurants(
                            restaurantIds = locationRestaurantIds,
                            limit = 10
                        )
                        var topDishes = topDishesResult.getOrElse {
                            Logger.e("LocationHomeViewModel", "Failed to load top dishes: ${it.message}")
                            emptyList()
                        }
                        // If still empty (DB has no dishes for this city), fall back to global dishes
                        if (topDishes.isEmpty()) {
                            Logger.d("LocationHomeViewModel", "Location dishes empty, falling back to global top dishes")
                            topDishes = databaseRepository.getTopRatedDishes(limit = 10).getOrElse { emptyList() }
                        }

                        val existingNearby = _uiState.value.nearbyRestaurants
                        val noResults = restaurants.isEmpty() && existingNearby.isEmpty()

                        Logger.d(
                            "LocationHomeViewModel",
                            "Fast phase done - topRestaurants=${topRestaurants.size}, " +
                                "allRestaurants=${distinctRestaurants.size}, topDishes=${topDishes.size}"
                        )

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialDataLoaded = true,
                                topRestaurants = topRestaurants,
                                topDishes = topDishes,
                                allRestaurants = distinctRestaurants,
                                error = null,
                                noRestaurantsFound = noResults
                            )
                        }
                    },
                    onFailure = { error ->
                        Logger.e("LocationHomeViewModel", "Database query failed: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialDataLoaded = true,
                                error = if (it.nearbyRestaurants.isEmpty())
                                    "Failed to load restaurants: ${error.message}"
                                else null,
                                topRestaurants = emptyList(),
                                topDishes = emptyList(),
                                allRestaurants = emptyList(),
                                noRestaurantsFound = it.nearbyRestaurants.isEmpty()
                            )
                        }
                    }
                )

                // Phase 2 (background enrich): geocode + nearby lookup
                Logger.d("LocationHomeViewModel", "Getting coordinates for: $location")
                var coordinates: LocationResult? = null

                if (locationService != null) {
                    try {
                        val geocodedLocation = locationService?.getCoordinatesForCity(location)
                        if (geocodedLocation != null) {
                            Logger.d("LocationHomeViewModel", "Native geocoder success for $location: ${geocodedLocation.latitude}, ${geocodedLocation.longitude}")
                            coordinates = geocodedLocation
                        } else {
                            Logger.d("LocationHomeViewModel", "Native geocoder returned null for $location")
                        }
                    } catch (e: Exception) {
                        Logger.e("LocationHomeViewModel", "Native geocoder error for $location: ${e.message}", e)
                    }
                }

                if (coordinates == null && placesService != null) {
                    Logger.d("LocationHomeViewModel", "Trying Google Places geocoding for: $location")
                    try {
                        val geocodedCity = placesService?.geocodeCity(location)
                        if (geocodedCity != null) {
                            Logger.d("LocationHomeViewModel", "Google Places geocoded $location to: ${geocodedCity.latitude}, ${geocodedCity.longitude}")
                            coordinates = LocationResult(
                                latitude = geocodedCity.latitude,
                                longitude = geocodedCity.longitude,
                                cityName = location,
                                fullAddress = geocodedCity.formattedAddress
                            )
                        } else {
                            Logger.d("LocationHomeViewModel", "Google Places geocoding returned null for $location")
                        }
                    } catch (e: Exception) {
                        Logger.e("LocationHomeViewModel", "Google Places geocoding error: ${e.message}", e)
                    }
                }

                if (coordinates == null) {
                    val lat = knownLatitude ?: _uiState.value.currentLatitude
                    val lng = knownLongitude ?: _uiState.value.currentLongitude
                    Logger.d("LocationHomeViewModel", "Fallback check - coords: lat=$lat, lng=$lng (known=${knownLatitude != null})")
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        Logger.d("LocationHomeViewModel", "Using fallback coordinates for $location: $lat, $lng")
                        coordinates = LocationResult(
                            latitude = lat,
                            longitude = lng,
                            cityName = location
                        )
                    } else {
                        Logger.w("LocationHomeViewModel", "All geocoding methods failed for $location - no coordinates available")
                    }
                }

                Logger.d("LocationHomeViewModel", "placesService=${placesService != null}, coordinates=${coordinates != null}")
                val nearbyRestaurants = if (placesService != null && coordinates != null) {
                    try {
                        _uiState.update {
                            it.copy(
                                currentLatitude = coordinates.latitude,
                                currentLongitude = coordinates.longitude
                            )
                        }
                        Logger.d("LocationHomeViewModel", "Calling Google Places API at: ${coordinates.latitude}, ${coordinates.longitude}")
                        val nearby = placesService?.findNearbyRestaurants(
                            latitude = coordinates.latitude,
                            longitude = coordinates.longitude,
                            radiusInMeters = 5000
                        ) ?: emptyList()
                        Logger.d("LocationHomeViewModel", "Google Places returned ${nearby.size} restaurants")
                        nearby
                    } catch (e: Exception) {
                        Logger.e("LocationHomeViewModel", "Failed to load nearby restaurants: ${e.message}", e)
                        emptyList()
                    }
                } else {
                    if (coordinates == null) {
                        Logger.d("LocationHomeViewModel", "Skipping Google Places - geocoding failed for '$location'")
                    } else {
                        Logger.d("LocationHomeViewModel", "Skipping Google Places - placesService is null")
                    }
                    emptyList()
                }

                _uiState.update {
                    val noResults = locationRestaurants.isEmpty() && nearbyRestaurants.isEmpty()
                    it.copy(
                        nearbyRestaurants = nearbyRestaurants,
                        noRestaurantsFound = noResults,
                        error = if (noResults && it.allRestaurants.isEmpty()) it.error else null
                    )
                }
            } catch (e: Exception) {
                Logger.e("LocationHomeViewModel", "Critical error: ${e.message}", e)
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
                Logger.e("GameViewModel", "Error loading game data: ${e.message}", e)
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
            Logger.e("GameViewModel", "Error loading leaderboard: ${e.message}", e)
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
            Logger.e("GameViewModel", "Error loading achievements: ${e.message}", e)
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
                    Logger.d("GameViewModel", "Challenge completed: ${challenge.title}")
                    // Reload game data to refresh challenges and XP
                    loadGameData()
                },
                onFailure = { error ->
                    Logger.e("GameViewModel", "Failed to complete challenge: ${error.message}")
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
