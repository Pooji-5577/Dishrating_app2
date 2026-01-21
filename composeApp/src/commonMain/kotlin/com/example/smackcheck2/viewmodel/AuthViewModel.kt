package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.model.AuthState
import com.example.smackcheck2.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication state management
 */
class AuthViewModel : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            // Simulate checking auth state
            delay(2000)
            // For demo purposes, start as unauthenticated
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Simulate sign in
                delay(1500)
                if (email.isNotBlank() && password.isNotBlank()) {
                    _authState.value = AuthState.Authenticated(
                        User(
                            id = "user_123",
                            name = "John Doe",
                            email = email
                        )
                    )
                    onSuccess()
                } else {
                    onError("Invalid credentials")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Sign in failed")
            }
        }
    }
    
    fun signInWithGoogle(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Simulate Google sign in
                delay(1500)
                _authState.value = AuthState.Authenticated(
                    User(
                        id = "user_google_123",
                        name = "Google User",
                        email = "user@gmail.com"
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Google sign in failed")
            }
        }
    }
    
    fun register(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Simulate registration
                delay(1500)
                _authState.value = AuthState.Authenticated(
                    User(
                        id = "user_new_123",
                        name = name,
                        email = email
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Registration failed")
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun getCurrentUser(): User? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }
}
