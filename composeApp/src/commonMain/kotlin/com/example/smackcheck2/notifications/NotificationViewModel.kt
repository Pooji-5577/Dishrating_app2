package com.example.smackcheck2.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.repository.NotificationService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SmackCheck - Notification ViewModel
 *
 * Kotlin port of useNotifications.ts hook, using MVVM pattern.
 * Manages push notification state: token, notification list, unread count,
 * mark-as-read, and logout cleanup.
 */
class NotificationViewModel : ViewModel() {

    private val notificationService = NotificationService()

    // ─── State ───────────────────────────────────────────────────

    private val _pushToken = MutableStateFlow<String?>(null)
    val pushToken: StateFlow<String?> = _pushToken.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val notifications: StateFlow<List<NotificationRecord>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private fun currentUserId(): String? =
        SupabaseClientProvider.client.auth.currentUserOrNull()?.id

    // ─── Initialize Push Notifications ───────────────────────────

    /**
     * Complete push notification setup:
     * 1. Setup notification channels (Android)
     * 2. Request permissions and get push token
     * 3. Save token to Supabase
     *
     * Call this once when the user is authenticated.
     */
    fun initializePushNotifications() {
        viewModelScope.launch {
            try {
                // 1. Setup platform notification channels
                setupNotificationChannels()

                // 2. Check if device supports push
                if (!isPushNotificationSupported()) {
                    _error.value = "Push notifications require a physical device."
                    return@launch
                }

                // 3. Register and get token
                val result = registerForPushNotifications()

                if (result.success && result.token != null) {
                    _pushToken.value = result.token

                    // 4. Save token to Supabase
                    val saveResult = notificationService.savePushToken(result.token)
                    if (saveResult.isFailure) {
                        println("Token generated but failed to save: ${saveResult.exceptionOrNull()?.message}")
                    }
                } else if (result.error != null) {
                    _error.value = result.error
                    println("Push notification init failed: ${result.error}")
                }
            } catch (e: Exception) {
                _error.value = e.message
                println("Push notification init error: ${e.message}")
            }
        }
    }

    // ─── Fetch Notifications ─────────────────────────────────────

    /**
     * Refresh the notification list and unread count from Supabase.
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = currentUserId()
                if (userId == null) {
                    _isLoading.value = false
                    return@launch
                }
                val notifResult = notificationService.fetchNotifications(userId, limit = 30)
                val count = notificationService.getUnreadCount(userId)

                notifResult.onSuccess { list ->
                    _notifications.value = list
                }
                notifResult.onFailure { e ->
                    _error.value = e.message
                }

                _unreadCount.value = count
            } catch (e: Exception) {
                _error.value = e.message
                println("Failed to refresh notifications: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Mark As Read ────────────────────────────────────────────

    /**
     * Mark a single notification as read.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationService.markAsRead(notificationId)
            _unreadCount.value = maxOf(0, _unreadCount.value - 1)
            _notifications.value = _notifications.value.map { notif ->
                if (notif.id == notificationId) notif.copy(isRead = true) else notif
            }
        }
    }

    /**
     * Mark all notifications as read.
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = currentUserId() ?: return@launch
            notificationService.markAllAsRead(userId)
            _unreadCount.value = 0
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        }
    }

    // ─── Logout Cleanup ──────────────────────────────────────────

    /**
     * Remove push token and clear local notification state.
     * Call this when the user logs out.
     */
    fun cleanupOnLogout() {
        viewModelScope.launch {
            notificationService.removePushToken()
            _pushToken.value = null
            _notifications.value = emptyList()
            _unreadCount.value = 0
        }
    }

    // ─── Handle Notification Tap ─────────────────────────────────

    /**
     * Process a notification tap and return the navigation target.
     * Mark the notification as read automatically.
     */
    fun handleNotificationTap(notification: NotificationRecord): NotificationNavigationTarget? {
        // Mark as read
        markAsRead(notification.id)

        // Return navigation target
        return getNavigationTarget(notification.data)
    }
}
