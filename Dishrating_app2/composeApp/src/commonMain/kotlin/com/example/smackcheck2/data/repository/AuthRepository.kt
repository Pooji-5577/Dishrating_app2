package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.AuthResult
import com.example.smackcheck2.data.FirebaseClientProvider
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.ProfileDto
import com.example.smackcheck2.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for authentication operations using Firebase Auth + Supabase Database
 *
 * Architecture:
 * - Firebase handles authentication (email/password, OAuth providers)
 * - Supabase handles user profiles and app data
 * - Firebase UID is used as the primary key in Supabase profiles table
 */
class AuthRepository {

    // Keep Supabase client for database operations
    private val supabaseClient = SupabaseClientProvider.client

    // Use Firebase for authentication
    private val firebaseAuth
        get() = FirebaseClientProvider.authProvider

    /**
     * Sign up a new user with email and password
     * 1. Create account in Firebase
     * 2. Create profile in Supabase using Firebase UID
     */
    suspend fun signUp(name: String, email: String, password: String): Result<User> {
        return try {
            // Trim whitespace from credentials
            val trimmedName = name.trim()
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()

            // Step 1: Create Firebase account
            when (val result = firebaseAuth.signUpWithEmail(trimmedEmail, trimmedPassword)) {
                is AuthResult.Success -> {
                    val firebaseUser = result.user

                    // Step 2: Create profile in Supabase using Firebase UID
                    try {
                        val profile = ProfileDto(
                            id = firebaseUser.uid,
                            name = trimmedName,
                            email = trimmedEmail
                        )
                        supabaseClient.postgrest["profiles"].insert(profile)

                        Result.success(
                            User(
                                id = firebaseUser.uid,
                                name = trimmedName,
                                email = trimmedEmail
                            )
                        )
                    } catch (e: Exception) {
                        // If profile creation fails, we still have a Firebase user
                        // Try to continue anyway - profile might exist from trigger
                        Result.success(
                            User(
                                id = firebaseUser.uid,
                                name = trimmedName,
                                email = trimmedEmail
                            )
                        )
                    }
                }
                is AuthResult.Error -> {
                    // Parse Firebase errors for better messages
                    val message = when {
                        result.message.contains("already", ignoreCase = true) ||
                        result.message.contains("exists", ignoreCase = true) ->
                            "This email is already registered"
                        result.message.contains("invalid", ignoreCase = true) ->
                            "Invalid email or password format"
                        result.message.contains("weak", ignoreCase = true) ->
                            "Password is too weak"
                        result.code == "email-already-in-use" ->
                            "This email is already registered"
                        result.code == "invalid-email" ->
                            "Invalid email format"
                        result.code == "weak-password" ->
                            "Password should be at least 6 characters"
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Registration failed"))
        }
    }

    /**
     * Sign in with email and password
     * 1. Authenticate with Firebase
     * 2. Fetch profile from Supabase using Firebase UID
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Trim whitespace from credentials
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()

            // Step 1: Sign in with Firebase
            when (val result = firebaseAuth.signInWithEmail(trimmedEmail, trimmedPassword)) {
                is AuthResult.Success -> {
                    val firebaseUser = result.user

                    // Step 2: Fetch profile from Supabase
                    val profile = supabaseClient.postgrest["profiles"]
                        .select {
                            filter {
                                eq("id", firebaseUser.uid)
                            }
                        }
                        .decodeSingleOrNull<ProfileDto>()

                    if (profile != null) {
                        Result.success(profile.toUser())
                    } else {
                        // Create profile if it doesn't exist (edge case)
                        val newProfile = ProfileDto(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: email.substringBefore("@"),
                            email = firebaseUser.email ?: email
                        )
                        supabaseClient.postgrest["profiles"].insert(newProfile)
                        Result.success(newProfile.toUser())
                    }
                }
                is AuthResult.Error -> {
                    val message = when {
                        result.message.contains("password", ignoreCase = true) ||
                        result.message.contains("credentials", ignoreCase = true) ||
                        result.message.contains("credential", ignoreCase = true) ||
                        result.message.contains("incorrect", ignoreCase = true) ||
                        result.message.contains("malformed", ignoreCase = true) ||
                        result.message.contains("expired", ignoreCase = true) ->
                            "Invalid email or password. If you signed up with Google, please use Google Sign-In."
                        result.code == "wrong-password" ->
                            "Invalid email or password"
                        result.code == "user-not-found" ->
                            "No account found with this email"
                        result.code == "invalid-email" ->
                            "Invalid email format"
                        result.code == "user-disabled" ->
                            "This account has been disabled"
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign in failed"))
        }
    }

    /**
     * Sign in with Google OAuth
     * 1. Authenticate with Firebase using Google
     * 2. Get or create profile in Supabase
     */
    suspend fun signInWithGoogle(): Result<User> {
        return try {
            when (val result = firebaseAuth.signInWithGoogle()) {
                is AuthResult.Success -> {
                    val firebaseUser = result.user

                    // Get or create user profile in Supabase
                    val profile = try {
                        var p = supabaseClient.postgrest["profiles"]
                            .select {
                                filter {
                                    eq("id", firebaseUser.uid)
                                }
                            }
                            .decodeSingleOrNull<ProfileDto>()

                        if (p == null) {
                            // Create profile for new Google user
                            val newProfile = ProfileDto(
                                id = firebaseUser.uid,
                                name = firebaseUser.displayName
                                    ?: firebaseUser.email?.substringBefore("@")
                                    ?: "User",
                                email = firebaseUser.email ?: ""
                            )
                            try { supabaseClient.postgrest["profiles"].insert(newProfile) } catch (_: Exception) {}
                            p = newProfile
                        }
                        p
                    } catch (e: Exception) {
                        println("AuthRepository: Supabase profile fetch failed (${e.message}), using Firebase data")
                        ProfileDto(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName
                                ?: firebaseUser.email?.substringBefore("@")
                                ?: "User",
                            email = firebaseUser.email ?: ""
                        )
                    }

                    Result.success(profile.toUser())
                }
                is AuthResult.Error -> {
                    println("AuthRepository: Google sign-in error: ${result.message}")
                    val message = when {
                        result.code == "not_implemented" ->
                            "Google Sign-In requires additional setup. Please check configuration."
                        result.message.contains("network", ignoreCase = true) ->
                            "Network error. Please check your connection."
                        result.message.contains("cancelled", ignoreCase = true) ->
                            "Google Sign-In was cancelled"
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            println("AuthRepository: Google sign-in exception: ${e.message}")
            Result.failure(Exception(e.message ?: "Google Sign-In failed"))
        }
    }

    /**
     * Sign in with Facebook OAuth
     * 1. Authenticate with Firebase using Facebook
     * 2. Get or create profile in Supabase
     */
    suspend fun signInWithFacebook(): Result<User> {
        return try {
            when (val result = firebaseAuth.signInWithFacebook()) {
                is AuthResult.Success -> {
                    val firebaseUser = result.user

                    // Get or create user profile in Supabase
                    var profile = supabaseClient.postgrest["profiles"]
                        .select {
                            filter {
                                eq("id", firebaseUser.uid)
                            }
                        }
                        .decodeSingleOrNull<ProfileDto>()

                    if (profile == null) {
                        // Create profile for new Facebook user
                        val newProfile = ProfileDto(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName
                                ?: firebaseUser.email?.substringBefore("@")
                                ?: "User",
                            email = firebaseUser.email ?: ""
                        )
                        supabaseClient.postgrest["profiles"].insert(newProfile)
                        profile = newProfile
                    }

                    Result.success(profile.toUser())
                }
                is AuthResult.Error -> {
                    println("AuthRepository: Facebook sign-in error: ${result.message}")
                    val message = when {
                        result.code == "not_implemented" ->
                            "Facebook Sign-In requires additional setup. Please check configuration."
                        result.message.contains("network", ignoreCase = true) ->
                            "Network error. Please check your connection."
                        result.message.contains("cancelled", ignoreCase = true) ->
                            "Facebook Sign-In was cancelled"
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            println("AuthRepository: Facebook sign-in exception: ${e.message}")
            Result.failure(Exception(e.message ?: "Facebook Sign-In failed"))
        }
    }

    /**
     * Sign in with Apple OAuth
     * 1. Authenticate with Firebase using Apple
     * 2. Get or create profile in Supabase
     */
    suspend fun signInWithApple(): Result<User> {
        return try {
            when (val result = firebaseAuth.signInWithApple()) {
                is AuthResult.Success -> {
                    val firebaseUser = result.user

                    // Get or create user profile in Supabase
                    var profile = supabaseClient.postgrest["profiles"]
                        .select {
                            filter {
                                eq("id", firebaseUser.uid)
                            }
                        }
                        .decodeSingleOrNull<ProfileDto>()

                    if (profile == null) {
                        // Create profile for new Apple user
                        val newProfile = ProfileDto(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName
                                ?: firebaseUser.email?.substringBefore("@")
                                ?: "User",
                            email = firebaseUser.email ?: ""
                        )
                        supabaseClient.postgrest["profiles"].insert(newProfile)
                        profile = newProfile
                    }

                    Result.success(profile.toUser())
                }
                is AuthResult.Error -> {
                    println("AuthRepository: Apple sign-in error: ${result.message}")
                    val message = when {
                        result.code == "not_implemented" ->
                            "Apple Sign-In requires additional setup. Please check configuration."
                        result.message.contains("network", ignoreCase = true) ->
                            "Network error. Please check your connection."
                        result.message.contains("cancelled", ignoreCase = true) ->
                            "Apple Sign-In was cancelled"
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            println("AuthRepository: Apple sign-in exception: ${e.message}")
            Result.failure(Exception(e.message ?: "Apple Sign-In failed"))
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the current signed-in user
     * Fetches profile from Supabase using Firebase UID
     */
    suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.getCurrentUser() ?: return null

        return try {
            val profile = supabaseClient.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", firebaseUser.uid)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            profile?.toUser()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean {
        return firebaseAuth.getCurrentUser() != null
    }

    /**
     * Get current user ID (Firebase UID)
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.getCurrentUser()?.uid
    }

    /**
     * Update user profile in Supabase
     */
    suspend fun updateProfile(user: User): Result<User> {
        return try {
            val profile = ProfileDto(
                id = user.id,
                name = user.name,
                email = user.email,
                profilePhotoUrl = user.profilePhotoUrl,
                level = user.level,
                xp = user.xp,
                streakCount = user.streakCount,
                lastLocation = user.lastLocation,
                bio = user.bio
            )

            supabaseClient.postgrest["profiles"]
                .update(profile) {
                    filter {
                        eq("id", user.id)
                    }
                }

            Result.success(user)
        } catch (e: Exception) {
            // Check if error is due to missing bio column
            if (e.message?.contains("bio", ignoreCase = true) == true) {
                // Try updating without bio field
                try {
                    val profileWithoutBio = ProfileDto(
                        id = user.id,
                        name = user.name,
                        email = user.email,
                        profilePhotoUrl = user.profilePhotoUrl,
                        level = user.level,
                        xp = user.xp,
                        streakCount = user.streakCount,
                        lastLocation = user.lastLocation,
                        bio = null
                    )

                    supabaseClient.postgrest["profiles"]
                        .update(profileWithoutBio) {
                            filter {
                                eq("id", user.id)
                            }
                        }

                    // Return success but without bio
                    return Result.success(user.copy(bio = null))
                } catch (retryException: Exception) {
                    return Result.failure(Exception("Failed to update profile. Please try again later."))
                }
            }

            // Return user-friendly error message
            val message = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                    "Session expired. Please sign in again."
                e.message?.contains("not found", ignoreCase = true) == true ->
                    "Profile not found. Please try signing in again."
                else -> "Failed to update profile. Please try again later."
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Update user password in Firebase
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            when (val result = firebaseAuth.updatePassword(newPassword)) {
                is AuthResult.Success -> Result.success(Unit)
                is AuthResult.Error -> {
                    val message = when {
                        result.message.contains("weak", ignoreCase = true) ->
                            "Password is too weak. Use at least 6 characters."
                        result.code == "weak-password" ->
                            "Password should be at least 6 characters"
                        result.code == "requires-recent-login" ->
                            "Please sign in again before changing your password"
                        result.message.contains("network", ignoreCase = true) ->
                            "Network error. Please check your connection and try again."
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to change password. Please try again later."))
        }
    }

    /**
     * Update user email in Firebase
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            when (val result = firebaseAuth.updateEmail(newEmail)) {
                is AuthResult.Success -> Result.success(Unit)
                is AuthResult.Error -> {
                    val message = when {
                        result.message.contains("already", ignoreCase = true) ->
                            "This email is already registered to another account."
                        result.code == "email-already-in-use" ->
                            "This email is already registered to another account."
                        result.code == "invalid-email" ->
                            "Invalid email format. Please enter a valid email address."
                        result.code == "requires-recent-login" ->
                            "Please sign in again before changing your email"
                        result.message.contains("network", ignoreCase = true) ->
                            "Network error. Please check your connection and try again."
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to change email. Please try again later."))
        }
    }

    /**
     * Delete user account
     * 1. Delete profile from Supabase
     * 2. Delete Firebase account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = firebaseAuth.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Not signed in. Please sign in and try again."))

            // Delete user profile from Supabase database
            try {
                supabaseClient.postgrest["profiles"].delete {
                    filter {
                        eq("id", userId)
                    }
                }
            } catch (e: Exception) {
                println("AuthRepository: Failed to delete profile from Supabase: ${e.message}")
                // Continue to delete Firebase account even if Supabase deletion fails
            }

            // Delete Firebase account
            when (val result = firebaseAuth.deleteAccount()) {
                is AuthResult.Success -> Result.success(Unit)
                is AuthResult.Error -> {
                    val message = when {
                        result.code == "requires-recent-login" ->
                            "Please sign in again before deleting your account"
                        result.message.contains("network", ignoreCase = true) ->
                            "Network error. Please check your connection and try again."
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("foreign key", ignoreCase = true) == true ->
                    "Cannot delete account. Please contact support."
                else -> "Failed to delete account. Please try again later."
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Send password reset email via Firebase
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            when (val result = firebaseAuth.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> Result.success(Unit)
                is AuthResult.Error -> {
                    val message = when {
                        result.code == "user-not-found" ->
                            "No account found with this email"
                        result.code == "invalid-email" ->
                            "Invalid email format"
                        result.message.contains("network", ignoreCase = true) ->
                            "Network error. Please check your connection."
                        else -> result.message
                    }
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to send reset email"))
        }
    }

    /**
     * Observe auth state changes from Firebase
     */
    fun observeAuthState(): Flow<User?> {
        return firebaseAuth.authStateFlow.map { firebaseUser ->
            when {
                firebaseUser != null -> getCurrentUser()
                else -> null
            }
        }
    }

    /**
     * Update user's last selected location in Supabase
     */
    suspend fun updateLastLocation(location: String): Result<Unit> {
        val userId = firebaseAuth.getCurrentUser()?.uid
            ?: return Result.failure(Exception("Not signed in"))

        return try {
            supabaseClient.postgrest["profiles"]
                .update(mapOf("last_location" to location)) {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's last selected location from Supabase
     */
    suspend fun getLastLocation(): String? {
        val userId = firebaseAuth.getCurrentUser()?.uid ?: return null

        return try {
            val profile = supabaseClient.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            profile?.lastLocation
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert ProfileDto to User model
     */
    private fun ProfileDto.toUser(): User {
        return User(
            id = id,
            name = name,
            email = email,
            profilePhotoUrl = profilePhotoUrl,
            level = level,
            xp = xp,
            streakCount = streakCount,
            lastLocation = lastLocation,
            bio = bio
        )
    }
}
