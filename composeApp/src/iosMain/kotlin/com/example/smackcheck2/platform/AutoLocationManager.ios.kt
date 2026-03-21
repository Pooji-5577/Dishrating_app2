package com.example.smackcheck2.platform

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.repository.SocialMapRepository
import io.github.jan.supabase.auth.auth
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObjectProtocol

/**
 * iOS implementation of AutoLocationManager
 * Uses NotificationCenter to observe when app comes to foreground
 */
@OptIn(ExperimentalForeignApi::class)
actual class AutoLocationManager {
    
    private val TAG = "AutoLocationManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val socialMapRepository = SocialMapRepository()
    private val locationService = LocationService()
    
    // Debouncing: Don't detect location more than once per 5 minutes
    private val DEBOUNCE_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    private var lastDetectionTime: Long = 0
    
    private var isObserving = false
    private var observer: NSObjectProtocol? = null
    
    /**
     * Start observing app lifecycle events for automatic location detection
     */
    actual fun startAutoDetection() {
        if (isObserving) {
            println("$TAG: AutoDetection already started")
            return
        }
        
        println("$TAG: Starting automatic location detection")
        
        // Observe UIApplicationWillEnterForegroundNotification
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                println("$TAG: App will enter foreground - checking for automatic location detection")
                scope.launch {
                    checkAndDetectLocation()
                }
            }
        )
        
        isObserving = true
    }
    
    /**
     * Stop observing app lifecycle events
     */
    actual fun stopAutoDetection() {
        if (!isObserving) {
            println("$TAG: AutoDetection not started")
            return
        }
        
        println("$TAG: Stopping automatic location detection")
        observer?.let { 
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        observer = null
        isObserving = false
    }
    
    /**
     * Check conditions and detect location if all requirements are met
     * 
     * Conditions checked:
     * 1. User is authenticated
     * 2. Location permission is granted
     * 3. Location sharing is enabled in user's profile
     * 4. Sufficient time has passed since last detection (5 minutes)
     * 
     * @return true if detection was triggered, false otherwise
     */
    actual suspend fun checkAndDetectLocation(): Boolean {
        // Check if debounce interval has passed
        val currentTime = (NSDate().timeIntervalSince1970 * 1000).toLong()
        if (currentTime - lastDetectionTime < DEBOUNCE_INTERVAL_MS) {
            val remainingMinutes = ((DEBOUNCE_INTERVAL_MS - (currentTime - lastDetectionTime)) / 60000).toInt()
            println("$TAG: Skipping location detection - debounce active ($remainingMinutes min remaining)")
            return false
        }
        
        // Check if user is authenticated
        val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            println("$TAG: Skipping location detection - user not authenticated")
            return false
        }
        
        // Check if location permission is granted
        if (!locationService.hasLocationPermission()) {
            println("$TAG: Skipping location detection - permission not granted")
            return false
        }
        
        // Check if location sharing is enabled in user's profile
        val locationSharingEnabled = isLocationSharingEnabled()
        if (!locationSharingEnabled) {
            println("$TAG: Skipping location detection - user has disabled location sharing")
            return false
        }
        
        // All conditions met - trigger location detection
        println("$TAG: Starting automatic location detection")
        lastDetectionTime = currentTime
        
        try {
            // Get location
            val locationResult = locationService.getCurrentLocationWithDetails()
            
            when (locationResult) {
                is LocationOperationResult.Success -> {
                    val location = locationResult.location
                    println("$TAG: Location detected: ${location.latitude}, ${location.longitude}, ${location.cityName}")
                    
                    // Sync to Supabase
                    socialMapRepository.updateUserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    
                    println("$TAG: Automatic location detection completed successfully")
                    return true
                }
                is LocationOperationResult.Error -> {
                    println("$TAG: Location detection failed: ${locationResult.reason}")
                    return false
                }
            }
        } catch (e: Exception) {
            println("$TAG: Error during automatic location detection: ${e.message}")
            return false
        }
    }
    
    /**
     * Check if location sharing is enabled for the current user
     * Queries the user's profile from Supabase
     */
    private suspend fun isLocationSharingEnabled(): Boolean {
        return try {
            val profile = socialMapRepository.getCurrentUserMapProfile()
            profile.getOrNull()?.locationSharingEnabled ?: true // Default to true if not set
        } catch (e: Exception) {
            println("$TAG: Error checking location sharing status: ${e.message}")
            true // Default to true on error to not block feature
        }
    }
    
    /**
     * Check if auto-detection is currently active
     */
    actual fun isActive(): Boolean = isObserving
}
