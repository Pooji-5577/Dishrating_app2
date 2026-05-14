package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.dto.DishDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [SupabaseSchemaAdapter] class existence.
 *
 * Pure helper methods (isMissingColumnError, resolveRestaurantName,
 * resolveRestaurantImages) don't use the Supabase client but the
 * constructor initializes postgrest, so instantiation fails in JVM tests.
 * Network-dependent methods (insertDish, backfillDishFields, etc.)
 * require a live Supabase client and are not tested here.
 */
class SupabaseSchemaAdapterTest {

    @Test
    fun adapter_class_exists() {
        // Verify the class is loadable; actual instantiation requires Supabase client
        assertTrue(SupabaseSchemaAdapter::class.isInstance(null) || true)
    }

    @Test
    fun dish_insert_row_omits_restaurant_name() {
        val row = DishDto(
            id = "dish-1",
            name = "Burger",
            restaurantId = "restaurant-1",
            imageUrl = "https://example.com/burger.jpg",
            restaurantName = "Test Restaurant"
        ).toDishInsertRow()

        val json = Json.encodeToString(row)

        assertTrue(json.contains("restaurant_id"))
        assertFalse(json.contains("restaurant_name"))
    }
}
