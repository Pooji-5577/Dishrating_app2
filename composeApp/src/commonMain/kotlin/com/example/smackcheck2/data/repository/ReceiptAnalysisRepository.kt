package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.SupabaseConfig
import com.example.smackcheck2.util.Logger
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class ReceiptPriceSuggestion(
    val dishName: String,
    val price: Double,
    val confidence: Float
)

data class ReceiptAnalysisResult(
    val suggestions: List<ReceiptPriceSuggestion> = emptyList(),
    val rawItems: List<String> = emptyList(),
    val summary: String? = null
)

class ReceiptAnalysisRepository {
    private val supabase = SupabaseClientProvider.client
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient()

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analyzeReceipt(
        receiptBytes: ByteArray,
        dishNames: List<String>,
        mimeType: String = "image/jpeg"
    ): Result<ReceiptAnalysisResult> {
        if (receiptBytes.isEmpty()) return Result.success(ReceiptAnalysisResult())

        return try {
            val accessToken = currentAccessToken()
                ?: return Result.success(ReceiptAnalysisResult())

            val requestBody = ReceiptAnalysisRequest(
                imageBase64 = Base64.encode(receiptBytes),
                mimeType = mimeType,
                dishNames = dishNames
            )
            val backendResult = analyzeReceiptWithBackend(requestBody, accessToken)
            if (backendResult.isSuccess) {
                backendResult
            } else {
                val backendError = backendResult.exceptionOrNull()
                Logger.e("ReceiptAnalysisRepository", "Receipt analysis backend unavailable or empty, trying edge fallback: ${backendError?.message}", backendError)
                analyzeReceiptWithEdgeFunction(requestBody)
            }
        } catch (e: Exception) {
            Logger.e("ReceiptAnalysisRepository", "Receipt analysis failed: ${e.message}", e)
            analyzeReceiptWithEdgeFunction(
                ReceiptAnalysisRequest(
                    imageBase64 = Base64.encode(receiptBytes),
                    mimeType = mimeType,
                    dishNames = dishNames
                )
            )
        }
    }

    private suspend fun analyzeReceiptWithBackend(
        requestBody: ReceiptAnalysisRequest,
        accessToken: String
    ): Result<ReceiptAnalysisResult> {
        repeat(2) { attempt ->
            try {
                val response = httpClient.post("${SupabaseConfig.BACKEND_URL.trimEnd('/')}/api/ai/analyze-receipt") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    setBody(json.encodeToString(requestBody))
                }
                val responseText = response.body<String>()
                Logger.d("ReceiptAnalysisRepository", "Receipt analysis backend status ${response.status.value}, response: ${responseText.take(500)}")
                if (response.status.value in 200..299) {
                    val parsedResult = parseReceiptAnalysis(responseText)
                    if (parsedResult.suggestions.isNotEmpty() || parsedResult.rawItems.isNotEmpty()) {
                        return Result.success(parsedResult)
                    }
                    Logger.e("ReceiptAnalysisRepository", "Receipt analysis backend returned no prices or line items")
                } else if (response.status.value !in listOf(429, 500, 502, 503, 504)) {
                    return Result.failure(IllegalStateException("Backend receipt analysis failed ${response.status.value}: ${responseText.take(160)}"))
                }
            } catch (e: Exception) {
                Logger.e("ReceiptAnalysisRepository", "Receipt analysis backend attempt ${attempt + 1} failed: ${e.message}", e)
            }
            delay(350L)
        }
        return Result.failure(IllegalStateException("Backend receipt analysis returned no usable result"))
    }

    private suspend fun analyzeReceiptWithEdgeFunction(
        requestBody: ReceiptAnalysisRequest
    ): Result<ReceiptAnalysisResult> {
        repeat(2) { attempt ->
            try {
                val response = supabase.functions.invoke(
                    function = "analyze-receipt",
                    body = requestBody
                )
                val responseText = response.body<String>()
                Logger.d("ReceiptAnalysisRepository", "Receipt analysis edge status ${response.status.value}, response: ${responseText.take(500)}")
                if (response.status.value in 200..299) {
                    val parsedResult = parseReceiptAnalysis(responseText)
                    if (parsedResult.suggestions.isNotEmpty() || parsedResult.rawItems.isNotEmpty()) {
                        return Result.success(parsedResult)
                    }
                    Logger.e("ReceiptAnalysisRepository", "Receipt analysis edge returned no prices or line items")
                } else if (response.status.value !in listOf(429, 500, 502, 503, 504)) {
                    return Result.success(ReceiptAnalysisResult())
                }
            } catch (e: Exception) {
                Logger.e("ReceiptAnalysisRepository", "Receipt analysis edge attempt ${attempt + 1} failed: ${e.message}", e)
            }
            delay(350L)
        }
        return Result.success(ReceiptAnalysisResult())
    }

    private fun parseReceiptAnalysis(responseText: String): ReceiptAnalysisResult {
        val parsed = json.decodeFromString<ReceiptAnalysisResponse>(responseText)
        val matches = parsed.matches.ifEmpty { parsed.suggestions }
        val rawItems = parsed.receiptItems.ifEmpty { parsed.rawItems }
        return ReceiptAnalysisResult(
            suggestions = matches.mapNotNull { match ->
                val price = match.price ?: return@mapNotNull null
                val dishName = match.dishName.ifBlank { match.dish_name }
                if (dishName.isBlank()) return@mapNotNull null
                ReceiptPriceSuggestion(
                    dishName = dishName,
                    price = price,
                    confidence = match.confidence.coerceIn(0f, 1f)
                )
            },
            rawItems = rawItems,
            summary = parsed.summary
        )
    }

    private fun currentAccessToken(): String? {
        val status = supabase.auth.sessionStatus.value
        return (status as? SessionStatus.Authenticated)?.session?.accessToken
    }
}

@Serializable
private data class ReceiptAnalysisRequest(
    val imageBase64: String,
    val mimeType: String,
    val dishNames: List<String>
)

@Serializable
private data class ReceiptAnalysisResponse(
    val matches: List<ReceiptMatchResponse> = emptyList(),
    val suggestions: List<ReceiptMatchResponse> = emptyList(),
    val receiptItems: List<String> = emptyList(),
    val rawItems: List<String> = emptyList(),
    val summary: String? = null
)

@Serializable
private data class ReceiptMatchResponse(
    val dishName: String = "",
    val dish_name: String = "",
    val price: Double? = null,
    val confidence: Float = 0f
)
