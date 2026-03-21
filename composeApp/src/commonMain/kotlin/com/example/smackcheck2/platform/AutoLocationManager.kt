package com.example.smackcheck2.platform

/**
 * AutoLocationManager - Automatically detects user location when app opens/resumes
 * 
 * This manager observes app lifecycle events and silently detects location when:
 * - The app comes to foreground (resumes)
 * - Location permission is already granted
 * - User has location sharing enabled in their profile
 * - Sufficient time has passed since last detection (debouncing)
 * 
 * Platform-specific implementations:
 * - Android: Uses ProcessLifecycleOwner to observe app foreground state
 * - iOS: Uses NotificationCenter to observe willEnterForeground events
 */
expect class AutoLocationManager {
    
    /**
     * Start observing app lifecycle events for automatic location detection
     * Should be called once during app initialization (e.g., in MainActivity.onCreate)
     */
    fun startAutoDetection()
    
    /**
     * Stop observing app lifecycle events
     * Should be called during cleanup (e.g., in Activity.onDestroy)
     */
    fun stopAutoDetection()
    
    /**
     * Manually trigger location detection (bypasses debouncing)
     * Used for testing or explicit user actions
     * 
     * @return true if detection was triggered, false if conditions not met
     */
    suspend fun checkAndDetectLocation(): Boolean
    
    /**
     * Check if auto-detection is currently active
     */
    fun isActive(): Boolean
}
