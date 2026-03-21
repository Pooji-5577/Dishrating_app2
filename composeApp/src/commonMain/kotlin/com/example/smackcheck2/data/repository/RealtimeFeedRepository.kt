package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.dto.*
import com.example.smackcheck2.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * RealtimeFeedRepository - Manages real-time feed updates using Supabase Realtime
 * 
 * Features:
 * - Real-time feed updates when new ratings are added
 * - Real-time like count updates
 * - Real-time comment count updates
 * - Real-time notification updates
 */
class RealtimeFeedRepository {
    
    private val client = SupabaseClientProvider.client
    private val postgrest = client.postgrest
    private val realtime = client.realtime
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Channels for different realtime subscriptions
    private var feedChannel: RealtimeChannel? = null
    private var likesChannel: RealtimeChannel? = null
    private var commentsChannel: RealtimeChannel? = null
    private var notificationsChannel: RealtimeChannel? = null
    
    // Flow to emit real-time feed updates
    private val _feedUpdates = MutableSharedFlow<FeedUpdate>(replay = 0, extraBufferCapacity = 64)
    val feedUpdates: SharedFlow<FeedUpdate> = _feedUpdates.asSharedFlow()
    
    // Flow to emit real-time notification updates
    private val _notificationUpdates = MutableSharedFlow<NotificationUpdate>(replay = 0, extraBufferCapacity = 64)
    val notificationUpdates: SharedFlow<NotificationUpdate> = _notificationUpdates.asSharedFlow()
    
    // Current feed items cache for efficient updates
    private val _feedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val feedItems: StateFlow<List<FeedItem>> = _feedItems.asStateFlow()
    
    /**
     * Subscribe to real-time feed updates for a user
     */
    suspend fun subscribeToFeed(userId: String) {
        try {
            // Create channel for feed updates
            feedChannel = realtime.channel("feed-$userId")
            
            // Listen for new ratings
            feedChannel?.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "ratings"
            }?.onEach { change ->
                println("RealtimeFeed: New rating received")
                val newRating = change.record
                // Fetch complete feed item and emit
                scope.launch {
                    try {
                        val ratingId = newRating["id"]?.toString()?.removeSurrounding("\"") ?: return@launch
                        val feedItem = fetchFeedItemById(ratingId, userId)
                        feedItem?.let {
                            _feedUpdates.emit(FeedUpdate.NewPost(it))
                            // Add to cache
                            _feedItems.update { items -> listOf(it) + items }
                        }
                    } catch (e: Exception) {
                        println("RealtimeFeed: Error processing new rating: ${e.message}")
                    }
                }
            }?.launchIn(scope)
            
            // Listen for rating deletions
            feedChannel?.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "ratings"
            }?.onEach { change ->
                println("RealtimeFeed: Rating deleted")
                val oldRating = change.oldRecord
                val ratingId = oldRating["id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                _feedUpdates.emit(FeedUpdate.PostDeleted(ratingId))
                // Remove from cache
                _feedItems.update { items -> items.filter { it.id != ratingId } }
            }?.launchIn(scope)
            
            // Subscribe to channel
            feedChannel?.subscribe()
            println("RealtimeFeed: Subscribed to feed channel for user $userId")
            
        } catch (e: Exception) {
            println("RealtimeFeed: Failed to subscribe to feed: ${e.message}")
        }
    }
    
    /**
     * Subscribe to real-time like updates
     */
    suspend fun subscribeToLikes(userId: String) {
        try {
            likesChannel = realtime.channel("likes-$userId")
            
            // Listen for new likes
            likesChannel?.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "likes"
            }?.onEach { change ->
                val newLike = change.record
                val ratingId = newLike["rating_id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                val likerUserId = newLike["user_id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                
                // Update like count in cache
                _feedItems.update { items ->
                    items.map { item ->
                        if (item.id == ratingId) {
                            item.copy(
                                likesCount = item.likesCount + 1,
                                isLiked = if (likerUserId == userId) true else item.isLiked
                            )
                        } else item
                    }
                }
                _feedUpdates.emit(FeedUpdate.LikeAdded(ratingId, likerUserId))
            }?.launchIn(scope)
            
            // Listen for unlike (delete)
            likesChannel?.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "likes"
            }?.onEach { change ->
                val oldLike = change.oldRecord
                val ratingId = oldLike["rating_id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                val unlikerUserId = oldLike["user_id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                
                // Update like count in cache
                _feedItems.update { items ->
                    items.map { item ->
                        if (item.id == ratingId) {
                            item.copy(
                                likesCount = maxOf(0, item.likesCount - 1),
                                isLiked = if (unlikerUserId == userId) false else item.isLiked
                            )
                        } else item
                    }
                }
                _feedUpdates.emit(FeedUpdate.LikeRemoved(ratingId, unlikerUserId))
            }?.launchIn(scope)
            
            likesChannel?.subscribe()
            println("RealtimeFeed: Subscribed to likes channel")
            
        } catch (e: Exception) {
            println("RealtimeFeed: Failed to subscribe to likes: ${e.message}")
        }
    }
    
    /**
     * Subscribe to real-time comment updates
     */
    suspend fun subscribeToComments(userId: String) {
        try {
            commentsChannel = realtime.channel("comments-$userId")
            
            // Listen for new comments
            commentsChannel?.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "comments"
            }?.onEach { change ->
                val newComment = change.record
                val ratingId = newComment["rating_id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                
                // Update comment count in cache
                _feedItems.update { items ->
                    items.map { item ->
                        if (item.id == ratingId) {
                            item.copy(commentsCount = item.commentsCount + 1)
                        } else item
                    }
                }
                _feedUpdates.emit(FeedUpdate.CommentAdded(ratingId))
            }?.launchIn(scope)
            
            // Listen for comment deletions
            commentsChannel?.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "comments"
            }?.onEach { change ->
                val oldComment = change.oldRecord
                val ratingId = oldComment["rating_id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                
                // Update comment count in cache
                _feedItems.update { items ->
                    items.map { item ->
                        if (item.id == ratingId) {
                            item.copy(commentsCount = maxOf(0, item.commentsCount - 1))
                        } else item
                    }
                }
                _feedUpdates.emit(FeedUpdate.CommentRemoved(ratingId))
            }?.launchIn(scope)
            
            commentsChannel?.subscribe()
            println("RealtimeFeed: Subscribed to comments channel")
            
        } catch (e: Exception) {
            println("RealtimeFeed: Failed to subscribe to comments: ${e.message}")
        }
    }
    
    /**
     * Subscribe to real-time notifications for a user
     */
    suspend fun subscribeToNotifications(userId: String) {
        try {
            notificationsChannel = realtime.channel("notifications-$userId")
            
            // Listen for new notifications
            // Note: Filtering is done client-side since channel is already user-specific
            notificationsChannel?.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
            }?.onEach { change ->
                val newNotification = change.record
                // Filter by user_id client-side
                val notificationUserId = newNotification["user_id"]?.toString()?.removeSurrounding("\"")
                if (notificationUserId != userId) return@onEach
                
                println("RealtimeFeed: New notification received")
                val notification = Notification(
                    id = newNotification["id"]?.toString()?.removeSurrounding("\"") ?: "",
                    type = newNotification["type"]?.toString()?.removeSurrounding("\"") ?: "",
                    title = newNotification["title"]?.toString()?.removeSurrounding("\"") ?: "",
                    body = newNotification["body"]?.toString()?.removeSurrounding("\"") ?: "",
                    isRead = false,
                    createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                )
                _notificationUpdates.emit(NotificationUpdate.NewNotification(notification))
            }?.launchIn(scope)
            
            // Listen for notification reads
            notificationsChannel?.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "notifications"
            }?.onEach { change ->
                val updatedNotification = change.record
                // Filter by user_id client-side
                val notificationUserId = updatedNotification["user_id"]?.toString()?.removeSurrounding("\"")
                if (notificationUserId != userId) return@onEach
                
                val notificationId = updatedNotification["id"]?.toString()?.removeSurrounding("\"") ?: return@onEach
                val isRead = updatedNotification["is_read"]?.toString() == "true"
                if (isRead) {
                    _notificationUpdates.emit(NotificationUpdate.NotificationRead(notificationId))
                }
            }?.launchIn(scope)
            
            notificationsChannel?.subscribe()
            println("RealtimeFeed: Subscribed to notifications channel for user $userId")
            
        } catch (e: Exception) {
            println("RealtimeFeed: Failed to subscribe to notifications: ${e.message}")
        }
    }
    
    /**
     * Fetch initial feed items for a user
     */
    suspend fun fetchFeed(
        userId: String,
        filter: FeedFilter = FeedFilter.ALL,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<FeedItem>> {
        return try {
            val feedItems = when (filter) {
                FeedFilter.ALL -> fetchAllFeed(userId, limit, offset)
                FeedFilter.FOLLOWING -> fetchFollowingFeed(userId, limit, offset)
                FeedFilter.NEARBY -> fetchNearbyFeed(userId, limit, offset)
            }
            _feedItems.value = feedItems
            Result.success(feedItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun fetchAllFeed(userId: String, limit: Int, offset: Int): List<FeedItem> {
        val ratings = postgrest["ratings"]
            .select {
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<RatingDto>()
        
        return mapRatingsToFeedItems(ratings, userId)
    }
    
    private suspend fun fetchFollowingFeed(userId: String, limit: Int, offset: Int): List<FeedItem> {
        // Get list of users the current user follows
        val following = try {
            postgrest["followers"]
                .select {
                    filter { eq("follower_id", userId) }
                }
                .decodeList<FollowerDto>()
        } catch (e: Exception) {
            emptyList()
        }
        
        val followingIds = following.map { it.followingId } + userId // Include own posts
        
        if (followingIds.isEmpty()) {
            return emptyList()
        }
        
        val ratings = postgrest["ratings"]
            .select {
                filter {
                    isIn("user_id", followingIds)
                }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<RatingDto>()
        
        return mapRatingsToFeedItems(ratings, userId)
    }
    
    private suspend fun fetchNearbyFeed(userId: String, limit: Int, offset: Int): List<FeedItem> {
        // For now, return all feed - nearby filtering would need location data
        // TODO: Implement location-based filtering when user location is available
        return fetchAllFeed(userId, limit, offset)
    }
    
    private suspend fun fetchFeedItemById(ratingId: String, currentUserId: String): FeedItem? {
        return try {
            val rating = postgrest["ratings"]
                .select {
                    filter { eq("id", ratingId) }
                }
                .decodeSingleOrNull<RatingDto>() ?: return null
            
            mapRatingsToFeedItems(listOf(rating), currentUserId).firstOrNull()
        } catch (e: Exception) {
            println("RealtimeFeed: Error fetching feed item: ${e.message}")
            null
        }
    }
    
    private suspend fun mapRatingsToFeedItems(
        ratings: List<RatingDto>,
        currentUserId: String?
    ): List<FeedItem> {
        return ratings.mapNotNull { rating ->
            try {
                val profile = postgrest["profiles"]
                    .select { filter { eq("id", rating.userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                
                val dish = postgrest["dishes"]
                    .select { filter { eq("id", rating.dishId) } }
                    .decodeSingleOrNull<DishDto>()
                
                val restaurant = postgrest["restaurants"]
                    .select { filter { eq("id", rating.restaurantId) } }
                    .decodeSingleOrNull<RestaurantDto>()
                
                // Get comments count
                val commentsCount = try {
                    postgrest["comments"]
                        .select {
                            filter { eq("rating_id", rating.id ?: "") }
                        }
                        .decodeList<CommentDto>()
                        .size
                } catch (e: Exception) { 0 }
                
                // Check if current user liked this rating
                val isLiked = if (currentUserId != null && rating.id != null) {
                    try {
                        val existing = postgrest["likes"]
                            .select {
                                filter {
                                    eq("user_id", currentUserId)
                                    eq("rating_id", rating.id)
                                }
                            }
                            .decodeSingleOrNull<LikeDto>()
                        existing != null
                    } catch (e: Exception) { false }
                } else false
                
                // Fetch additional images
                val additionalImages = try {
                    postgrest["rating_images"]
                        .select {
                            filter { eq("rating_id", rating.id ?: "") }
                            order("sort_order", Order.ASCENDING)
                        }
                        .decodeList<RatingImageDto>()
                        .map { it.imageUrl }
                } catch (e: Exception) { emptyList() }
                
                val allImages = buildList {
                    rating.imageUrl?.let { add(it) }
                    dish?.imageUrl?.let { if (rating.imageUrl == null) add(it) }
                    addAll(additionalImages)
                }
                
                FeedItem(
                    id = rating.id ?: return@mapNotNull null,
                    userId = rating.userId,
                    userProfileImageUrl = profile?.profilePhotoUrl,
                    userName = profile?.name ?: "Unknown",
                    dishImageUrl = allImages.firstOrNull(),
                    dishName = dish?.name ?: "Unknown Dish",
                    restaurantName = restaurant?.name ?: "Unknown Restaurant",
                    rating = rating.rating,
                    likesCount = rating.likesCount,
                    commentsCount = commentsCount,
                    isLiked = isLiked,
                    timestamp = parseTimestamp(rating.createdAt),
                    comment = rating.comment,
                    imageUrls = allImages
                )
            } catch (e: Exception) {
                println("RealtimeFeed: Error mapping rating: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Toggle like on a rating
     */
    suspend fun toggleLike(ratingId: String, userId: String): Result<Boolean> {
        return try {
            println("[DEBUG][RealtimeRepo] toggleLike: ratingId=$ratingId, userId=$userId")
            // Check if already liked
            val existingLike = try {
                postgrest["likes"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("rating_id", ratingId)
                        }
                    }
                    .decodeSingleOrNull<LikeDto>()
            } catch (e: Exception) {
                println("[DEBUG][RealtimeRepo] toggleLike: Query existing like failed: ${e::class.simpleName} - ${e.message}")
                null
            }

            if (existingLike != null) {
                // Unlike — remove from likes table
                println("[DEBUG][RealtimeRepo] toggleLike: Existing like found (id=${existingLike.id}), deleting...")
                postgrest["likes"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("rating_id", ratingId)
                    }
                }
                // Decrement likes_count on ratings table so count persists across reloads
                updateLikesCount(ratingId, delta = -1)
                println("[DEBUG][RealtimeRepo] toggleLike: DELETE success — unliked")
                Result.success(false)
            } else {
                // Like — generate UUID for id since the DB column has no default
                println("[DEBUG][RealtimeRepo] toggleLike: No existing like, inserting...")
                @OptIn(ExperimentalUuidApi::class)
                val dto = LikeDto(id = Uuid.random().toString(), userId = userId, ratingId = ratingId)
                postgrest["likes"].insert(dto)
                // Increment likes_count on ratings table so count persists across reloads
                updateLikesCount(ratingId, delta = 1)
                println("[DEBUG][RealtimeRepo] toggleLike: INSERT success — liked")
                Result.success(true)
            }
        } catch (e: Exception) {
            println("[DEBUG][RealtimeRepo] toggleLike FAILED: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()
            println("RealtimeFeed: toggleLike error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update the likes_count column on the ratings table.
     * This ensures the count is persisted and correct when the feed reloads.
     */
    private suspend fun updateLikesCount(ratingId: String, delta: Int) {
        try {
            val rating = postgrest["ratings"]
                .select {
                    filter { eq("id", ratingId) }
                }
                .decodeSingleOrNull<RatingDto>()

            if (rating != null) {
                val newCount = (rating.likesCount + delta).coerceAtLeast(0)
                postgrest["ratings"]
                    .update(mapOf("likes_count" to newCount)) {
                        filter { eq("id", ratingId) }
                    }
            }
        } catch (e: Exception) {
            println("RealtimeFeed: updateLikesCount error: ${e.message}")
        }
    }
    
    /**
     * Unsubscribe from all real-time channels
     */
    suspend fun unsubscribeAll() {
        try {
            feedChannel?.unsubscribe()
            likesChannel?.unsubscribe()
            commentsChannel?.unsubscribe()
            notificationsChannel?.unsubscribe()
            
            feedChannel = null
            likesChannel = null
            commentsChannel = null
            notificationsChannel = null
            
            println("RealtimeFeed: Unsubscribed from all channels")
        } catch (e: Exception) {
            println("RealtimeFeed: Error unsubscribing: ${e.message}")
        }
    }
    
    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return 0L
        return try {
            Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * Sealed class representing different types of feed updates
 */
sealed class FeedUpdate {
    data class NewPost(val feedItem: FeedItem) : FeedUpdate()
    data class PostDeleted(val ratingId: String) : FeedUpdate()
    data class LikeAdded(val ratingId: String, val userId: String) : FeedUpdate()
    data class LikeRemoved(val ratingId: String, val userId: String) : FeedUpdate()
    data class CommentAdded(val ratingId: String) : FeedUpdate()
    data class CommentRemoved(val ratingId: String) : FeedUpdate()
}

/**
 * Sealed class representing notification updates
 */
sealed class NotificationUpdate {
    data class NewNotification(val notification: Notification) : NotificationUpdate()
    data class NotificationRead(val notificationId: String) : NotificationUpdate()
}
