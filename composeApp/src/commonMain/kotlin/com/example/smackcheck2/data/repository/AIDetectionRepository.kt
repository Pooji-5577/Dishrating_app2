package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
    val debugInfo: String? = null
)

/**
 * Repository for AI-powered dish detection using Supabase Edge Function
 * 
 * This implementation calls the 'analyze-dish' Edge Function which handles
 * the Gemini API call server-side, keeping the API key secure.
 */
class AIDetectionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val supabase = SupabaseClientProvider.client

    /**
     * Detect dish name from image bytes using Supabase Edge Function
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun detectDish(imageBytes: ByteArray, mimeType: String = "image/jpeg"): DishDetectionResult {
        // Validate image bytes
        if (imageBytes.isEmpty()) {
            println("AIDetection: Image bytes are empty!")
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

        println("AIDetection: Starting detection with ${imageBytes.size} bytes, mimeType: $actualMimeType")

        return try {
            // Encode image to base64
            val base64Image = Base64.encode(imageBytes)
            println("AIDetection: Base64 encoded successfully, length: ${base64Image.length}")

            // Build the request body
            val requestBody = EdgeFunctionRequest(
                imageBase64 = base64Image,
                mimeType = actualMimeType
            )

            println("AIDetection: Calling Supabase Edge Function 'analyze-dish'...")

            // Call the Supabase Edge Function
            val response = supabase.functions.invoke(
                function = "analyze-dish",
                body = requestBody
            )

            println("AIDetection: Response status: ${response.status.value}")

            if (response.status.value != 200) {
                val errorText = response.body<String>()
                println("AIDetection: Edge Function error: $errorText")

                // Handle specific HTTP errors
                val errorMessage = when (response.status.value) {
                    401 -> "Authentication required. Please log in."
                    403 -> "Access denied. Check your permissions."
                    429 -> "Rate limit exceeded. Please try again in a few moments."
                    500, 503 -> "Server error. Please try again later."
                    else -> "Error: ${response.status.value}"
                }

                return createFallbackResult("Unknown").copy(
                    alternatives = listOf(errorMessage),
                    debugInfo = "HTTP ${response.status.value}: ${errorText.take(100)}"
                )
            }

            // Parse the response
            val responseText = response.body<String>()
            println("AIDetection: Response (first 500 chars): ${responseText.take(500)}")

            // Robust deserialization: if full JSON parsing fails, fall back to regex extraction
            val edgeResponse = try {
                json.decodeFromString<EdgeFunctionResponse>(responseText)
            } catch (parseException: Exception) {
                println("AIDetection: JSON parse failed (${parseException.message}), using regex fallback...")
                val dishNameMatch = Regex("\"dishName\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
                val confidenceMatch = Regex("\"confidence\"\\s*:\\s*([\\d.]+)").find(responseText)
                val cuisineMatch = Regex("\"cuisine\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
                val itemTypeMatch = Regex("\"itemType\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
                val errorMatch = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
                val extracted = dishNameMatch?.groupValues?.getOrNull(1) ?: ""
                val extractedConf = confidenceMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
                println("AIDetection: Regex extracted -> dishName='$extracted', confidence=$extractedConf")
                EdgeFunctionResponse(
                    dishName = extracted,
                    cuisine = cuisineMatch?.groupValues?.getOrNull(1) ?: "",
                    confidence = extractedConf,
                    itemType = itemTypeMatch?.groupValues?.getOrNull(1) ?: "unknown",
                    error = errorMatch?.groupValues?.getOrNull(1)
                )
            }

            // Check for error in response
            if (!edgeResponse.error.isNullOrBlank()) {
                println("AIDetection: Edge Function returned error: ${edgeResponse.error}")
                return createFallbackResult("Unknown").copy(
                    alternatives = listOf(edgeResponse.error!!),
                    debugInfo = "Edge Function error: ${edgeResponse.error}"
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
            val itemType = when (edgeResponse.itemType.lowercase()) {
                "food" -> "food"
                "beverage" -> "beverage"
                else -> if (dishName != "Unknown") inferItemTypeFromName(dishName) else "unknown"
            }

            println("AIDetection: Final dishName='$dishName', itemType='$itemType', confidence=${edgeResponse.confidence}")

            DishDetectionResult(
                dishName = dishName,
                confidence = edgeResponse.confidence.coerceIn(0f, 1f),
                alternatives = edgeResponse.alternatives,
                cuisine = edgeResponse.cuisine.takeIf { it.isNotBlank() },
                // Detected if we have a real name — removed the confidence > 0.1 gate
                isAIDetected = dishName != "Unknown",
                itemType = itemType,
                debugInfo = "OK via Edge Function (type=$itemType, conf=${edgeResponse.confidence})"
            )

        } catch (e: Exception) {
            println("AIDetection: Exception: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()
            createFallbackResult("Unknown").copy(
                alternatives = listOf("Error: ${e.message?.take(40) ?: "Unknown"}"),
                debugInfo = "Exception: ${e::class.simpleName} - ${e.message?.take(80)}"
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
    val confidence: Float = 0f,
    val alternatives: List<String> = emptyList(),
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val itemType: String = "unknown",
    val error: String? = null
)
