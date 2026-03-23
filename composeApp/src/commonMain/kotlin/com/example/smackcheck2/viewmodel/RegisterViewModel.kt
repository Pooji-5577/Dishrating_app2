package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.model.RegisterUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")

/**
 * ViewModel for Register screen
 */
class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
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
            checkUsernameAvailability(username)
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun register(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        var hasError = false
        var nameError: String? = null
        var usernameError: String? = null
        var emailError: String? = null
        var passwordError: String? = null
        var confirmPasswordError: String? = null

        if (currentState.name.isBlank()) {
            nameError = "Name is required"
            hasError = true
        } else if (currentState.name.length < 2) {
            nameError = "Name must be at least 2 characters"
            hasError = true
        }

        if (currentState.username.isBlank()) {
            usernameError = "Username is required"
            hasError = true
        } else {
            val formatError = validateUsernameFormat(currentState.username)
            if (formatError != null) {
                usernameError = formatError
                hasError = true
            } else if (currentState.usernameAvailable != true) {
                usernameError = if (currentState.usernameAvailable == false)
                    "That username is already taken"
                else
                    "Please wait while we check username availability"
                hasError = true
            }
        }

        if (currentState.email.isBlank()) {
            emailError = "Email is required"
            hasError = true
        } else if (!isValidEmail(currentState.email)) {
            emailError = "Invalid email format"
            hasError = true
        }

        if (currentState.password.isBlank()) {
            passwordError = "Password is required"
            hasError = true
        } else if (currentState.password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            hasError = true
        } else if (!hasUpperCase(currentState.password)) {
            passwordError = "Password must contain at least one uppercase letter"
            hasError = true
        } else if (!hasDigit(currentState.password)) {
            passwordError = "Password must contain at least one digit"
            hasError = true
        }

        if (currentState.confirmPassword.isBlank()) {
            confirmPasswordError = "Please confirm your password"
            hasError = true
        } else if (currentState.password != currentState.confirmPassword) {
            confirmPasswordError = "Passwords do not match"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    usernameError = usernameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = authRepository.signUp(
                    name = currentState.name,
                    username = currentState.username,
                    email = currentState.email,
                    password = currentState.password
                )

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        onSuccess()
                    },
                    onFailure = { error ->
                        if (error.message == "CHECK_EMAIL") {
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                            onSuccess()
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Registration failed"
                                )
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Registration failed"
                    )
                }
            }
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun setSuccess(isSuccess: Boolean) {
        _uiState.update { it.copy(isSuccess = isSuccess) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
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

    private fun validateUsernameFormat(username: String): String? {
        return when {
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 20 -> "Username must be 20 characters or fewer"
            !USERNAME_REGEX.matches(username) -> "Username can only contain letters, numbers, and underscores"
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun hasUpperCase(password: String): Boolean {
        return password.any { it.isUpperCase() }
    }

    private fun hasDigit(password: String): Boolean {
        return password.any { it.isDigit() }
    }
}
