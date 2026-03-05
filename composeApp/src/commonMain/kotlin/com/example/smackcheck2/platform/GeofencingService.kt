package com.example.smackcheck2.platform

import androidx.compose.runtime.staticCompositionLocalOf

data class GeofenceRegion(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val name: String
)

expect class GeofencingService {
    fun startMonitoring(regions: List<GeofenceRegion>)
    fun stopMonitoring()
    fun stopMonitoringRegion(id: String)
}

val LocalGeofencingService = staticCompositionLocalOf<GeofencingService?> { null }
