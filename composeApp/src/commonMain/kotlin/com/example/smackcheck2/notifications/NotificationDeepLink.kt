package com.example.smackcheck2.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Target the app should navigate to when the user taps a push notification.
 *
 * The payload mirrors the `data` map sent by [NotificationRepository] (`screen`
 * + ids such as `dishId`, `reviewId`, `ratingId`). The navigation layer is
 * responsible for translating a target into an actual [com.example.smackcheck2.navigation.Screen].
 */
data class NotificationRouteTarget(
    val screen: String,
    val dishId: String? = null,
    val reviewId: String? = null,
    val ratingId: String? = null,
    val userId: String? = null
)

/**
 * Bridges platform-level notification taps (FCM intent extras on Android,
 * userInfo on iOS) into the shared Compose navigation state.
 *
 * The platform layer calls [push] with the parsed target; the NavHost
 * observes [pendingTarget] and calls [consume] after it has navigated so
 * the same tap isn't handled twice.
 */
object NotificationDeepLink {
    private val _pendingTarget = MutableStateFlow<NotificationRouteTarget?>(null)
    val pendingTarget: StateFlow<NotificationRouteTarget?> = _pendingTarget.asStateFlow()

    fun push(target: NotificationRouteTarget) {
        _pendingTarget.value = target
    }

    fun consume() {
        _pendingTarget.value = null
    }
}
