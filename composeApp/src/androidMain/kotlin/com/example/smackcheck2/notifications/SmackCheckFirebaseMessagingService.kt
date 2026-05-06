package com.example.smackcheck2.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.smackcheck2.MainActivity
import com.example.smackcheck2.R
import com.example.smackcheck2.data.repository.NotificationService
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
        showSystemNotification(title, body, channelId, remoteMessage.data)
    }

    override fun onNewToken(token: String) {
        println("SmackCheckFCM: New token received, saving to Supabase...")
        CoroutineScope(Dispatchers.IO).launch {
            NotificationService().savePushToken(token)
        }
    }

    private fun showSystemNotification(
        title: String,
        body: String,
        channelId: String,
        data: Map<String, String>
    ) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildDeepLinkIntent(data))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Bundle the FCM data fields that describe a deep-link target into an
     * Intent that re-launches [MainActivity]. MainActivity extracts them and
     * forwards to [NotificationDeepLink] so the NavHost can route.
     */
    private fun buildDeepLinkIntent(data: Map<String, String>): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            data["screen"]?.let { putExtra(EXTRA_SCREEN, it) }
            data["dishId"]?.let { putExtra(EXTRA_DISH_ID, it) }
            data["reviewId"]?.let { putExtra(EXTRA_REVIEW_ID, it) }
            data["ratingId"]?.let { putExtra(EXTRA_RATING_ID, it) }
            data["userId"]?.let { putExtra(EXTRA_USER_ID, it) }
        }

        return PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_FROM_NOTIFICATION = "smackcheck.fromNotification"
        const val EXTRA_SCREEN = "smackcheck.screen"
        const val EXTRA_DISH_ID = "smackcheck.dishId"
        const val EXTRA_REVIEW_ID = "smackcheck.reviewId"
        const val EXTRA_RATING_ID = "smackcheck.ratingId"
        const val EXTRA_USER_ID = "smackcheck.userId"
    }
}
