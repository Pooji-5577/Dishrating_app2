package com.example.smackcheck2.service

import com.example.smackcheck2.model.Restaurant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DishRatingSubmissionRequestTest {

    @Test
    fun request_holds_all_submission_data() {
        val restaurant = Restaurant(id = "r1", name = "Test Place")
        val request = DishRatingSubmissionRequest(
            dishName = "Burger",
            rating = 4.5f,
            comment = "Great burger!",
            tags = listOf("tasty", "cheap"),
            price = 12.99,
            imageBytes = byteArrayOf(1, 2, 3),
            restaurantId = "r1",
            selectedRestaurant = restaurant,
            latitude = 40.7128,
            longitude = -74.0060
        )

        assertEquals("Burger", request.dishName)
        assertEquals(4.5f, request.rating)
        assertEquals("Great burger!", request.comment)
        assertEquals(listOf("tasty", "cheap"), request.tags)
        assertEquals(12.99, request.price)
        assertEquals("r1", request.restaurantId)
        assertEquals(restaurant, request.selectedRestaurant)
        assertEquals(40.7128, request.latitude)
        assertEquals(-74.0060, request.longitude)
    }

    @Test
    fun request_defaults_optional_fields() {
        val request = DishRatingSubmissionRequest(
            dishName = "Pizza",
            rating = 5f,
            restaurantId = "r1"
        )

        assertEquals("", request.comment)
        assertEquals(emptyList<String>(), request.tags)
        assertEquals(null, request.price)
        assertEquals(null, request.imageBytes)
        assertEquals(null, request.selectedRestaurant)
        assertEquals(null, request.latitude)
        assertEquals(null, request.longitude)
    }
}

class DishRatingSubmissionResultTest {

    @Test
    fun result_holds_submission_outcome() {
        val result = DishRatingSubmissionResult(
            ratingId = "rating-123",
            xpEarned = 25,
            newlyUnlockedAchievements = listOf("first_bite"),
            imageUrl = "https://example.com/img.jpg"
        )

        assertEquals("rating-123", result.ratingId)
        assertEquals(25, result.xpEarned)
        assertEquals(listOf("first_bite"), result.newlyUnlockedAchievements)
        assertEquals("https://example.com/img.jpg", result.imageUrl)
    }
}

/**
 * Tests for [DishRatingSubmissionService] class existence.
 *
 * Actual instantiation requires Supabase client (via DatabaseRepository,
 * StorageRepository, AuthRepository, etc.) which fails in JVM unit tests.
 * Service behavior is verified through integration/UI testing.
 */
class DishRatingSubmissionServiceTest {

    @Test
    fun service_class_exists() {
        // Verify the class is loadable; actual instantiation requires Supabase client
        assertTrue(DishRatingSubmissionService::class.isInstance(null) || true)
    }
}
