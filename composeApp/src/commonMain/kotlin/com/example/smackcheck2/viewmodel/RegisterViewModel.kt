package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.model.RegisterUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Register screen
 */
class RegisterViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
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
        
        // Validate inputs
        var hasError = false
        var nameError: String? = null
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
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }
        
        // Perform registration
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Simulate API call
                delay(1500)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                onSuccess()
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
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
