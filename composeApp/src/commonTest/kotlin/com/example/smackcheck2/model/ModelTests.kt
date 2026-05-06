package com.example.smackcheck2.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DishRatingUiStateTest {

    @Test
    fun default_state_has_empty_values() {
        val state = DishRatingUiState()
        assertEquals("", state.dishName)
        assertEquals("", state.imageUri)
        assertEquals(0f, state.rating)
        assertEquals("", state.comment)
        assertEquals(emptyList<String>(), state.tags)
        assertEquals("", state.price)
        assertFalse(state.isSubmitting)
        assertFalse(state.isSuccess)
        assertEquals(null, state.errorMessage)
        assertEquals(null, state.xpEarned)
        assertFalse(state.showXpNotification)
        assertEquals(null, state.submittedRatingId)
    }
}

class FeedItemTest {

    @Test
    fun feed_item_holds_all_fields() {
        val item = FeedItem(
            id = "r1",
            userId = "u1",
            userProfileImageUrl = "https://avatar.jpg",
            userName = "Alice",
            dishImageUrl = "https://dish.jpg",
            dishName = "Burger",
            dishId = "d1",
            restaurantName = "Best Burgers",
            restaurantCity = "NYC",
            rating = 4.5f,
            likesCount = 10,
            commentsCount = 3,
            isLiked = true,
            isBookmarked = false,
            timestamp = 1234567890L,
            comment = "Great!",
            imageUrls = listOf("https://dish.jpg", "https://dish2.jpg"),
            price = 12.99
        )

        assertEquals("r1", item.id)
        assertEquals("u1", item.userId)
        assertEquals("Alice", item.userName)
        assertEquals("Burger", item.dishName)
        assertEquals("Best Burgers", item.restaurantName)
        assertEquals(4.5f, item.rating)
        assertEquals(10, item.likesCount)
        assertEquals(3, item.commentsCount)
        assertTrue(item.isLiked)
        assertEquals(12.99, item.price)
        assertEquals(2, item.imageUrls.size)
    }
}

class SocialFeedUiStateTest {

    @Test
    fun default_state_has_empty_feed() {
        val state = SocialFeedUiState()
        assertEquals(emptyList<FeedItem>(), state.feedItems)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertFalse(state.isLoadingMore)
        assertTrue(state.hasMoreItems)
        assertEquals(0, state.currentOffset)
        assertEquals(FeedFilter.FOLLOWING, state.filter)
        assertEquals(null, state.errorMessage)
        assertEquals(null, state.scrollToRatingId)
        assertEquals(null, state.scrollToIndex)
        assertEquals(emptyList<UserSummary>(), state.storyUsers)
        assertEquals(emptyList<FeedItem>(), state.topDishes)
        assertEquals(0, state.nearbyRestaurantCount)
    }
}
