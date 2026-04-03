package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.ProfileDto
import com.example.smackcheck2.model.User
import com.example.smackcheck2.notifications.NotificationRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
     * Create or update a profile row, handling both id and email conflicts.
     * Uses upsert on id first; if that hits an email conflict, updates the
     * existing row by email instead.
     */
    @Serializable
    private data class UsernameCheckRequest(val username: String)

    @Serializable
    private data class UsernameCheckResponse(
        val available: Boolean,
        val error: String? = null
    )

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val response = client.functions.invoke(
                function = "check-username",
                body = UsernameCheckRequest(username)
            )
            val responseText = response.body<String>()
            val parsed = json.decodeFromString<UsernameCheckResponse>(responseText)
            if (parsed.available) {
                Result.success(true)
            } else {
                Result.failure(Exception(parsed.error ?: "Username is not available."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not check username availability. Please try again."))
        }
    }

    private suspend fun upsertProfile(profile: ProfileDto): ProfileDto {
        try {
            // Try upsert on primary key (id)
            client.postgrest["profiles"].upsert(profile) {
                onConflict = "id"
            }
            return profile
        } catch (e: Exception) {
            if (e.message?.contains("profiles_username_lower_idx", ignoreCase = true) == true) {
                throw Exception("That username is already taken. Please choose a different one.")
            }
            if (e.message?.contains("profiles_email_key", ignoreCase = true) == true ||
                e.message?.contains("duplicate key", ignoreCase = true) == true
            ) {
                // A row with the same email but a different id exists.
                // Update that row to use the current auth id.
                client.postgrest["profiles"]
                    .update(
                        mapOf(
                            "id" to profile.id,
                            "name" to profile.name
                        )
                    ) {
                        filter { eq("email", profile.email) }
                    }
                return profile
            }
            throw e
        }
    }

    /**
     * Sign up a new user with email and password
     */
    suspend fun signUp(name: String, username: String, email: String, password: String): Result<User> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            // Check if user is immediately available (email confirmation disabled)
            val userId = auth.currentUserOrNull()?.id

            if (userId != null) {
                // User is logged in, create or update profile
                try {
                    val profile = ProfileDto(
                        id = userId,
                        name = name,
                        username = username.ifBlank { null },
                        email = email
                    )
                    upsertProfile(profile)
                } catch (e: Exception) {
                    // Profile might already exist from trigger, ignore error
                    println("AuthRepository: Profile upsert note: ${e.message}")
                }

                // Send welcome notification
                try {
                    NotificationRepository.notifyWelcome(userId, name)
                } catch (e: Exception) {
                    println("AuthRepository: Welcome notification failed: ${e.message}")
                }

                Result.success(
                    User(
                        id = userId,
                        name = name,
                        username = username,
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
                else -> sanitizeErrorMessage(e.message ?: "Registration failed")
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
                upsertProfile(newProfile)
                Result.success(newProfile.toUser())
            }
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeErrorMessage(e.message ?: "Sign in failed")))
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

            val userInfo = auth.currentUserOrNull()
            val userEmail = userInfo?.email ?: ""

            // Get or create user profile
            var profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile == null) {
                // Create profile for new Google user (handles email conflict gracefully)
                val newProfile = ProfileDto(
                    id = userId,
                    name = userInfo?.userMetadata?.get("full_name")?.toString()
                        ?: userInfo?.email?.substringBefore("@")
                        ?: "User",
                    email = userEmail
                )
                upsertProfile(newProfile)
                profile = newProfile
            }

            Result.success(profile.toUser())
        } catch (e: Exception) {
            println("AuthRepository: Google sign-in error: ${e.message}")
            val message = when {
                e.message?.contains("OAuth", ignoreCase = true) == true ->
                    "Google Sign-In is not configured. Please set up Google OAuth in Supabase dashboard."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                else -> sanitizeErrorMessage(e.message ?: "Google Sign-In failed")
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

            val userInfo = auth.currentUserOrNull()
            val userEmail = userInfo?.email ?: ""

            // Get or create user profile
            var profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile == null) {
                // Create profile for new Facebook user (handles email conflict gracefully)
                val newProfile = ProfileDto(
                    id = userId,
                    name = userInfo?.userMetadata?.get("full_name")?.toString()
                        ?: userInfo?.userMetadata?.get("name")?.toString()
                        ?: userInfo?.email?.substringBefore("@")
                        ?: "User",
                    email = userEmail
                )
                upsertProfile(newProfile)
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
                else -> sanitizeErrorMessage(e.message ?: "Facebook Sign-In failed")
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

            val userInfo = auth.currentUserOrNull()
            val userEmail = userInfo?.email ?: ""

            // Get or create user profile
            var profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile == null) {
                // Create profile for new Apple user (handles email conflict gracefully)
                val newProfile = ProfileDto(
                    id = userId,
                    name = userInfo?.userMetadata?.get("full_name")?.toString()
                        ?: userInfo?.userMetadata?.get("name")?.toString()
                        ?: userInfo?.email?.substringBefore("@")
                        ?: "User",
                    email = userEmail
                )
                upsertProfile(newProfile)
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
                else -> sanitizeErrorMessage(e.message ?: "Apple Sign-In failed")
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
        val authUser = auth.currentUserOrNull() ?: return null
        val userId = authUser.id

        return try {
            val profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()

            if (profile != null) {
                profile.toUser()
            } else {
                // Profile missing (e.g. email-confirmation signup, OAuth race) — create it now
                val email = authUser.email ?: ""
                val name = authUser.userMetadata?.get("full_name")?.toString()
                    ?: authUser.userMetadata?.get("name")?.toString()
                    ?: email.substringBefore("@").ifEmpty { "User" }
                val newProfile = ProfileDto(id = userId, name = name, email = email)
                try {
                    upsertProfile(newProfile)
                    newProfile.toUser()
                } catch (e: Exception) {
                    println("AuthRepository: auto-profile creation failed: ${e.message}")
                    // Return null to indicate profile could not be created
                    // This forces the caller to handle the error rather than proceeding
                    // with a user that doesn't exist in the database
                    null
                }
            }
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

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    private data class ProfileSetupDto(
        val username: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        @kotlinx.serialization.SerialName("profile_photo_url")
        val profilePhotoUrl: String? = null
    )

    /**
     * Save username and optional profile photo during onboarding setup.
     */
    suspend fun saveProfileSetup(userId: String, username: String, profilePhotoUrl: String?): Result<Unit> {
        return try {
            client.postgrest["profiles"].update(ProfileSetupDto(username, profilePhotoUrl)) {
                filter { eq("id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e.message?.contains("profiles_username_lower_idx", ignoreCase = true) == true) {
                Result.failure(Exception("That username is already taken."))
            } else {
                Result.failure(Exception(sanitizeErrorMessage(e.message ?: "Failed to save profile")))
            }
        }
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
            username = username ?: "",
            email = email,
            profilePhotoUrl = profilePhotoUrl,
            level = level,
            xp = xp,
            streakCount = streakCount,
            lastLocation = lastLocation,
            bio = bio,
            followersCount = followersCount,
            followingCount = followingCount
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

    /**
     * Strip sensitive data (tokens, headers, URLs with query params) from error messages
     * so they are safe to display in the UI.
     */
    private fun sanitizeErrorMessage(message: String): String {
        // Map known Supabase errors to user-friendly messages
        if (message.contains("invalid_credentials", ignoreCase = true) ||
            message.contains("Invalid login credentials", ignoreCase = true)
        ) {
            return "Invalid email or password. Please try again."
        }
        if (message.contains("infinite recursion", ignoreCase = true)) {
            return "A temporary error occurred. Please try again."
        }
        if (message.contains("email not confirmed", ignoreCase = true)) {
            return "Please verify your email before signing in."
        }
        if (message.contains("too many requests", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true)
        ) {
            return "Too many attempts. Please wait and try again."
        }

        // If the message contains Authorization headers or JWT tokens, return a generic message
        if (message.contains("Authorization", ignoreCase = true) ||
            message.contains("Bearer ", ignoreCase = true) ||
            message.contains("eyJ", ignoreCase = true) ||  // JWT prefix
            message.contains("apikey=", ignoreCase = true) ||
            message.contains("Headers:", ignoreCase = true)
        ) {
            // Try to extract just the meaningful part before the URL/header dump
            val meaningfulPart = message.substringBefore("URL:").substringBefore("Headers:").trim()
            return if (meaningfulPart.isNotBlank() && meaningfulPart.length > 10) {
                meaningfulPart
            } else {
                "An unexpected error occurred. Please try again."
            }
        }
        return message
    }
}
