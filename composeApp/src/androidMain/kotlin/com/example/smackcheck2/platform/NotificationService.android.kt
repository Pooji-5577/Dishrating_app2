package com.example.smackcheck2.platform

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

actual class NotificationService(private val context: Context) {
    actual suspend fun requestPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }
}
