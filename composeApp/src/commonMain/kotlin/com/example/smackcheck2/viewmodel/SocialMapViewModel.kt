package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.SocialMapRepository
import com.example.smackcheck2.model.MapMode
import com.example.smackcheck2.model.MapUserMarker
import com.example.smackcheck2.model.SocialMapUiState
import com.example.smackcheck2.platform.LocationOperationResult
import com.example.smackcheck2.platform.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * ViewModel for the Social Map screen (Snapchat-style map with user avatars)
 */
class SocialMapViewModel(
    private val locationService: LocationService?
) : ViewModel() {
    companion object {
        private const val DEFAULT_RADIUS_METERS = 3000
        private const val AUTO_REFRESH_INTERVAL_MS = 30_000L // 30 seconds
        private const val DEFAULT_HOURS_AGO = 168 // 7 days
        private const val WORLD_POSTS_LIMIT = 140
        private const val MIN_NEARBY_REFRESH_INTERVAL_MS = 8_000L
        private const val WORLD_POSTS_CACHE_TTL_MS = 90_000L

        private var cachedWorldPosts: List<MapUserMarker> = emptyList()
        private var cachedWorldPostsAtMs: Long = 0L
    }

    private val repository = SocialMapRepository()

    private val _uiState = MutableStateFlow(SocialMapUiState())
    val uiState: StateFlow<SocialMapUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var nearbyUsersJob: Job? = null
    private var locationRequestJob: Job? = null
    private var myRatingsJob: Job? = null
    private var hasLoadedMyRatings = false
    private var lastNearbyFetchAtMs = 0L

    init {
        loadCurrentUserProfile()
        hydrateNearbyPostsFromCache()
        loadNearbyUsers() // World posts preload (throttled + deduped)
        checkExistingLocationPermission()
    }

    /**
     * Background warm-up hook used from Home so Map opens faster.
     */
    fun preloadFromHome() {
        loadNearbyUsers(force = false)
    }

    /**
     * Called when the map screen becomes visible.
     */
    fun onActive() {
        // Resume auto-refresh if we already have a location
        if (_uiState.value.currentLatitude != null) {
            startAutoRefresh()
        }
        // If posts were never loaded, warm them now
        if (_uiState.value.nearbyUsers.isEmpty() && !_uiState.value.isLoading) {
            loadNearbyUsers(force = false)
        }
    }

    /**
     * Called when the map screen is hidden (user switched to another tab).
     */
    fun onInactive() {
        stopAutoRefresh()
        dismissUserPreview()
    }

    private fun hydrateNearbyPostsFromCache() {
        val now = Clock.System.now().toEpochMilliseconds()
        val cacheValid = cachedWorldPosts.isNotEmpty() &&
            (now - cachedWorldPostsAtMs) <= WORLD_POSTS_CACHE_TTL_MS
        if (!cacheValid) return
        _uiState.update {
            it.copy(
                nearbyUsers = cachedWorldPosts,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    /**
     * On init, if the system permission is already granted (e.g. user granted it
     * earlier via the onboarding screen), automatically fetch the current location
     * so the "Location Required" prompt doesn't appear unnecessarily.
     */
    private fun checkExistingLocationPermission() {
        viewModelScope.launch {
            try {
                val service = locationService ?: return@launch
                if (service.hasLocationPermission()) {
                    _uiState.update { it.copy(locationPermissionGranted = true) }
                    requestCurrentLocation()
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Initialize the map with user's current location
     */
    fun initializeWithLocation(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(
            currentLatitude = latitude,
            currentLongitude = longitude,
            locationPermissionGranted = true,
            isLoading = false
        ) }

        // Update user's location in database
        updateUserLocationInDb(latitude, longitude)

        // Start auto-refresh (posts were already loaded in init)
        startAutoRefresh()
    }

    /**
     * Request current location from device GPS
     */
    fun requestCurrentLocation() {
        if (locationRequestJob?.isActive == true) return
        locationRequestJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.nearbyUsers.isEmpty()) }
            
            if (locationService == null) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Location service not available"
                ) }
                return@launch
            }

            try {
                if (!locationService.hasLocationPermission()) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        locationPermissionGranted = false
                    ) }
                    return@launch
                }

                when (val result = locationService.getCurrentLocationWithDetails()) {
                    is LocationOperationResult.Success -> {
                        initializeWithLocation(result.location.latitude, result.location.longitude)
                    }
                    is LocationOperationResult.Error -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            errorMessage = "Failed to get location: ${result.reason}"
                        ) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Failed to get location: ${e.message}"
                ) }
            }
        }
    }

    /**
     * Load current user's map profile
     */
    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            repository.getCurrentUserMapProfile()
                .onSuccess { profile ->
                    _uiState.update { it.copy(currentUserProfile = profile) }
                }
                .onFailure { error ->
                    println("SocialMapViewModel: Failed to load user profile: ${error.message}")
                }
        }
    }

    /**
     * Load nearby users with dish posts
     */
    fun loadNearbyUsers(force: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        val state = _uiState.value
        val cacheValid = cachedWorldPosts.isNotEmpty() &&
            (now - cachedWorldPostsAtMs) <= WORLD_POSTS_CACHE_TTL_MS

        if (!force && cacheValid && state.nearbyUsers.isEmpty()) {
            _uiState.update {
                it.copy(
                    nearbyUsers = cachedWorldPosts,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
        }

        if (!force) {
            val recentlyFetched = now - lastNearbyFetchAtMs < MIN_NEARBY_REFRESH_INTERVAL_MS
            if (state.isRefreshing || (recentlyFetched && state.nearbyUsers.isNotEmpty())) return
        }

        nearbyUsersJob?.cancel()
        nearbyUsersJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, isLoading = it.nearbyUsers.isEmpty()) }

            // Load all posts worldwide, each placed at the restaurant's coordinates
            repository.getAllDishPosts(limit = WORLD_POSTS_LIMIT)
                .onSuccess { posts ->
                    lastNearbyFetchAtMs = Clock.System.now().toEpochMilliseconds()
                    cachedWorldPosts = posts
                    cachedWorldPostsAtMs = lastNearbyFetchAtMs
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            nearbyUsers = posts,
                            lastRefreshTime = Clock.System.now().toEpochMilliseconds(),
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Failed to load dish posts: ${error.message}"
                    ) }
                }
        }
    }

    /**
     * Change the search radius
     */
    fun setRadius(radiusMeters: Int) {
        _uiState.update { it.copy(radiusMeters = radiusMeters) }
        loadNearbyUsers(force = true)
    }

    /**
     * Select a user marker to show their dish preview
     */
    fun selectUser(user: MapUserMarker?) {
        _uiState.update { it.copy(selectedUser = user) }
    }

    /**
     * Dismiss the selected user's preview
     */
    fun dismissUserPreview() {
        _uiState.update { it.copy(selectedUser = null) }
    }

    /**
     * Load the current user's own dish posts for MY RATINGS mode.
     */
    fun loadMyRatings(force: Boolean = false) {
        if (myRatingsJob?.isActive == true) {
            if (!force) return
            myRatingsJob?.cancel()
        }
        if (!force && hasLoadedMyRatings && _uiState.value.myRatingMarkers.isNotEmpty()) return
        myRatingsJob = viewModelScope.launch {
            repository.getMyRatingPosts()
                .onSuccess { markers ->
                    hasLoadedMyRatings = true
                    val validMarkers = markers.filter { it.latitude != 0.0 || it.longitude != 0.0 }
                    println("SocialMapViewModel: loadMyRatings success - ${markers.size} total, ${validMarkers.size} valid markers")
                    _uiState.update {
                        it.copy(
                            myRatingMarkers = markers,
                            fitBoundsTrigger = if (validMarkers.isNotEmpty()) it.fitBoundsTrigger + 1 else it.fitBoundsTrigger
                        )
                    }
                }
                .onFailure { error ->
                    println("SocialMapViewModel: loadMyRatings failed: ${error.message}")
                }
        }
    }

    /**
     * Switch between NEARBY and MY_RATINGS map modes.
     */
    fun setMapMode(mode: MapMode) {
        _uiState.update { it.copy(mapMode = mode, selectedUser = null) }
        if (mode == MapMode.MY_RATINGS) {
            loadMyRatings(force = true)
        } else {
            loadNearbyUsers(force = _uiState.value.nearbyUsers.isEmpty())
        }
    }

    /**
     * Re-center the map on the user's current location.
     */
    fun recenter() {
        viewModelScope.launch {
            requestCurrentLocation()
            _uiState.update { it.copy(recenterTrigger = it.recenterTrigger + 1) }
        }
    }

    /**
     * Manually refresh nearby users
     */
    fun refresh() {
        if (_uiState.value.mapMode == MapMode.MY_RATINGS) {
            hasLoadedMyRatings = false
            loadMyRatings()
        } else {
            loadNearbyUsers(force = true)
        }
    }

    /**
     * Toggle location sharing
     */
    fun toggleLocationSharing(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleLocationSharing(enabled)
                .onSuccess {
                    loadCurrentUserProfile() // Reload profile to get updated status
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        errorMessage = "Failed to update location sharing: ${error.message}"
                    ) }
                }
        }
    }

    /**
     * Update user's location in the database
     */
    private fun updateUserLocationInDb(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.updateUserLocation(latitude, longitude)
                .onFailure { error ->
                    println("SocialMapViewModel: Failed to update location: ${error.message}")
                }
        }
    }

    /**
     * Start auto-refresh every 30 seconds
     */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                if (_uiState.value.currentLatitude != null) {
                    loadNearbyUsers()
                }
            }
        }
    }

    /**
     * Stop auto-refresh
     */
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
