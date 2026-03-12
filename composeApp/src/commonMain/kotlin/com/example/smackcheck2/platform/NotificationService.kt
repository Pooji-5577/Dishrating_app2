package com.example.smackcheck2.platform

import androidx.compose.runtime.staticCompositionLocalOf

expect class NotificationService {
    suspend fun requestPermission(): Boolean
}

val LocalNotificationService = staticCompositionLocalOf<NotificationService?> { null }
