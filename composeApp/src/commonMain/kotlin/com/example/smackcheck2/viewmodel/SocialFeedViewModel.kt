package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.RealtimeFeedRepository
import com.example.smackcheck2.data.repository.FeedUpdate
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
    private val realtimeFeedRepository = RealtimeFeedRepository()

    private val _uiState = MutableStateFlow(SocialFeedUiState())
    val uiState: StateFlow<SocialFeedUiState> = _uiState.asStateFlow()
    
    // Track if we're subscribed to real-time updates
    private var isSubscribedToRealtime = false

    init {
        loadFeed()
        subscribeToRealtimeUpdates()
    }
    
    /**
     * Subscribe to real-time feed updates for live feed experience
     */
    private fun subscribeToRealtimeUpdates() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            if (!isSubscribedToRealtime) {
                // Subscribe to all real-time channels
                realtimeFeedRepository.subscribeToFeed(userId)
                realtimeFeedRepository.subscribeToLikes(userId)
                realtimeFeedRepository.subscribeToComments(userId)
                isSubscribedToRealtime = true
                
                println("SocialFeedViewModel: Subscribed to real-time updates")
            }
            
            // Collect real-time feed updates
            realtimeFeedRepository.feedUpdates.collect { update ->
                handleFeedUpdate(update)
            }
        }
    }
    
    /**
     * Handle real-time feed updates
     */
    private fun handleFeedUpdate(update: FeedUpdate) {
        _uiState.update { state ->
            when (update) {
                is FeedUpdate.NewPost -> {
                    // Add new post to top of feed
                    val updatedItems = listOf(update.feedItem) + state.feedItems.filter { it.id != update.feedItem.id }
                    state.copy(feedItems = updatedItems)
                }
                is FeedUpdate.PostDeleted -> {
                    // Remove deleted post from feed
                    state.copy(feedItems = state.feedItems.filter { it.id != update.ratingId })
                }
                is FeedUpdate.LikeAdded -> {
                    // Update like count for the post
                    val userId = authRepository.getCurrentUserId()
                    val updatedItems = state.feedItems.map { item ->
                        if (item.id == update.ratingId) {
                            item.copy(
                                likesCount = item.likesCount + 1,
                                isLiked = if (update.userId == userId) true else item.isLiked
                            )
                        } else item
                    }
                    state.copy(feedItems = updatedItems)
                }
                is FeedUpdate.LikeRemoved -> {
                    // Update like count for the post
                    val userId = authRepository.getCurrentUserId()
                    val updatedItems = state.feedItems.map { item ->
                        if (item.id == update.ratingId) {
                            item.copy(
                                likesCount = maxOf(0, item.likesCount - 1),
                                isLiked = if (update.userId == userId) false else item.isLiked
                            )
                        } else item
                    }
                    state.copy(feedItems = updatedItems)
                }
                is FeedUpdate.CommentAdded -> {
                    // Update comment count for the post
                    val updatedItems = state.feedItems.map { item ->
                        if (item.id == update.ratingId) {
                            item.copy(commentsCount = item.commentsCount + 1)
                        } else item
                    }
                    state.copy(feedItems = updatedItems)
                }
                is FeedUpdate.CommentRemoved -> {
                    // Update comment count for the post
                    val updatedItems = state.feedItems.map { item ->
                        if (item.id == update.ratingId) {
                            item.copy(commentsCount = maxOf(0, item.commentsCount - 1))
                        } else item
                    }
                    state.copy(feedItems = updatedItems)
                }
            }
        }
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
        // Capture pre-toggle state so we can revert on failure
        val wasLiked = _uiState.value.feedItems.find { it.id == itemId }?.isLiked ?: false

        // Optimistic update - will be confirmed by real-time subscription
        _uiState.update { state ->
            val updatedItems = state.feedItems.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        isLiked = !wasLiked,
                        likesCount = if (!wasLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                    )
                } else item
            }
            state.copy(feedItems = updatedItems)
        }

        viewModelScope.launch {
            val result = realtimeFeedRepository.toggleLike(itemId, userId)
            result.fold(
                onSuccess = { isNowLiked ->
                    // Real-time subscription will handle the update, but verify consistency
                    println("SocialFeedViewModel: Like toggled successfully for $itemId, isLiked=$isNowLiked")
                },
                onFailure = { e ->
                    println("SocialFeedViewModel: toggleLike failed for $itemId: ${e.message}")
                    // Revert optimistic UI toggle
                    _uiState.update { state ->
                        val revertedItems = state.feedItems.map { item ->
                            if (item.id == itemId) {
                                item.copy(
                                    isLiked = wasLiked,
                                    likesCount = if (wasLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                                )
                            } else item
                        }
                        state.copy(feedItems = revertedItems)
                    }
                }
            )
        }
    }
    
    /**
     * Clean up real-time subscriptions when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeFeedRepository.unsubscribeAll()
            isSubscribedToRealtime = false
            println("SocialFeedViewModel: Unsubscribed from real-time updates")
        }
    }
}
