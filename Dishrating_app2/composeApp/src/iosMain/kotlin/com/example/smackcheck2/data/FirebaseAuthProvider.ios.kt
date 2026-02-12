package com.example.smackcheck2.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AuthenticationServices.*
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of FirebaseAuthProvider
 */
actual class FirebaseAuthProvider {

    private val auth = Firebase.auth

    private val _authStateFlow = MutableStateFlow<FirebaseUser?>(null)
    actual val authStateFlow: StateFlow<FirebaseUser?> = _authStateFlow.asStateFlow()

    init {
        // Initialize auth state listener
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authStateFlow.value = user?.toFirebaseUser()
        }
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
            // Google Sign-In on iOS requires native implementation
            // This is a placeholder - actual implementation would use GIDSignIn
            AuthResult.Error(
                "Google Sign-In requires native iOS implementation with GIDSignIn",
                "not_implemented"
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google Sign-In failed", extractErrorCode(e))
        }
    }

    /**
     * Helper method for Google Sign-In with ID token
     */
    suspend fun signInWithGoogleToken(idToken: String, accessToken: String?): AuthResult {
        return try {
            val credential = GoogleAuthProvider.credential(idToken, accessToken)
            val result = auth.signInWithCredential(credential)
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.toFirebaseUser())
            } else {
                AuthResult.Error("Failed to sign in with Google", "unknown")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google Sign-In failed", extractErrorCode(e))
        }
    }

    actual suspend fun signInWithFacebook(): AuthResult {
        return try {
            // Facebook Sign-In on iOS requires native implementation
            AuthResult.Error(
                "Facebook Sign-In requires native iOS implementation",
                "not_implemented"
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Facebook Sign-In failed", extractErrorCode(e))
        }
    }

    /**
     * Helper method for Facebook Sign-In with access token
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
            // Apple Sign-In using ASAuthorizationController
            val appleIDProvider = ASAuthorizationAppleIDProvider()
            val request = appleIDProvider.createRequest()
            request.requestedScopes = listOf(
                ASAuthorizationScopeFullName,
                ASAuthorizationScopeEmail
            )

            // This requires implementation with ASAuthorizationControllerDelegate
            // and ASAuthorizationControllerPresentationContextProviding
            AuthResult.Error(
                "Apple Sign-In requires native iOS implementation with ASAuthorizationController",
                "not_implemented"
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Apple Sign-In failed", extractErrorCode(e))
        }
    }

    /**
     * Helper method for Apple Sign-In with credential
     */
    suspend fun signInWithAppleCredential(
        idToken: String,
        rawNonce: String,
        fullName: String? = null
    ): AuthResult {
        return try {
            val credential = dev.gitlive.firebase.auth.OAuthProvider.credential(
                providerId = "apple.com",
                idToken = idToken,
                rawNonce = rawNonce
            )
            val result = auth.signInWithCredential(credential)
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.toFirebaseUser())
            } else {
                AuthResult.Error("Failed to sign in with Apple", "unknown")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Apple Sign-In failed", extractErrorCode(e))
        }
    }

    actual suspend fun signOut() {
        auth.signOut()
    }

    actual suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email)
            AuthResult.Success(
                FirebaseUser(
                    uid = "",
                    email = email,
                    displayName = null,
                    photoUrl = null
                )
            )
        } catch (e: Exception) {
            AuthResult.Error(
                e.message ?: "Failed to send password reset email",
                extractErrorCode(e)
            )
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
            AuthResult.Error(
                e.message ?: "Failed to send verification email",
                extractErrorCode(e)
            )
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
 * Factory function to create iOS FirebaseAuthProvider
 */
actual fun createFirebaseAuthProvider(): FirebaseAuthProvider {
    return FirebaseAuthProvider()
}
