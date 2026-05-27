package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
 * Repository for AI-powered dish detection using the backend API.
 * 
 * The backend handles the Gemini API call server-side, keeping the API key secure.
 */
class AIDetectionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val supabase = SupabaseClientProvider.client
    private val httpClient = HttpClient()

    /**
     * Detect dish name from image bytes using the backend API.
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

            // Build the request body
            val requestBody = EdgeFunctionRequest(
                imageBase64 = base64Image,
                mimeType = actualMimeType
            )

            val accessToken = currentAccessToken()
            if (accessToken == null) {
                Logger.e("AIDetectionRepository", "AIDetection: Missing authenticated session")
                return createFallbackResult("Unknown").copy(
                    alternatives = listOf("Authentication required. Please log in."),
                    debugInfo = "No access token"
                )
            }

            val edgeResponse = detectDishWithBackend(requestBody, accessToken)
                .getOrElse { backendError ->
                    Logger.e("AIDetectionRepository", "AIDetection: Backend unavailable or empty, trying Edge Function: ${backendError.message}", backendError)
                    detectDishWithEdgeFunction(requestBody).getOrElse { edgeError ->
                        return createFallbackResult("Unknown").copy(
                            alternatives = listOf("AI service unavailable. Please enter dish name manually."),
                            debugInfo = "Backend: ${backendError.message?.take(80)}; Edge: ${edgeError.message?.take(80)}",
                            isOutage = true
                        )
                    }
                }

            // Check for error in response
            if (!edgeResponse.error.isNullOrBlank()) {
                Logger.e("AIDetectionRepository", "AIDetection: Edge Function returned error: ${edgeResponse.error}")
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

            Logger.d("AIDetectionRepository", "AIDetection: Final dishName='$dishName', itemType='$itemType', confidence=${edgeResponse.confidence}")

            DishDetectionResult(
                dishName = dishName,
                confidence = edgeResponse.confidence.coerceIn(0f, 1f),
                alternatives = edgeResponse.alternatives,
                cuisine = edgeResponse.cuisine.takeIf { it.isNotBlank() },
                // Detected if we have a real name — removed the confidence > 0.1 gate
                isAIDetected = dishName != "Unknown",
                itemType = itemType,
                restaurantChain = edgeResponse.restaurantChain.takeIf { it.isNotBlank() },
                restaurantType = edgeResponse.restaurantType.takeIf { it.isNotBlank() },
                debugInfo = "OK via backend (type=$itemType, conf=${edgeResponse.confidence}, chain=${edgeResponse.restaurantChain})"
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

    private suspend fun detectDishWithBackend(
        requestBody: EdgeFunctionRequest,
        accessToken: String
    ): Result<EdgeFunctionResponse> {
        repeat(2) { attempt ->
            try {
                Logger.d("AIDetectionRepository", "AIDetection: Calling backend /api/ai/detect-dish attempt ${attempt + 1}...")
                val response = httpClient.post("${SupabaseConfig.BACKEND_URL.trimEnd('/')}/api/ai/detect-dish") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    setBody(json.encodeToString(requestBody))
                }
                val responseText = response.body<String>()
                Logger.d("AIDetectionRepository", "AIDetection: Backend status ${response.status.value}, response: ${responseText.take(500)}")
                if (response.status.value in 200..299) {
                    val parsed = parseDishDetectionResponse(responseText)
                    if (parsed.isUsableDishResult()) return Result.success(parsed)
                    Logger.e("AIDetectionRepository", "AIDetection: Backend returned empty/unknown dish result")
                } else if (response.status.value !in listOf(429, 500, 502, 503, 504)) {
                    return Result.failure(IllegalStateException("Backend AI failed ${response.status.value}: ${responseText.take(160)}"))
                }
            } catch (e: Exception) {
                Logger.e("AIDetectionRepository", "AIDetection: Backend attempt ${attempt + 1} failed: ${e.message}", e)
            }
            delay(350L)
        }
        return Result.failure(IllegalStateException("Backend AI returned no usable dish result"))
    }

    private suspend fun detectDishWithEdgeFunction(
        requestBody: EdgeFunctionRequest
    ): Result<EdgeFunctionResponse> {
        repeat(2) { attempt ->
            try {
                Logger.d("AIDetectionRepository", "AIDetection: Calling analyze-dish Edge Function attempt ${attempt + 1}...")
                val response = supabase.functions.invoke(
                    function = "analyze-dish",
                    body = requestBody
                )
                val responseText = response.body<String>()
                Logger.d("AIDetectionRepository", "AIDetection: Edge status ${response.status.value}, response: ${responseText.take(500)}")
                if (response.status.value in 200..299) {
                    val parsed = parseDishDetectionResponse(responseText)
                    if (parsed.isUsableDishResult()) return Result.success(parsed)
                    Logger.e("AIDetectionRepository", "AIDetection: Edge returned empty/unknown dish result")
                } else if (response.status.value !in listOf(429, 500, 502, 503, 504)) {
                    return Result.failure(IllegalStateException("Edge AI failed ${response.status.value}: ${responseText.take(160)}"))
                }
            } catch (e: Exception) {
                Logger.e("AIDetectionRepository", "AIDetection: Edge attempt ${attempt + 1} failed: ${e.message}", e)
            }
            delay(350L)
        }
        return Result.failure(IllegalStateException("Edge AI returned no usable dish result"))
    }

    private fun parseDishDetectionResponse(responseText: String): EdgeFunctionResponse {
        return try {
            json.decodeFromString<EdgeFunctionResponse>(responseText)
        } catch (parseException: Exception) {
            Logger.e("AIDetectionRepository", "AIDetection: JSON parse failed (${parseException.message}), using regex fallback...", parseException)
            val dishNameMatch = Regex("\"dishName\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
                ?: Regex("\"dish_name\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
            val confidenceMatch = Regex("\"confidence\"\\s*:\\s*([\\d.]+)").find(responseText)
            val cuisineMatch = Regex("\"cuisine\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
            val itemTypeMatch = Regex("\"itemType\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
                ?: Regex("\"item_type\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
            val chainMatch = Regex("\"restaurantChain\"\\s*:\\s*\"([^\"]*)\"").find(responseText)
                ?: Regex("\"restaurant_chain\"\\s*:\\s*\"([^\"]*)\"").find(responseText)
            val typeMatch = Regex("\"restaurantType\"\\s*:\\s*\"([^\"]*)\"").find(responseText)
                ?: Regex("\"restaurant_type\"\\s*:\\s*\"([^\"]*)\"").find(responseText)
            val errorMatch = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
            EdgeFunctionResponse(
                dishName = dishNameMatch?.groupValues?.getOrNull(1) ?: "",
                cuisine = cuisineMatch?.groupValues?.getOrNull(1) ?: "",
                confidence = confidenceMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f,
                itemType = itemTypeMatch?.groupValues?.getOrNull(1) ?: "unknown",
                restaurantChain = chainMatch?.groupValues?.getOrNull(1) ?: "",
                restaurantType = typeMatch?.groupValues?.getOrNull(1) ?: "",
                error = errorMatch?.groupValues?.getOrNull(1)
            )
        }
    }

    private fun EdgeFunctionResponse.isUsableDishResult(): Boolean =
        error.isNullOrBlank() &&
            dishName.isNotBlank() &&
            !dishName.equals("Unknown", ignoreCase = true) &&
            !dishName.equals("Unknown Dish", ignoreCase = true)

    private fun currentAccessToken(): String? {
        val status = supabase.auth.sessionStatus.value
        return (status as? SessionStatus.Authenticated)?.session?.accessToken
    }
}

// Backend Request/Response DTOs

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
    val restaurantChain: String = "",
    val restaurantType: String = "",
    val error: String? = null
)
