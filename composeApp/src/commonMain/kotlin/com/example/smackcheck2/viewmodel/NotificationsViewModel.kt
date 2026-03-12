package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.model.NotificationsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val socialRepository = SocialRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            socialRepository.getNotifications(userId).fold(
                onSuccess = { notifications ->
                    val unread = socialRepository.getUnreadNotificationCount(userId)
                    _uiState.update {
                        it.copy(
                            notifications = notifications,
                            unreadCount = unread,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load notifications"
                        )
                    }
                }
            )
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            socialRepository.markNotificationAsRead(notificationId)
            _uiState.update { state ->
                val updated = state.notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                state.copy(
                    notifications = updated,
                    unreadCount = updated.count { !it.isRead }
                )
            }
        }
    }

    fun markAllAsRead() {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            socialRepository.markAllNotificationsAsRead(userId)
            _uiState.update { state ->
                val updated = state.notifications.map { it.copy(isRead = true) }
                state.copy(notifications = updated, unreadCount = 0)
            }
        }
    }
}
