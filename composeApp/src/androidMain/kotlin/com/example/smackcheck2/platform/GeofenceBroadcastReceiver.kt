package com.example.smackcheck2.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smackcheck2.data.ApiClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
                ApiClient.post<VisitStartRequest, VisitResponse>(
                    "visits",
                    VisitStartRequest(restaurantId = restaurantId)
                )
                Log.d(TAG, "Recorded visit start for restaurant: $restaurantId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record visit", e)
            }
        }
    }

    private fun recordVisitEnd(restaurantId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.patch<VisitEndRequest, VisitResponse>(
                    "visits/end",
                    VisitEndRequest(restaurantId = restaurantId)
                )
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

@Serializable
private data class VisitStartRequest(
    @SerialName("restaurant_id") val restaurantId: String
)

@Serializable
private data class VisitEndRequest(
    @SerialName("restaurant_id") val restaurantId: String
)

@Serializable
private data class VisitResponse(
    val id: String? = null,
    val success: Boolean? = null,
    val updated: Boolean? = null
)
