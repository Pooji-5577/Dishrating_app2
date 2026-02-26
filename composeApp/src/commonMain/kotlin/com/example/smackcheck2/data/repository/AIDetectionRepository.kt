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
            return createFallbackResult("Unknown Dish").copy(
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

                return createFallbackResult("Unknown Dish").copy(
                    alternatives = listOf(errorMessage),
                    debugInfo = "HTTP ${response.status.value}: ${errorText.take(100)}"
                )
            }

            // Parse the response
            val responseText = response.body<String>()
            println("AIDetection: Response (first 500 chars): ${responseText.take(500)}")

            val edgeResponse = json.decodeFromString<EdgeFunctionResponse>(responseText)

            // Check for error in response
            if (!edgeResponse.error.isNullOrBlank()) {
                println("AIDetection: Edge Function returned error: ${edgeResponse.error}")
                return createFallbackResult("Unknown Dish").copy(
                    alternatives = listOf(edgeResponse.error),
                    debugInfo = "Edge Function error: ${edgeResponse.error}"
                )
            }

            // Map response to DishDetectionResult
            val dishName = if (edgeResponse.dishName.isBlank() || edgeResponse.dishName.equals("Unknown", ignoreCase = true)) {
                "Unknown Dish"
            } else {
                edgeResponse.dishName
            }

            DishDetectionResult(
                dishName = dishName,
                confidence = edgeResponse.confidence.coerceIn(0f, 1f),
                alternatives = edgeResponse.alternatives,
                cuisine = edgeResponse.cuisine.takeIf { it.isNotBlank() },
                isAIDetected = dishName != "Unknown Dish" && edgeResponse.confidence > 0.1f,
                debugInfo = "OK via Edge Function"
            )

        } catch (e: Exception) {
            println("AIDetection: Exception: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()
            createFallbackResult("Unknown Dish").copy(
                alternatives = listOf("Error: ${e.message?.take(40) ?: "Unknown"}"),
                debugInfo = "Exception: ${e::class.simpleName} - ${e.message?.take(80)}"
            )
        }
    }

    private fun createFallbackResult(dishName: String): DishDetectionResult {
        return DishDetectionResult(
            dishName = dishName,
            confidence = 0f,
            alternatives = emptyList(),
            cuisine = null,
            isAIDetected = false
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
    val error: String? = null
)
