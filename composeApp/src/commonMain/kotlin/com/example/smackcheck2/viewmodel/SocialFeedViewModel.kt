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
                    val currentUserId = authRepository.getCurrentUserId()
                    // Skip count update for own likes — already handled by optimistic update in toggleLike()
                    if (update.userId == currentUserId) {
                        println("[DEBUG][SocialFeed] Ignoring own LikeAdded real-time event (already optimistic)")
                        state
                    } else {
                        val updatedItems = state.feedItems.map { item ->
                            if (item.id == update.ratingId) {
                                item.copy(likesCount = item.likesCount + 1)
                            } else item
                        }
                        state.copy(feedItems = updatedItems)
                    }
                }
                is FeedUpdate.LikeRemoved -> {
                    val currentUserId = authRepository.getCurrentUserId()
                    // Skip count update for own unlikes — already handled by optimistic update in toggleLike()
                    if (update.userId == currentUserId) {
                        println("[DEBUG][SocialFeed] Ignoring own LikeRemoved real-time event (already optimistic)")
                        state
                    } else {
                        val updatedItems = state.feedItems.map { item ->
                            if (item.id == update.ratingId) {
                                item.copy(likesCount = maxOf(0, item.likesCount - 1))
                            } else item
                        }
                        state.copy(feedItems = updatedItems)
                    }
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

    fun setScrollToRatingId(ratingId: String?) {
        _uiState.update { it.copy(scrollToRatingId = ratingId) }
    }

    fun clearScrollTarget() {
        _uiState.update { it.copy(scrollToRatingId = null, scrollToIndex = null) }
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
                    val targetId = _uiState.value.scrollToRatingId
                    val scrollIndex = if (targetId != null) {
                        feedItems.indexOfFirst { it.id == targetId }.takeIf { it >= 0 }
                    } else null
                    _uiState.update {
                        it.copy(
                            feedItems = feedItems,
                            isLoading = false,
                            isRefreshing = false,
                            scrollToIndex = scrollIndex
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
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            println("[DEBUG][SocialFeed] toggleLike: No user ID — user not logged in")
            return
        }
        // Capture pre-toggle state so we can revert on failure
        val wasLiked = _uiState.value.feedItems.find { it.id == itemId }?.isLiked ?: false
        println("[DEBUG][SocialFeed] toggleLike: itemId=$itemId, wasLiked=$wasLiked, userId=$userId")

        // Optimistic update
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
            // Ensure user profile exists
            val user = authRepository.getCurrentUser()
            if (user == null) {
                println("SocialFeedViewModel: User not signed in, cannot toggle like")
                return@launch
            }
            val userId = user.id
            
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

            val result = realtimeFeedRepository.toggleLike(itemId, userId)
            result.fold(
                onSuccess = { isNowLiked ->
                    println("[DEBUG][SocialFeed] toggleLike SUCCESS: itemId=$itemId, isNowLiked=$isNowLiked")
                },
                onFailure = { e ->
                    println("[DEBUG][SocialFeed] toggleLike FAILED: itemId=$itemId, error=${e::class.simpleName} - ${e.message}")
                    e.printStackTrace()
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
