package com.example.smackcheck2.navigation

import androidx.activity.compose.BackHandler
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
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.model.Restaurant
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.smackcheck2.ui.screens.ManualRestaurantEntryScreen
import com.example.smackcheck2.ui.screens.NearbyRestaurantsScreen
import com.example.smackcheck2.ui.screens.DarkProfileScreen
import com.example.smackcheck2.ui.screens.RegisterScreen
import com.example.smackcheck2.ui.screens.RestaurantDetailScreen
import com.example.smackcheck2.ui.screens.SearchScreen
import com.example.smackcheck2.ui.screens.SocialFeedScreen
import com.example.smackcheck2.ui.screens.SplashScreen
import com.example.smackcheck2.ui.screens.TopDishesScreen
import com.example.smackcheck2.ui.screens.TopRestaurantsScreen
import com.example.smackcheck2.ui.screens.UserProgressScreen
import com.example.smackcheck2.platform.LocalLocationService
import com.example.smackcheck2.platform.LocalPlacesService
import com.example.smackcheck2.platform.RequestLocationPermission
import com.example.smackcheck2.platform.LocalImagePicker
import com.example.smackcheck2.viewmodel.AuthViewModel
import com.example.smackcheck2.viewmodel.NearbyRestaurantsViewModel
import com.example.smackcheck2.viewmodel.DishCaptureViewModel
import com.example.smackcheck2.viewmodel.DishRatingViewModel
import com.example.smackcheck2.viewmodel.GameViewModel
import com.example.smackcheck2.viewmodel.LocationHomeViewModel
import com.example.smackcheck2.viewmodel.LoginViewModel
import com.example.smackcheck2.viewmodel.ManualRestaurantViewModel
import com.example.smackcheck2.viewmodel.ProfileViewModel
import com.example.smackcheck2.viewmodel.RegisterViewModel
import com.example.smackcheck2.viewmodel.RestaurantDetailViewModel
import com.example.smackcheck2.viewmodel.SearchViewModel
import com.example.smackcheck2.viewmodel.UserProgressViewModel
import com.example.smackcheck2.viewmodel.EditProfileViewModel
import com.example.smackcheck2.viewmodel.NotificationSettingsViewModel
import com.example.smackcheck2.viewmodel.AccountSettingsViewModel
import com.example.smackcheck2.viewmodel.PrivacySettingsViewModel
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.ui.screens.EditProfileScreen
import com.example.smackcheck2.ui.screens.NotificationSettingsScreen
import com.example.smackcheck2.ui.screens.AccountSettingsScreen
import com.example.smackcheck2.ui.screens.PrivacySettingsScreen

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

    var imageBytes by mutableStateOf<ByteArray?>(null)
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

    fun updateImageBytes(bytes: ByteArray?) {
        imageBytes = bytes
    }
}

/**
 * Main Navigation Host composable
 * Manages navigation between all screens
 */
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
                authViewModel = authViewModel,
                onNavigateToRegister = { navigationState.navigateTo(Screen.Register) },
                onNavigateToHome = { navigationState.navigateTo(Screen.DarkHome) }
            )
        }
        
        is Screen.Register -> {
            val registerViewModel: RegisterViewModel = viewModel { RegisterViewModel() }
            RegisterScreen(
                viewModel = registerViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToHome = { navigationState.navigateTo(Screen.DarkHome) }
            )
        }
        
        is Screen.Home -> {
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
            val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(authViewModel) }

            // Refresh profile data every time screen is shown (to show updated XP)
            LaunchedEffect(Unit) {
                profileViewModel.refresh()
            }

            DarkProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onEditProfile = { navigationState.navigateTo(Screen.EditProfile) },
                onSignOut = {
                    authViewModel.signOut()
                    navigationState.navigateTo(Screen.Login)
                },
                onNavigateToGames = { navigationState.navigateTo(Screen.Game) },
                onNavigateToNotifications = { navigationState.navigateTo(Screen.NotificationSettings) },
                onNavigateToAccount = { navigationState.navigateTo(Screen.AccountSettings) },
                onNavigateToPrivacy = { navigationState.navigateTo(Screen.PrivacySettings) }
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
                onNavigateBack = { navigationState.navigateBack() }
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
                onSubmitSuccess = { navigationState.popToRoot() }
            )
        }
        
        is Screen.ManualRestaurantEntry -> {
            val manualRestaurantViewModel: ManualRestaurantViewModel = viewModel { ManualRestaurantViewModel() }
            ManualRestaurantEntryScreen(
                viewModel = manualRestaurantViewModel,
                onNavigateBack = { navigationState.navigateBack() },
                onSaveSuccess = { navigationState.navigateBack() }
            )
        }
        
        is Screen.SocialFeed -> {
            SocialFeedScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onShareClick = { /* Show share bottom sheet */ }
            )
        }
        
        is Screen.Search -> {
            val searchViewModel: SearchViewModel = viewModel {
                SearchViewModel(locationService, placesService)
            }
            DarkSearchScreen(
                viewModel = searchViewModel,
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
            val placesService = LocalPlacesService.current
            val restaurantDetailViewModel: RestaurantDetailViewModel = viewModel {
                RestaurantDetailViewModel(placesService = placesService)
            }
            RestaurantDetailScreen(
                viewModel = restaurantDetailViewModel,
                restaurantId = navigationState.restaurantId,
                onNavigateBack = { navigationState.navigateBack() }
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
            var locationDetectedAndSelected by remember { mutableStateOf(false) }

            // Navigate back after GPS location is successfully detected and selected
            androidx.compose.runtime.LaunchedEffect(locationDetectedAndSelected) {
                if (locationDetectedAndSelected) {
                    navigationState.navigateBack()
                }
            }

            RequestLocationPermission(
                onPermissionResult = { granted ->
                    if (granted) {
                        locationHomeViewModel.useCurrentLocation()
                    }
                }
            ) { requestPermission ->
                // Watch for successful location detection
                val wasDetecting = remember { mutableStateOf(false) }
                if (uiState.isDetectingLocation) {
                    wasDetecting.value = true
                } else if (wasDetecting.value && !uiState.isDetectingLocation && uiState.locationError == null) {
                    // Location was detected successfully
                    wasDetecting.value = false
                    locationDetectedAndSelected = true
                }

                LocationSelectionScreen(
                    currentLocation = uiState.selectedLocation,
                    isDetectingLocation = uiState.isDetectingLocation,
                    locationError = uiState.locationError,
                    searchResults = uiState.searchResults,
                    onNavigateBack = { navigationState.navigateBack() },
                    onLocationSelected = { location ->
                        locationHomeViewModel.selectLocation(location)
                        navigationState.navigateBack()
                    },
                    onUseCurrentLocation = {
                        // Request permission first, then get location
                        requestPermission()
                    },
                    onSearchLocation = { query ->
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
                    longitude = nearby.longitude
                )
            }

            // Combine database restaurants and nearby restaurants
            val combinedRestaurants = (uiState.allRestaurants + nearbyAsRestaurants).distinctBy { it.id }

            AllRestaurantsScreen(
                location = uiState.selectedLocation ?: "Unknown",
                restaurants = combinedRestaurants,
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
            val gameViewModel: GameViewModel = viewModel { GameViewModel() }

            // Refresh game data every time screen is shown (to show updated XP and leaderboard)
            LaunchedEffect(Unit) {
                gameViewModel.loadGameData()
            }

            DarkGameScreen(
                viewModel = gameViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        is Screen.NearbyRestaurants -> {
            val placesService = LocalPlacesService.current
            val nearbyRestaurantsViewModel: NearbyRestaurantsViewModel = viewModel {
                NearbyRestaurantsViewModel(locationService, placesService)
            }
            NearbyRestaurantsScreen(
                viewModel = nearbyRestaurantsViewModel,
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

            // Convert nearby restaurants to Restaurant format
            val nearbyAsRestaurants = uiState.nearbyRestaurants.map { nearby ->
                Restaurant(
                    id = nearby.id,
                    name = nearby.name,
                    city = uiState.selectedLocation ?: "Unknown",
                    cuisine = "Restaurant",
                    imageUrls = emptyList(),
                    averageRating = nearby.rating?.toFloat() ?: 0f,
                    reviewCount = nearby.userRatingsTotal ?: 0,
                    latitude = nearby.latitude,
                    longitude = nearby.longitude
                )
            }

            // Combine database and nearby restaurants
            val combinedRestaurants = (uiState.allRestaurants + nearbyAsRestaurants).distinctBy { it.id }

            DarkHomeScreen(
                currentLocation = uiState.selectedLocation ?: "Select Location",
                allRestaurants = combinedRestaurants,
                allDishes = uiState.topDishes,
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
                onTopRestaurantsClick = { navigationState.navigateTo(Screen.TopRestaurants) },
                onNearbyRestaurantsClick = { navigationState.navigateTo(Screen.NearbyRestaurants) }
            )
        }
        
        is Screen.DarkDishCapture -> {
            val imagePicker = LocalImagePicker.current
            val dishCaptureViewModel: DishCaptureViewModel = viewModel { DishCaptureViewModel() }

            DarkDishCaptureScreen(
                viewModel = dishCaptureViewModel,
                imagePicker = imagePicker,
                onNavigateBack = { navigationState.navigateBack() },
                onImageCaptured = { imageUri, dishName, imageBytes ->
                    navigationState.updateImageBytes(imageBytes)
                    navigationState.navigateToWithArgs(
                        Screen.DarkDishRating,
                        "imageUri" to imageUri,
                        "dishName" to dishName
                    )
                }
            )
        }
        
        is Screen.DarkDishRating -> {
            val dishRatingViewModel: DishRatingViewModel = viewModel { DishRatingViewModel() }
            val ratingUiState by dishRatingViewModel.uiState.collectAsState()
            val databaseRepository = remember { DatabaseRepository() }
            val locationUiState by locationHomeViewModel.uiState.collectAsState()

            // State for restaurants
            var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
            var nearbyRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
            var isLoadingRestaurants by remember { mutableStateOf(true) }

            // Load restaurants when screen opens - both all and nearby
            LaunchedEffect(Unit) {
                isLoadingRestaurants = true
                println("DarkDishRating: Loading restaurants...")

                // Load all restaurants
                val allResult = databaseRepository.getRestaurants()
                allResult.onSuccess { restaurants ->
                    allRestaurants = restaurants
                    println("DarkDishRating: Loaded ${restaurants.size} total restaurants")
                }.onFailure { error ->
                    println("DarkDishRating: Failed to load restaurants: ${error.message}")
                }

                // Load nearby restaurants based on current selected city
                val currentCity = locationUiState.selectedLocation
                println("DarkDishRating: Current location = $currentCity")

                if (!currentCity.isNullOrBlank()) {
                    println("DarkDishRating: Loading nearby restaurants for: $currentCity")
                    val nearbyResult = databaseRepository.getRestaurantsByCity(currentCity)
                    nearbyResult.onSuccess { restaurants ->
                        nearbyRestaurants = restaurants
                        println("DarkDishRating: ✓ Loaded ${restaurants.size} nearby restaurants in $currentCity")
                    }.onFailure { error ->
                        println("DarkDishRating: ✗ Failed to load nearby restaurants: ${error.message}")
                    }
                } else {
                    println("DarkDishRating: ⚠ No location selected - nearby restaurants unavailable")
                    println("DarkDishRating: User should select a location from the home screen first")
                }

                isLoadingRestaurants = false
            }

            // Reload nearby restaurants when location changes
            LaunchedEffect(locationUiState.selectedLocation) {
                val currentCity = locationUiState.selectedLocation
                if (!currentCity.isNullOrBlank() && allRestaurants.isNotEmpty()) {
                    println("DarkDishRating: Location changed to $currentCity, reloading nearby restaurants...")
                    val nearbyResult = databaseRepository.getRestaurantsByCity(currentCity)
                    nearbyResult.onSuccess { restaurants ->
                        nearbyRestaurants = restaurants
                        println("DarkDishRating: ✓ Updated nearby restaurants: ${restaurants.size} found")
                    }
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

            // Navigate back to home on success (after showing reward for 6 seconds)
            LaunchedEffect(ratingUiState.isSuccess) {
                if (ratingUiState.isSuccess) {
                    kotlinx.coroutines.delay(6000) // Show XP reward for 6 seconds
                    navigationState.popToRoot()
                }
            }

            DarkDishRatingScreen(
                dishName = navigationState.dishName,
                imageUri = navigationState.imageUri,
                imageBytes = ratingUiState.imageBytes,
                restaurants = allRestaurants,
                nearbyRestaurants = nearbyRestaurants,
                isLoadingRestaurants = isLoadingRestaurants,
                isSubmitting = ratingUiState.isSubmitting,
                showSuccess = ratingUiState.isSuccess,
                errorMessage = ratingUiState.errorMessage,
                onNavigateBack = { navigationState.navigateBack() },
                onSubmitRating = { rating, comment, tags, restaurantId ->
                    dishRatingViewModel.onRatingChange(rating)
                    dishRatingViewModel.onCommentChange(comment)
                    if (restaurantId != null) {
                        dishRatingViewModel.setRestaurantId(restaurantId)
                        dishRatingViewModel.submitRating {
                            // Success is handled by LaunchedEffect above
                        }
                    }
                },
                onDismissError = { dishRatingViewModel.clearError() }
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
    }
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
