package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.data.repository.ServerSettingsRepository
import com.example.smackcheck2.model.NotificationSettings
import com.example.smackcheck2.model.NotificationSettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val serverSettingsRepository: ServerSettingsRepository = ServerSettingsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val localSettings = preferencesRepository.getAppPreferences().notificationSettings
                val settings = serverSettingsRepository.getNotificationSettings().getOrElse { localSettings }
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun updateSetting(updater: (NotificationSettings) -> NotificationSettings) {
        val newSettings = updater(_uiState.value.settings)
        _uiState.update { it.copy(settings = newSettings, isSaving = true) }

        viewModelScope.launch {
            val localResult = preferencesRepository.saveNotificationSettings(newSettings)
            val serverResult = serverSettingsRepository.saveNotificationSettings(newSettings)
            val result = if (serverResult.isSuccess) localResult else serverResult
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false) } },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
