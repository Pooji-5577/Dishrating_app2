package com.example.smackcheck2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.backhandler.BackHandler
import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.data.repository.SocialMapRepository
import com.example.smackcheck2.gamification.GamificationViewModel
import com.example.smackcheck2.gamification.PointsEarnedEvent
import com.example.smackcheck2.gamification.PointsConfig
import com.example.smackcheck2.location.CommonLocationState
import com.example.smackcheck2.location.SharedLocationState
import com.example.smackcheck2.location.requestCurrentLocationDetection
import com.example.smackcheck2.model.AuthState
import com.example.smackcheck2.notifications.NotificationDeepLink
import com.example.smackcheck2.notifications.NotificationViewModel
import com.example.smackcheck2.ui.components.PointsEarnedPopup
import com.example.smackcheck2.ui.screens.PermissionsOnboardingScreen
import com.example.smackcheck2.viewmodel.AuthViewModel
import com.example.smackcheck2.viewmodel.LocationHomeViewModel
import com.example.smackcheck2.viewmodel.SocialFeedViewModel

/**
 * App-level coordinator that handles cross-cutting concerns:
 * - Auth state reactions (notification init, feed preload, retention tracking)
 * - Notification deep link routing
 * - Location permission flow and state propagation
 * - Shared ViewModel lifecycle
 * - Points popup overlay
 * - Permissions onboarding dialog
 * - System back handling
 *
 * This keeps [SmackCheckNavHost] focused purely on navigation routing.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppCoordinator(
    preferencesRepository: PreferencesRepository,
    navigationState: NavigationState,
    authViewModel: AuthViewModel,
    locationHomeViewModel: LocationHomeViewModel,
    restaurantPhotoViewModel: com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel,
    gamificationViewModel: GamificationViewModel,
    notificationViewModel: NotificationViewModel,
    socialFeedViewModel: SocialFeedViewModel,
    locationService: com.example.smackcheck2.platform.LocationService?,
    placesService: com.example.smackcheck2.platform.PlacesService?,
    geofencingService: com.example.smackcheck2.platform.GeofencingService?,
    imagePicker: com.example.smackcheck2.platform.ImagePicker?,
    shareService: com.example.smackcheck2.platform.ShareService?,
    content: @Composable () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val locationState by SharedLocationState.locationState.collectAsState()
    val locationUiState by locationHomeViewModel.uiState.collectAsState()

    var pointsEvent by remember { mutableStateOf<PointsEarnedEvent?>(null) }
    var hasRequestedPermission by remember { mutableStateOf(false) }
    var shouldShowPermissionDialog by remember { mutableStateOf(false) }

    val isAuthenticated = when (authState) {
        is AuthState.Authenticated -> true
        is AuthState.Unauthenticated -> false
        is AuthState.Unknown -> null
    }

    // Set location and places service on LocationHomeViewModel
    LaunchedEffect(locationService, placesService) {
        locationHomeViewModel.setLocationService(locationService)
        locationHomeViewModel.setPlacesService(placesService)
    }

    // Points-earned popup collection
    LaunchedEffect(Unit) {
        gamificationViewModel.pointsEarned.collect { event ->
            pointsEvent = event
        }
    }

    // SharedLocationState → LocationHomeViewModel sync
    LaunchedEffect(locationState) {
        if (locationUiState.isManuallySelected) return@LaunchedEffect
        when (val state = locationState) {
            is CommonLocationState.Success -> {
                val data = state.data
                val city = data.city ?: "Unknown Location"
                locationHomeViewModel.selectLocationWithCoordinates(
                    city = city,
                    latitude = data.latitude,
                    longitude = data.longitude,
                    countryCode = data.countryCode
                )
            }
            else -> { /* handled by individual screens */ }
        }
    }

    // System back handler
    BackHandler(enabled = true) {
        when {
            navigationState.canGoBack -> navigationState.navigateBack()
            navigationState.currentScreen is Screen.DarkHome -> { /* consume to prevent app exit */ }
            else -> {
                if (!navigationState.navigateBack()) {
                    println("AppCoordinator: Cannot navigate back, staying on current screen")
                }
            }
        }
    }

    // Auth reactions: init push notifications, preload feed, track retention
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) {
            notificationViewModel.initializePushNotifications()
            notificationViewModel.refresh()
            socialFeedViewModel.loadFeed()

            try {
                val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                val firstOpen = preferencesRepository.getFirstOpenTimestamp()
                if (firstOpen == 0L) {
                    preferencesRepository.saveFirstOpenTimestamp(now)
                } else if (!preferencesRepository.isDay1RetentionTracked()) {
                    val hoursSinceFirstOpen = (now - firstOpen) / (1000 * 60 * 60)
                    if (hoursSinceFirstOpen >= 24) {
                        Analytics.track("day_1_retention", mapOf(
                            "hours_since_first_open" to hoursSinceFirstOpen
                        ))
                        preferencesRepository.setDay1RetentionTracked()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // Notification deep link routing
    val pendingNotificationTarget by NotificationDeepLink.pendingTarget.collectAsState()
    LaunchedEffect(pendingNotificationTarget, isAuthenticated) {
        val target = pendingNotificationTarget ?: return@LaunchedEffect
        if (isAuthenticated != true) return@LaunchedEffect
        when (target.screen) {
            "DishDetail" -> {
                val dishId = target.dishId
                if (!dishId.isNullOrBlank()) {
                    navigationState.navigateToWithArgs(Screen.DishDetail, "dishId" to dishId)
                } else {
                    navigationState.navigateToMainTab(Screen.SocialFeed)
                }
            }
            "SocialFeed" -> navigationState.navigateToMainTab(Screen.SocialFeed)
            "GameScreen", "Game" -> navigationState.navigateTo(Screen.Game)
            "NotificationsList", "Notifications" -> navigationState.navigateTo(Screen.NotificationsList)
            "UserProfile" -> {
                val userId = target.userId
                if (!userId.isNullOrBlank()) {
                    navigationState.navigateToWithArgs(Screen.UserProfile, "userId" to userId)
                } else {
                    navigationState.navigateTo(Screen.DarkHome)
                }
            }
            "Home" -> navigationState.navigateTo(Screen.DarkHome)
            else -> navigationState.navigateTo(Screen.NotificationsList)
        }
        NotificationDeepLink.consume()
    }

    // Location permission tracking
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true && !hasRequestedPermission) {
            hasRequestedPermission = true
            val alreadySeen = try {
                preferencesRepository.hasSeenPermissionsOnboarding()
            } catch (_: Exception) { false }
            if (!alreadySeen) {
                shouldShowPermissionDialog = true
            }
        }
    }

    // Render main content
    content()

    // Points earned popup overlay
    PointsEarnedPopup(
        event = pointsEvent,
        onDismissed = { pointsEvent = null }
    )

    // Permissions onboarding dialog
    if (shouldShowPermissionDialog && isAuthenticated == true) {
        PermissionsOnboardingScreen(
            onComplete = { locationWasGranted ->
                shouldShowPermissionDialog = false
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        preferencesRepository.setPermissionsOnboardingSeen()
                    } catch (_: Exception) { }
                }
                if (locationWasGranted) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            locationService?.getCurrentLocation()?.let { location ->
                                SharedLocationState.onLocationDetected(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    city = location.cityName ?: "Unknown",
                                    countryCode = location.countryCode
                                )
                                val repository = SocialMapRepository()
                                repository.updateUserLocation(location.latitude, location.longitude)
                            }
                        } catch (e: Exception) {
                            println("Auto location detection error: ${e.message}")
                        }
                    }
                }
            }
        )
    }
}
