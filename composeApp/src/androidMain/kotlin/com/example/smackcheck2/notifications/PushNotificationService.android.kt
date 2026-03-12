package com.example.smackcheck2.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * SmackCheck - Push Notification Service (Android)
 *
 * Uses Firebase Cloud Messaging (FCM) for push token registration.
 * The actual push sending is handled server-side by Supabase Edge Functions
 * + database webhooks. This client-side code only handles:
 * 1. Getting the FCM device token
 * 2. Setting up notification channels
 *
 * Prerequisites:
 * 1. Add google-services.json to composeApp/ directory
 *    (Download from Firebase Console > Project Settings > Your Apps > Android)
 * 2. Firebase dependencies are already added to build.gradle.kts
 * 3. google-services plugin is already applied
 */

actual suspend fun registerForPushNotifications(): PushTokenResult {
    return try {
        val token = FirebaseMessaging.getInstance().token.await()
        println("FCM push token generated: $token")
        PushTokenResult(success = true, token = token)
    } catch (e: Exception) {
        println("FCM token registration failed: ${e.message}")
        PushTokenResult(success = false, token = null, error = e.message)
    }
}

actual fun setupNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            // We need a Context to create notification channels.
            // Use the ApplicationContext from the SmackCheck Application class.
            val context = SmackCheckNotificationHelper.appContext ?: run {
                println("Push notifications: App context not available for notification channels.")
                return
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultChannel = NotificationChannel(
                "default", "Default",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Default notifications"
                lightColor = 0xFFFF6B35.toInt() // SmackCheck brand color
            }

            val socialChannel = NotificationChannel(
                "social", "Social",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Likes, comments, and social activity"
                lightColor = 0xFFFF6B35.toInt()
            }

            val gamificationChannel = NotificationChannel(
                "gamification", "Achievements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Points earned, challenges completed, streaks"
                lightColor = 0xFFFFD700.toInt()
            }

            val discoveryChannel = NotificationChannel(
                "discovery", "Food Discovery",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Trending dishes and recommendations near you"
                lightColor = 0xFF4CAF50.toInt()
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, socialChannel, gamificationChannel, discoveryChannel)
            )
            println("Push notifications: Notification channels created successfully.")
        } catch (e: Exception) {
            println("Push notifications: Failed to create notification channels: ${e.message}")
        }
    }
}

actual fun isPushNotificationSupported(): Boolean {
    // On a real Android device with Google Play Services, push is supported
    return true
}

/**
 * Helper object to hold the Application context for notification channel setup.
 * Set this in your Application.onCreate() or MainActivity.onCreate().
 */
object SmackCheckNotificationHelper {
    var appContext: Context? = null
}
