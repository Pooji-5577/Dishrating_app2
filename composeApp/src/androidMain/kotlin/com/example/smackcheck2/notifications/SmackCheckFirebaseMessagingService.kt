package com.example.smackcheck2.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.smackcheck2.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles incoming FCM push notifications and token refresh events.
 */
class SmackCheckFirebaseMessagingService : FirebaseMessagingService() {

    private val eventChannelMap = mapOf(
        "review_liked" to "social",
        "new_follower" to "social",
        "dish_comment" to "social",
        "comment_reply" to "social",
        "points_earned" to "gamification",
        "challenge_completed" to "gamification",
        "level_up" to "gamification",
        "badge_earned" to "gamification",
        "trending_dish" to "discovery",
        "nearby_restaurant" to "discovery",
        "geofence_enter" to "discovery",
    )

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: return
        val body = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: return
        val eventType = remoteMessage.data["eventType"]
        val channelId = remoteMessage.data["channelId"]
            ?: eventChannelMap[eventType]
            ?: "default"
        showSystemNotification(title, body, channelId)
    }

    override fun onNewToken(token: String) {
        println("SmackCheckFCM: New token received, saving to Supabase...")
        CoroutineScope(Dispatchers.IO).launch {
            NotificationRepository.savePushTokenToSupabase(token)
        }
    }

    private fun showSystemNotification(title: String, body: String, channelId: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
