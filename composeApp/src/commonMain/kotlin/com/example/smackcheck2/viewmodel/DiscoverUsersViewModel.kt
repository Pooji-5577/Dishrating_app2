package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.model.DiscoverUsersUiState
import com.example.smackcheck2.model.UserSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiscoverUsersViewModel : ViewModel() {

    private val socialRepository = SocialRepository()
    private val authRepository = AuthRepository()

    private val crashGuard = CoroutineExceptionHandler { _, throwable ->
        println("DiscoverUsersViewModel: Uncaught coroutine error: ${throwable::class.simpleName} - ${throwable.message}")
        throwable.printStackTrace()
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = throwable.message ?: "Something went wrong"
            )
        }
    }

    private val _uiState = MutableStateFlow(DiscoverUsersUiState())
    val uiState: StateFlow<DiscoverUsersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(crashGuard) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            fetch()
        }
    }

    fun refresh() {
        viewModelScope.launch(crashGuard) {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            fetch()
        }
    }

    private suspend fun fetch() {
        try {
            val currentUserId = authRepository.getCurrentUserId()
            val result = socialRepository.getDiscoverableUsers(currentUserId)
            result.fold(
                onSuccess = { users ->
                    _uiState.update {
                        it.copy(
                            users = users,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "Failed to load users"
                        )
                    }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "Failed to load users"
                )
            }
        }
    }

    fun toggleFollow(user: UserSummary) {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        if (currentUserId == user.id) return
        if (_uiState.value.togglingUserIds.contains(user.id)) return

        val wasFollowing = user.isFollowing

        // Optimistic update
        _uiState.update { state ->
            state.copy(
                users = state.users.map { u ->
                    if (u.id == user.id) u.copy(isFollowing = !wasFollowing) else u
                },
                togglingUserIds = state.togglingUserIds + user.id
            )
        }

        viewModelScope.launch(crashGuard) {
            try {
                val result = if (wasFollowing) {
                    socialRepository.unfollowUser(currentUserId, user.id)
                } else {
                    socialRepository.followUser(currentUserId, user.id)
                }
                result.fold(
                    onSuccess = {
                        _uiState.update { state ->
                            state.copy(togglingUserIds = state.togglingUserIds - user.id)
                        }
                    },
                    onFailure = { error ->
                        // Revert optimistic update
                        _uiState.update { state ->
                            state.copy(
                                users = state.users.map { u ->
                                    if (u.id == user.id) u.copy(isFollowing = wasFollowing) else u
                                },
                                togglingUserIds = state.togglingUserIds - user.id,
                                errorMessage = error.message ?: "Could not update follow"
                            )
                        }
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        users = state.users.map { u ->
                            if (u.id == user.id) u.copy(isFollowing = wasFollowing) else u
                        },
                        togglingUserIds = state.togglingUserIds - user.id,
                        errorMessage = e.message ?: "Could not update follow"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
