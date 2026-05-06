package com.example.smackcheck2.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedUpdateTest {

    @Test
    fun new_post_update_holds_feed_item() {
        val item = com.example.smackcheck2.model.FeedItem(
            id = "r1", userId = "u1", userProfileImageUrl = null,
            userName = "Alice", dishImageUrl = null, dishName = "Burger",
            restaurantName = "Place", rating = 4f, likesCount = 0,
            commentsCount = 0, isLiked = false, timestamp = 0L
        )
        val update = FeedUpdate.NewPost(item)
        assertIs<FeedUpdate.NewPost>(update)
        assertEquals("r1", update.feedItem.id)
    }

    @Test
    fun post_deleted_update_holds_rating_id() {
        val update = FeedUpdate.PostDeleted("r1")
        assertIs<FeedUpdate.PostDeleted>(update)
        assertEquals("r1", update.ratingId)
    }

    @Test
    fun like_added_update_holds_ids() {
        val update = FeedUpdate.LikeAdded("r1", "u2")
        assertIs<FeedUpdate.LikeAdded>(update)
        assertEquals("r1", update.ratingId)
        assertEquals("u2", update.userId)
    }

    @Test
    fun like_removed_update_holds_ids() {
        val update = FeedUpdate.LikeRemoved("r1", "u2")
        assertIs<FeedUpdate.LikeRemoved>(update)
        assertEquals("r1", update.ratingId)
        assertEquals("u2", update.userId)
    }

    @Test
    fun comment_added_update_holds_rating_id() {
        val update = FeedUpdate.CommentAdded("r1")
        assertIs<FeedUpdate.CommentAdded>(update)
        assertEquals("r1", update.ratingId)
    }

    @Test
    fun comment_removed_update_holds_rating_id() {
        val update = FeedUpdate.CommentRemoved("r1")
        assertIs<FeedUpdate.CommentRemoved>(update)
        assertEquals("r1", update.ratingId)
    }
}

class NotificationUpdateTest {

    @Test
    fun new_notification_update_holds_notification() {
        val notif = com.example.smackcheck2.model.Notification(
            id = "n1", type = "new_post", title = "New Post",
            body = "Alice posted", isRead = false, createdAt = 0L
        )
        val update = NotificationUpdate.NewNotification(notif)
        assertIs<NotificationUpdate.NewNotification>(update)
        assertEquals("n1", update.notification.id)
    }

    @Test
    fun notification_read_update_holds_id() {
        val update = NotificationUpdate.NotificationRead("n1")
        assertIs<NotificationUpdate.NotificationRead>(update)
        assertEquals("n1", update.notificationId)
    }
}
