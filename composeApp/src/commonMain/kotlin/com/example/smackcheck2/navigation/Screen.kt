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
    data object NearbyRestaurants : Screen("nearby_restaurants")
    
    // Manual dish entry (AI fallback)
    data object ManualDishEntry : Screen("manual_dish_entry")
    
    // Dark theme screens
    data object DarkHome : Screen("dark_home")
    data object DarkDishCapture : Screen("dark_dish_capture")
    data object DarkDishRating : Screen("dark_dish_rating")
    data object DishDetail : Screen("dish_detail/{dishId}") {
        fun createRoute(dishId: String) = "dish_detail/$dishId"
    }

    // Notifications screen (from main)
    data object Notifications : Screen("notifications")

    // Profile-related screens
    data object EditProfile : Screen("edit_profile")
    data object NotificationSettings : Screen("notification_settings")
    data object AccountSettings : Screen("account_settings")
    data object PrivacySettings : Screen("privacy_settings")

    // Social screens
    data object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    data object FollowersList : Screen("followers_list/{userId}") {
        fun createRoute(userId: String) = "followers_list/$userId"
    }
    data object FollowingList : Screen("following_list/{userId}") {
        fun createRoute(userId: String) = "following_list/$userId"
    }
    data object Comments : Screen("comments/{ratingId}") {
        fun createRoute(ratingId: String) = "comments/$ratingId"
    }
    data object NotificationsList : Screen("notifications_list")
    
    // Social Map (Snapchat-style map with nearby users)
    data object SocialMap : Screen("social_map")

    // Onboarding — set username + profile photo after first sign-in
    data object ProfileSetup : Screen("profile_setup")
}
