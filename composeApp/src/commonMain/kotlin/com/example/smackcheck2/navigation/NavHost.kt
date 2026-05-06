package com.example.smackcheck2.navigation

import com.example.smackcheck2.analytics.Analytics
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.model.CapturedImage
import com.example.smackcheck2.model.Restaurant
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smackcheck2.model.AuthState
import com.example.smackcheck2.ui.screens.AllRestaurantsScreen
import com.example.smackcheck2.ui.screens.BadgesScreen
import com.example.smackcheck2.ui.screens.DarkDishCaptureScreen
import com.example.smackcheck2.ui.screens.DarkDishConfirmScreen
import com.example.smackcheck2.ui.screens.DarkDishRatingScreen
import com.example.smackcheck2.ui.screens.DarkGameScreen
import com.example.smackcheck2.ui.screens.DarkHomeScreen
import com.example.smackcheck2.ui.screens.DarkLoginScreen
import com.example.smackcheck2.ui.screens.DarkProfileScreen
import com.example.smackcheck2.ui.screens.DarkSearchScreen
import com.example.smackcheck2.ui.screens.DarkSplashScreen
import com.example.smackcheck2.ui.screens.DarkTopDishesScreen
import com.example.smackcheck2.ui.screens.DarkTopRestaurantsScreen
import com.example.smackcheck2.ui.screens.DishCaptureScreen
import com.example.smackcheck2.ui.screens.DishDetailScreen
import com.example.smackcheck2.ui.screens.DishPreviewScreen
import com.example.smackcheck2.ui.screens.DishRatingScreen
import com.example.smackcheck2.ui.screens.GameScreen
import com.example.smackcheck2.ui.screens.LocationHomeScreen
import com.example.smackcheck2.ui.screens.LocationPermissionScreen
import com.example.smackcheck2.ui.screens.LocationSelectionScreen
import com.example.smackcheck2.ui.screens.LoginScreen
import com.example.smackcheck2.ui.screens.ManualDishEntryScreen
import com.example.smackcheck2.ui.screens.ManualRestaurantEntryScreen
import com.example.smackcheck2.ui.screens.NearbyRestaurantsScreen
import com.example.smackcheck2.ui.screens.RegisterScreen
import com.example.smackcheck2.ui.screens.RestaurantDetailScreen
import com.example.smackcheck2.ui.screens.SearchScreen
import com.example.smackcheck2.ui.screens.PermissionsOnboardingScreen
import com.example.smackcheck2.ui.screens.SocialFeedScreen
import com.example.smackcheck2.ui.screens.SplashScreen
import com.example.smackcheck2.ui.screens.TopDishesScreen
import com.example.smackcheck2.ui.screens.TopRestaurantsScreen
import com.example.smackcheck2.ui.screens.UserProgressScreen
import com.example.smackcheck2.ui.screens.AchievementsListScreen
import com.example.smackcheck2.ui.screens.ProgressDashboardScreen
import com.example.smackcheck2.platform.LocalLocationService
import com.example.smackcheck2.platform.LocalPlacesService
import com.example.smackcheck2.platform.LocalGeofencingService
import com.example.smackcheck2.platform.RequestLocationPermission
import com.example.smackcheck2.platform.LocalImagePicker
import com.example.smackcheck2.platform.LocalShareService
import com.example.smackcheck2.viewmodel.AuthViewModel
import com.example.smackcheck2.viewmodel.NearbyRestaurantsViewModel
import com.example.smackcheck2.viewmodel.DishCaptureViewModel
import com.example.smackcheck2.viewmodel.DishDetailViewModel
import com.example.smackcheck2.viewmodel.DishRatingViewModel
import com.example.smackcheck2.gamification.GamificationViewModel
import com.example.smackcheck2.viewmodel.LocationHomeViewModel
import com.example.smackcheck2.viewmodel.LoginViewModel
import com.example.smackcheck2.viewmodel.ManualDishEntryViewModel
import com.example.smackcheck2.viewmodel.ManualRestaurantViewModel
import com.example.smackcheck2.viewmodel.ProfileViewModel
import com.example.smackcheck2.viewmodel.RegisterViewModel
import com.example.smackcheck2.viewmodel.RestaurantDetailViewModel
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import com.example.smackcheck2.viewmodel.SearchViewModel
import com.example.smackcheck2.viewmodel.UserProgressViewModel
import com.example.smackcheck2.viewmodel.EditProfileViewModel
import com.example.smackcheck2.viewmodel.NotificationSettingsViewModel
import com.example.smackcheck2.viewmodel.AccountSettingsViewModel
import com.example.smackcheck2.viewmodel.PrivacySettingsViewModel
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.ui.screens.EditProfileScreen
import com.example.smackcheck2.ui.screens.NotificationSettingsScreen
import com.example.smackcheck2.ui.screens.AccountSettingsScreen
import com.example.smackcheck2.ui.screens.PrivacySettingsScreen
import com.example.smackcheck2.ui.screens.HelpFaqScreen
import com.example.smackcheck2.ui.screens.ContactSupportScreen
import com.example.smackcheck2.ui.screens.UserProfileScreen
import com.example.smackcheck2.ui.screens.FollowersListScreen
import com.example.smackcheck2.ui.screens.DiscoverUsersScreen
import com.example.smackcheck2.viewmodel.DiscoverUsersViewModel
import com.example.smackcheck2.ui.screens.CommentsScreen
import com.example.smackcheck2.ui.screens.NotificationsListScreen
import com.example.smackcheck2.ui.screens.SocialMapScreen
import com.example.smackcheck2.ui.screens.ProfileSetupScreen
import com.example.smackcheck2.viewmodel.ProfileSetupViewModel
import com.example.smackcheck2.viewmodel.SocialFeedViewModel
import com.example.smackcheck2.viewmodel.SocialMapViewModel
import com.example.smackcheck2.viewmodel.CommentsViewModel
import com.example.smackcheck2.viewmodel.NotificationsViewModel
import com.example.smackcheck2.notifications.NotificationDeepLink
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.FollowListUiState
import com.example.smackcheck2.model.UserProfileUiState
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
// Main branch additions: gamification, notifications, location state
import com.example.smackcheck2.gamification.PointsEarnedEvent
import com.example.smackcheck2.ui.components.PointsEarnedPopup
import com.example.smackcheck2.notifications.NotificationViewModel
import com.example.smackcheck2.ui.screens.NotificationsScreen
import com.example.smackcheck2.location.CommonLocationState
import com.example.smackcheck2.location.SharedLocationState
import com.example.smackcheck2.location.requestCurrentLocationDetection

/**
 * Navigation state holder for managing current screen with Compose state
 */
class NavigationState {
    var currentScreen by mutableStateOf<Screen>(Screen.Splash)
        private set

    private val backStack = mutableListOf<Screen>()

    /**
     * Check if back navigation is possible
     */
    val canGoBack: Boolean
        get() = backStack.isNotEmpty()
    
    // Route arguments with observable state
    var imageUri by mutableStateOf("")
        private set
    
    var dishName by mutableStateOf("")
        private set
    
    var restaurantId by mutableStateOf("")
        private set
    
    var dishId by mutableStateOf("")
        private set

    var userId by mutableStateOf("")
        private set

    var ratingId by mutableStateOf("")
        private set

    var imageBytes by mutableStateOf<ByteArray?>(null)
        private set

    var allCapturedImages by mutableStateOf<List<CapturedImage>>(emptyList())
        private set

    var dishCuisine by mutableStateOf<String?>(null)
        private set

    var dishConfidence by mutableStateOf(0f)
        private set

    var detectedChain by mutableStateOf<String?>(null)
        private set

    var detectedType by mutableStateOf<String?>(null)
        private set

    fun updateDishMeta(cuisine: String?, confidence: Float, chain: String? = null, type: String? = null) {
        dishCuisine = cuisine
        dishConfidence = confidence
        detectedChain = chain
        detectedType = type
    }

    fun navigateTo(screen: Screen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }
    
    fun navigateToWithArgs(screen: Screen, vararg args: Pair<String, String>) {
        backStack.add(currentScreen)
        args.forEach { (key, value) ->
            when (key) {
                "imageUri" -> imageUri = value
                "dishName" -> dishName = value
                "restaurantId" -> restaurantId = value
                "dishId" -> dishId = value
                "userId" -> userId = value
                "ratingId" -> ratingId = value
            }
        }
        currentScreen = screen
    }
    
    fun navigateBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeLast()
            true
        } else {
            false
        }
    }
    
    fun popToRoot() {
        backStack.clear()
        currentScreen = Screen.DarkHome
    }

    /**
     * Switch to a primary tab (home, map, feed, profile) without stacking duplicate destinations.
     */
    fun navigateToMainTab(screen: Screen) {
        backStack.clear()
        currentScreen = screen
    }

    fun updateImageBytes(bytes: ByteArray?) {
        imageBytes = bytes
    }

    fun updateAllCapturedImages(images: List<CapturedImage>) {
        allCapturedImages = images
    }

    fun clearCaptureData() {
        imageUri = ""
        dishName = ""
        imageBytes = null
        allCapturedImages = emptyList()
        dishCuisine = null
        dishConfidence = 0f
        detectedChain = null
        detectedType = null
    }
}

/**
 * Main Navigation Host composable
 * Manages navigation between all screens
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SmackCheckNavHost(preferencesRepository: PreferencesRepository) {
    val navigationState = remember { NavigationState() }
    val authViewModel: AuthViewModel = viewModel { AuthViewModel() }
    val authState by authViewModel.authState.collectAsState()

    // Shared LocationHomeViewModel across all screens
    val locationHomeViewModel: LocationHomeViewModel = viewModel { LocationHomeViewModel() }
    val locationService = LocalLocationService.current
    val placesService = LocalPlacesService.current

    // Set location and places service once at the top level
    androidx.compose.runtime.LaunchedEffect(locationService, placesService) {
        locationHomeViewModel.setLocationService(locationService)
        locationHomeViewModel.setPlacesService(placesService)
    }

    // Shared RestaurantPhotoViewModel for Google Places photos (from main)
    val restaurantPhotoViewModel: RestaurantPhotoViewModel = viewModel { RestaurantPhotoViewModel() }

    // Shared GamificationViewModel (from main)
    val gamificationViewModel: GamificationViewModel = viewModel { GamificationViewModel() }

    // Shared NotificationViewModel for push notifications (from main)
    val notificationViewModel: NotificationViewModel = viewModel { NotificationViewModel() }

    // Shared SocialFeedViewModel — created early so feed pre-loads on auth
    val socialFeedViewModel: SocialFeedViewModel = viewModel { SocialFeedViewModel(preferencesRepository) }

    // Points-earned popup state (from main)
    var pointsEvent by remember { mutableStateOf<PointsEarnedEvent?>(null) }
    LaunchedEffect(Unit) {
        gamificationViewModel.pointsEarned.collect { event ->
            pointsEvent = event
        }
    }

    // Observe SharedLocationState and auto-update the LocationHomeViewModel (from main)
    // Skip if user has manually selected a location
    val locationState by SharedLocationState.locationState.collectAsState()
    val locationUiState by locationHomeViewModel.uiState.collectAsState()
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
            else -> { /* Loading, Idle, Permission, Disabled, Error - handled by individual screens */ }
        }
    }

    // Handle system back button press
    // Always enable back handler to prevent app from closing unexpectedly
    BackHandler(enabled = true) {
        when {
            // If there's a back stack, navigate back
            navigationState.canGoBack -> {
                navigationState.navigateBack()
            }
            // On home screen, do nothing (stay on home screen instead of exiting)
            navigationState.currentScreen is Screen.DarkHome -> {
                // Consume the back press - don't exit the app
                println("NavHost: On home screen, back press ignored to prevent app exit")
            }
            // On other screens (Login, Profile, etc.), navigate back if possible
            else -> {
                // Attempt to go back, if not possible, stay on current screen
                if (!navigationState.navigateBack()) {
                    println("NavHost: Cannot navigate back, staying on current screen")
                }
            }
        }
    }

    val isAuthenticated = when (authState) {
        is AuthState.Authenticated -> true
        is AuthState.Unauthenticated -> false
        is AuthState.Unknown -> null
    }

    // Initialize push notifications and pre-load feed when authenticated
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) {
            notificationViewModel.initializePushNotifications()
            notificationViewModel.refresh()
            socialFeedViewModel.loadFeed()

            // Day 1 retention tracking
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
    
    // Route pending push-notification taps to the relevant screen once the
    // user is authenticated. The Android FCM service (and iOS equivalent)
    // publishes targets to NotificationDeepLink; we consume each target
    // after navigating so the same tap isn't replayed on recomposition.
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

    // Track if we've already requested location permission this session
    var hasRequestedPermission by remember { mutableStateOf(false) }
    var shouldShowPermissionDialog by remember { mutableStateOf(false) }

    // Automatically request location permission after authentication, but only on first launch
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

    // Main content with points popup overlay (from main)
    Box(modifier = Modifier.fillMaxSize()) {
    when (navigationState.currentScreen) {
        is Screen.Splash -> {
            DarkSplashScreen(
                onNavigateToLogin = { navigationState.navigateTo(Screen.Login) },
                onNavigateToHome = {
                    val user = authViewModel.getCurrentUser()
                    if (user != null && user.username.isBlank()) {
                        navigationState.navigateTo(Screen.ProfileSetup)
                    } else {
                        navigationState.navigateTo(Screen.DarkHome)
                    }
                },
                isAuthenticated = isAuthenticated
            )
        }

        is Screen.Login -> {
            val loginViewModel: LoginViewModel = viewModel { LoginViewModel() }
            DarkLoginScreen(
                viewModel = loginViewModel,
                authViewModel = authViewModel,
                onNavigateToRegister = { navigationState.navigateTo(Screen.Register) },
                onNavigateToHome = {
                    val user = authViewModel.getCurrentUser()
                    if (user != null && user.username.isBlank()) {
                        navigationState.navigateTo(Screen.ProfileSetup)
                    } else {
                        navigationState.navigateTo(Screen.DarkHome)
                    }
                }
            )
        }

        is Screen.Register -> {
            val registerViewModel: RegisterViewModel = viewModel { RegisterViewModel() }
            RegisterScreen(
                viewModel = registerViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToHome = { navigationState.navigateTo(Screen.ProfileSetup) }
            )
        }
        
        is Screen.ProfileSetup -> {
            val profileSetupViewModel: ProfileSetupViewModel = viewModel { ProfileSetupViewModel() }
            ProfileSetupScreen(
                viewModel = profileSetupViewModel,
                currentUser = authViewModel.getCurrentUser(),
                onComplete = { navigationState.navigateTo(Screen.DarkHome) }
            )
        }

        is Screen.Home -> {
            LocationHomeScreen(
                viewModel = locationHomeViewModel,
                photoViewModel = restaurantPhotoViewModel,
                onNavigateToAddDish = { navigationState.navigateTo(Screen.DishCapture) },
                onNavigateToProfile = { navigationState.navigateTo(Screen.Profile) },
                onNavigateToSearch = { navigationState.navigateTo(Screen.Search) },
                onNavigateToLocationSelection = { navigationState.navigateTo(Screen.LocationSelection) },
                onNavigateToGame = { navigationState.navigateTo(Screen.Game) },
                onNavigateToRestaurant = { restaurantId ->
                    navigationState.navigateToWithArgs(
                        Screen.RestaurantDetail,
                        "restaurantId" to restaurantId
                    )
                },
                onNavigateToAllRestaurants = { navigationState.navigateTo(Screen.AllRestaurants) },
                onNavigateToTopDishes = { navigationState.navigateTo(Screen.TopDishes) },
                onNavigateToTopRestaurants = { navigationState.navigateTo(Screen.TopRestaurants) }
            )
        }
        
        is Screen.Profile -> {
            val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(authViewModel) }

            // Refresh profile data every time screen is shown (to show updated XP)
            LaunchedEffect(Unit) {
                profileViewModel.refresh()
            }

            DarkProfileScreen(
                viewModel = profileViewModel,
                preferencesRepository = preferencesRepository,
                onNavigateBack = { navigationState.navigateBack() },
                onEditProfile = { navigationState.navigateTo(Screen.EditProfile) },
                onSignOut = {
                    authViewModel.signOut()
                    navigationState.navigateTo(Screen.Login)
                },
                onNavigateToGames = { navigationState.navigateTo(Screen.Game) },
                onNavigateToNotifications = { navigationState.navigateTo(Screen.NotificationSettings) },
                onNavigateToAccount = { navigationState.navigateTo(Screen.AccountSettings) },
                onNavigateToPrivacy = { navigationState.navigateTo(Screen.PrivacySettings) },
                onNavigateToProgress = { navigationState.navigateTo(Screen.UserProgress) },
                onNavigateToHelpFaq = { navigationState.navigateTo(Screen.HelpFaq) },
                onNavigateToContactSupport = { navigationState.navigateTo(Screen.ContactSupport) },
                onNavHome = { navigationState.navigateToMainTab(Screen.DarkHome) },
                onNavMap = { navigationState.navigateToMainTab(Screen.SocialMap) },
                onNavCamera = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onNavExplore = { navigationState.navigateToMainTab(Screen.SocialFeed) },
                onNavProfile = { navigationState.navigateToMainTab(Screen.Profile) }
            )
        }

        // Edit Profile Screen
        is Screen.EditProfile -> {
            val currentUser = when (val state = authState) {
                is AuthState.Authenticated -> state.user
                else -> null
            }
            val editProfileViewModel: EditProfileViewModel = viewModel {
                EditProfileViewModel(initialUser = currentUser)
            }

            EditProfileScreen(
                viewModel = editProfileViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onNavHome = { navigationState.navigateToMainTab(Screen.DarkHome) },
                onNavMap = { navigationState.navigateToMainTab(Screen.SocialMap) },
                onNavCamera = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onNavExplore = { navigationState.navigateToMainTab(Screen.SocialFeed) },
                onNavProfile = { navigationState.navigateToMainTab(Screen.Profile) }
            )
        }

        // Notification Settings Screen
        is Screen.NotificationSettings -> {
            val notificationSettingsViewModel: NotificationSettingsViewModel = viewModel {
                NotificationSettingsViewModel(preferencesRepository)
            }

            NotificationSettingsScreen(
                viewModel = notificationSettingsViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        // Account Settings Screen
        is Screen.AccountSettings -> {
            val accountSettingsViewModel: AccountSettingsViewModel = viewModel {
                AccountSettingsViewModel()
            }

            AccountSettingsScreen(
                viewModel = accountSettingsViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onAccountDeleted = {
                    navigationState.navigateTo(Screen.Login)
                }
            )
        }

        // Privacy Settings Screen
        is Screen.PrivacySettings -> {
            val privacySettingsViewModel: PrivacySettingsViewModel = viewModel {
                PrivacySettingsViewModel(preferencesRepository)
            }

            PrivacySettingsScreen(
                viewModel = privacySettingsViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        is Screen.HelpFaq -> {
            HelpFaqScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToContactSupport = { navigationState.navigateTo(Screen.ContactSupport) }
            )
        }

        is Screen.ContactSupport -> {
            val currentUser = when (val state = authState) {
                is AuthState.Authenticated -> state.user
                else -> null
            }

            ContactSupportScreen(
                initialEmail = currentUser?.email ?: "",
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToHelpFaq = { navigationState.navigateTo(Screen.HelpFaq) }
            )
        }

        is Screen.DishCapture -> {
            val imagePicker = LocalImagePicker.current
            DishCaptureScreen(
                imagePicker = imagePicker,
                onNavigateBack = { navigationState.navigateBack() },
                onImageCaptured = { imageUri, imageBytes ->
                    navigationState.updateImageBytes(imageBytes)
                    navigationState.navigateToWithArgs(
                        Screen.DishPreview,
                        "imageUri" to imageUri
                    )
                }
            )
        }
        
        is Screen.DishPreview -> {
            DishPreviewScreen(
                imageUri = navigationState.imageUri,
                imageBytes = navigationState.imageBytes,
                onNavigateBack = { navigationState.navigateBack() },
                onConfirm = { dishName ->
                    navigationState.navigateToWithArgs(
                        Screen.DishRating,
                        "dishName" to dishName,
                        "imageUri" to navigationState.imageUri
                    )
                },
                onRetake = { navigationState.navigateBack() }
            )
        }
        
        is Screen.DishRating -> {
            val dishRatingViewModel: DishRatingViewModel = viewModel { DishRatingViewModel() }
            val databaseRepository = remember { DatabaseRepository() }
            var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }

            // Pass image bytes captured in preview screen into the view model for upload
            LaunchedEffect(navigationState.imageUri) {
                navigationState.imageBytes?.let { bytes ->
                    dishRatingViewModel.setImageBytes(bytes)
                }
            }

            // Capture GPS location for the rating
            LaunchedEffect(Unit) {
                val location = locationService?.getCurrentLocation()
                dishRatingViewModel.setRatingLocation(location?.latitude, location?.longitude)
            }

            // Load restaurants when screen opens
            LaunchedEffect(Unit) {
                val result = databaseRepository.getRestaurants()
                result.onSuccess { restaurants ->
                    allRestaurants = restaurants
                }
            }

            DishRatingScreen(
                viewModel = dishRatingViewModel,
                dishName = navigationState.dishName,
                imageUri = navigationState.imageUri,
                restaurants = allRestaurants,
                onNavigateBack = { navigationState.navigateBack() },
                onSubmitSuccess = { ratingId ->
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_RATE_DISH,
                        actionLabel = "Dish Rated"
                    )
                    dishRatingViewModel.resetForm()
                    navigationState.clearCaptureData()
                    navigationState.popToRoot()
                    navigationState.navigateToWithArgs(
                        Screen.SocialFeed,
                        "ratingId" to ratingId
                    )
                }
            )
        }
        
        is Screen.ManualRestaurantEntry -> {
            val manualRestaurantViewModel: ManualRestaurantViewModel = viewModel { ManualRestaurantViewModel() }
            ManualRestaurantEntryScreen(
                viewModel = manualRestaurantViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onSaveSuccess = {
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_ADD_RESTAURANT,
                        actionLabel = "Restaurant Added"
                    )
                    navigationState.navigateBack()
                }
            )
        }
        
        is Screen.SocialFeed -> {
            val socialFeedState by socialFeedViewModel.uiState.collectAsState()
            val shareService = LocalShareService.current
            val socialFeedUserPhoto = when (val state = authState) {
                is AuthState.Authenticated -> state.user.profilePhotoUrl
                else -> null
            }

            // Set scroll target if navigated from rating submission
            LaunchedEffect(Unit) {
                val scrollToId = navigationState.ratingId
                if (scrollToId.isNotBlank()) {
                    socialFeedViewModel.setScrollToRatingId(scrollToId)
                }
                // loadFeed() already called on auth — no need to call again here
            }

            SocialFeedScreen(
                uiState = socialFeedState,
                onNavigateBack = { navigationState.navigateBack() },
                currentUserAvatarUrl = socialFeedUserPhoto,
                onFilterSelected = { filter -> socialFeedViewModel.setFilter(filter) },
                onLikeClick = { ratingId -> socialFeedViewModel.toggleLike(ratingId) },
                onCommentClick = { ratingId ->
                    navigationState.navigateToWithArgs(
                        Screen.Comments,
                        "ratingId" to ratingId
                    )
                },
                onShareClick = { item ->
                    shareService?.shareText(
                        text = "Check out ${item.dishName} at ${item.restaurantName} - rated ${item.rating}/5 on SmackCheck!",
                        title = "Share Dish Rating"
                    )
                },
                onUserClick = { userId ->
                    navigationState.navigateToWithArgs(
                        Screen.UserProfile,
                        "userId" to userId
                    )
                },
                onRefresh = { socialFeedViewModel.refresh() },
                onLoadMore = { socialFeedViewModel.loadMoreFeed() },
                onScrollComplete = { socialFeedViewModel.clearScrollTarget() },
                onExploreClick = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onFindFriendsClick = { navigationState.navigateTo(Screen.DiscoverUsers) },
                onBookmarkClick = { ratingId -> socialFeedViewModel.toggleBookmark(ratingId) },
                onMapBannerClick = { navigationState.navigateToMainTab(Screen.SocialMap) },
                onAvatarClick = { navigationState.navigateToMainTab(Screen.Profile) },
                onStoryClick = { userId ->
                    navigationState.navigateToWithArgs(
                        Screen.UserProfile,
                        "userId" to userId
                    )
                },
                onAddStoryClick = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onTopDishClick = { /* TODO: navigate to dish detail */ },
                onSeeAllTopDishes = { /* TODO: navigate to top dishes list */ },
                onHomeClick = { navigationState.navigateToMainTab(Screen.DarkHome) },
                onMapClick = { navigationState.navigateToMainTab(Screen.SocialMap) },
                onCameraClick = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onNavExploreClick = { navigationState.navigateToMainTab(Screen.SocialFeed) },
                onProfileClick = { navigationState.navigateToMainTab(Screen.Profile) },
                onNotificationsClick = { navigationState.navigateTo(Screen.NotificationsList) }
            )
        }

        is Screen.SocialMap -> {
            val socialMapViewModel: SocialMapViewModel = viewModel {
                SocialMapViewModel(locationService)
            }
            
            SocialMapScreen(
                viewModel = socialMapViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onUserProfileClick = { userId ->
                    navigationState.navigateToWithArgs(
                        Screen.UserProfile,
                        "userId" to userId
                    )
                },
                onDishDetailClick = { ratingId ->
                    navigationState.navigateToWithArgs(
                        Screen.DishDetail,
                        "dishId" to ratingId
                    )
                },
                onRateDishClick = { navigationState.navigateTo(Screen.DishCapture) },
                onHomeClick = { navigationState.navigateToMainTab(Screen.DarkHome) },
                onMapClick = { navigationState.navigateToMainTab(Screen.SocialMap) },
                onCameraClick = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onExploreClick = { navigationState.navigateToMainTab(Screen.SocialFeed) },
                onProfileClick = { navigationState.navigateToMainTab(Screen.Profile) }
            )
        }
        
        is Screen.Search -> {
            val searchViewModel: SearchViewModel = viewModel {
                SearchViewModel(locationService, placesService)
            }
            // Pass current location to search for location-biased results
            LaunchedEffect(locationUiState.currentLatitude, locationUiState.currentLongitude) {
                val lat = locationUiState.currentLatitude
                val lng = locationUiState.currentLongitude
                if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                    searchViewModel.setUserLocation(lat, lng)
                }
            }
            DarkSearchScreen(
                viewModel = searchViewModel,
                photoViewModel = restaurantPhotoViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onRestaurantClick = { restaurantId ->
                    navigationState.navigateToWithArgs(
                        Screen.RestaurantDetail,
                        "restaurantId" to restaurantId
                    )
                },
                onAddRestaurantClick = { navigationState.navigateTo(Screen.ManualRestaurantEntry) }
            )
        }
        
        is Screen.RestaurantDetail -> {
            val placesService = LocalPlacesService.current
            val restaurantDetailViewModel: RestaurantDetailViewModel = viewModel(key = navigationState.restaurantId) {
                RestaurantDetailViewModel(placesService = placesService)
            }
            val currentUser = authViewModel.getCurrentUser()
            RestaurantDetailScreen(
                viewModel = restaurantDetailViewModel,
                photoViewModel = restaurantPhotoViewModel,
                preferencesRepository = preferencesRepository,
                restaurantId = navigationState.restaurantId,
                userAvatarUrl = currentUser?.profilePhotoUrl,
                onNavigateBack = { navigationState.navigateBack() },
                onNotificationClick = { navigationState.navigateTo(Screen.Notifications) },
                onNavItemClick = { index ->
                    when (index) {
                        0 -> navigationState.navigateToMainTab(Screen.DarkHome)
                        1 -> navigationState.navigateToMainTab(Screen.SocialMap)
                        2 -> navigationState.navigateTo(Screen.DarkDishCapture)
                        3 -> navigationState.navigateToMainTab(Screen.SocialFeed)
                        4 -> navigationState.navigateToMainTab(Screen.Profile)
                    }
                },
                onNavHome = { navigationState.navigateToMainTab(Screen.DarkHome) },
                onNavMap = { navigationState.navigateToMainTab(Screen.SocialMap) },
                onNavCamera = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onNavExplore = { navigationState.navigateToMainTab(Screen.SocialFeed) },
                onNavProfile = { navigationState.navigateToMainTab(Screen.Profile) },
                currencySymbol = com.example.smackcheck2.util.CurrencyHelper.forCountry(locationUiState.countryCode).symbol
            )
        }

        is Screen.LocationPermission -> {
            LocationPermissionScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onPermissionGranted = { navigationState.navigateBack() },
                onPermissionDenied = { navigationState.navigateBack() }
            )
        }
        
        is Screen.UserProgress -> {
            val userProgressViewModel: UserProgressViewModel = viewModel { UserProgressViewModel() }
            val progressGamificationViewModel: GamificationViewModel = viewModel(key = "progress_gamification") { GamificationViewModel() }
            ProgressDashboardScreen(
                progressViewModel = userProgressViewModel,
                gamificationViewModel = progressGamificationViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onViewAllAchievements = { navigationState.navigateTo(Screen.Badges) },
                onNavHome = { navigationState.navigateToMainTab(Screen.DarkHome) },
                onNavMap = { navigationState.navigateToMainTab(Screen.SocialMap) },
                onNavCamera = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onNavExplore = { navigationState.navigateToMainTab(Screen.SocialFeed) },
                onNavProfile = { navigationState.navigateToMainTab(Screen.Profile) }
            )
        }
        
        is Screen.Badges -> {
            val userProgressViewModel: UserProgressViewModel = viewModel { UserProgressViewModel() }
            val badgesGamificationViewModel: GamificationViewModel = viewModel(key = "badges_gamification") { GamificationViewModel() }
            AchievementsListScreen(
                progressViewModel = userProgressViewModel,
                gamificationViewModel = badgesGamificationViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }
        
        is Screen.LocationSelection -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()

            RequestLocationPermission(
                onPermissionResult = { granted ->
                    println("NavHost: LocationSelection - permission result: $granted")
                    if (granted) {
                        // Permission granted, now get the location
                        locationHomeViewModel.useCurrentLocation()
                    }
                }
            ) { requestPermission ->
                LocationSelectionScreen(
                    currentLocation = uiState.selectedLocation,
                    isDetectingLocation = uiState.isDetectingLocation,
                    locationError = uiState.locationError,
                    searchResults = uiState.searchResults,
                    onNavigateBack = { 
                        println("NavHost: LocationSelection - navigating back")
                        navigationState.navigateBack() 
                    },
                    onLocationSelected = { location, lat, lng ->
                        println("NavHost: LocationSelection - location selected: $location ($lat, $lng)")
                        if (lat != 0.0 && lng != 0.0) {
                            locationHomeViewModel.selectLocationWithCoordinates(location, lat, lng, isManual = true)
                        } else {
                            locationHomeViewModel.selectLocation(location)
                        }
                        navigationState.navigateBack()
                    },
                    onUseCurrentLocation = {
                        println("NavHost: LocationSelection - use current location clicked, requesting permission")
                        // First request permission - if already granted, callback fires immediately
                        // If not granted, popup shows and callback fires after user responds
                        requestPermission()
                    },
                    onSearchLocation = { query ->
                        println("NavHost: LocationSelection - searching: $query")
                        locationHomeViewModel.searchLocations(query)
                    },
                    onClearError = {
                        locationHomeViewModel.clearLocationError()
                    }
                )
            }
        }
        
        is Screen.AllRestaurants -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()

            // Convert nearby restaurants to Restaurant format and combine with database restaurants
            val nearbyAsRestaurants = uiState.nearbyRestaurants.map { nearby ->
                Restaurant(
                    id = nearby.id,
                    name = nearby.name,
                    city = uiState.selectedLocation ?: "Unknown",
                    cuisine = "Restaurant", // Default cuisine for nearby restaurants
                    imageUrls = emptyList(),
                    averageRating = nearby.rating?.toFloat() ?: 0f,
                    reviewCount = nearby.userRatingsTotal ?: 0,
                    latitude = nearby.latitude,
                    longitude = nearby.longitude,
                    googlePlaceId = nearby.id,  // nearby.id is the Google Place ID
                    photoUrl = nearby.photoUrl
                )
            }

            // Combine database restaurants and nearby restaurants
            val combinedRestaurants = (uiState.allRestaurants + nearbyAsRestaurants).distinctBy { it.id }

            AllRestaurantsScreen(
                location = uiState.selectedLocation ?: "Unknown",
                restaurants = combinedRestaurants,
                photoViewModel = restaurantPhotoViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onRestaurantClick = { restaurantId ->
                    navigationState.navigateToWithArgs(
                        Screen.RestaurantDetail,
                        "restaurantId" to restaurantId
                    )
                }
            )
        }
        
        is Screen.TopDishes -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()
            DarkTopDishesScreen(
                location = uiState.selectedLocation ?: "Nearby",
                dishes = uiState.topDishes,
                onNavigateBack = { navigationState.navigateBack() },
                onDishClick = { dishId ->
                    navigationState.navigateToWithArgs(
                        Screen.DishDetail,
                        "dishId" to dishId
                    )
                }
            )
        }
        
        is Screen.TopRestaurants -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()
            DarkTopRestaurantsScreen(
                location = uiState.selectedLocation ?: "Nearby",
                restaurants = uiState.topRestaurants,
                photoViewModel = restaurantPhotoViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onRestaurantClick = { restaurantId ->
                    navigationState.navigateToWithArgs(
                        Screen.RestaurantDetail,
                        "restaurantId" to restaurantId
                    )
                }
            )
        }
        
        is Screen.Game -> {
            val gameViewModel: GamificationViewModel = viewModel { GamificationViewModel() }

            // Refresh game data every time screen is shown (to show updated XP and leaderboard)
            LaunchedEffect(Unit) {
                gameViewModel.loadAll()
            }

            DarkGameScreen(
                viewModel = gameViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        is Screen.NearbyRestaurants -> {
            val placesService = LocalPlacesService.current
            val geofencingService = LocalGeofencingService.current
            val nearbyRestaurantsViewModel: NearbyRestaurantsViewModel = viewModel {
                NearbyRestaurantsViewModel(locationService, placesService, geofencingService)
            }
            NearbyRestaurantsScreen(
                viewModel = nearbyRestaurantsViewModel,
                photoViewModel = restaurantPhotoViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onRestaurantClick = { restaurant ->
                    // Navigate to restaurant detail or handle restaurant click
                    navigationState.navigateToWithArgs(
                        Screen.RestaurantDetail,
                        "restaurantId" to restaurant.id
                    )
                }
            )
        }

        is Screen.DarkHome -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()
            
            // Track if we've already requested permission on this screen visit
            var hasRequestedPermission by remember { mutableStateOf(false) }
            
            // Convert nearby restaurants to Restaurant format
            // Use SmackCheck ratings (0 for new restaurants), NOT Google ratings
            val nearbyAsRestaurants = uiState.nearbyRestaurants.map { nearby ->
                Restaurant(
                    id = nearby.id,
                    name = nearby.name,
                    city = uiState.selectedLocation ?: "Unknown",
                    cuisine = "Restaurant",
                    imageUrls = emptyList(),
                    averageRating = 0f,  // SmackCheck rating - 0 until users rate it
                    reviewCount = 0,      // SmackCheck reviews - 0 until users review it
                    latitude = nearby.latitude,
                    longitude = nearby.longitude,
                    googlePlaceId = nearby.id,  // nearby.id is the Google Place ID
                    photoUrl = nearby.photoUrl  // Pass the Google Places photo URL
                )
            }

            // Combine database and nearby restaurants
            val combinedRestaurants = (uiState.allRestaurants + nearbyAsRestaurants).distinctBy { it.id }

            // Refresh from live data sources whenever Home screen is entered.
            LaunchedEffect(Unit) {
                locationHomeViewModel.refreshHomeData()
                locationHomeViewModel.refreshUserLocation()
                socialFeedViewModel.refreshHomeData()
            }

            RequestLocationPermission(
                onPermissionResult = { granted ->
                    println("NavHost: DarkHome - permission result: $granted")
                    if (granted) {
                        // Permission granted, auto-detect location
                        locationHomeViewModel.useCurrentLocation()
                    }
                }
            ) { requestPermission ->
                // Auto-request permission when screen first loads (skip if user manually selected a location)
                LaunchedEffect(Unit) {
                    val manuallySelected = locationHomeViewModel.uiState.value.isManuallySelected
                    if (!hasRequestedPermission && !manuallySelected) {
                        hasRequestedPermission = true
                        println("NavHost: DarkHome - auto-requesting location permission")
                        // Small delay to let the UI settle before showing permission dialog
                        kotlinx.coroutines.delay(500)
                        requestPermission()
                    }
                }

                val currentUserName = when (val state = authState) {
                    is AuthState.Authenticated -> state.user.name
                    else -> ""
                }
                val currentUserPhoto = when (val state = authState) {
                    is AuthState.Authenticated -> state.user.profilePhotoUrl
                    else -> null
                }
                val followingUsersState by socialFeedViewModel.uiState.collectAsState()
                val notifUnreadCount by notificationViewModel.unreadCount.collectAsState()

                DarkHomeScreen(
                    currentLocation = uiState.selectedLocation ?: "Select Location",
                    userName = currentUserName,
                    userProfilePhotoUrl = currentUserPhoto,
                    isLoading = uiState.isLoading,
                    allRestaurants = combinedRestaurants,
                    allDishes = uiState.topDishes,
                    topDishFeedItems = followingUsersState.topDishes,
                    followingUsers = followingUsersState.storyUsers,
                    noRestaurantsFound = uiState.noRestaurantsFound,
                    photoViewModel = restaurantPhotoViewModel,
                    currentLatitude = uiState.userLatitude,
                    currentLongitude = uiState.userLongitude,
                    hasUnreadNotifications = notifUnreadCount > 0,
                    onLocationClick = {
                        println("NavHost: Location clicked, navigating to LocationSelection")
                        navigationState.navigateTo(Screen.LocationSelection)
                    },
                    onDishClick = { dishId ->
                        navigationState.navigateToWithArgs(
                            Screen.DishDetail,
                            "dishId" to dishId
                        )
                    },
                    onFeedItemDishClick = { dishId ->
                        navigationState.navigateToWithArgs(
                            Screen.DishDetail,
                            "dishId" to dishId
                        )
                    },
                    onRestaurantClick = { restaurantId ->
                        navigationState.navigateToWithArgs(
                            Screen.RestaurantDetail,
                            "restaurantId" to restaurantId
                        )
                    },
                    onSearchClick = { navigationState.navigateTo(Screen.Search) },
                    onMapClick = { navigationState.navigateToMainTab(Screen.SocialMap) },
                    onProfileClick = { navigationState.navigateToMainTab(Screen.Profile) },
                    onGameClick = { navigationState.navigateTo(Screen.Game) },
                    onCameraClick = { navigationState.navigateTo(Screen.DarkDishCapture) },
                    onTopDishesClick = { navigationState.navigateTo(Screen.TopDishes) },
                    onTopRestaurantsClick = { navigationState.navigateTo(Screen.TopRestaurants) },
                    onNearbyRestaurantsClick = { navigationState.navigateTo(Screen.NearbyRestaurants) },
                    onChipSelected = { chip ->
                        val keyword = when (chip) {
                            "Italian"  -> "Italian"
                            "Asian"    -> "Asian"
                            "Desserts" -> "Dessert"
                            else       -> null
                        }
                        locationHomeViewModel.fetchNearbyForCuisine(keyword)
                    },
                    onSocialFeedClick = { navigationState.navigateToMainTab(Screen.SocialFeed) },
                    onNotificationsClick = { navigationState.navigateTo(Screen.NotificationsList) },
                    onAddRestaurantClick = { navigationState.navigateTo(Screen.ManualRestaurantEntry) }
                )
            }
        }
        
        is Screen.DarkDishCapture -> {
            val imagePicker = LocalImagePicker.current
            val dishCaptureViewModel: DishCaptureViewModel = viewModel { DishCaptureViewModel() }

            // Reset capture form each time screen is entered
            LaunchedEffect(Unit) {
                dishCaptureViewModel.retake()
            }

            DarkDishCaptureScreen(
                viewModel = dishCaptureViewModel,
                imagePicker = imagePicker,
                onNavigateBack = { navigationState.navigateBack() },
                onImageCaptured = { imageUri, dishName, imageBytes, allImages, cuisine, confidence, chain, type ->
                    // Award points for photo upload (from main)
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_UPLOAD_PHOTO,
                        actionLabel = "Photo Uploaded"
                    )
                    // Store all images and dish meta for confirm screen
                    navigationState.updateImageBytes(imageBytes)
                    navigationState.updateAllCapturedImages(allImages)
                    navigationState.updateDishMeta(cuisine, confidence, chain, type)
                    navigationState.navigateToWithArgs(
                        Screen.DarkDishConfirm,
                        "imageUri" to imageUri,
                        "dishName" to dishName
                    )
                },
                onAddManually = { imageUri ->
                    // AI fallback -> open manual dish entry with the captured photo
                    navigationState.navigateToWithArgs(
                        Screen.ManualDishEntry,
                        "imageUri" to imageUri
                    )
                }
            )
        }

        is Screen.DarkDishConfirm -> {
            DarkDishConfirmScreen(
                dishName = navigationState.dishName,
                imageBytes = navigationState.imageBytes,
                cuisine = navigationState.dishCuisine,
                confidence = navigationState.dishConfidence,
                onNavigateBack = { navigationState.navigateBack() },
                onRateNow = {
                    navigationState.navigateTo(Screen.DarkDishRating)
                }
            )
        }

        // Manual Dish Entry screen (from main)
        is Screen.ManualDishEntry -> {
            val manualDishViewModel: ManualDishEntryViewModel = viewModel { ManualDishEntryViewModel() }
            ManualDishEntryScreen(
                viewModel = manualDishViewModel,
                imageUri = navigationState.imageUri,
                onNavigateBack = { navigationState.navigateBack() },
                onSubmitSuccess = {
                    // Award points for manual dish entry (photo + review)
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_UPLOAD_PHOTO,
                        actionLabel = "Dish Added"
                    )
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_WRITE_REVIEW,
                        actionLabel = "Review Written"
                    )
                    navigationState.popToRoot()
                }
            )
        }
        
        is Screen.DarkDishRating -> {
            val dishRatingViewModel: DishRatingViewModel = viewModel { DishRatingViewModel() }
            val ratingUiState by dishRatingViewModel.uiState.collectAsState()
            val databaseRepository = remember { DatabaseRepository() }
            val locationUiState by locationHomeViewModel.uiState.collectAsState()
            val placesService = LocalPlacesService.current

            // State for restaurants
            var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
            var nearbyRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
            var isLoadingRestaurants by remember { mutableStateOf(true) }
            
            // State for text search
            var searchedRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
            var isSearchingRestaurants by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }

            // Convert Google Places nearby restaurants to Restaurant format
            val placesNearbyAsRestaurants = locationUiState.nearbyRestaurants.map { nearby ->
                Restaurant(
                    id = nearby.id,
                    name = nearby.name,
                    city = locationUiState.selectedLocation ?: "Unknown",
                    cuisine = "Restaurant", // Default cuisine for nearby restaurants
                    imageUrls = emptyList(),
                    averageRating = nearby.rating?.toFloat() ?: 0f,
                    reviewCount = nearby.userRatingsTotal ?: 0,
                    latitude = nearby.latitude,
                    longitude = nearby.longitude,
                    googlePlaceId = nearby.id,
                    photoUrl = nearby.photoUrl
                )
            }

            // Load restaurants when screen opens - both all and nearby
            LaunchedEffect(Unit) {
                isLoadingRestaurants = true
                println("DarkDishRating: Loading restaurants...")

                // Load all restaurants from database
                val allResult = databaseRepository.getRestaurants()
                allResult.onSuccess { restaurants ->
                    allRestaurants = restaurants
                    println("DarkDishRating: Loaded ${restaurants.size} total restaurants from database")
                }.onFailure { error ->
                    println("DarkDishRating: Failed to load restaurants: ${error.message}")
                }

                // Load nearby restaurants based on current selected city from database
                val currentCity = locationUiState.selectedLocation
                println("DarkDishRating: Current location = $currentCity")

                if (!currentCity.isNullOrBlank()) {
                    println("DarkDishRating: Loading nearby restaurants for: $currentCity")
                    val nearbyResult = databaseRepository.getRestaurantsByCity(currentCity)
                    nearbyResult.onSuccess { restaurants ->
                        // Combine database restaurants with Google Places restaurants
                        val combinedNearby = (restaurants + placesNearbyAsRestaurants).distinctBy { it.id }
                        nearbyRestaurants = combinedNearby
                        println("DarkDishRating: Loaded ${restaurants.size} database + ${placesNearbyAsRestaurants.size} Places API = ${combinedNearby.size} total nearby restaurants")
                    }.onFailure { error ->
                        // If database fails, still use Google Places restaurants
                        nearbyRestaurants = placesNearbyAsRestaurants
                        println("DarkDishRating: Database failed (${error.message}), using ${placesNearbyAsRestaurants.size} Places API restaurants")
                    }
                } else {
                    // No location selected - use Google Places restaurants if available
                    nearbyRestaurants = placesNearbyAsRestaurants
                    println("DarkDishRating: No location selected, using ${placesNearbyAsRestaurants.size} Places API restaurants")
                }

                isLoadingRestaurants = false
            }

            // Reload nearby restaurants when location changes or Places API data updates
            LaunchedEffect(locationUiState.selectedLocation, locationUiState.nearbyRestaurants) {
                val currentCity = locationUiState.selectedLocation
                
                // Update placesNearbyAsRestaurants conversion (it's already computed above, but we need to re-combine)
                val updatedPlacesRestaurants = locationUiState.nearbyRestaurants.map { nearby ->
                    Restaurant(
                        id = nearby.id,
                        name = nearby.name,
                        city = currentCity ?: "Unknown",
                        cuisine = "Restaurant",
                        imageUrls = emptyList(),
                        averageRating = nearby.rating?.toFloat() ?: 0f,
                        reviewCount = nearby.userRatingsTotal ?: 0,
                        latitude = nearby.latitude,
                        longitude = nearby.longitude
                    )
                }
                
                if (!currentCity.isNullOrBlank()) {
                    println("DarkDishRating: Location/Places data changed to $currentCity, reloading...")
                    val nearbyResult = databaseRepository.getRestaurantsByCity(currentCity)
                    nearbyResult.onSuccess { restaurants ->
                        val combinedNearby = (restaurants + updatedPlacesRestaurants).distinctBy { it.id }
                        nearbyRestaurants = combinedNearby
                        println("DarkDishRating: Updated to ${combinedNearby.size} total nearby restaurants")
                    }.onFailure {
                        nearbyRestaurants = updatedPlacesRestaurants
                    }
                } else {
                    nearbyRestaurants = updatedPlacesRestaurants
                }
            }
            
            // Search restaurants via Google Places Text Search API
            LaunchedEffect(searchQuery) {
                if (searchQuery.length >= 3 && placesService != null) {
                    isSearchingRestaurants = true
                    // Add small delay for debouncing
                    kotlinx.coroutines.delay(300)
                    
                    val currentCity = locationUiState.selectedLocation
                    val fullQuery = if (!currentCity.isNullOrBlank()) {
                        "$searchQuery $currentCity"
                    } else {
                        searchQuery
                    }
                    
                    println("DarkDishRating: Searching for restaurants: $fullQuery")
                    val searchResults = placesService.searchRestaurantsByText(fullQuery)
                    
                    // Convert NearbyRestaurant to Restaurant
                    searchedRestaurants = searchResults.map { nearby ->
                        Restaurant(
                            id = nearby.id,
                            name = nearby.name,
                            city = currentCity ?: "Unknown",
                            cuisine = "Restaurant",
                            imageUrls = emptyList(),
                            averageRating = nearby.rating?.toFloat() ?: 0f,
                            reviewCount = nearby.userRatingsTotal ?: 0,
                            latitude = nearby.latitude,
                            longitude = nearby.longitude
                        )
                    }
                    println("DarkDishRating: Found ${searchedRestaurants.size} restaurants from text search")
                    isSearchingRestaurants = false
                } else {
                    searchedRestaurants = emptyList()
                    isSearchingRestaurants = false
                }
            }

            // Initialize the ViewModel with data
            LaunchedEffect(navigationState.dishName, navigationState.imageUri) {
                dishRatingViewModel.initialize(
                    dishName = navigationState.dishName,
                    imageUri = navigationState.imageUri
                )
                navigationState.imageBytes?.let { bytes ->
                    dishRatingViewModel.setImageBytes(bytes)
                }
            }

            // Capture GPS location for the rating
            LaunchedEffect(Unit) {
                val location = locationService?.getCurrentLocation()
                dishRatingViewModel.setRatingLocation(location?.latitude, location?.longitude)
            }

            // Auto-navigate to home after showing XP reward (fallback if user doesn't tap Continue)
            LaunchedEffect(ratingUiState.isSuccess) {
                if (ratingUiState.isSuccess) {
                    kotlinx.coroutines.delay(6000)
                    if (ratingUiState.isSuccess) { // still on success screen
                        dishRatingViewModel.resetForm()
                        navigationState.clearCaptureData()
                        navigationState.popToRoot()
                    }
                }
            }

            DarkDishRatingScreen(
                dishName = navigationState.dishName,
                imageUri = navigationState.imageUri,
                imageBytes = ratingUiState.imageBytes,
                restaurants = run {
                    val selectedCity = locationUiState.selectedLocation
                    val cityFiltered = if (!selectedCity.isNullOrBlank()) {
                        allRestaurants.filter { it.city.contains(selectedCity, ignoreCase = true) }
                    } else {
                        allRestaurants
                    }
                    (cityFiltered + placesNearbyAsRestaurants).distinctBy { it.id }
                },
                nearbyRestaurants = nearbyRestaurants,
                searchedRestaurants = searchedRestaurants,
                isLoadingRestaurants = isLoadingRestaurants,
                isSearchingRestaurants = isSearchingRestaurants,
                isSubmitting = ratingUiState.isSubmitting,
                showSuccess = ratingUiState.isSuccess,
                xpEarned = ratingUiState.xpEarned,
                errorMessage = ratingUiState.errorMessage,
                onNavigateBack = { navigationState.navigateBack() },
                onRatingComplete = {
                    dishRatingViewModel.resetForm()
                    navigationState.clearCaptureData()
                    navigationState.popToRoot()
                },
                onSubmitRating = { rating, comment, tags, restaurant ->
                    // Award points for rating a dish (from main)
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_RATE_DISH,
                        actionLabel = "Dish Rated"
                    )
                    dishRatingViewModel.onRatingChange(rating)
                    dishRatingViewModel.onCommentChange(comment)
                    dishRatingViewModel.onTagsChange(tags)
                    if (restaurant != null) {
                        dishRatingViewModel.setRestaurant(restaurant)
                        dishRatingViewModel.submitRating { _ ->
                            // Success is handled by LaunchedEffect above
                        }
                    }
                },
                onPriceChange = { dishRatingViewModel.onPriceChange(it) },
                onDismissError = { dishRatingViewModel.clearError() },
                onAddRestaurantManually = { navigationState.navigateTo(Screen.ManualRestaurantEntry) },
                onSearchRestaurants = { query -> searchQuery = query },
                detectedChain = navigationState.detectedChain,
                detectedType = navigationState.detectedType,
                currencySymbol = com.example.smackcheck2.util.CurrencyHelper.forCountry(locationUiState.countryCode).symbol
            )
        }

        is Screen.DishDetail -> {
            val dishDetailViewModel: DishDetailViewModel = viewModel { DishDetailViewModel() }
            DishDetailScreen(
                viewModel = dishDetailViewModel,
                dishId = navigationState.dishId,
                onBackClick = { navigationState.navigateBack() },
                onRelatedDishClick = { dishId ->
                    navigationState.navigateToWithArgs(
                        Screen.DishDetail,
                        "dishId" to dishId
                    )
                }
            )
        }

        // Push Notifications screen (from main) - different from social NotificationsList
        is Screen.Notifications -> {
            NotificationsScreen(
                viewModel = notificationViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateTo = { screen, params ->
                    when (screen) {
                        "DishDetail" -> {
                            val dishId = params["dishId"] ?: ""
                            if (dishId.isNotEmpty()) {
                                navigationState.navigateToWithArgs(
                                    Screen.DishDetail,
                                    "dishId" to dishId
                                )
                            }
                        }
                        "GameScreen" -> navigationState.navigateTo(Screen.Game)
                        "SocialFeed" -> navigationState.navigateToMainTab(Screen.SocialFeed)
                    }
                },
                onExploreClick = { navigationState.navigateToMainTab(Screen.SocialFeed) }
            )
        }

        is Screen.UserProfile -> {
            val targetUserId = navigationState.userId
            val socialRepository = remember { SocialRepository() }
            val currentUserId = remember { mutableStateOf<String?>(null) }
            val userProfileState = remember { mutableStateOf(com.example.smackcheck2.model.UserProfileUiState()) }
            val shareService = LocalShareService.current

            LaunchedEffect(targetUserId) {
                val me = com.example.smackcheck2.data.SupabaseClientProvider.client.auth.currentUserOrNull()
                currentUserId.value = me?.id
                try {
                    val profile = socialRepository.getUserProfile(targetUserId).getOrThrow()
                    val ratings = socialRepository.getUserRatings(targetUserId).getOrThrow()
                    val following = if (me != null) socialRepository.isFollowing(me.id, targetUserId) else false
                    userProfileState.value = userProfileState.value.copy(
                        user = profile,
                        ratings = ratings,
                        isFollowing = following,
                        isLoading = false
                    )
                } catch (e: Exception) {
                    userProfileState.value = userProfileState.value.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }

            UserProfileScreen(
                uiState = userProfileState.value,
                onNavigateBack = { navigationState.navigateBack() },
                onFollowClick = {
                    userProfileState.value = userProfileState.value.copy(isFollowLoading = true)
                    kotlinx.coroutines.MainScope().launch {
                        try {
                            val me = currentUserId.value ?: return@launch
                            if (userProfileState.value.isFollowing) {
                                socialRepository.unfollowUser(me, targetUserId).getOrThrow()
                            } else {
                                socialRepository.followUser(me, targetUserId).getOrThrow()
                            }
                            userProfileState.value = userProfileState.value.copy(
                                isFollowing = !userProfileState.value.isFollowing,
                                isFollowLoading = false
                            )
                        } catch (_: Exception) {
                            userProfileState.value = userProfileState.value.copy(isFollowLoading = false)
                        }
                    }
                },
                onFollowersClick = {
                    navigationState.navigateToWithArgs(
                        Screen.FollowersList,
                        "userId" to targetUserId
                    )
                },
                onFollowingClick = {
                    navigationState.navigateToWithArgs(
                        Screen.FollowingList,
                        "userId" to targetUserId
                    )
                },
                onLikeClick = { /* TODO */ },
                onCommentClick = { item ->
                    navigationState.navigateToWithArgs(
                        Screen.Comments,
                        "ratingId" to item.id
                    )
                },
                onShareClick = { item ->
                    shareService?.shareText(
                        text = "Check out ${item.dishName} at ${item.restaurantName} - rated ${item.rating}/5 on SmackCheck!",
                        title = "Share Dish Rating"
                    )
                }
            )
        }

        is Screen.DiscoverUsers -> {
            val discoverUsersViewModel: DiscoverUsersViewModel = viewModel { DiscoverUsersViewModel() }
            val discoverState by discoverUsersViewModel.uiState.collectAsState()
            DiscoverUsersScreen(
                uiState = discoverState,
                onNavigateBack = { navigationState.navigateBack() },
                onUserClick = { user ->
                    navigationState.navigateToWithArgs(
                        Screen.UserProfile,
                        "userId" to user.id
                    )
                },
                onFollowToggle = { user -> discoverUsersViewModel.toggleFollow(user) },
                onRetry = { discoverUsersViewModel.load() }
            )
        }

        is Screen.FollowersList -> {
            val targetUserId = navigationState.userId
            val socialRepository = remember { SocialRepository() }
            val currentUserId = remember { mutableStateOf<String?>(null) }
            val followListState = remember { mutableStateOf(com.example.smackcheck2.model.FollowListUiState()) }

            LaunchedEffect(targetUserId) {
                val me = com.example.smackcheck2.data.SupabaseClientProvider.client.auth.currentUserOrNull()
                currentUserId.value = me?.id
                try {
                    val followers = socialRepository.getFollowers(targetUserId).getOrThrow()
                    followListState.value = followListState.value.copy(
                        users = followers,
                        isLoading = false
                    )
                } catch (e: Exception) {
                    followListState.value = followListState.value.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }

            FollowersListScreen(
                title = "Followers",
                uiState = followListState.value,
                onNavigateBack = { navigationState.navigateBack() },
                onUserClick = { userSummary ->
                    navigationState.navigateToWithArgs(
                        Screen.UserProfile,
                        "userId" to userSummary.id
                    )
                },
                onFollowToggle = { userSummary ->
                    kotlinx.coroutines.MainScope().launch {
                        try {
                            val me = currentUserId.value ?: return@launch
                            val following = socialRepository.isFollowing(me, userSummary.id)
                            if (following) socialRepository.unfollowUser(me, userSummary.id).getOrThrow()
                            else socialRepository.followUser(me, userSummary.id).getOrThrow()
                        } catch (_: Exception) {}
                    }
                },
                onExploreClick = { navigationState.navigateToMainTab(Screen.SocialFeed) }
            )
        }

        is Screen.FollowingList -> {
            val targetUserId = navigationState.userId
            val socialRepository = remember { SocialRepository() }
            val currentUserId = remember { mutableStateOf<String?>(null) }
            val followListState = remember { mutableStateOf(com.example.smackcheck2.model.FollowListUiState()) }

            LaunchedEffect(targetUserId) {
                val me = com.example.smackcheck2.data.SupabaseClientProvider.client.auth.currentUserOrNull()
                currentUserId.value = me?.id
                try {
                    val following = socialRepository.getFollowing(targetUserId).getOrThrow()
                    followListState.value = followListState.value.copy(
                        users = following,
                        isLoading = false
                    )
                } catch (e: Exception) {
                    followListState.value = followListState.value.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }

            FollowersListScreen(
                title = "Following",
                uiState = followListState.value,
                onNavigateBack = { navigationState.navigateBack() },
                onUserClick = { userSummary ->
                    navigationState.navigateToWithArgs(
                        Screen.UserProfile,
                        "userId" to userSummary.id
                    )
                },
                onFollowToggle = { userSummary ->
                    kotlinx.coroutines.MainScope().launch {
                        try {
                            val me = currentUserId.value ?: return@launch
                            val following = socialRepository.isFollowing(me, userSummary.id)
                            if (following) socialRepository.unfollowUser(me, userSummary.id).getOrThrow()
                            else socialRepository.followUser(me, userSummary.id).getOrThrow()
                        } catch (_: Exception) {}
                    }
                },
                onExploreClick = { navigationState.navigateToMainTab(Screen.SocialFeed) }
            )
        }

        is Screen.Comments -> {
            val ratingId = navigationState.ratingId
            val commentsViewModel: CommentsViewModel = viewModel(key = "comments_$ratingId") { CommentsViewModel(ratingId) }
            val commentsState by commentsViewModel.uiState.collectAsState()
            val currentUserId = remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                val me = com.example.smackcheck2.data.SupabaseClientProvider.client.auth.currentUserOrNull()
                currentUserId.value = me?.id ?: ""
                commentsViewModel.loadComments()
            }

            CommentsScreen(
                uiState = commentsState,
                currentUserId = currentUserId.value,
                onNavigateBack = { navigationState.navigateBack() },
                onSubmitComment = { content, _ ->
                    commentsViewModel.addComment(content)
                    socialFeedViewModel.incrementCommentCount(ratingId)
                },
                onDeleteComment = { commentId -> commentsViewModel.deleteComment(commentId) },
                onReplyClick = { comment -> commentsViewModel.setReplyingTo(comment) },
                onCancelReply = { commentsViewModel.setReplyingTo(null) }
            )
        }

        is Screen.NotificationsList -> {
            val notificationsViewModel: NotificationsViewModel = viewModel { NotificationsViewModel() }
            val notificationsState by notificationsViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                notificationsViewModel.loadNotifications()
            }

            NotificationsListScreen(
                uiState = notificationsState,
                onNavigateBack = { navigationState.navigateBack() },
                onNotificationClick = { notification ->
                    notificationsViewModel.markAsRead(notification.id)
                },
                onMarkAllRead = { notificationsViewModel.markAllAsRead() }
            )
        }
    }

    // Points earned popup overlay (always on top) - from main
    PointsEarnedPopup(
        event = pointsEvent,
        onDismissed = { pointsEvent = null },
        modifier = Modifier.align(Alignment.TopCenter).zIndex(100f)
    )
    
    // Permissions onboarding screen shown on first login (location + camera + notifications)
    if (shouldShowPermissionDialog && isAuthenticated == true) {
        PermissionsOnboardingScreen(
            onComplete = { locationWasGranted ->
                shouldShowPermissionDialog = false
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    try {
                        preferencesRepository.setPermissionsOnboardingSeen()
                    } catch (_: Exception) { }
                }
                if (locationWasGranted) {
                    // Location granted - trigger automatic location detection
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        try {
                            locationService?.getCurrentLocation()?.let { location ->
                                SharedLocationState.onLocationDetected(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    city = location.cityName ?: "Unknown",
                                    countryCode = location.countryCode
                                )
                                val repository = com.example.smackcheck2.data.repository.SocialMapRepository()
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
    } // end Box
}

// Placeholder screens for profile settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPlaceholderScreen(
    title: String,
    onNavigateBack: () -> Unit
) {
    val colors = appColors()
    Scaffold(
        containerColor = colors.Background,
        topBar = {
            TopAppBar(
                title = { Text(title, color = colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.Background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$title\n\nComing Soon!",
                color = colors.TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfilePlaceholderScreen(
    onNavigateBack: () -> Unit
) {
    val colors = appColors()
    Scaffold(
        containerColor = colors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.Background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Edit Profile\n\nComing Soon!\n\nYou'll be able to:\n• Change your name\n• Update profile photo\n• Edit bio",
                color = colors.TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
        }
    }
}
