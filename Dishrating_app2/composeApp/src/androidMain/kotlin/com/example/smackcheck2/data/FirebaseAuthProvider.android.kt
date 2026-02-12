package com.example.smackcheck2.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.smackcheck2.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.ActionCodeSettings
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Android implementation of FirebaseAuthProvider
 */
actual class FirebaseAuthProvider(private val context: Context) {

    private val auth = Firebase.auth

    private val _authStateFlow = MutableStateFlow<FirebaseUser?>(null)
    actual val authStateFlow: StateFlow<FirebaseUser?> = _authStateFlow.asStateFlow()

    init {
        // Set initial value
        _authStateFlow.value = auth.currentUser?.toFirebaseUser()
    }

    actual fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser?.toFirebaseUser()
    }

    actual suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.toFirebaseUser())
            } else {
                AuthResult.Error("Failed to create user", "unknown")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed", extractErrorCode(e))
        }
    }

    actual suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password)
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.toFirebaseUser())
            } else {
                AuthResult.Error("Failed to sign in", "unknown")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed", extractErrorCode(e))
        }
    }

    actual suspend fun signInWithGoogle(): AuthResult {
        return try {
            val webClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID

            // Use Credential Manager API for Google Sign-In
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)

                    val idToken = googleIdTokenCredential.idToken

                    // Sign in to Firebase with Google ID token
                    val authCredential = GoogleAuthProvider.credential(idToken, null)
                    val authResult = auth.signInWithCredential(authCredential)

                    val user = authResult.user
                    if (user != null) {
                        AuthResult.Success(user.toFirebaseUser())
                    } else {
                        AuthResult.Error("Failed to sign in with Google", "unknown")
                    }
                } catch (e: GoogleIdTokenParsingException) {
                    AuthResult.Error("Invalid Google ID token: ${e.message}", "google_token_error")
                }
            } else {
                AuthResult.Error("Unexpected credential type", "credential_type_error")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google Sign-In failed", extractErrorCode(e))
        }
    }

    actual suspend fun signInWithFacebook(): AuthResult {
        return try {
            // Facebook Sign-In requires Activity context and LoginManager
            // This is a simplified implementation - in production, you'd handle this via Activity
            AuthResult.Error(
                "Facebook Sign-In requires implementation in Activity",
                "not_implemented"
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Facebook Sign-In failed", extractErrorCode(e))
        }
    }

    /**
     * Helper method for Facebook Sign-In that should be called from Activity
     */
    suspend fun signInWithFacebookToken(accessToken: String): AuthResult {
        return try {
            val credential = FacebookAuthProvider.credential(accessToken)
            val result = auth.signInWithCredential(credential)
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.toFirebaseUser())
            } else {
                AuthResult.Error("Failed to sign in with Facebook", "unknown")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Facebook Sign-In failed", extractErrorCode(e))
        }
    }

    actual suspend fun signInWithApple(): AuthResult {
        return try {
            // Apple Sign-In on Android uses Credential Manager with custom provider
            AuthResult.Error(
                "Apple Sign-In on Android requires additional configuration",
                "not_implemented"
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Apple Sign-In failed", extractErrorCode(e))
        }
    }

    actual suspend fun signOut() {
        auth.signOut()
    }

    actual suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            // Validate email format first
            if (email.isBlank() || !email.contains("@")) {
                Log.w("FirebaseAuth", "Invalid email format: $email")
                return AuthResult.Error("Invalid email format", "invalid-email")
            }

            Log.d("FirebaseAuth", "Sending password reset email to: $email")
            auth.sendPasswordResetEmail(email)
            Log.d("FirebaseAuth", "Password reset email request sent to Firebase for: $email")

            // IMPORTANT NOTE: Firebase API returns success even if:
            // 1. The email doesn't exist (for security reasons, to prevent email enumeration)
            // 2. Email delivery fails due to SMTP configuration issues
            // 3. The email goes to spam folder
            //
            // Users should:
            // - Check spam/junk folder
            // - Verify Firebase email settings in Firebase Console
            // - Wait a few minutes for delivery

            AuthResult.Success(FirebaseUser(
                uid = "",
                email = email,
                displayName = null,
                photoUrl = null
            ))
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Failed to send password reset email: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection and try again."
                e.message?.contains("too-many-requests", ignoreCase = true) == true ->
                    "Too many attempts. Please try again in a few minutes."
                e.message?.contains("user-not-found", ignoreCase = true) == true ->
                    "If an account exists with this email, a reset link will be sent."
                e.message?.contains("invalid-email", ignoreCase = true) == true ->
                    "Invalid email format. Please check and try again."
                else -> "Failed to send password reset email. ${e.message ?: ""}"
            }
            AuthResult.Error(errorMessage, extractErrorCode(e))
        }
    }

    actual suspend fun updateEmail(newEmail: String): AuthResult {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.updateEmail(newEmail)
                AuthResult.Success(currentUser.toFirebaseUser())
            } else {
                AuthResult.Error("No user signed in", "no_user")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to update email", extractErrorCode(e))
        }
    }

    actual suspend fun updatePassword(newPassword: String): AuthResult {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.updatePassword(newPassword)
                AuthResult.Success(currentUser.toFirebaseUser())
            } else {
                AuthResult.Error("No user signed in", "no_user")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to update password", extractErrorCode(e))
        }
    }

    actual suspend fun deleteAccount(): AuthResult {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userSnapshot = currentUser.toFirebaseUser()
                currentUser.delete()
                AuthResult.Success(userSnapshot)
            } else {
                AuthResult.Error("No user signed in", "no_user")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to delete account", extractErrorCode(e))
        }
    }

    actual suspend fun reauthenticate(email: String, password: String): AuthResult {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val credential = EmailAuthProvider.credential(email, password)
                currentUser.reauthenticate(credential)
                AuthResult.Success(currentUser.toFirebaseUser())
            } else {
                AuthResult.Error("No user signed in", "no_user")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Reauthentication failed", extractErrorCode(e))
        }
    }

    actual suspend fun sendEmailVerification(): AuthResult {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.sendEmailVerification()
                AuthResult.Success(currentUser.toFirebaseUser())
            } else {
                AuthResult.Error("No user signed in", "no_user")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send verification email", extractErrorCode(e))
        }
    }

    actual suspend fun reloadUser(): AuthResult {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.reload()
                AuthResult.Success(currentUser.toFirebaseUser())
            } else {
                AuthResult.Error("No user signed in", "no_user")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to reload user", extractErrorCode(e))
        }
    }

    /**
     * Convert Firebase user to FirebaseUser data class
     */
    private fun dev.gitlive.firebase.auth.FirebaseUser.toFirebaseUser(): FirebaseUser {
        return FirebaseUser(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoURL,
            isEmailVerified = isEmailVerified
        )
    }

    /**
     * Extract error code from exception
     */
    private fun extractErrorCode(exception: Exception): String {
        return exception::class.simpleName ?: "unknown"
    }
}

/**
 * Factory function to create Android FirebaseAuthProvider
 */
actual fun createFirebaseAuthProvider(): FirebaseAuthProvider {
    // This will need to be called with context
    throw IllegalStateException("Use createFirebaseAuthProvider(context) for Android")
}

/**
 * Android-specific factory function with context
 */
fun createFirebaseAuthProvider(context: Context): FirebaseAuthProvider {
    return FirebaseAuthProvider(context)
}
