package com.example.smackcheck2.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Firebase User data class
 * Represents a user authenticated with Firebase
 */
data class FirebaseUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean = false
)

/**
 * Sealed class for authentication results
 */
sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String, val code: String? = null) : AuthResult()
}

/**
 * Firebase Authentication Provider interface
 * Uses expect/actual pattern for platform-specific implementations
 */
expect class FirebaseAuthProvider {

    /**
     * Observable auth state
     * Emits the current Firebase user or null if not authenticated
     */
    val authStateFlow: StateFlow<FirebaseUser?>

    /**
     * Get the current authenticated user
     * @return FirebaseUser if authenticated, null otherwise
     */
    fun getCurrentUser(): FirebaseUser?

    /**
     * Sign up with email and password
     * @param email User's email address
     * @param password User's password
     * @return AuthResult with user data or error
     */
    suspend fun signUpWithEmail(email: String, password: String): AuthResult

    /**
     * Sign in with email and password
     * @param email User's email address
     * @param password User's password
     * @return AuthResult with user data or error
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult

    /**
     * Sign in with Google
     * @return AuthResult with user data or error
     */
    suspend fun signInWithGoogle(): AuthResult

    /**
     * Sign in with Facebook
     * @return AuthResult with user data or error
     */
    suspend fun signInWithFacebook(): AuthResult

    /**
     * Sign in with Apple
     * @return AuthResult with user data or error
     */
    suspend fun signInWithApple(): AuthResult

    /**
     * Sign out the current user
     */
    suspend fun signOut()

    /**
     * Send password reset email
     * @param email User's email address
     * @return AuthResult indicating success or error
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult

    /**
     * Update user email
     * @param newEmail New email address
     * @return AuthResult indicating success or error
     */
    suspend fun updateEmail(newEmail: String): AuthResult

    /**
     * Update user password
     * @param newPassword New password
     * @return AuthResult indicating success or error
     */
    suspend fun updatePassword(newPassword: String): AuthResult

    /**
     * Delete the current user account
     * @return AuthResult indicating success or error
     */
    suspend fun deleteAccount(): AuthResult

    /**
     * Reauthenticate with email and password (required before sensitive operations)
     * @param email User's email address
     * @param password User's password
     * @return AuthResult indicating success or error
     */
    suspend fun reauthenticate(email: String, password: String): AuthResult

    /**
     * Send email verification to the current user
     * @return AuthResult indicating success or error
     */
    suspend fun sendEmailVerification(): AuthResult

    /**
     * Reload the current user to refresh data
     */
    suspend fun reloadUser(): AuthResult
}

/**
 * Factory function to create a platform-specific FirebaseAuthProvider
 */
expect fun createFirebaseAuthProvider(): FirebaseAuthProvider
