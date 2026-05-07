package com.example.smackcheck2.notifications

import com.example.smackcheck2.data.repository.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import kotlin.coroutines.resume

/**
 * SmackCheck - Push Notification Service (iOS)
 *
 * Uses Apple Push Notification Service (APNs) for push token registration.
 * The actual push sending is handled server-side by Supabase Edge Functions
 * + database webhooks.
 *
 * Prerequisites:
 * 1. Enable Push Notifications capability in Xcode
 * 2. Create an APNs key in Apple Developer Portal
 * 3. Upload the APNs key to your Supabase project or configure
 *    FCM with your APNs key for cross-platform support
 *
 * Note: The actual APNs device token is delivered to AppDelegate's
 * didRegisterForRemoteNotificationsWithDeviceToken callback.
 * You need to bridge that token back to Kotlin via IosPushTokenHolder.
 */

actual suspend fun registerForPushNotifications(): PushTokenResult {
    return suspendCancellableCoroutine { continuation ->
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
        ) { granted, error ->
            if (granted) {
                // Request remote notification registration on main thread
                // Note: registerForRemoteNotifications is handled at the Swift/AppDelegate level
                // The Kotlin side just requests authorization
                // Note: The actual device token is received asynchronously in AppDelegate's
                // application:didRegisterForRemoteNotificationsWithDeviceToken:
                // The token will be saved to Supabase from there via IosPushTokenHolder.
                // Return success - if token is already available, include it.
                continuation.resume(
                    PushTokenResult(
                        success = true,
                        token = IosPushTokenHolder.currentToken,
                        error = null
                    )
                )
            } else {
                val message = error?.localizedDescription ?: "Notification permission denied"
                println("Push notifications: APNs authorization denied - $message")
                continuation.resume(
                    PushTokenResult(
                        success = false,
                        token = null,
                        error = message
                    )
                )
            }
        }
    }
}

actual fun setupNotificationChannels() {
    // iOS does not use notification channels (Android-only concept)
    // No-op
}

actual fun isPushNotificationSupported(): Boolean {
    // On a real iOS device, push is supported.
    // On the simulator, push registration will fail gracefully.
    return true
}

/**
 * Holder for the iOS APNs device token.
 * Set this from the Swift AppDelegate when the token is received:
 *
 * ```swift
 * func application(_ application: UIApplication,
 *     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
 *     let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
 *     IosPushTokenHolder.companion.setToken(token: tokenString)
 * }
 * ```
 */
object IosPushTokenHolder {
    var currentToken: String? = null
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Call this from Swift AppDelegate when the APNs device token is received.
     * Automatically saves the token to Supabase.
     */
    fun setToken(token: String) {
        currentToken = token
        println("APNs device token received: $token")
        // Auto-save to Supabase when token is set
        scope.launch {
            try {
                val result = NotificationService().savePushToken(token)
                result.fold(
                    onSuccess = { println("APNs token saved to Supabase successfully") },
                    onFailure = { println("Failed to save APNs token to Supabase: ${it.message}") }
                )
            } catch (e: Exception) {
                println("Error saving APNs token: ${e.message}")
            }
        }
    }
}
