package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.model.EditProfileUiState
import com.example.smackcheck2.model.User
import com.example.smackcheck2.util.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val storageRepository: StorageRepository = StorageRepository(),
    initialUser: User?
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState(
        name = initialUser?.name ?: "",
        bio = initialUser?.bio ?: "",
        profilePhotoUrl = initialUser?.profilePhotoUrl
    ))
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = initialUser?.id

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onBioChange(bio: String) {
        // Limit bio to 150 characters
        if (bio.length <= 150) {
            _uiState.update { it.copy(bio = bio) }
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
                    val message = ErrorHandler.handleError(error, "Upload Profile Photo")
                    _uiState.update {
                        it.copy(isUploadingPhoto = false, errorMessage = message)
                    }
                }
            )
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val state = _uiState.value

        // Validation
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name cannot be empty") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null, nameError = null) }

        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Not signed in. Please sign in and try again.") }
                return@launch
            }

            val updatedUser = currentUser.copy(
                name = state.name,
                bio = state.bio,
                profilePhotoUrl = state.profilePhotoUrl
            )

            val result = authRepository.updateProfile(updatedUser)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                    onSuccess()
                },
                onFailure = { error ->
                    val message = ErrorHandler.handleError(error, "Save Profile")
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = message)
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
