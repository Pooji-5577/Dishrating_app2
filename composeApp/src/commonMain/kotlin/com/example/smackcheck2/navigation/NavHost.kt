package com.example.smackcheck2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.smackcheck2.ui.screens.DarkProfileScreen
import com.example.smackcheck2.ui.screens.RegisterScreen
import com.example.smackcheck2.ui.screens.RestaurantDetailScreen
import com.example.smackcheck2.ui.screens.SearchScreen
import com.example.smackcheck2.ui.screens.SocialFeedScreen
import com.example.smackcheck2.ui.screens.SplashScreen
import com.example.smackcheck2.ui.screens.TopDishesScreen
import com.example.smackcheck2.ui.screens.TopRestaurantsScreen
import com.example.smackcheck2.ui.screens.UserProgressScreen
import com.example.smackcheck2.viewmodel.AuthViewModel
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
            val locationHomeViewModel: LocationHomeViewModel = viewModel { LocationHomeViewModel() }
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
            val searchViewModel: SearchViewModel = viewModel { SearchViewModel() }
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
            val restaurantDetailViewModel: RestaurantDetailViewModel = viewModel { RestaurantDetailViewModel() }
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
            val locationHomeViewModel: LocationHomeViewModel = viewModel { LocationHomeViewModel() }
            val uiState by locationHomeViewModel.uiState.collectAsState()
            LocationSelectionScreen(
                currentLocation = uiState.selectedLocation,
                onNavigateBack = { navigationState.navigateBack() },
                onLocationSelected = { location ->
                    locationHomeViewModel.selectLocation(location)
                    navigationState.navigateBack()
                },
                onUseCurrentLocation = {
                    locationHomeViewModel.useCurrentLocation()
                    navigationState.navigateBack()
                }
            )
        }
        
        is Screen.AllRestaurants -> {
            val locationHomeViewModel: LocationHomeViewModel = viewModel { LocationHomeViewModel() }
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
            val locationHomeViewModel: LocationHomeViewModel = viewModel { LocationHomeViewModel() }
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
            val locationHomeViewModel: LocationHomeViewModel = viewModel { LocationHomeViewModel() }
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
            DarkGameScreen(
                viewModel = gameViewModel,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }
        
        is Screen.DarkHome -> {
            DarkHomeScreen(
                currentLocation = "123 Main Street, New York, NY",
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
                    navigationState.navigateToWithArgs(
                        Screen.DarkDishRating,
                        "imageUri" to imageUri,
                        "dishName" to dishName
                    )
                }
            )
        }
        
        is Screen.DarkDishRating -> {
            DarkDishRatingScreen(
                dishName = navigationState.dishName,
                imageUri = navigationState.imageUri,
                onNavigateBack = { navigationState.navigateBack() },
                onSubmitRating = { _, _, _ ->
                    // Rating submitted - will show success screen then navigate back
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
    }
}
