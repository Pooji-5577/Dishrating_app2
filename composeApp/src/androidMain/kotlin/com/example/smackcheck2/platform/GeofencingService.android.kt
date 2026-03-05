package com.example.smackcheck2.platform

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

private const val TAG = "GeofencingService"

actual class GeofencingService(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    actual fun startMonitoring(regions: List<GeofenceRegion>) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Missing location permission, cannot start geofencing")
            return
        }

        val geofences = regions.map { region ->
            Geofence.Builder()
                .setRequestId(region.id)
                .setCircularRegion(region.latitude, region.longitude, region.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        if (geofences.isEmpty()) return

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Added ${geofences.size} geofences")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add geofences", e)
            }
    }

    actual fun stopMonitoring() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Removed all geofences")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofences", e)
            }
    }

    actual fun stopMonitoringRegion(id: String) {
        geofencingClient.removeGeofences(listOf(id))
            .addOnSuccessListener {
                Log.d(TAG, "Removed geofence: $id")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofence: $id", e)
            }
    }
}
