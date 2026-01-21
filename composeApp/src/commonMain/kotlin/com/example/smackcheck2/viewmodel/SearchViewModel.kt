package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.model.RestaurantDetailUiState
import com.example.smackcheck2.model.SearchUiState
import com.example.smackcheck2.model.ManualRestaurantUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Search screen
 */
class SearchViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    val availableCuisines = listOf(
        "Italian", "Japanese", "Indian", "Mexican", "Chinese", 
        "Thai", "American", "French", "Mediterranean", "Korean"
    )
    
    val availableCities = listOf(
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
        "San Francisco", "Seattle", "Miami", "Boston", "Denver"
    )
    
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length >= 2) {
            search()
        }
    }
    
    fun onCuisineToggle(cuisine: String) {
        _uiState.update { state ->
            val updatedCuisines = if (state.selectedCuisines.contains(cuisine)) {
                state.selectedCuisines - cuisine
            } else {
                state.selectedCuisines + cuisine
            }
            state.copy(selectedCuisines = updatedCuisines)
        }
        search()
    }
    
    fun onRatingSelect(rating: Float?) {
        _uiState.update { it.copy(selectedRating = rating) }
        search()
    }
    
    fun onCitySelect(city: String?) {
        _uiState.update { it.copy(selectedCity = city) }
        search()
    }
    
    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                delay(500)
                // Simulate search results
                val results = listOf(
                    Restaurant(
                        id = "1",
                        name = "Italian Kitchen",
                        city = "New York",
                        cuisine = "Italian",
                        averageRating = 4.5f,
                        reviewCount = 128
                    ),
                    Restaurant(
                        id = "2",
                        name = "Tokyo Bites",
                        city = "Los Angeles",
                        cuisine = "Japanese",
                        averageRating = 4.8f,
                        reviewCount = 256
                    ),
                    Restaurant(
                        id = "3",
                        name = "Spice Garden",
                        city = "Chicago",
                        cuisine = "Indian",
                        averageRating = 4.2f,
                        reviewCount = 89
                    )
                )
                
                _uiState.update {
                    it.copy(
                        results = results,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Search failed"
                    )
                }
            }
        }
    }
    
    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCuisines = emptySet(),
                selectedRating = null,
                selectedCity = null
            )
        }
        search()
    }
}

/**
 * ViewModel for Restaurant Detail screen
 */
class RestaurantDetailViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()
    
    fun loadRestaurant(restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                delay(800)
                val restaurant = Restaurant(
                    id = restaurantId,
                    name = "Italian Kitchen",
                    city = "New York",
                    cuisine = "Italian",
                    imageUrls = listOf(
                        "https://example.com/image1.jpg",
                        "https://example.com/image2.jpg",
                        "https://example.com/image3.jpg"
                    ),
                    averageRating = 4.5f,
                    reviewCount = 128
                )
                
                // Mock timestamp for demo data
                val currentTime = 1737417600000L
                
                val reviews = listOf(
                    Review(
                        id = "1",
                        userId = "user1",
                        userName = "John Doe",
                        dishName = "Margherita Pizza",
                        rating = 5f,
                        comment = "Best pizza in town!",
                        likesCount = 12,
                        createdAt = currentTime
                    ),
                    Review(
                        id = "2",
                        userId = "user2",
                        userName = "Jane Smith",
                        dishName = "Pasta Carbonara",
                        rating = 4f,
                        comment = "Great pasta, authentic taste.",
                        likesCount = 8,
                        createdAt = currentTime - 86400000
                    )
                )
                
                _uiState.update {
                    it.copy(
                        restaurant = restaurant,
                        reviews = reviews,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load restaurant"
                    )
                }
            }
        }
    }
}

/**
 * ViewModel for Manual Restaurant Entry screen
 */
class ManualRestaurantViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(ManualRestaurantUiState())
    val uiState: StateFlow<ManualRestaurantUiState> = _uiState.asStateFlow()
    
    fun onRestaurantNameChange(name: String) {
        _uiState.update { it.copy(restaurantName = name, restaurantNameError = null) }
    }
    
    fun onCityChange(city: String) {
        _uiState.update { it.copy(city = city, cityError = null) }
    }
    
    fun onCuisineChange(cuisine: String) {
        _uiState.update { it.copy(cuisine = cuisine, cuisineError = null) }
    }
    
    fun saveRestaurant(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        
        // Validate inputs
        var hasError = false
        var restaurantNameError: String? = null
        var cityError: String? = null
        var cuisineError: String? = null
        
        if (currentState.restaurantName.isBlank()) {
            restaurantNameError = "Restaurant name is required"
            hasError = true
        }
        
        if (currentState.city.isBlank()) {
            cityError = "City is required"
            hasError = true
        }
        
        if (currentState.cuisine.isBlank()) {
            cuisineError = "Cuisine is required"
            hasError = true
        }
        
        if (hasError) {
            _uiState.update {
                it.copy(
                    restaurantNameError = restaurantNameError,
                    cityError = cityError,
                    cuisineError = cuisineError
                )
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                delay(1000)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to save restaurant"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
