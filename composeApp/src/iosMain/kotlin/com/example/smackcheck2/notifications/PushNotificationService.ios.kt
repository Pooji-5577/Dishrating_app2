package com.example.smackcheck2.notifications

/**
 * SmackCheck - Push Notification Service (iOS)
 *
 * Uses Apple Push Notification Service (APNs) for push token registration.
 *
 * Prerequisites:
 * 1. Enable Push Notifications capability in Xcode
 * 2. Create APNs key in Apple Developer Portal
 * 3. Upload the APNs key to your Supabase project (or your push provider)
 *
 * TODO: Uncomment the APNs code below once you configure push in Xcode.
 * For now, this provides a stub implementation that won't crash the app.
 */

// TODO: Uncomment these imports once APNs is configured:
// import platform.UserNotifications.UNUserNotificationCenter
// import platform.UserNotifications.UNAuthorizationOptionAlert
// import platform.UserNotifications.UNAuthorizationOptionBadge
// import platform.UserNotifications.UNAuthorizationOptionSound
// import platform.UIKit.UIApplication
// import kotlinx.coroutines.suspendCancellableCoroutine
// import kotlin.coroutines.resume

actual suspend fun registerForPushNotifications(): PushTokenResult {
    // ── Stub implementation ──
    // Replace with APNs registration once push is configured in Xcode:
    //
    // return suspendCancellableCoroutine { continuation ->
    //     val center = UNUserNotificationCenter.currentNotificationCenter()
    //     center.requestAuthorizationWithOptions(
    //         UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
    //     ) { granted, error ->
    //         if (granted) {
    //             // Registration for remote notifications must happen on main thread
    //             dispatch_async(dispatch_get_main_queue()) {
    //                 UIApplication.sharedApplication.registerForRemoteNotifications()
    //             }
    //             // Note: The actual device token is received in AppDelegate
    //             // You'll need to pass it back to Kotlin via a callback or shared state
    //             continuation.resume(PushTokenResult(success = true, token = null, error = null))
    //         } else {
    //             val message = error?.localizedDescription ?: "Notification permission denied"
    //             continuation.resume(PushTokenResult(success = false, token = null, error = message))
    //         }
    //     }
    // }

    println("Push notifications: APNs not yet configured. Enable Push Notifications in Xcode.")
    return PushTokenResult(
        success = false,
        token = null,
        error = "APNs not yet configured. See PushNotificationService.ios.kt for setup instructions."
    )
}

actual fun setupNotificationChannels() {
    // iOS does not use notification channels (Android-only concept)
    // No-op
}

actual fun isPushNotificationSupported(): Boolean {
    // On a real iOS device, push is supported
    // On the simulator, push is NOT supported
    // TODO: Detect simulator vs device
    return true
}
