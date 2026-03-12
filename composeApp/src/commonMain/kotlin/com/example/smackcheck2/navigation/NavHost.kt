package com.example.smackcheck2.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smackcheck2.location.CommonLocationState
import com.example.smackcheck2.location.SharedLocationState
import com.example.smackcheck2.location.requestCurrentLocationDetection
import com.example.smackcheck2.model.AuthState
import com.example.smackcheck2.ui.screens.AllRestaurantsScreen
import com.example.smackcheck2.ui.screens.BadgesScreen
import com.example.smackcheck2.ui.screens.DarkDishCaptureScreen
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
import com.example.smackcheck2.ui.screens.RegisterScreen
import com.example.smackcheck2.ui.screens.RestaurantDetailScreen
import com.example.smackcheck2.ui.screens.SearchScreen
import com.example.smackcheck2.ui.screens.SocialFeedScreen
import com.example.smackcheck2.ui.screens.SplashScreen
import com.example.smackcheck2.ui.screens.TopDishesScreen
import com.example.smackcheck2.ui.screens.TopRestaurantsScreen
import com.example.smackcheck2.ui.screens.UserProgressScreen
import com.example.smackcheck2.gamification.GamificationViewModel
import com.example.smackcheck2.gamification.PointsEarnedEvent
import com.example.smackcheck2.ui.components.PointsEarnedPopup
import com.example.smackcheck2.viewmodel.AuthViewModel
import com.example.smackcheck2.viewmodel.DishRatingViewModel
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
import com.example.smackcheck2.notifications.NotificationViewModel
import com.example.smackcheck2.ui.screens.NotificationsScreen

/**
 * Navigation state holder for managing current screen with Compose state
 */
class NavigationState {
    var currentScreen by mutableStateOf<Screen>(Screen.Splash)
        private set
    
    private val backStack = mutableListOf<Screen>()
    
    // Route arguments with observable state
    var imageUri by mutableStateOf("")
        private set
    
    var dishName by mutableStateOf("")
        private set
    
    var restaurantId by mutableStateOf("")
        private set
    
    var dishId by mutableStateOf("")
        private set
    
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
}

/**
 * Main Navigation Host composable
 * Manages navigation between all screens
 */
@Composable
fun SmackCheckNavHost() {
    val navigationState = remember { NavigationState() }
    val authViewModel: AuthViewModel = viewModel { AuthViewModel() }
    val authState by authViewModel.authState.collectAsState()
    
    // ── Shared LocationHomeViewModel (single instance for the whole app) ──
    val locationHomeViewModel: LocationHomeViewModel = viewModel { LocationHomeViewModel() }

    // ── Shared RestaurantPhotoViewModel for Google Places photos ──
    val restaurantPhotoViewModel: RestaurantPhotoViewModel = viewModel { RestaurantPhotoViewModel() }

    // ── Shared GamificationViewModel (single instance for the whole app) ──
    val gamificationViewModel: GamificationViewModel = viewModel { GamificationViewModel() }

    // ── Shared NotificationViewModel (single instance for the whole app) ──
    val notificationViewModel: NotificationViewModel = viewModel { NotificationViewModel() }

    // ── Points-earned popup state ──
    var pointsEvent by remember { mutableStateOf<PointsEarnedEvent?>(null) }
    LaunchedEffect(Unit) {
        gamificationViewModel.pointsEarned.collect { event ->
            pointsEvent = event
        }
    }
    
    // ── Observe SharedLocationState and auto-update the LocationHomeViewModel ──
    val locationState by SharedLocationState.locationState.collectAsState()
    LaunchedEffect(locationState) {
        when (val state = locationState) {
            is CommonLocationState.Success -> {
                val data = state.data
                val city = data.city ?: "Unknown Location"
                locationHomeViewModel.selectLocationWithCoordinates(
                    city = city,
                    latitude = data.latitude,
                    longitude = data.longitude
                )
            }
            else -> { /* Loading, Idle, Permission, Disabled, Error – handled by individual screens */ }
        }
    }
    
    val isAuthenticated = when (authState) {
        is AuthState.Authenticated -> true
        is AuthState.Unauthenticated -> false
        is AuthState.Unknown -> null
    }

    // ── Initialize push notifications when authenticated ──
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) {
            notificationViewModel.initializePushNotifications()
            notificationViewModel.refresh()
        }
    }
    
    // ── Main content with points popup overlay ──
    Box(modifier = Modifier.fillMaxSize()) {
    when (navigationState.currentScreen) {
        is Screen.Splash -> {
            DarkSplashScreen(
                onNavigateToLogin = { navigationState.navigateTo(Screen.Login) },
                onNavigateToHome = { navigationState.navigateTo(Screen.DarkHome) },
                isAuthenticated = isAuthenticated
            )
        }
        
        is Screen.Login -> {
            val loginViewModel: LoginViewModel = viewModel { LoginViewModel() }
            DarkLoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = { navigationState.navigateTo(Screen.Register) },
                onNavigateToHome = { navigationState.navigateTo(Screen.DarkHome) }
            )
        }
        
        is Screen.Register -> {
            val registerViewModel: RegisterViewModel = viewModel { RegisterViewModel() }
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToHome = { navigationState.navigateTo(Screen.DarkHome) }
            )
        }
        
        is Screen.Home -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()
            LocationHomeScreen(
                viewModel = locationHomeViewModel,
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
            val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel() }
            DarkProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onEditProfile = { /* Navigate to edit profile */ },
                onSignOut = {
                    authViewModel.signOut()
                    navigationState.navigateTo(Screen.Login)
                },
                onNavigateToGames = { navigationState.navigateTo(Screen.Game) }
            )
        }
        
        is Screen.DishCapture -> {
            DishCaptureScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onImageCaptured = { imageUri ->
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
            DishRatingScreen(
                viewModel = dishRatingViewModel,
                dishName = navigationState.dishName,
                imageUri = navigationState.imageUri,
                onNavigateBack = { navigationState.navigateBack() },
                onSubmitSuccess = {
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_RATE_DISH,
                        actionLabel = "Dish Rated"
                    )
                    navigationState.popToRoot()
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
            SocialFeedScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onShareClick = { /* Show share bottom sheet */ }
            )
        }
        
        is Screen.Search -> {
            val searchViewModel: SearchViewModel = viewModel { SearchViewModel() }
            DarkSearchScreen(
                viewModel = searchViewModel,
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
        
        is Screen.RestaurantDetail -> {
            val restaurantDetailViewModel: RestaurantDetailViewModel = viewModel { RestaurantDetailViewModel() }
            RestaurantDetailScreen(
                viewModel = restaurantDetailViewModel,
                photoViewModel = restaurantPhotoViewModel,
                restaurantId = navigationState.restaurantId,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }
        
        is Screen.LocationPermission -> {
            LocationPermissionScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onPermissionGranted = {
                    // Permission granted → trigger location detection
                    requestCurrentLocationDetection()
                    navigationState.navigateBack()
                },
                onPermissionDenied = { navigationState.navigateBack() }
            )
        }
        
        is Screen.UserProgress -> {
            val userProgressViewModel: UserProgressViewModel = viewModel { UserProgressViewModel() }
            UserProgressScreen(
                viewModel = userProgressViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToBadges = { navigationState.navigateTo(Screen.Badges) }
            )
        }
        
        is Screen.Badges -> {
            val userProgressViewModel: UserProgressViewModel = viewModel { UserProgressViewModel() }
            BadgesScreen(
                viewModel = userProgressViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }
        
        is Screen.LocationSelection -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()
            LocationSelectionScreen(
                currentLocation = uiState.selectedLocation,
                onNavigateBack = { navigationState.navigateBack() },
                onLocationSelected = { location ->
                    locationHomeViewModel.selectLocation(location)
                    navigationState.navigateBack()
                },
                onUseCurrentLocation = {
                    // Trigger real GPS location detection via platform layer
                    requestCurrentLocationDetection()
                    navigationState.navigateBack()
                }
            )
        }
        
        is Screen.AllRestaurants -> {
            val uiState by locationHomeViewModel.uiState.collectAsState()
            AllRestaurantsScreen(
                location = uiState.selectedLocation ?: "Unknown",
                restaurants = uiState.allRestaurants,
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
                location = uiState.selectedLocation ?: "New York, NY",
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
                location = uiState.selectedLocation ?: "New York, NY",
                restaurants = uiState.topRestaurants,
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
            DarkGameScreen(
                viewModel = gamificationViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }
        
        is Screen.DarkHome -> {
            // Use the auto-detected or manually-selected location
            val homeUiState by locationHomeViewModel.uiState.collectAsState()
            val detectedLocation = homeUiState.selectedLocation ?: "Detecting location..."
            DarkHomeScreen(
                currentLocation = detectedLocation,
                onLocationClick = { navigationState.navigateTo(Screen.LocationSelection) },
                onDishClick = { dishId ->
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
                onProfileClick = { navigationState.navigateTo(Screen.Profile) },
                onGameClick = { navigationState.navigateTo(Screen.Game) },
                onCameraClick = { navigationState.navigateTo(Screen.DarkDishCapture) },
                onTopDishesClick = { navigationState.navigateTo(Screen.TopDishes) },
                onTopRestaurantsClick = { navigationState.navigateTo(Screen.TopRestaurants) }
            )
        }
        
        is Screen.DarkDishCapture -> {
            DarkDishCaptureScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onImageCaptured = { imageUri, dishName ->
                    // Award points for photo upload
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_UPLOAD_PHOTO,
                        actionLabel = "Photo Uploaded"
                    )
                    navigationState.navigateToWithArgs(
                        Screen.DarkDishRating,
                        "imageUri" to imageUri,
                        "dishName" to dishName
                    )
                },
                onAddManually = { imageUri ->
                    // AI fallback → open manual dish entry with the captured photo
                    navigationState.navigateToWithArgs(
                        Screen.ManualDishEntry,
                        "imageUri" to imageUri
                    )
                }
            )
        }

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
            DarkDishRatingScreen(
                dishName = navigationState.dishName,
                imageUri = navigationState.imageUri,
                onNavigateBack = { navigationState.navigateBack() },
                onSubmitRating = { _, _, _ ->
                    // Award points for rating a dish
                    gamificationViewModel.recordAction(
                        actionType = com.example.smackcheck2.gamification.PointsConfig.ACTION_RATE_DISH,
                        actionLabel = "Dish Rated"
                    )
                }
            )
        }
        
        is Screen.DishDetail -> {
            DishDetailScreen(
                dishId = navigationState.dishId,
                onBackClick = { navigationState.navigateBack() },
                onAddToCart = { /* Add to cart logic */ },
                onRelatedDishClick = { dishId ->
                    navigationState.navigateToWithArgs(
                        Screen.DishDetail,
                        "dishId" to dishId
                    )
                }
            )
        }

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
                        "SocialFeed" -> navigationState.navigateTo(Screen.SocialFeed)
                    }
                }
            )
        }
    }

    // ── Points earned popup overlay (always on top) ──
    PointsEarnedPopup(
        event = pointsEvent,
        onDismissed = { pointsEvent = null },
        modifier = Modifier.align(Alignment.TopCenter).zIndex(100f)
    )
    } // end Box
}
