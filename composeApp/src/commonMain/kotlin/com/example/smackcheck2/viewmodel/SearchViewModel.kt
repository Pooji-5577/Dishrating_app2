package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.model.RestaurantDetailUiState
import com.example.smackcheck2.model.SearchUiState
import com.example.smackcheck2.model.ManualRestaurantUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Search screen
 */
class SearchViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()

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
                val currentState = _uiState.value
                val result = databaseRepository.searchRestaurants(
                    query = currentState.query.takeIf { it.isNotBlank() },
                    cuisines = currentState.selectedCuisines,
                    city = currentState.selectedCity,
                    minRating = currentState.selectedRating
                )

                result.fold(
                    onSuccess = { restaurants ->
                        _uiState.update {
                            it.copy(
                                results = restaurants,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Search failed"
                            )
                        }
                    }
                )
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

    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()

    fun loadRestaurant(restaurantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val restaurantResult = databaseRepository.getRestaurantById(restaurantId)
                val reviewsResult = databaseRepository.getRatingsForRestaurant(restaurantId)

                val restaurant = restaurantResult.getOrNull()
                val reviews = reviewsResult.getOrDefault(emptyList())

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

    private val databaseRepository = DatabaseRepository()

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
                val restaurant = Restaurant(
                    name = currentState.restaurantName,
                    city = currentState.city,
                    cuisine = currentState.cuisine
                )

                val result = databaseRepository.createRestaurant(restaurant)

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to save restaurant"
                            )
                        }
                    }
                )
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
