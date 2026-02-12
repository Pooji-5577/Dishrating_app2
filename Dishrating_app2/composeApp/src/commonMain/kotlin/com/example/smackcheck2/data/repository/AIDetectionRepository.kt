package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
 * Repository for AI-powered dish detection using Google Gemini API
 */
class AIDetectionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Detect dish name from image bytes using Google Gemini API
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun detectDish(imageBytes: ByteArray, mimeType: String = "image/jpeg"): DishDetectionResult {
        val apiKey = SupabaseConfig.GEMINI_API_KEY

        // Check if API key is configured
        if (apiKey.isEmpty() || apiKey.startsWith("MISSING_") || apiKey == "your_gemini_api_key_here") {
            println("AIDetection: GEMINI_API_KEY not configured. Value: ${apiKey.take(15)}...")
            return createFallbackResult("Unknown Dish").copy(
                alternatives = listOf("API key not configured"),
                debugInfo = "ERROR: API key not configured"
            )
        }

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

            // Build the request
            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = actualMimeType,
                                    data = base64Image
                                )
                            ),
                            GeminiPart(text = DETECTION_PROMPT)
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.4f,
                    maxOutputTokens = 500
                )
            )

            // Use gemini-3-flash-preview for image analysis (newer model)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey"

            println("AIDetection: Sending request to Gemini API (gemini-3-flash-preview)...")

            val response: HttpResponse = try {
                httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
            } catch (e: Exception) {
                println("AIDetection: Network error: ${e.message}")
                return createFallbackResult("Unknown Dish").copy(
                    alternatives = listOf("Network error - check internet connection"),
                    debugInfo = "Network exception: ${e.message}"
                )
            }

            println("AIDetection: Response status: ${response.status.value}")

            // Get raw response text first for debugging
            val responseText = response.bodyAsText()
            println("AIDetection: Raw response (first 500 chars): ${responseText.take(500)}")

            if (response.status.value != 200) {
                println("AIDetection: API error response: $responseText")

                // Handle specific HTTP errors
                val errorMessage = when (response.status.value) {
                    429 -> "API rate limit exceeded. Please try again in a few moments."
                    401 -> "API key invalid or expired. Please check your Gemini API key."
                    403 -> "API key doesn't have permission. Check Google Cloud Console."
                    500, 503 -> "Gemini API server error. Please try again later."
                    else -> "API error: ${response.status.value}"
                }

                return createFallbackResult("Unknown Dish").copy(
                    alternatives = listOf(errorMessage),
                    debugInfo = "HTTP ${response.status.value}: ${responseText.take(100)}"
                )
            }

            // Parse the response
            val geminiResponse = json.decodeFromString<GeminiResponse>(responseText)
            val content = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            println("AIDetection: Extracted content: ${content.take(300)}")

            if (content.isBlank()) {
                println("AIDetection: Empty content from API")
                return createFallbackResult("Unknown Dish").copy(
                    debugInfo = "Empty response from Gemini API"
                )
            }

            parseDetectionResponse(content).copy(
                debugInfo = "OK: ${content.take(50)}..."
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

    private fun parseDetectionResponse(content: String): DishDetectionResult {
        return try {
            // Extract JSON from response (may be wrapped in markdown code block)
            val jsonStr = extractJson(content)
            println("AIDetection: Extracted JSON: $jsonStr")

            val parsed = json.decodeFromString<DetectionResponse>(jsonStr)
            println("AIDetection: Parsed dish_name: ${parsed.dish_name}, confidence: ${parsed.confidence}")

            // Validate parsed result
            val dishName = if (parsed.dish_name.isBlank() || parsed.dish_name.equals("Unknown", ignoreCase = true)) {
                "Unknown Dish"
            } else {
                parsed.dish_name
            }

            DishDetectionResult(
                dishName = dishName,
                confidence = parsed.confidence.coerceIn(0f, 1f),
                alternatives = parsed.alternatives.filter { it.isNotBlank() },
                cuisine = parsed.cuisine?.takeIf { it.isNotBlank() },
                isAIDetected = dishName != "Unknown Dish" && parsed.confidence > 0.1f
            )
        } catch (e: Exception) {
            println("AIDetection: Parse error: ${e.message}")
            e.printStackTrace()

            // Try to extract dish name from raw text as fallback
            val fallbackName = tryExtractDishNameFromText(content)
            createFallbackResult(fallbackName)
        }
    }

    private fun tryExtractDishNameFromText(content: String): String {
        // Simple heuristic: look for common patterns
        val patterns = listOf(
            Regex("\"dish_name\"\\s*:\\s*\"([^\"]+)\""),
            Regex("dish[_\\s]?name[\"']?\\s*[=:]\\s*[\"']?([^\"',}]+)"),
            Regex("This (?:appears to be|looks like|is)(?: a| an)?\\s+([^.!,]+)", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null && match.groupValues.size > 1) {
                val extracted = match.groupValues[1].trim()
                if (extracted.isNotBlank() && extracted.length < 50) {
                    println("AIDetection: Extracted dish name from text: $extracted")
                    return extracted
                }
            }
        }

        return "Unknown Dish"
    }

    private fun extractJson(content: String): String {
        // Try to extract JSON from markdown code block
        val codeBlockPattern = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = codeBlockPattern.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // Try to find JSON object directly
        val jsonPattern = Regex("\\{[\\s\\S]*\\}")
        val jsonMatch = jsonPattern.find(content)
        if (jsonMatch != null) {
            return jsonMatch.value
        }

        return content
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

    companion object {
        private const val DETECTION_PROMPT = """You are a food identification expert. Look at this image and identify the food dish shown.

IMPORTANT: You MUST respond with ONLY a JSON object, no other text before or after.

Response format (JSON only):
{"dish_name":"Pizza Margherita","cuisine":"Italian","confidence":0.9,"alternatives":["Cheese Pizza","Flatbread"]}

Guidelines:
1. dish_name: The most common English name for this dish (be specific, e.g., "Chicken Tikka Masala" not just "Curry")
2. cuisine: The type of cuisine (Italian, Indian, Mexican, Chinese, American, Japanese, Thai, etc.)
3. confidence: A number between 0.0 and 1.0 indicating how certain you are
4. alternatives: 1-3 other possible names for the dish if you're not 100% sure

If the image doesn't clearly show food or you cannot identify it, respond with:
{"dish_name":"Unknown Dish","cuisine":"Unknown","confidence":0.0,"alternatives":[]}

Remember: Output ONLY the JSON, nothing else."""
    }
}

// Gemini API Request/Response DTOs
@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
private data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
private data class GeminiGenerationConfig(
    val temperature: Float = 0.4f,
    val maxOutputTokens: Int = 300
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiResponseContent? = null
)

@Serializable
private data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>? = null
)

@Serializable
private data class GeminiResponsePart(
    val text: String? = null
)

@Serializable
private data class DetectionResponse(
    val dish_name: String,
    val cuisine: String? = null,
    val confidence: Float = 0f,
    val alternatives: List<String> = emptyList()
)
