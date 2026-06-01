package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.ProfileDto
import com.example.smackcheck2.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.smackcheck2.util.Logger
import com.example.smackcheck2.util.decodePercentEncodedText

// Custom OAuth Providers for Facebook and Apple
private object Facebook : OAuthProvider() {
    override val name = "facebook"
}

private object Apple : OAuthProvider() {
    override val name = "apple"
}

/**
 * Authentication repository.
 *
 * Auth operations (sign-up, sign-in, OAuth, password/email changes) still go
 * through the Supabase GoTrue SDK directly — the JWT it issues is forwarded
 * on every request to our custom Node.js backend.
 *
 * Profile CRUD now goes through the custom backend via ApiClient.
 */
class AuthRepository(
    private val notificationService: NotificationService = NotificationService()
) {

    private val auth get() = SupabaseClientProvider.client.auth

    // ── Username check ──────────────────────────────────────────────────────

    suspend fun checkUsernameAvailable(username: String): Result<Boolean> {
        return try {
            @Serializable data class Resp(val available: Boolean)
            val resp: Resp = ApiClient.get("auth/check-username", mapOf("username" to username))
            if (resp.available) Result.success(true)
            else Result.failure(Exception("Username '$username' is already taken."))
        } catch (e: Exception) {
            Result.failure(Exception("Could not check username availability. Please try again."))
        }
    }

    // ── Sign-up ─────────────────────────────────────────────────────────────

    suspend fun signUp(name: String, username: String, email: String, password: String): Result<User> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = auth.currentUserOrNull()?.id
            if (userId != null) {
                @Serializable data class ProfileReq(
                    val name: String,
                    val username: String?,
                    val email: String,
                    @SerialName("profile_setup_completed")
                    val profileSetupCompleted: Boolean
                )
                val profile: ProfileDto = ApiClient.post(
                    "auth/profile",
                    ProfileReq(name, username.ifBlank { null }, email, false)
                )
                try { notificationService.notifyWelcome(userId, name) } catch (_: Exception) {}
                Result.success(profile.toUser())
            } else {
                Result.failure(Exception("CHECK_EMAIL"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeError(e.message ?: "Registration failed")))
        }
    }

    // ── Sign-in (email) ─────────────────────────────────────────────────────

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Sign in failed"))
            val profile = getOrCreateProfile(email)
            Result.success(profile.toUser())
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeError(e.message ?: "Sign in failed")))
        }
    }

    // ── OAuth sign-ins ──────────────────────────────────────────────────────

    suspend fun signInWithGoogle(): Result<User> = oAuthSignIn { auth.signInWith(Google) }
    suspend fun signInWithFacebook(): Result<User> = oAuthSignIn { auth.signInWith(Facebook) }
    suspend fun signInWithApple(): Result<User> = oAuthSignIn { auth.signInWith(Apple) }

    private suspend fun oAuthSignIn(block: suspend () -> Unit): Result<User> {
        return try {
            block()
            val authUser = auth.currentUserOrNull()
                ?: return Result.failure(Exception("OAuth sign in failed"))
            waitForAccessToken()
                ?: return Result.failure(Exception("OAuth session was not ready. Please try again."))
            val email = authUser.email ?: ""
            val profile = getOrCreateProfile(email,
                displayName = authUser.userMetadata?.get("full_name")?.toString()
                    ?: authUser.userMetadata?.get("name")?.toString()
                    ?: email.substringBefore("@").ifEmpty { "User" })
            Result.success(profile.toUser())
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeError(e.message ?: "OAuth sign in failed")))
        }
    }

    private suspend fun getOrCreateProfile(email: String, displayName: String? = null): ProfileDto {
        return try {
            ApiClient.get("auth/profile")
        } catch (e: Exception) {
            if (!e.isNotFound()) throw e
            @Serializable data class ProfileReq(
                val name: String,
                val email: String,
                @SerialName("profile_setup_completed")
                val profileSetupCompleted: Boolean
            )
            ApiClient.post("auth/profile", ProfileReq(displayName ?: email.substringBefore("@"), email, false))
        }
    }

    private suspend fun waitForAccessToken(): String? {
        repeat(10) {
            val token = auth.currentSessionOrNull()?.accessToken
            if (!token.isNullOrBlank()) return token
            delay(150)
        }
        return auth.currentSessionOrNull()?.accessToken?.takeIf { it.isNotBlank() }
    }

    // ── Sign-out ────────────────────────────────────────────────────────────

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Current user ────────────────────────────────────────────────────────

    suspend fun getCurrentUser(): User? {
        val authUser = auth.currentUserOrNull() ?: return null
        return try {
            val profile: ProfileDto = ApiClient.get("auth/profile")
            profile.toUser()
        } catch (e: Exception) {
            if (!e.isNotFound()) return null
            try {
                val email = authUser.email ?: ""
                @Serializable data class ProfileReq(
                    val name: String,
                    val email: String,
                    @SerialName("profile_setup_completed")
                    val profileSetupCompleted: Boolean
                )
                val profile: ProfileDto = ApiClient.post(
                    "auth/profile",
                    ProfileReq(email.substringBefore("@").ifEmpty { "User" }, email, false)
                )
                profile.toUser()
            } catch (_: Exception) { null }
        }
    }

    suspend fun restorePersistedUser(maxAttempts: Int = 3): Result<User?> {
        if (auth.currentUserOrNull() == null) return Result.success(null)

        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                val profile: ProfileDto = ApiClient.get("auth/profile")
                return Result.success(profile.toUser())
            } catch (e: Exception) {
                if (e.isNotFound()) {
                    try {
                        val authUser = auth.currentUserOrNull() ?: return Result.success(null)
                        val email = authUser.email ?: ""
                        @Serializable data class ProfileReq(
                            val name: String,
                            val email: String,
                            @SerialName("profile_setup_completed")
                            val profileSetupCompleted: Boolean
                        )
                        ApiClient.post<ProfileReq, ProfileDto>(
                            "auth/profile",
                            ProfileReq(email.substringBefore("@").ifEmpty { "User" }, email, false)
                        )
                    } catch (repairError: Exception) {
                        lastError = repairError
                    }
                } else {
                    lastError = e
                }
            }
            delay(250L * (attempt + 1))
        }

        return Result.failure(lastError ?: Exception("Failed to restore session"))
    }

    fun isSignedIn(): Boolean = auth.currentUserOrNull() != null
    fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id

    // ── Profile updates ─────────────────────────────────────────────────────

    suspend fun saveProfileSetup(userId: String, username: String, profilePhotoUrl: String?): Result<Unit> {
        return try {
            @Serializable data class Req(
                val username: String,
                val profile_photo_url: String?,
                @SerialName("profile_setup_completed")
                val profileSetupCompleted: Boolean
            )
            ApiClient.put<Req, ProfileDto>("auth/profile", Req(username, profilePhotoUrl, true))
            Result.success(Unit)
        } catch (e: Exception) {
            if (e.message?.contains("username", ignoreCase = true) == true)
                Result.failure(Exception("That username is already taken."))
            else
                Result.failure(Exception(sanitizeError(e.message ?: "Failed to save profile")))
        }
    }

    suspend fun updateProfile(user: User): Result<User> {
        return try {
            @Serializable data class Req(
                val name: String, val username: String?, val bio: String?,
                val profile_photo_url: String?, val last_location: String?
            )
            val updated: ProfileDto = ApiClient.put("auth/profile",
                Req(user.name, user.username.ifBlank { null }, user.bio, user.profilePhotoUrl, user.lastLocation))
            Result.success(updated.toUser())
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeError(e.message ?: "Failed to update profile")))
        }
    }

    suspend fun updateLastLocation(location: String): Result<Unit> {
        return try {
            @Serializable data class Req(val last_location: String)
            ApiClient.put<Req, ProfileDto>("auth/profile", Req(location))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getLastLocation(): String? {
        return try {
            val profile: ProfileDto = ApiClient.get("auth/profile")
            profile.lastLocation
        } catch (_: Exception) { null }
    }

    // ── Password / email ────────────────────────────────────────────────────

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            auth.updateUser { password = newPassword }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeError(e.message ?: "Failed to change password")))
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            auth.updateUser { email = newEmail }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeError(e.message ?: "Failed to change email")))
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Account deletion ────────────────────────────────────────────────────

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            ApiClient.delete("auth/account")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(sanitizeError(e.message ?: "Failed to delete account")))
        }
    }

    // ── Push token ──────────────────────────────────────────────────────────

    suspend fun savePushToken(token: String): Result<Unit> {
        return try {
            @Serializable data class Req(val token: String)
            ApiClient.put<Req, ProfileDto>("auth/push-token", Req(token))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removePushToken(): Result<Unit> {
        return try {
            @Serializable data class Req(val token: String?)
            ApiClient.put<Req, ProfileDto>("auth/push-token", Req(null))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Auth state observation ──────────────────────────────────────────────

    fun observeAuthState(): Flow<User?> {
        return auth.sessionStatus.map {
            if (auth.currentUserOrNull() != null) getCurrentUser() else null
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun ProfileDto.toUser() = User(
        id = id,
        name = decodePercentEncodedText(name),
        username = username?.let(::decodePercentEncodedText) ?: "",
        email = email,
        profilePhotoUrl = profilePhotoUrl,
        level = level,
        xp = xp,
        streakCount = streakCount,
        lastLocation = lastLocation?.let(::decodePercentEncodedText),
        bio = bio?.let(::decodePercentEncodedText),
        followersCount = followersCount,
        followingCount = followingCount,
        profileSetupCompleted = profileSetupCompleted
    )

    private fun Exception.isNotFound(): Boolean =
        this is ClientRequestException && response.status == HttpStatusCode.NotFound

    private fun sanitizeError(message: String): String = when {
        message.contains("invalid_credentials", ignoreCase = true) ||
                message.contains("Invalid login credentials", ignoreCase = true) ->
            "Invalid email or password. Please try again."
        message.contains("already registered", ignoreCase = true) ->
            "This email is already registered."
        message.contains("email not confirmed", ignoreCase = true) ->
            "Please verify your email before signing in."
        message.contains("too many requests", ignoreCase = true) ||
                message.contains("rate limit", ignoreCase = true) ->
            "Too many attempts. Please wait and try again."
        message.contains("eyJ", ignoreCase = true) ||
                message.contains("Authorization", ignoreCase = true) ->
            "An unexpected error occurred. Please try again."
        else -> message
    }
}
