package com.example.smackcheck2.data.repository

import com.example.smackcheck2.platform.GeofenceRegion
import com.example.smackcheck2.platform.GeofencingService
import com.example.smackcheck2.platform.NearbyRestaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Event triggered when user enters or exits a geofenced restaurant area
 */
sealed class GeofenceEvent {
    data class Entered(val restaurantId: String, val restaurantName: String) : GeofenceEvent()
    data class Exited(val restaurantId: String, val restaurantName: String) : GeofenceEvent()
}

/**
 * Repository for managing restaurant geofences
 * Converts NearbyRestaurant objects to GeofenceRegions and monitors them
 */
class GeofenceRepository(
    private val geofencingService: GeofencingService?
) {
    companion object {
        private const val DEFAULT_RADIUS_METERS = 100f
        private const val MAX_GEOFENCES = 100 // Android has a limit of ~100 geofences
    }
    
    private val _monitoredRestaurants = MutableStateFlow<Set<String>>(emptySet())
    val monitoredRestaurants: StateFlow<Set<String>> = _monitoredRestaurants.asStateFlow()
    
    private val _lastEvent = MutableStateFlow<GeofenceEvent?>(null)
    val lastEvent: StateFlow<GeofenceEvent?> = _lastEvent.asStateFlow()
    
    private val restaurantNameMap = mutableMapOf<String, String>()
    
    /**
     * Start monitoring geofences for the given restaurants
     * @param restaurants List of nearby restaurants to monitor
     * @param radiusMeters Radius of each geofence in meters (default 100m)
     */
    fun startMonitoringRestaurants(
        restaurants: List<NearbyRestaurant>,
        radiusMeters: Float = DEFAULT_RADIUS_METERS
    ) {
        if (geofencingService == null) {
            println("GeofenceRepository: GeofencingService not available")
            return
        }
        
        // Limit to MAX_GEOFENCES (take closest restaurants first - list should be sorted by distance)
        val restaurantsToMonitor = restaurants.take(MAX_GEOFENCES)
        
        // Convert to GeofenceRegions
        val regions = restaurantsToMonitor.map { restaurant ->
            // Store name for event notifications
            restaurantNameMap[restaurant.id] = restaurant.name
            
            GeofenceRegion(
                id = restaurant.id,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                radiusMeters = radiusMeters,
                name = restaurant.name
            )
        }
        
        // Update monitored set
        _monitoredRestaurants.value = regions.map { it.id }.toSet()
        
        // Start monitoring
        geofencingService.startMonitoring(regions)
        
        println("GeofenceRepository: Started monitoring ${regions.size} restaurant geofences")
    }
    
    /**
     * Stop monitoring all restaurant geofences
     */
    fun stopMonitoringAll() {
        geofencingService?.stopMonitoring()
        _monitoredRestaurants.value = emptySet()
        restaurantNameMap.clear()
        println("GeofenceRepository: Stopped all geofence monitoring")
    }
    
    /**
     * Stop monitoring a specific restaurant
     */
    fun stopMonitoringRestaurant(restaurantId: String) {
        geofencingService?.stopMonitoringRegion(restaurantId)
        _monitoredRestaurants.value = _monitoredRestaurants.value - restaurantId
        restaurantNameMap.remove(restaurantId)
    }
    
    /**
     * Called when user enters a geofenced region
     * This should be called from the platform-specific broadcast receiver
     */
    fun onGeofenceEntered(regionId: String) {
        val restaurantName = restaurantNameMap[regionId] ?: "Restaurant"
        _lastEvent.value = GeofenceEvent.Entered(regionId, restaurantName)
        println("GeofenceRepository: User entered geofence for $restaurantName")
    }
    
    /**
     * Called when user exits a geofenced region
     * This should be called from the platform-specific broadcast receiver
     */
    fun onGeofenceExited(regionId: String) {
        val restaurantName = restaurantNameMap[regionId] ?: "Restaurant"
        _lastEvent.value = GeofenceEvent.Exited(regionId, restaurantName)
        println("GeofenceRepository: User exited geofence for $restaurantName")
    }
    
    /**
     * Clear the last event after it's been handled
     */
    fun clearLastEvent() {
        _lastEvent.value = null
    }
    
    /**
     * Check if a restaurant is currently being monitored
     */
    fun isMonitoring(restaurantId: String): Boolean {
        return restaurantId in _monitoredRestaurants.value
    }
    
    /**
     * Get count of monitored restaurants
     */
    fun monitoredCount(): Int = _monitoredRestaurants.value.size
}
