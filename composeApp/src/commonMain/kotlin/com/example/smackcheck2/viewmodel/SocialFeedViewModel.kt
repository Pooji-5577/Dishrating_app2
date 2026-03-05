package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.SocialFeedUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SocialFeedViewModel : ViewModel() {

    private val socialRepository = SocialRepository()
    private val authRepository = AuthRepository()
    private val databaseRepository = DatabaseRepository()

    private val _uiState = MutableStateFlow(SocialFeedUiState())
    val uiState: StateFlow<SocialFeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userId = authRepository.getCurrentUserId()

            val result = when (_uiState.value.filter) {
                FeedFilter.ALL -> socialRepository.getFeedAll(currentUserId = userId)
                FeedFilter.FOLLOWING -> {
                    if (userId != null) {
                        socialRepository.getFollowingFeed(userId)
                    } else {
                        socialRepository.getFeedAll(currentUserId = null)
                    }
                }
                FeedFilter.NEARBY -> {
                    // For now, same as ALL - will be filtered by location later
                    socialRepository.getFeedAll(currentUserId = userId)
                }
            }

            result.fold(
                onSuccess = { feedItems ->
                    _uiState.update {
                        it.copy(
                            feedItems = feedItems,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "Failed to load feed"
                        )
                    }
                }
            )
        }
    }

    fun setFilter(filter: FeedFilter) {
        _uiState.update { it.copy(filter = filter) }
        loadFeed()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadFeed()
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
                                    likesCount = if (isNowLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                                )
                            } else {
                                item
                            }
                        }
                        state.copy(feedItems = updatedItems)
                    }
                },
                onFailure = { /* silently fail */ }
            )
        }
    }
}
