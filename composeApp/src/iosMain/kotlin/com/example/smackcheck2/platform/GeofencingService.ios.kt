package com.example.smackcheck2.platform

import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager

actual class GeofencingService {
    private val locationManager = CLLocationManager()

    actual fun startMonitoring(regions: List<GeofenceRegion>) {
        regions.forEach { region ->
            val clRegion = CLCircularRegion(
                center = CLLocationCoordinate2DMake(region.latitude, region.longitude),
                radius = region.radiusMeters.toDouble(),
                identifier = region.id
            )
            clRegion.notifyOnEntry = true
            clRegion.notifyOnExit = true
            locationManager.startMonitoringForRegion(clRegion)
        }
    }

    actual fun stopMonitoring() {
        locationManager.monitoredRegions.forEach { region ->
            if (region is CLCircularRegion) {
                locationManager.stopMonitoringForRegion(region)
            }
        }
    }

    actual fun stopMonitoringRegion(id: String) {
        locationManager.monitoredRegions.forEach { region ->
            if (region is CLCircularRegion && region.identifier == id) {
                locationManager.stopMonitoringForRegion(region)
            }
        }
    }
}
