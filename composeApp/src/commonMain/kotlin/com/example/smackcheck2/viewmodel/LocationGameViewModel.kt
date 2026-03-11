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
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.ui.screens.Achievement
import com.example.smackcheck2.ui.screens.Challenge
import com.example.smackcheck2.ui.screens.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI State for Location-based Home Screen
 */
data class LocationHomeUiState(
    val isLoading: Boolean = false,
    val selectedLocation: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val topRestaurants: List<Restaurant> = emptyList(),
    val topDishes: List<Dish> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for Location-based Home Screen
 */
class LocationHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LocationHomeUiState())
    val uiState: StateFlow<LocationHomeUiState> = _uiState.asStateFlow()
    
    init {
        // Don't load mock data by default — wait for real location
        // If no location is detected, screens will show "Select Location" prompt
    }
    
    /**
     * Select a location by name (e.g. from the city list or from auto-detect)
     */
    fun selectLocation(location: String) {
        _uiState.update { it.copy(selectedLocation = location, isLoading = true) }
        loadDataForLocation(location)
    }

    /**
     * Select a location with full GPS data (called after auto-detection)
     *
     * @param city     Reverse-geocoded city name
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     */
    fun selectLocationWithCoordinates(city: String, latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                selectedLocation = city,
                latitude = latitude,
                longitude = longitude,
                isLoading = true
            )
        }
        loadDataForLocation(city)
    }

    /**
     * Called when "Use Current Location" is tapped.
     * The actual GPS detection happens in the platform layer (MainActivity / AppLocationManager).
     * This is a fallback that sets a placeholder until the real location arrives.
     */
    fun useCurrentLocation() {
        _uiState.update { it.copy(isLoading = true) }
        // The actual location will be set by the platform layer calling
        // selectLocationWithCoordinates() once GPS data is available.
    }
    
    private fun loadDataForLocation(location: String) {
        // Mock data for the selected location
        val mockRestaurants = listOf(
            Restaurant(
                id = "1",
                name = "Italian Kitchen",
                city = location,
                cuisine = "Italian",
                averageRating = 4.8f,
                reviewCount = 342,
                latitude = 40.7128,
                longitude = -74.0060
            ),
            Restaurant(
                id = "2",
                name = "Tokyo Bites",
                city = location,
                cuisine = "Japanese",
                averageRating = 4.7f,
                reviewCount = 256,
                latitude = 40.7130,
                longitude = -74.0065
            ),
            Restaurant(
                id = "3",
                name = "Spice Garden",
                city = location,
                cuisine = "Indian",
                averageRating = 4.6f,
                reviewCount = 189,
                latitude = 40.7125,
                longitude = -74.0055
            ),
            Restaurant(
                id = "4",
                name = "Taco Fiesta",
                city = location,
                cuisine = "Mexican",
                averageRating = 4.5f,
                reviewCount = 210,
                latitude = 40.7135,
                longitude = -74.0070
            ),
            Restaurant(
                id = "5",
                name = "Golden Dragon",
                city = location,
                cuisine = "Chinese",
                averageRating = 4.4f,
                reviewCount = 178,
                latitude = 40.7120,
                longitude = -74.0050
            ),
            Restaurant(
                id = "6",
                name = "Le Petit Bistro",
                city = location,
                cuisine = "French",
                averageRating = 4.9f,
                reviewCount = 420,
                latitude = 40.7140,
                longitude = -74.0075
            ),
            Restaurant(
                id = "7",
                name = "Burger Joint",
                city = location,
                cuisine = "American",
                averageRating = 4.3f,
                reviewCount = 534,
                latitude = 40.7145,
                longitude = -74.0080
            ),
            Restaurant(
                id = "8",
                name = "Mediterranean Delight",
                city = location,
                cuisine = "Mediterranean",
                averageRating = 4.6f,
                reviewCount = 267,
                latitude = 40.7150,
                longitude = -74.0085
            )
        )
        
        val mockDishes = listOf(
            Dish(
                id = "d1",
                name = "Margherita Pizza",
                comment = "Classic Italian pizza with fresh basil",
                restaurantId = "1",
                restaurantName = "Italian Kitchen",
                rating = 4.9f
            ),
            Dish(
                id = "d2",
                name = "Sushi Platter",
                comment = "Assorted fresh sushi",
                restaurantId = "2",
                restaurantName = "Tokyo Bites",
                rating = 4.8f
            ),
            Dish(
                id = "d3",
                name = "Butter Chicken",
                comment = "Creamy tomato curry with tender chicken",
                restaurantId = "3",
                restaurantName = "Spice Garden",
                rating = 4.7f
            ),
            Dish(
                id = "d4",
                name = "Tacos al Pastor",
                comment = "Traditional pork tacos",
                restaurantId = "4",
                restaurantName = "Taco Fiesta",
                rating = 4.6f
            ),
            Dish(
                id = "d5",
                name = "Kung Pao Chicken",
                comment = "Spicy stir-fried chicken with peanuts",
                restaurantId = "5",
                restaurantName = "Golden Dragon",
                rating = 4.5f
            ),
            Dish(
                id = "d6",
                name = "Croissant",
                comment = "Buttery French pastry",
                restaurantId = "6",
                restaurantName = "Le Petit Bistro",
                rating = 4.9f
            )
        )
        
        // Sort by rating for "top" lists
        val topRestaurants = mockRestaurants.sortedByDescending { it.averageRating }.take(5)
        val topDishes = mockDishes.sortedByDescending { it.rating }
        
        _uiState.update {
            it.copy(
                isLoading = false,
                topRestaurants = topRestaurants,
                topDishes = topDishes,
                allRestaurants = mockRestaurants
            )
        }
    }
}

/**
 * UI State for Game Screen
 */
data class GameUiState(
    val isLoading: Boolean = false,
    val totalXp: Int = 2450,
    val level: Int = 8,
    val rank: Int = 42,
    val streakDays: Int = 7,
    val dailyChallenges: List<Challenge> = emptyList(),
    val weeklyChallenges: List<Challenge> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

/**
 * ViewModel for Game Screen
 */
class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    
    init {
        loadGameData()
    }
    
    private fun loadGameData() {
        val dailyChallenges = listOf(
            Challenge(
                id = "dc1",
                title = "Rate 3 Dishes",
                description = "Rate any 3 dishes today",
                icon = Icons.Filled.Star,
                xpReward = 50,
                progress = 0.66f,
                isCompleted = false
            ),
            Challenge(
                id = "dc2",
                title = "Capture a New Dish",
                description = "Take a photo of a dish you haven't tried",
                icon = Icons.Filled.CameraAlt,
                xpReward = 30,
                progress = 1f,
                isCompleted = true
            ),
            Challenge(
                id = "dc3",
                title = "Visit a New Restaurant",
                description = "Check in at a restaurant you haven't visited",
                icon = Icons.Filled.Restaurant,
                xpReward = 40,
                progress = 0f,
                isCompleted = false
            )
        )
        
        val weeklyChallenges = listOf(
            Challenge(
                id = "wc1",
                title = "Food Explorer",
                description = "Try 5 different cuisines this week",
                icon = Icons.Filled.Explore,
                xpReward = 200,
                progress = 0.4f,
                isCompleted = false
            ),
            Challenge(
                id = "wc2",
                title = "Review Master",
                description = "Write 10 detailed reviews",
                icon = Icons.Filled.RateReview,
                xpReward = 250,
                progress = 0.3f,
                isCompleted = false
            ),
            Challenge(
                id = "wc3",
                title = "Social Foodie",
                description = "Share 5 dishes on social media",
                icon = Icons.Filled.Share,
                xpReward = 150,
                progress = 0.6f,
                isCompleted = false
            ),
            Challenge(
                id = "wc4",
                title = "Streak Champion",
                description = "Maintain a 7-day rating streak",
                icon = Icons.Filled.LocalFireDepartment,
                xpReward = 300,
                progress = 1f,
                isCompleted = true
            )
        )
        
        val leaderboard = listOf(
            LeaderboardEntry("u1", "FoodMaster", 15420, 25),
            LeaderboardEntry("u2", "TasteHunter", 14850, 24),
            LeaderboardEntry("u3", "GourmetKing", 13200, 22),
            LeaderboardEntry("u4", "DishExplorer", 12800, 21),
            LeaderboardEntry("u5", "FlavorSeeker", 11500, 20),
            LeaderboardEntry("u6", "CuisinePro", 10200, 18),
            LeaderboardEntry("u7", "BiteReviewer", 9800, 17),
            LeaderboardEntry("u8", "FoodNinja", 8500, 15),
            LeaderboardEntry("u9", "TastyTraveler", 7200, 13),
            LeaderboardEntry("u10", "PlateRater", 6800, 12)
        )
        
        val achievements = listOf(
            Achievement(
                id = "a1",
                title = "First Bite",
                description = "Rate your first dish",
                icon = Icons.Filled.Star,
                isUnlocked = true
            ),
            Achievement(
                id = "a2",
                title = "Foodie Explorer",
                description = "Try 10 different restaurants",
                icon = Icons.Filled.Explore,
                isUnlocked = true
            ),
            Achievement(
                id = "a3",
                title = "Rating Streak",
                description = "Maintain a 7-day streak",
                icon = Icons.Filled.LocalFireDepartment,
                isUnlocked = true
            ),
            Achievement(
                id = "a4",
                title = "Cuisine Master",
                description = "Try 15 different cuisines",
                icon = Icons.Filled.Fastfood,
                isUnlocked = false
            ),
            Achievement(
                id = "a5",
                title = "Top Reviewer",
                description = "Be in the top 10 on leaderboard",
                icon = Icons.Filled.EmojiEvents,
                isUnlocked = false
            ),
            Achievement(
                id = "a6",
                title = "Trending Taste",
                description = "Have a review go viral",
                icon = Icons.Filled.TrendingUp,
                isUnlocked = false
            )
        )
        
        _uiState.update {
            it.copy(
                dailyChallenges = dailyChallenges,
                weeklyChallenges = weeklyChallenges,
                leaderboard = leaderboard,
                achievements = achievements
            )
        }
    }
    
    fun completeChallenge(challengeId: String) {
        // Mark challenge as completed and award XP
        _uiState.update { state ->
            val updatedDaily = state.dailyChallenges.map { 
                if (it.id == challengeId) it.copy(isCompleted = true, progress = 1f) else it 
            }
            val updatedWeekly = state.weeklyChallenges.map { 
                if (it.id == challengeId) it.copy(isCompleted = true, progress = 1f) else it 
            }
            
            val xpGained = (state.dailyChallenges + state.weeklyChallenges)
                .find { it.id == challengeId }?.xpReward ?: 0
            
            state.copy(
                dailyChallenges = updatedDaily,
                weeklyChallenges = updatedWeekly,
                totalXp = state.totalXp + xpGained
            )
        }
    }
}
