package com.example.smackcheck2

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.example.smackcheck2.platform.AutoLocationManager
import com.example.smackcheck2.platform.GeofencingService
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.PreferencesManager
import com.example.smackcheck2.platform.ShareService

// Global AutoLocationManager instance for iOS
private val autoLocationManager = AutoLocationManager()

fun MainViewController() = ComposeUIViewController {
    // Start automatic location detection on first composition
    remember {
        autoLocationManager.startAutoDetection()
    }
    
    val preferencesManager = PreferencesManager()
    val locationService = remember { LocationService() }
    val imagePicker = remember { ImagePicker() }
    val placesService = remember { PlacesService() }
    val shareService = remember { ShareService() }
    val geofencingService = remember { GeofencingService() }
    App(
        preferencesManager = preferencesManager,
        locationService = locationService,
        imagePicker = imagePicker,
        placesService = placesService,
        shareService = shareService,
        geofencingService = geofencingService
    )
}
