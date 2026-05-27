package com.example.smackcheck2.data.repository

import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.util.Logger
import kotlinx.serialization.Serializable
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
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analyzeReceipt(
        receiptBytes: ByteArray,
        dishNames: List<String>,
        mimeType: String = "image/jpeg"
    ): Result<ReceiptAnalysisResult> {
        if (receiptBytes.isEmpty()) return Result.success(ReceiptAnalysisResult())

        return try {
            val requestBody = ReceiptAnalysisRequest(
                imageBase64 = Base64.encode(receiptBytes),
                mimeType = mimeType,
                dishNames = dishNames
            )
            val response = ApiClient.post<ReceiptAnalysisRequest, ReceiptAnalysisResponse>(
                "ai/analyze-receipt",
                requestBody
            )
            Result.success(response.toResult())
        } catch (e: Exception) {
            Logger.e("ReceiptAnalysisRepository", "Receipt analysis failed: ${e.message}", e)
            Result.success(ReceiptAnalysisResult())
        }
    }

    private fun ReceiptAnalysisResponse.toResult(): ReceiptAnalysisResult {
        val matches = matches.ifEmpty { suggestions }
        val rawItems = receiptItems.ifEmpty { this.rawItems }
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
            summary = summary
        )
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
