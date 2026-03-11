package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.SearchRepository
import com.example.smackcheck2.data.SupabaseClient
import com.example.smackcheck2.data.SupabaseRestaurantRow
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.model.RestaurantDetailUiState
import com.example.smackcheck2.model.SearchUiState
import com.example.smackcheck2.model.ManualRestaurantUiState
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Search screen
 * Searches restaurants from Supabase with support for
 * "Restaurants & Cafes Only" filter, cuisine, rating and city filters.
 */
@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {
    
    private val repository = SearchRepository()
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // Debounce channel for search queries
    private val searchQuery = MutableStateFlow("")
    
    val availableCuisines = listOf(
        "Italian", "Japanese", "Indian", "Mexican", "Chinese", 
        "Thai", "American", "French", "Mediterranean", "Korean"
    )
    
    val availableCities = listOf(
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
        "San Francisco", "Seattle", "Miami", "Boston", "Denver"
    )
    
    init {
        // Debounced search — waits 400ms after user stops typing
        viewModelScope.launch {
            searchQuery
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length >= 2) {
                        performSearch()
                    } else if (query.isEmpty()) {
                        _uiState.update { it.copy(results = emptyList(), errorMessage = null) }
                    }
                }
        }
    }
    
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchQuery.value = query
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
    
    /**
     * Toggle "Restaurants & Cafes Only" filter
     */
    fun toggleRestaurantsAndCafesFilter() {
        _uiState.update { 
            it.copy(restaurantsAndCafesOnly = !it.restaurantsAndCafesOnly) 
        }
        search()
    }
    
    fun search() {
        viewModelScope.launch {
            performSearch()
        }
    }
    
    /**
     * Perform the actual search against Supabase
     */
    private suspend fun performSearch() {
        val state = _uiState.value
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        try {
            val results = repository.searchRestaurants(
                query = state.query,
                cuisines = state.selectedCuisines,
                minRating = state.selectedRating,
                city = state.selectedCity,
                restaurantsAndCafesOnly = state.restaurantsAndCafesOnly
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
    
    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCuisines = emptySet(),
                selectedRating = null,
                selectedCity = null,
                restaurantsAndCafesOnly = false
            )
        }
        search()
    }
}

/**
 * ViewModel for Restaurant Detail screen
 * Loads restaurant data and photos from Supabase
 */
class RestaurantDetailViewModel : ViewModel() {
    
    private val client = SupabaseClient.client
    
    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()
    
    /**
     * Load restaurant details from Supabase by ID.
     * Fetches restaurant info and builds image URL list from photo_urls/image_url.
     */
    fun loadRestaurant(restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Fetch restaurant from Supabase
                val row = client.from("restaurants")
                    .select {
                        filter { eq("id", restaurantId) }
                    }
                    .decodeSingleOrNull<SupabaseRestaurantRow>()
                
                if (row != null) {
                    // Build image URLs: prefer photo_urls, fallback to image_url
                    val images = when {
                        !row.photoUrls.isNullOrEmpty() -> row.photoUrls
                        !row.imageUrl.isNullOrBlank() -> listOf(row.imageUrl)
                        else -> emptyList()
                    }
                    
                    val restaurant = Restaurant(
                        id = row.id,
                        name = row.name,
                        city = row.city ?: "",
                        cuisine = row.cuisine ?: "",
                        category = row.category ?: "",
                        imageUrls = images,
                        averageRating = row.averageRating?.toFloat() ?: 0f,
                        reviewCount = row.ratingCount ?: 0,
                        latitude = row.latitude,
                        longitude = row.longitude
                    )
                    
                    // TODO: Fetch reviews from Supabase ratings table
                    val reviews = emptyList<Review>()
                    
                    _uiState.update {
                        it.copy(
                            restaurant = restaurant,
                            reviews = reviews,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Restaurant not found"
                        )
                    }
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
