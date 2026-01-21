package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.model.Badge
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.HomeFeedUiState
import com.example.smackcheck2.model.ProfileUiState
import com.example.smackcheck2.model.User
import com.example.smackcheck2.model.UserProgressUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Home screen
 */
class HomeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeFeedUiState())
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()
    
    init {
        loadFeed()
    }
    
    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                delay(1000)
                // Mock timestamp for demo data
                val currentTime = 1737417600000L
                // Simulate loading feed items
                val feedItems = listOf(
                    FeedItem(
                        id = "1",
                        userProfileImageUrl = null,
                        userName = "John Doe",
                        dishImageUrl = null,
                        dishName = "Margherita Pizza",
                        restaurantName = "Italian Kitchen",
                        rating = 4.5f,
                        likesCount = 24,
                        commentsCount = 5,
                        isLiked = false,
                        timestamp = currentTime
                    ),
                    FeedItem(
                        id = "2",
                        userProfileImageUrl = null,
                        userName = "Jane Smith",
                        dishImageUrl = null,
                        dishName = "Sushi Platter",
                        restaurantName = "Tokyo Bites",
                        rating = 5f,
                        likesCount = 42,
                        commentsCount = 8,
                        isLiked = true,
                        timestamp = currentTime - 3600000
                    ),
                    FeedItem(
                        id = "3",
                        userProfileImageUrl = null,
                        userName = "Mike Johnson",
                        dishImageUrl = null,
                        dishName = "Butter Chicken",
                        restaurantName = "Spice Garden",
                        rating = 4f,
                        likesCount = 18,
                        commentsCount = 3,
                        isLiked = false,
                        timestamp = currentTime - 7200000
                    )
                )
                
                _uiState.update {
                    it.copy(
                        feedItems = feedItems,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load feed"
                    )
                }
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(1000)
            loadFeed()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    fun toggleLike(itemId: String) {
        _uiState.update { state ->
            val updatedItems = state.feedItems.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        isLiked = !item.isLiked,
                        likesCount = if (item.isLiked) item.likesCount - 1 else item.likesCount + 1
                    )
                } else {
                    item
                }
            }
            state.copy(feedItems = updatedItems)
        }
    }
}

/**
 * ViewModel for Profile screen
 */
class ProfileViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                delay(800)
                val user = User(
                    id = "user_123",
                    name = "John Doe",
                    email = "john.doe@example.com",
                    profilePhotoUrl = null,
                    level = 5,
                    xp = 450,
                    streakCount = 7,
                    badges = listOf(
                        Badge("1", "First Bite", "Rate your first dish", null, true),
                        Badge("2", "Foodie", "Rate 10 dishes", null, true),
                        Badge("3", "Explorer", "Visit 5 restaurants", null, false)
                    )
                )
                
                _uiState.update {
                    it.copy(
                        user = user,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }
}

/**
 * ViewModel for User Progress Dashboard
 */
class UserProgressViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserProgressUiState())
    val uiState: StateFlow<UserProgressUiState> = _uiState.asStateFlow()
    
    init {
        loadProgress()
    }
    
    fun loadProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                delay(800)
                _uiState.update {
                    it.copy(
                        currentXp = 450,
                        maxXp = 1000,
                        level = 5,
                        streakCount = 7,
                        badges = listOf(
                            Badge("1", "First Bite", "Rate your first dish", null, true),
                            Badge("2", "Foodie", "Rate 10 dishes", null, true),
                            Badge("3", "Explorer", "Visit 5 restaurants", null, true),
                            Badge("4", "Critic", "Write 20 reviews", null, false),
                            Badge("5", "Streak Master", "7-day streak", null, true),
                            Badge("6", "Social Butterfly", "Get 50 likes", null, false)
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load progress"
                    )
                }
            }
        }
    }
}
