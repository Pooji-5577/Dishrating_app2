package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.RealtimeFeedRepository
import com.example.smackcheck2.location.SharedLocationState
import com.example.smackcheck2.data.repository.FeedUpdate
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.SocialFeedUiState
import com.example.smackcheck2.model.UserSummary
import com.example.smackcheck2.data.repository.NotificationService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
    private val notificationService = NotificationService()

    private val crashGuard = CoroutineExceptionHandler { _, throwable ->
        println("SocialFeedViewModel: Uncaught coroutine error: ${throwable::class.simpleName} - ${throwable.message}")
        throwable.printStackTrace()
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                isRefreshing = false,
                errorMessage = throwable.message ?: "Something went wrong"
            )
        }
    }

    private val PAGE_SIZE = 10

    private val _uiState = MutableStateFlow(SocialFeedUiState())
    val uiState: StateFlow<SocialFeedUiState> = _uiState.asStateFlow()

    private var loadFeedJob: Job? = null

    // Track if we're subscribed to real-time updates
    private var isSubscribedToRealtime = false

    init {
        loadFeed()
        loadStoryUsers()
        loadTopDishes()
        loadNearbyRestaurantCount()
        subscribeToRealtimeUpdates()
    }

    private fun loadNearbyRestaurantCount() {
        viewModelScope.launch(crashGuard) {
            try {
                databaseRepository.getRestaurants().onSuccess { restaurants ->
                    _uiState.update { it.copy(nearbyRestaurantCount = restaurants.size) }
                }
            } catch (e: Exception) {
                // Non-critical; banner will show 0 restaurants
            }
        }
    }

    private fun loadStoryUsers() {
        viewModelScope.launch(crashGuard) {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                socialRepository.getStories().onSuccess { stories ->
                    val storyUsers = stories
                        .filter { it.userId != userId }
                        .distinctBy { it.userId }
                        .map { story ->
                            UserSummary(
                                id = story.userId,
                                name = story.userName,
                                profilePhotoUrl = story.userProfileUrl,
                                isFollowing = true
                            )
                        }
                    _uiState.update {
                        it.copy(
                            currentUserId = userId,
                            stories = stories,
                            storyUsers = storyUsers
                        )
                    }
                }
            } catch (e: Exception) {
                println("SocialFeedViewModel: Failed to load story users: ${e.message}")
            }
        }
    }

    private fun loadTopDishes() {
        viewModelScope.launch(crashGuard) {
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
     * Refresh home-specific social data (following avatars + counters).
     */
    fun refreshHomeData() {
        loadStoryUsers()
        loadNearbyRestaurantCount()
    }
    
    /**
     * Subscribe to real-time feed updates for live feed experience
     */
    private fun subscribeToRealtimeUpdates() {
        viewModelScope.launch(crashGuard) {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch

                if (!isSubscribedToRealtime) {
                    realtimeFeedRepository.subscribeToFeed(userId)
                    realtimeFeedRepository.subscribeToLikes(userId)
                    realtimeFeedRepository.subscribeToComments(userId)
                    isSubscribedToRealtime = true
                    println("SocialFeedViewModel: Subscribed to real-time updates")
                }

                realtimeFeedRepository.feedUpdates.collect { update ->
                    try {
                        handleFeedUpdate(update)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        println("SocialFeedViewModel: handleFeedUpdate failed: ${e.message}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("SocialFeedViewModel: realtime subscription failed: ${e.message}")
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
        loadFeedJob?.cancel()
        loadFeedJob = viewModelScope.launch(crashGuard) {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, currentOffset = 0, hasMoreItems = true) }

                val userId = authRepository.getCurrentUserId()
                val result = fetchPage(offset = 0, userId = userId)

                result.fold(
                    onSuccess = { feedItems ->
                        // Dedupe by id to protect LazyColumn from duplicate keys
                        val deduped = feedItems.distinctBy { it.id }
                        val targetId = _uiState.value.scrollToRatingId
                        val scrollIndex = if (targetId != null) {
                            deduped.indexOfFirst { it.id == targetId }.takeIf { it >= 0 }
                        } else null
                        _uiState.update {
                            it.copy(
                                feedItems = deduped,
                                isLoading = false,
                                isRefreshing = false,
                                currentOffset = PAGE_SIZE,
                                hasMoreItems = deduped.size == PAGE_SIZE,
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.message ?: "Failed to load feed"
                    )
                }
            }
        }
    }

    fun loadMoreFeed() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreItems || state.isLoading) return

        Analytics.track("feed_scroll_depth", mapOf(
            "offset" to state.currentOffset,
            "filter" to state.filter.name
        ))

        viewModelScope.launch(crashGuard) {
            try {
                _uiState.update { it.copy(isLoadingMore = true) }
                val userId = authRepository.getCurrentUserId()
                val result = fetchPage(offset = state.currentOffset, userId = userId)

                result.fold(
                    onSuccess = { newItems ->
                        _uiState.update { current ->
                            val existingIds = current.feedItems.map { it.id }.toHashSet()
                            val deduped = newItems.filter { it.id !in existingIds }
                            current.copy(
                                feedItems = current.feedItems + deduped,
                                isLoadingMore = false,
                                currentOffset = current.currentOffset + PAGE_SIZE,
                                hasMoreItems = newItems.size == PAGE_SIZE
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoadingMore = false, errorMessage = error.message) }
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = e.message ?: "Failed to load more") }
            }
        }
    }

    private suspend fun fetchPage(offset: Int, userId: String?): Result<List<FeedItem>> {
        val location = SharedLocationState.currentLocation.value
        val hasLocation = location != null && (location.latitude != 0.0 || location.longitude != 0.0)

        val result = try {
            when (_uiState.value.filter) {
                FeedFilter.FOLLOWING -> {
                    if (userId != null) socialRepository.getFollowingFeed(userId, limit = PAGE_SIZE, offset = offset)
                    else socialRepository.getFeedAll(limit = PAGE_SIZE, offset = offset, currentUserId = userId)
                }
                FeedFilter.TRENDING -> socialRepository.getHighestRatedFeed(
                    limit = PAGE_SIZE,
                    offset = offset,
                    currentUserId = userId,
                    userLat = if (hasLocation) location?.latitude else null,
                    userLon = if (hasLocation) location?.longitude else null
                )
                FeedFilter.NEARBY -> {
                    if (hasLocation) {
                        socialRepository.getNearbyFeed(
                            userLat = location!!.latitude,
                            userLon = location.longitude,
                            limit = PAGE_SIZE,
                            offset = offset,
                            currentUserId = userId,
                            userCity = location.city
                        )
                    } else {
                        // Strictly location-scoped: never show unrelated ratings.
                        Result.success(emptyList())
                    }
                }
                FeedFilter.MY_RATINGS -> {
                    if (userId != null) socialRepository.getUserRatings(userId, limit = PAGE_SIZE, offset = offset)
                    else Result.success(emptyList())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("SocialFeedViewModel: fetchPage failed: ${e::class.simpleName} - ${e.message}")
            Result.failure(e)
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
        if (_uiState.value.filter == filter) return
        _uiState.update {
            it.copy(
                filter = filter,
                feedItems = emptyList(),
                currentOffset = 0,
                hasMoreItems = true,
                errorMessage = null,
                isLoadingMore = false,
                scrollToIndex = null,
                scrollToRatingId = null
            )
        }
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
                                notificationService.notifyReviewLiked(
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
