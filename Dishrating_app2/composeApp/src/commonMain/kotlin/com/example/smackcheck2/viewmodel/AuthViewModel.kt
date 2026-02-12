package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.FirebaseClientProvider
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.model.AuthState
import com.example.smackcheck2.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication state management
 */
class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Initialize Firebase session
        // Note: FirebaseClientProvider must be initialized in MainActivity/MainViewController
        FirebaseClientProvider.initializeSession()
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                // Firebase session restoration is instant (no delay needed)
                val user = authRepository.getCurrentUser()
                _authState.value = if (user != null) {
                    println("AuthViewModel: User restored from session: ${user.email}")
                    AuthState.Authenticated(user)
                } else {
                    println("AuthViewModel: No saved session found")
                    AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                println("AuthViewModel: Error checking auth state: ${e.message}")
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authRepository.signIn(email, password)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                        onSuccess()
                    },
                    onFailure = { error ->
                        onError(error.message ?: "Sign in failed")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Sign in failed")
            }
        }
    }

    fun signInWithGoogle(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithGoogle()
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                        onSuccess()
                    },
                    onFailure = { error ->
                        onError(error.message ?: "Google Sign-In failed")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Google Sign-In failed")
            }
        }
    }

    fun signInWithFacebook(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithFacebook()
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                        onSuccess()
                    },
                    onFailure = { error ->
                        onError(error.message ?: "Facebook Sign-In failed")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Facebook Sign-In failed")
            }
        }
    }

    fun signInWithApple(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithApple()
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                        onSuccess()
                    },
                    onFailure = { error ->
                        onError(error.message ?: "Apple Sign-In failed")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Apple Sign-In failed")
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
                val result = authRepository.signUp(name, email, password)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                        onSuccess()
                    },
                    onFailure = { error ->
                        onError(error.message ?: "Registration failed")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Registration failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun getCurrentUser(): User? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    /**
     * Sign in as demo user (no Supabase call)
     */
    fun signInAsDemo(onSuccess: () -> Unit) {
        val demoUser = User(
            id = "demo_user_001",
            name = "Demo User",
            email = "demo@smackcheck.com",
            level = 5,
            xp = 450,
            streakCount = 7
        )
        _authState.value = AuthState.Authenticated(demoUser)
        onSuccess()
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authRepository.resetPassword(email)
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { error -> onError(error.message ?: "Password reset failed") }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Password reset failed")
            }
        }
    }
}
