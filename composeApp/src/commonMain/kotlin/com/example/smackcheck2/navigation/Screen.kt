package com.example.smackcheck2.navigation

/**
 * Sealed class representing all navigation routes in the app
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object DishCapture : Screen("dish_capture")
    data object DishPreview : Screen("dish_preview/{imageUri}") {
        fun createRoute(imageUri: String) = "dish_preview/$imageUri"
    }
    data object DishRating : Screen("dish_rating/{dishName}/{imageUri}") {
        fun createRoute(dishName: String, imageUri: String) = "dish_rating/$dishName/$imageUri"
    }
    data object ManualRestaurantEntry : Screen("manual_restaurant_entry")
    data object SocialFeed : Screen("social_feed")
    data object Search : Screen("search")
    data object RestaurantDetail : Screen("restaurant_detail/{restaurantId}") {
        fun createRoute(restaurantId: String) = "restaurant_detail/$restaurantId"
    }
    data object LocationPermission : Screen("location_permission")
    data object UserProgress : Screen("user_progress")
    data object Badges : Screen("badges")
    
    // New location & game screens
    data object LocationSelection : Screen("location_selection")
    data object AllRestaurants : Screen("all_restaurants")
    data object TopDishes : Screen("top_dishes")
    data object TopRestaurants : Screen("top_restaurants")
    data object Game : Screen("game")
    
    // Manual dish entry (AI fallback)
    data object ManualDishEntry : Screen("manual_dish_entry")
    
    // Dark theme screens
    data object DarkHome : Screen("dark_home")
    data object DarkDishCapture : Screen("dark_dish_capture")
    data object DarkDishRating : Screen("dark_dish_rating")
    data object DishDetail : Screen("dish_detail/{dishId}") {
        fun createRoute(dishId: String) = "dish_detail/$dishId"
    }
    
    // Notifications screen
    data object Notifications : Screen("notifications")
}
