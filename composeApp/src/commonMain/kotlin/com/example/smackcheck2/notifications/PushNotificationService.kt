package com.example.smackcheck2.notifications

/**
 * SmackCheck - Push Notification Service (Common expect declarations)
 *
 * Platform-specific implementations handle:
 * - Android: Firebase Cloud Messaging (FCM)
 * - iOS: Apple Push Notification Service (APNs)
 *
 * The common code calls these expect functions, and each platform
 * provides its own `actual` implementation.
 */

/**
 * Request notification permissions and register for push notifications.
 * Returns a [PushTokenResult] with the device token or an error.
 *
 * Android: Requests POST_NOTIFICATIONS permission (API 33+), then retrieves FCM token.
 * iOS: Requests APNs authorization, then retrieves device token.
 */
expect suspend fun registerForPushNotifications(): PushTokenResult

/**
 * Setup platform-specific notification channels.
 * Android: Creates notification channels (social, gamification, discovery).
 * iOS: No-op (iOS doesn't use channels).
 */
expect fun setupNotificationChannels()

/**
 * Check if the current device supports push notifications.
 * Returns false on simulators/emulators.
 */
expect fun isPushNotificationSupported(): Boolean
