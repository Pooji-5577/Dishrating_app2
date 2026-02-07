package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.ProfileDto
import com.example.smackcheck2.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Custom OAuth Providers for Facebook and Apple
private object Facebook : OAuthProvider() {
    override val name = "facebook"
}

private object Apple : OAuthProvider() {
    override val name = "apple"
}

/**
 * Repository for authentication operations using Supabase Auth
 */
class AuthRepository {

    private val client = SupabaseClientProvider.client
    private val auth = client.auth

    /**
     * Sign up a new user with email and password
     */
    suspend fun signUp(name: String, email: String, password: String): Result<User> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            // Check if user is immediately available (email confirmation disabled)
            val userId = auth.currentUserOrNull()?.id

            if (userId != null) {
                // User is logged in, create profile
                try {
                    val profile = ProfileDto(
                        id = userId,
                        name = name,
                        email = email
                    )
                    client.postgrest["profiles"].insert(profile)
                } catch (e: Exception) {
                    // Profile might already exist from trigger, ignore error
                }

                Result.success(
                    User(
                        id = userId,
                        name = name,
                        email = email
                    )
                )
            } else {
                // Email confirmation is required - this is actually a success!
                // Return a special result indicating email verification needed
                Result.failure(Exception("CHECK_EMAIL"))
            }
        } catch (e: Exception) {
            // Parse common Supabase errors for better messages
            val message = when {
                e.message?.contains("already registered", ignoreCase = true) == true ->
                    "This email is already registered"
                e.message?.contains("User already registered", ignoreCase = true) == true ->
                    "This email is already registered"
                e.message?.contains("invalid", ignoreCase = true) == true ->
                    "Invalid email or password format"
                e.message?.contains("weak password", ignoreCase = true) == true ->
                    "Password is too weak"
                else -> e.message ?: "Registration failed"
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Sign in failed"))

            // Fetch user profile
            val profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile != null) {
                Result.success(profile.toUser())
            } else {
                // Create profile if it doesn't exist (edge case)
                val newProfile = ProfileDto(
                    id = userId,
                    name = email.substringBefore("@"),
                    email = email
                )
                client.postgrest["profiles"].insert(newProfile)
                Result.success(newProfile.toUser())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth
     * Note: Requires Google OAuth to be configured in Supabase dashboard
     */
    suspend fun signInWithGoogle(): Result<User> {
        return try {
            // Initiate Google OAuth flow
            auth.signInWith(Google)

            // After successful OAuth, get the user
            val userId = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Google sign in failed - no user"))

            // Get or create user profile
            var profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile == null) {
                // Create profile for new Google user
                val userInfo = auth.currentUserOrNull()
                val newProfile = ProfileDto(
                    id = userId,
                    name = userInfo?.userMetadata?.get("full_name")?.toString()
                        ?: userInfo?.email?.substringBefore("@")
                        ?: "User",
                    email = userInfo?.email ?: ""
                )
                client.postgrest["profiles"].insert(newProfile)
                profile = newProfile
            }

            Result.success(profile.toUser())
        } catch (e: Exception) {
            println("AuthRepository: Google sign-in error: ${e.message}")
            // Provide helpful error message based on the exception
            val message = when {
                e.message?.contains("OAuth", ignoreCase = true) == true ->
                    "Google Sign-In is not configured. Please set up Google OAuth in Supabase dashboard."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                else -> e.message ?: "Google Sign-In failed"
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Sign in with Facebook OAuth
     * Note: Requires Facebook OAuth to be configured in Supabase dashboard
     */
    suspend fun signInWithFacebook(): Result<User> {
        return try {
            // Initiate Facebook OAuth flow
            auth.signInWith(Facebook)

            // After successful OAuth, get the user
            val userId = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Facebook sign in failed - no user"))

            // Get or create user profile
            var profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile == null) {
                // Create profile for new Facebook user
                val userInfo = auth.currentUserOrNull()
                val newProfile = ProfileDto(
                    id = userId,
                    name = userInfo?.userMetadata?.get("full_name")?.toString()
                        ?: userInfo?.userMetadata?.get("name")?.toString()
                        ?: userInfo?.email?.substringBefore("@")
                        ?: "User",
                    email = userInfo?.email ?: ""
                )
                client.postgrest["profiles"].insert(newProfile)
                profile = newProfile
            }

            Result.success(profile.toUser())
        } catch (e: Exception) {
            println("AuthRepository: Facebook sign-in error: ${e.message}")
            val message = when {
                e.message?.contains("OAuth", ignoreCase = true) == true ->
                    "Facebook Sign-In is not configured. Please set up Facebook OAuth in Supabase dashboard."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                else -> e.message ?: "Facebook Sign-In failed"
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Sign in with Apple OAuth
     * Note: Requires Apple OAuth to be configured in Supabase dashboard
     */
    suspend fun signInWithApple(): Result<User> {
        return try {
            // Initiate Apple OAuth flow
            auth.signInWith(Apple)

            // After successful OAuth, get the user
            val userId = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Apple sign in failed - no user"))

            // Get or create user profile
            var profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile == null) {
                // Create profile for new Apple user
                val userInfo = auth.currentUserOrNull()
                val newProfile = ProfileDto(
                    id = userId,
                    name = userInfo?.userMetadata?.get("full_name")?.toString()
                        ?: userInfo?.userMetadata?.get("name")?.toString()
                        ?: userInfo?.email?.substringBefore("@")
                        ?: "User",
                    email = userInfo?.email ?: ""
                )
                client.postgrest["profiles"].insert(newProfile)
                profile = newProfile
            }

            Result.success(profile.toUser())
        } catch (e: Exception) {
            println("AuthRepository: Apple sign-in error: ${e.message}")
            val message = when {
                e.message?.contains("OAuth", ignoreCase = true) == true ->
                    "Apple Sign-In is not configured. Please set up Apple OAuth in Supabase dashboard."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                else -> e.message ?: "Apple Sign-In failed"
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the current signed-in user
     */
    suspend fun getCurrentUser(): User? {
        val userId = auth.currentUserOrNull()?.id ?: return null

        return try {
            val profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
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
        return auth.currentUserOrNull() != null
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    /**
     * Update user profile
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

            client.postgrest["profiles"]
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

                    client.postgrest["profiles"]
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
     * Update user password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("weak", ignoreCase = true) == true ->
                    "Password is too weak. Use at least 8 characters with mix of letters and numbers."
                e.message?.contains("same", ignoreCase = true) == true ->
                    "New password cannot be the same as the old password."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                    "Session expired. Please sign in again."
                else -> "Failed to change password. Please try again later."
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Update user email
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            auth.updateUser {
                email = newEmail
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("already", ignoreCase = true) == true ->
                    "This email is already registered to another account."
                e.message?.contains("invalid", ignoreCase = true) == true ->
                    "Invalid email format. Please enter a valid email address."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                    "Session expired. Please sign in again."
                else -> "Failed to change email. Please try again later."
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Delete user account
     * Note: This deletes the profile but Supabase Auth user deletion
     * requires additional setup (database function or REST API)
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not signed in. Please sign in and try again."))

            // Delete user profile from database
            client.postgrest["profiles"].delete {
                filter {
                    eq("id", userId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                    "Session expired. Please sign in again."
                e.message?.contains("foreign key", ignoreCase = true) == true ->
                    "Cannot delete account. Please contact support."
                else -> "Failed to delete account. Please try again later."
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Send password reset email
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe auth state changes
     */
    fun observeAuthState(): Flow<User?> {
        return auth.sessionStatus.map { status ->
            when {
                auth.currentUserOrNull() != null -> getCurrentUser()
                else -> null
            }
        }
    }

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

    /**
     * Update user's last selected location
     */
    suspend fun updateLastLocation(location: String): Result<Unit> {
        val userId = auth.currentUserOrNull()?.id ?: return Result.failure(Exception("Not signed in"))

        return try {
            client.postgrest["profiles"]
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
     * Get user's last selected location
     */
    suspend fun getLastLocation(): String? {
        val userId = auth.currentUserOrNull()?.id ?: return null

        return try {
            val profile = client.postgrest["profiles"]
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
}
