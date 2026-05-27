package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.example.smackcheck2.util.Logger

/**
 * Result of AI dish detection
 */
data class DishDetectionResult(
    val dishName: String,
    val confidence: Float,
    val alternatives: List<String>,
    val cuisine: String?,
    val isAIDetected: Boolean,
    val itemType: String = "unknown", // "food", "beverage", or "unknown"
    val restaurantChain: String? = null, // e.g. "Starbucks" when branded packaging is visible
    val restaurantType: String? = null,  // e.g. "cafe", "pizzeria", "fast food"
    val debugInfo: String? = null,
    val isOutage: Boolean = false // true when AI service is unavailable
)

/**
 * Repository for AI-powered dish detection using Supabase Edge Function
 * 
 * This implementation calls the 'analyze-dish' Edge Function which handles
 * the Gemini API call server-side, keeping the API key secure.
 */
class AIDetectionRepository {

    /**
     * Detect dish name from image bytes using the SmackCheck backend.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun detectDish(imageBytes: ByteArray, mimeType: String = "image/jpeg"): DishDetectionResult {
        // Validate image bytes
        if (imageBytes.isEmpty()) {
            Logger.d("AIDetectionRepository", "AIDetection: Image bytes are empty!")
            return createFallbackResult("Unknown").copy(
                debugInfo = "ERROR: Image bytes empty"
            )
        }

        // Determine correct mime type
        val actualMimeType = when {
            mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "image/jpeg"
            mimeType.contains("png", ignoreCase = true) -> "image/png"
            mimeType.contains("webp", ignoreCase = true) -> "image/webp"
            mimeType.contains("gif", ignoreCase = true) -> "image/gif"
            else -> "image/jpeg" // Default to JPEG
        }

        Logger.d("AIDetectionRepository", "AIDetection: Starting detection with ${imageBytes.size} bytes, mimeType: $actualMimeType")

        return try {
            // Encode image to base64
            val base64Image = Base64.encode(imageBytes)
            Logger.d("AIDetectionRepository", "AIDetection: Base64 encoded successfully, length: ${base64Image.length}")

            val requestBody = EdgeFunctionRequest(
                imageBase64 = base64Image,
                mimeType = actualMimeType
            )

            Logger.d("AIDetectionRepository", "AIDetection: Calling backend /api/ai/detect-dish...")
            val edgeResponse = ApiClient.post<EdgeFunctionRequest, EdgeFunctionResponse>(
                "ai/detect-dish",
                requestBody
            )

            // Check for error in response
            if (!edgeResponse.error.isNullOrBlank()) {
                Logger.e("AIDetectionRepository", "AIDetection: Edge Function returned error: ${edgeResponse.error}")
                return createFallbackResult("Unknown").copy(
                    alternatives = listOf(edgeResponse.error!!),
                    debugInfo = "Backend AI error: ${edgeResponse.error}"
                )
            }

            // Normalise dish name
            val dishName = when {
                edgeResponse.dishName.isBlank() -> "Unknown"
                edgeResponse.dishName.equals("Unknown", ignoreCase = true) -> "Unknown"
                edgeResponse.dishName.equals("Unknown Dish", ignoreCase = true) -> "Unknown"
                else -> edgeResponse.dishName
            }

            // Normalise item type — only accept known values
            // If AI didn't return a type, infer from the dish name as a fallback
            val itemType = when {
                edgeResponse.isFood == false -> "unknown"
                edgeResponse.itemType.equals("food", ignoreCase = true) -> "food"
                edgeResponse.itemType.equals("beverage", ignoreCase = true) -> "beverage"
                else -> if (dishName != "Unknown") inferItemTypeFromName(dishName) else "unknown"
            }

            Logger.d("AIDetectionRepository", "AIDetection: Final dishName='$dishName', itemType='$itemType', confidence=${edgeResponse.confidence}")

            DishDetectionResult(
                dishName = dishName,
                confidence = edgeResponse.confidence.coerceIn(0f, 1f),
                alternatives = edgeResponse.alternatives,
                cuisine = (edgeResponse.cuisine.takeIf { it.isNotBlank() } ?: edgeResponse.cuisineType),
                // Detected if we have a real name — removed the confidence > 0.1 gate
                isAIDetected = dishName != "Unknown" && itemType != "unknown",
                itemType = itemType,
                restaurantChain = edgeResponse.restaurantChain.takeIf { it.isNotBlank() },
                restaurantType = edgeResponse.restaurantType.takeIf { it.isNotBlank() },
                debugInfo = "OK via backend AI (type=$itemType, conf=${edgeResponse.confidence}, chain=${edgeResponse.restaurantChain})"
            )

        } catch (e: Exception) {
            Logger.e("AIDetectionRepository", "AIDetection: Exception: ${e::class.simpleName} - ${e.message}", e)
            createFallbackResult("Unknown").copy(
                alternatives = listOf("AI service unavailable. Please enter dish name manually."),
                debugInfo = "Exception: ${e::class.simpleName} - ${e.message?.take(80)}",
                isOutage = true
            )
        }
    }

    /**
     * Infer item type from the dish/drink name when the AI doesn't provide item_type.
     * Matches common beverage keywords; everything else is treated as food.
     */
    private fun inferItemTypeFromName(dishName: String): String {
        val lower = dishName.lowercase()
        val beverageKeywords = listOf(
            "coffee", "tea", "juice", "beer", "wine", "cocktail", "smoothie", "shake",
            "milkshake", "latte", "cappuccino", "espresso", "chai", "soda", "cola",
            "water", "drink", "beverage", "mojito", "lemonade", "cider", "punch",
            "americano", "macchiato", "mocha", "frappe", "matcha", "lassi", "kombucha",
            "whiskey", "vodka", "rum", "gin", "ale", "lager", "sangria", "liquor",
            "margarita", "daiquiri", "spritzer", "tonic", "fizz", "brew", "shot"
        )
        return if (beverageKeywords.any { lower.contains(it) }) "beverage" else "food"
    }

    private fun createFallbackResult(dishName: String): DishDetectionResult {
        return DishDetectionResult(
            dishName = dishName,
            confidence = 0f,
            alternatives = emptyList(),
            cuisine = null,
            isAIDetected = false,
            itemType = "unknown"
        )
    }
}

// Edge Function Request/Response DTOs

@Serializable
private data class EdgeFunctionRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg"
)

@Serializable
private data class EdgeFunctionResponse(
    val dishName: String = "",
    val cuisine: String = "",
    val cuisineType: String? = null,
    val confidence: Float = 0f,
    val alternatives: List<String> = emptyList(),
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val isFood: Boolean? = null,
    val itemType: String = "unknown",
    val restaurantChain: String = "",
    val restaurantType: String = "",
    val error: String? = null
)
