package com.example.smackcheck2.platform

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.repository.SocialMapRepository
import com.example.smackcheck2.location.AppLocationManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android implementation of AutoLocationManager
 * Uses ProcessLifecycleOwner to observe when app comes to foreground
 */
actual class AutoLocationManager(private val context: Context) {
    
    private val TAG = "AutoLocationManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val socialMapRepository = SocialMapRepository()
    
    // Debouncing: Don't detect location more than once per 5 minutes
    private val DEBOUNCE_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    private var lastDetectionTime: Long = 0
    
    private var isObserving = false
    
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            Log.d(TAG, "App resumed - checking for automatic location detection")
            scope.launch {
                checkAndDetectLocation()
            }
        }
    }
    
    /**
     * Start observing app lifecycle events for automatic location detection
     */
    actual fun startAutoDetection() {
        if (isObserving) {
            Log.d(TAG, "AutoDetection already started")
            return
        }
        
        Log.d(TAG, "Starting automatic location detection")
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        isObserving = true
    }
    
    /**
     * Stop observing app lifecycle events
     */
    actual fun stopAutoDetection() {
        if (!isObserving) {
            Log.d(TAG, "AutoDetection not started")
            return
        }
        
        Log.d(TAG, "Stopping automatic location detection")
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
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
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDetectionTime < DEBOUNCE_INTERVAL_MS) {
            val remainingMinutes = ((DEBOUNCE_INTERVAL_MS - (currentTime - lastDetectionTime)) / 60000).toInt()
            Log.d(TAG, "Skipping location detection - debounce active ($remainingMinutes min remaining)")
            return false
        }
        
        // Check if user is authenticated
        val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            Log.d(TAG, "Skipping location detection - user not authenticated")
            return false
        }
        
        // Check if location permission is granted
        if (!AppLocationManager.hasPermission()) {
            Log.d(TAG, "Skipping location detection - permission not granted")
            return false
        }
        
        // Check if location sharing is enabled in user's profile
        val locationSharingEnabled = isLocationSharingEnabled()
        if (!locationSharingEnabled) {
            Log.d(TAG, "Skipping location detection - user has disabled location sharing")
            return false
        }
        
        // All conditions met - trigger location detection
        Log.d(TAG, "Starting automatic location detection")
        lastDetectionTime = currentTime
        
        try {
            AppLocationManager.detectLocation()
            Log.d(TAG, "Automatic location detection triggered successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during automatic location detection: ${e.message}", e)
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
            Log.e(TAG, "Error checking location sharing status: ${e.message}", e)
            true // Default to true on error to not block feature
        }
    }
    
    /**
     * Check if auto-detection is currently active
     */
    actual fun isActive(): Boolean = isObserving
}
