package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.model.HomeFeedUiState
import com.example.smackcheck2.model.ProfileUiState
import com.example.smackcheck2.model.UserProgressUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Home screen
 */
class HomeViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(HomeFeedUiState())
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = databaseRepository.getFeed()
                result.fold(
                    onSuccess = { feedItems ->
                        // Check which items the user has liked
                        val userId = authRepository.getCurrentUserId()
                        val itemsWithLikeStatus = if (userId != null) {
                            feedItems.map { item ->
                                val isLiked = databaseRepository.hasUserLiked(userId, item.id)
                                item.copy(isLiked = isLiked)
                            }
                        } else {
                            feedItems
                        }

                        _uiState.update {
                            it.copy(
                                feedItems = itemsWithLikeStatus,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to load feed"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load feed"
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadFeed()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleLike(itemId: String) {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            val result = databaseRepository.toggleLike(userId, itemId)
            result.fold(
                onSuccess = { isNowLiked ->
                    _uiState.update { state ->
                        val updatedItems = state.feedItems.map { item ->
                            if (item.id == itemId) {
                                item.copy(
                                    isLiked = isNowLiked,
                                    likesCount = if (isNowLiked) item.likesCount + 1 else item.likesCount - 1
                                )
                            } else {
                                item
                            }
                        }
                        state.copy(feedItems = updatedItems)
                    }
                },
                onFailure = {
                    // Optionally show error
                }
            )
        }
    }
}

/**
 * ViewModel for Profile screen
 */
class ProfileViewModel(private val authViewModel: AuthViewModel) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val user = authViewModel.getCurrentUser()

                _uiState.update {
                    it.copy(
                        user = user,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }
}

/**
 * ViewModel for User Progress Dashboard
 */
class UserProgressViewModel : ViewModel() {

    private val databaseRepository = DatabaseRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(UserProgressUiState())
    val uiState: StateFlow<UserProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    fun loadProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Not signed in"
                        )
                    }
                    return@launch
                }

                val user = authRepository.getCurrentUser()
                val badgesResult = databaseRepository.getUserBadges(userId)

                _uiState.update {
                    it.copy(
                        currentXp = user?.xp ?: 0,
                        maxXp = ((user?.level ?: 1) * 100),
                        level = user?.level ?: 1,
                        streakCount = user?.streakCount ?: 0,
                        badges = badgesResult.getOrDefault(emptyList()),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load progress"
                    )
                }
            }
        }
    }
}
