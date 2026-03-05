package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.RestaurantVisitDto
import com.example.smackcheck2.platform.GeofenceRegion
import com.example.smackcheck2.platform.GeofencingService
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VisitTrackingUiState(
    val isTracking: Boolean = false,
    val activeRegions: List<GeofenceRegion> = emptyList()
)

class VisitTrackingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VisitTrackingUiState())
    val uiState: StateFlow<VisitTrackingUiState> = _uiState

    private var geofencingService: GeofencingService? = null

    fun setGeofencingService(service: GeofencingService?) {
        geofencingService = service
    }

    fun startTrackingNearby(restaurants: List<RestaurantInfo>) {
        val regions = restaurants.take(20).map { restaurant ->
            GeofenceRegion(
                id = restaurant.id,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                radiusMeters = 100f,
                name = restaurant.name
            )
        }

        geofencingService?.startMonitoring(regions)
        _uiState.value = _uiState.value.copy(
            isTracking = true,
            activeRegions = regions
        )
    }

    fun stopTracking() {
        geofencingService?.stopMonitoring()
        _uiState.value = _uiState.value.copy(
            isTracking = false,
            activeRegions = emptyList()
        )
    }

    fun recordVisit(restaurantId: String) {
        viewModelScope.launch {
            try {
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@launch
                val dto = RestaurantVisitDto(
                    userId = userId,
                    restaurantId = restaurantId
                )
                SupabaseClientProvider.client.postgrest["restaurant_visits"].insert(dto)
            } catch (_: Exception) {}
        }
    }
}

data class RestaurantInfo(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
