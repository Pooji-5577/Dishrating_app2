package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.model.ProfileSetupUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")

class ProfileSetupViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null
    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUser()?.id
        }
    }

    fun onUsernameChange(username: String) {
        _uiState.update {
            it.copy(
                username = username,
                usernameError = null,
                usernameAvailable = null,
                isCheckingUsername = false
            )
        }
        usernameCheckJob?.cancel()
        if (username.isBlank()) return

        val formatError = validateUsernameFormat(username)
        if (formatError != null) {
            _uiState.update { it.copy(usernameError = formatError) }
            return
        }

        usernameCheckJob = viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(isCheckingUsername = true, usernameError = null) }
            val result = authRepository.checkUsernameAvailable(username)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isCheckingUsername = false, usernameAvailable = true, usernameError = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingUsername = false,
                            usernameAvailable = false,
                            usernameError = error.message
                        )
                    }
                }
            )
        }
    }

    fun uploadProfilePhoto(imageBytes: ByteArray, fileName: String) {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Not signed in. Please sign in and try again.") }
            return
        }

        _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }

        viewModelScope.launch {
            val result = storageRepository.uploadProfileImage(userId, imageBytes, fileName)
            result.fold(
                onSuccess = { url ->
                    _uiState.update { it.copy(profilePhotoUrl = url, isUploadingPhoto = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isUploadingPhoto = false, errorMessage = error.message ?: "Photo upload failed")
                    }
                }
            )
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Username is required") }
            return
        }

        val formatError = validateUsernameFormat(state.username)
        if (formatError != null) {
            _uiState.update { it.copy(usernameError = formatError) }
            return
        }

        if (state.usernameAvailable != true) {
            _uiState.update {
                it.copy(usernameError = if (state.usernameAvailable == false)
                    "That username is already taken"
                else
                    "Please wait while we check username availability"
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val userId = currentUserId ?: authRepository.getCurrentUser()?.id
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Not signed in. Please sign in and try again.") }
                return@launch
            }

            val result = authRepository.saveProfileSetup(userId, state.username, state.profilePhotoUrl)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = error.message ?: "Failed to save profile")
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun validateUsernameFormat(username: String): String? {
        return when {
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 20 -> "Username must be 20 characters or fewer"
            !USERNAME_REGEX.matches(username) -> "Username can only contain letters, numbers, and underscores"
            else -> null
        }
    }
}
