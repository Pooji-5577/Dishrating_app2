package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.util.Logger
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.Serializable
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

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analyzeReceipt(
        receiptBytes: ByteArray,
        dishNames: List<String>,
        mimeType: String = "image/jpeg"
    ): Result<ReceiptAnalysisResult> {
        if (receiptBytes.isEmpty()) return Result.success(ReceiptAnalysisResult())

        return try {
            val response = supabase.functions.invoke(
                function = "analyze-receipt",
                body = ReceiptAnalysisRequest(
                    imageBase64 = Base64.encode(receiptBytes),
                    mimeType = mimeType,
                    dishNames = dishNames
                )
            )
            val responseText = response.body<String>()
            val parsed = json.decodeFromString<ReceiptAnalysisResponse>(responseText)
            Result.success(
                ReceiptAnalysisResult(
                    suggestions = parsed.matches.mapNotNull { match ->
                        val price = match.price ?: return@mapNotNull null
                        if (match.dishName.isBlank()) return@mapNotNull null
                        ReceiptPriceSuggestion(
                            dishName = match.dishName,
                            price = price,
                            confidence = match.confidence.coerceIn(0f, 1f)
                        )
                    },
                    rawItems = parsed.receiptItems,
                    summary = parsed.summary
                )
            )
        } catch (e: Exception) {
            Logger.e("ReceiptAnalysisRepository", "Receipt analysis failed: ${e.message}", e)
            Result.success(ReceiptAnalysisResult())
        }
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
    val receiptItems: List<String> = emptyList(),
    val summary: String? = null
)

@Serializable
private data class ReceiptMatchResponse(
    val dishName: String = "",
    val price: Double? = null,
    val confidence: Float = 0f
)
