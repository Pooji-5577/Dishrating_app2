package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.RealtimeFeedRepository
import com.example.smackcheck2.data.repository.FeedUpdate
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.SocialFeedUiState
import com.example.smackcheck2.notifications.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SocialFeedViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val socialRepository = SocialRepository()
    private val authRepository = AuthRepository()
    private val databaseRepository = DatabaseRepository()
    private val realtimeFeedRepository = RealtimeFeedRepository()

    private val PAGE_SIZE = 10

    private val _uiState = MutableStateFlow(SocialFeedUiState())
    val uiState: StateFlow<SocialFeedUiState> = _uiState.asStateFlow()
    
    // Track if we're subscribed to real-time updates
    private var isSubscribedToRealtime = false

    init {
        loadFeed()
        loadStoryUsers()
        loadTopDishes()
        subscribeToRealtimeUpdates()
    }

    private fun loadStoryUsers() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                val result = socialRepository.getFollowing(userId)
                result.onSuccess { users ->
                    _uiState.update { it.copy(storyUsers = users) }
                }
            } catch (e: Exception) {
                println("SocialFeedViewModel: Failed to load story users: ${e.message}")
            }
        }
    }

    private fun loadTopDishes() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val result = socialRepository.getTrendingFeed(limit = 6, offset = 0, currentUserId = userId)
                result.onSuccess { dishes ->
                    _uiState.update { it.copy(topDishes = dishes) }
                }
            } catch (e: Exception) {
                println("SocialFeedViewModel: Failed to load top dishes: ${e.message}")
            }
        }
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null, currentOffset = 0, hasMoreItems = true) }

            val userId = authRepository.getCurrentUserId()
            val result = fetchPage(offset = 0, userId = userId)

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
                            currentOffset = PAGE_SIZE,
                            hasMoreItems = feedItems.size == PAGE_SIZE,
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

    fun loadMoreFeed() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreItems || state.isLoading) return

        Analytics.track("feed_scroll_depth", mapOf(
            "offset" to state.currentOffset,
            "filter" to state.filter.name
        ))

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val userId = authRepository.getCurrentUserId()
            val result = fetchPage(offset = state.currentOffset, userId = userId)

            result.fold(
                onSuccess = { newItems ->
                    _uiState.update {
                        it.copy(
                            feedItems = it.feedItems + newItems,
                            isLoadingMore = false,
                            currentOffset = it.currentOffset + PAGE_SIZE,
                            hasMoreItems = newItems.size == PAGE_SIZE
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoadingMore = false, errorMessage = error.message) }
                }
            )
        }
    }

    private suspend fun fetchPage(offset: Int, userId: String?): Result<List<FeedItem>> {
        val result = when (_uiState.value.filter) {
            FeedFilter.FOLLOWING,
            FeedFilter.TRENDING,
            FeedFilter.NEARBY -> socialRepository.getFeedAll(limit = PAGE_SIZE, offset = offset, currentUserId = userId)
            FeedFilter.MY_RATINGS -> {
                if (userId != null) socialRepository.getUserRatings(userId, limit = PAGE_SIZE)
                else Result.success(emptyList())
            }
        }
        // Apply bookmark state from local storage
        return result.map { items ->
            val bookmarks = preferencesRepository.getBookmarks()
            items.map { item ->
                item.copy(isBookmarked = bookmarks.contains(item.id))
            }
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
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            println("[DEBUG][SocialFeed] toggleLike: No user ID — user not logged in")
            return
        }
        // Capture pre-toggle state so we can revert on failure
        val wasLiked = _uiState.value.feedItems.find { it.id == itemId }?.isLiked ?: false
        println("[DEBUG][SocialFeed] toggleLike: itemId=$itemId, wasLiked=$wasLiked, userId=$currentUserId")

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
            val user = authRepository.getCurrentUser()
            if (user == null) {
                println("SocialFeedViewModel: User not signed in, cannot toggle like")
                return@launch
            }

            val result = realtimeFeedRepository.toggleLike(itemId, user.id)
            result.fold(
                onSuccess = { isNowLiked ->
                    println("[DEBUG][SocialFeed] toggleLike SUCCESS: itemId=$itemId, isNowLiked=$isNowLiked")
                    if (isNowLiked) {
                        val item = _uiState.value.feedItems.find { it.id == itemId }
                        if (item != null && item.userId != user.id) {
                            viewModelScope.launch {
                                NotificationRepository.notifyReviewLiked(
                                    reviewOwnerId = item.userId,
                                    likerName = user.name,
                                    dishName = item.dishName,
                                    reviewId = itemId
                                )
                            }
                        }
                    }
                },
                onFailure = { e ->
                    println("[DEBUG][SocialFeed] toggleLike FAILED: itemId=$itemId, error=${e::class.simpleName} - ${e.message}")
                    e.printStackTrace()
                    // Revert optimistic UI toggle back to original state
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
    
    fun toggleBookmark(itemId: String) {
        viewModelScope.launch {
            val isNowBookmarked = preferencesRepository.toggleBookmark(itemId)
            _uiState.update { state ->
                val updatedItems = state.feedItems.map { item ->
                    if (item.id == itemId) item.copy(isBookmarked = isNowBookmarked) else item
                }
                state.copy(feedItems = updatedItems)
            }
        }
    }

    /**
     * Increment the comment count for a feed item (called after a comment is posted).
     */
    fun incrementCommentCount(ratingId: String) {
        _uiState.update { state ->
            val updatedItems = state.feedItems.map { item ->
                if (item.id == ratingId) {
                    item.copy(commentsCount = item.commentsCount + 1)
                } else item
            }
            state.copy(feedItems = updatedItems)
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
