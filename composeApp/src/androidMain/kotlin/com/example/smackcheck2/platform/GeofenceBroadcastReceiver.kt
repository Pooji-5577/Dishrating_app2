package com.example.smackcheck2.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.RestaurantVisitDto
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "GeofenceReceiver"
private const val CHANNEL_ID = "geofence_channel"

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            Log.e(TAG, "Geofence error: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences ?: return

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                triggeringGeofences.forEach { geofence ->
                    Log.d(TAG, "Entered geofence: ${geofence.requestId}")
                    showNotification(
                        context,
                        "Restaurant Nearby!",
                        "You're at a restaurant. Rate a dish?"
                    )
                    recordVisitStart(geofence.requestId)
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                triggeringGeofences.forEach { geofence ->
                    Log.d(TAG, "Exited geofence: ${geofence.requestId}")
                    recordVisitEnd(geofence.requestId)
                }
            }
        }
    }

    private fun recordVisitStart(restaurantId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@launch
                val dto = RestaurantVisitDto(
                    userId = userId,
                    restaurantId = restaurantId
                )
                SupabaseClientProvider.client.postgrest["restaurant_visits"].insert(dto)
                Log.d(TAG, "Recorded visit start for restaurant: $restaurantId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record visit", e)
            }
        }
    }

    private fun recordVisitEnd(restaurantId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@launch
                SupabaseClientProvider.client.postgrest["restaurant_visits"]
                    .update(mapOf("exited_at" to "now()")) {
                        filter {
                            eq("user_id", userId)
                            eq("restaurant_id", restaurantId)
                            exact("exited_at", null)
                        }
                    }
                Log.d(TAG, "Recorded visit end for restaurant: $restaurantId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record visit end", e)
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Restaurant Visits",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
