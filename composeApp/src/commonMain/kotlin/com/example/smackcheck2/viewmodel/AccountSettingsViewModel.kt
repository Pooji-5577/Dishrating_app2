package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.model.AccountSettingsUiState
import com.example.smackcheck2.util.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountSettingsViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()

    init {
        loadEmail()
    }

    private fun loadEmail() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.update { it.copy(email = user?.email ?: "") }
        }
    }

    fun changePassword(newPassword: String, onSuccess: () -> Unit) {
        if (newPassword.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters") }
            return
        }

        _uiState.update { it.copy(isChangingPassword = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = authRepository.updatePassword(newPassword)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            successMessage = "Password changed successfully"
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    val message = ErrorHandler.handleError(error, "Change Password")
                    _uiState.update {
                        it.copy(isChangingPassword = false, errorMessage = message)
                    }
                }
            )
        }
    }

    fun changeEmail(newEmail: String, onSuccess: () -> Unit) {
        if (!newEmail.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Invalid email address") }
            return
        }

        _uiState.update { it.copy(isChangingEmail = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = authRepository.updateEmail(newEmail)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isChangingEmail = false,
                            email = newEmail,
                            successMessage = "Email changed successfully. Please check your inbox to verify."
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    val message = ErrorHandler.handleError(error, "Change Email")
                    _uiState.update {
                        it.copy(isChangingEmail = false, errorMessage = message)
                    }
                }
            )
        }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isDeletingAccount = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeletingAccount = false, showDeleteConfirmation = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    val message = ErrorHandler.handleError(error, "Delete Account")
                    _uiState.update {
                        it.copy(isDeletingAccount = false, errorMessage = message, showDeleteConfirmation = false)
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
