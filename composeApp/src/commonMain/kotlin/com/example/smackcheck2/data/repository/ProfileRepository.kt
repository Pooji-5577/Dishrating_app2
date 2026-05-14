package com.example.smackcheck2.data.repository

import com.example.smackcheck2.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class EditableProfileUpdate(
    val name: String,
    val username: String,
    val bio: String,
    val location: String,
    val email: String,
    val profilePhotoUrl: String?
)

/**
 * Central profile module for read/write operations that are user-editable from UI.
 * Keeps write scope narrow and provides a shared invalidation signal for profile consumers.
 */
class ProfileRepository(
    private val authRepository: AuthRepository = AuthRepository()
) {
    suspend fun getCurrentProfile(): User? = authRepository.getCurrentUser()

    suspend fun updateEditableProfile(currentUser: User, update: EditableProfileUpdate): Result<User> {
        val updatedUser = currentUser.copy(
            name = update.name,
            username = update.username,
            bio = update.bio,
            lastLocation = update.location.ifBlank { null },
            email = update.email,
            profilePhotoUrl = update.profilePhotoUrl
        )

        val result = authRepository.updateProfile(updatedUser)
        if (result.isSuccess) {
            ProfileInvalidationBus.notifyUpdated()
        }
        return result
    }
}

object ProfileInvalidationBus {
    private val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = updates.asSharedFlow()

    fun notifyUpdated() {
        updates.tryEmit(Unit)
    }
}

