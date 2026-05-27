package com.example.smackcheck2.service

import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.SupabaseConfig
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.model.CapturedImage
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.util.Logger
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class GroupedDishReviewItemRequest(
    val dishName: String,
    val image: CapturedImage,
    val price: Double? = null,
    val aiConfidence: Float? = null
)

data class GroupedDishReviewSubmissionRequest(
    val items: List<GroupedDishReviewItemRequest>,
    val rating: Float,
    val comment: String = "",
    val tags: List<String> = emptyList(),
    val restaurantId: String,
    val selectedRestaurant: Restaurant? = null,
    val receiptImageBytes: ByteArray? = null,
    val receiptAnalysisSummary: String? = null,
    val receiptRawItems: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class GroupedDishReviewSubmissionResult(
    val groupId: String,
    val primaryRatingId: String,
    val ratingIds: List<String>,
    val xpEarned: Int
)

class GroupedDishReviewSubmissionService(
    private val databaseRepository: DatabaseRepository = DatabaseRepository(),
    private val storageRepository: StorageRepository = StorageRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val achievementService: AchievementService = AchievementService(),
    private val socialRepository: SocialRepository = SocialRepository(),
    private val sideEffectScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val httpClient = HttpClient()

    suspend fun submit(request: GroupedDishReviewSubmissionRequest): Result<GroupedDishReviewSubmissionResult> {
        validate(request)?.let { return Result.failure(IllegalArgumentException(it)) }

        val user = authRepository.getCurrentUser()
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        val userId = user.id

        val uploadedImageUrls = request.items.mapIndexed { index, item ->
            uploadDishImage(userId, item, index).getOrElse {
                return Result.failure(it)
            }
        }
        val firstImageUrl = uploadedImageUrls.first()

        val restaurant = resolveRestaurant(request, firstImageUrl).getOrElse {
            return Result.failure(it)
        }

        val receiptImageUrl = request.receiptImageBytes?.let { bytes ->
            storageRepository.uploadReceiptImage(
                userId = userId,
                imageBytes = bytes,
                fileName = "receipt.jpg"
            ).getOrNull()
        }

        val backendResult = submitGroupedToBackend(
            request = request,
            restaurantId = restaurant.id,
            uploadedImageUrls = uploadedImageUrls,
            receiptImageUrl = receiptImageUrl
        ).getOrElse { return Result.failure(it) }

        val xpEarned = calculateXp(request)
        trackAnalytics(request, xpEarned)
        runSideEffects(userId, backendResult.groupId, backendResult.primaryRatingId, restaurant.id, firstImageUrl, xpEarned)

        return Result.success(
            GroupedDishReviewSubmissionResult(
                groupId = backendResult.groupId,
                primaryRatingId = backendResult.primaryRatingId,
                ratingIds = listOf(backendResult.primaryRatingId),
                xpEarned = xpEarned
            )
        )
    }

    private fun validate(request: GroupedDishReviewSubmissionRequest): String? {
        if (request.items.isEmpty()) return "Add at least one dish photo"
        if (request.items.any { it.dishName.isBlank() }) return "Every dish needs a name"
        if (request.rating == 0f) return "Please provide a rating"
        if (request.restaurantId.isBlank() && request.selectedRestaurant == null) return "Please select a restaurant"
        return null
    }

    private suspend fun uploadDishImage(
        userId: String,
        item: GroupedDishReviewItemRequest,
        index: Int
    ): Result<String> =
        storageRepository.uploadDishImage(
            userId = userId,
            imageBytes = item.image.bytes,
            fileName = "${item.dishName.ifBlank { "dish" }}_$index.jpg"
        )

    private suspend fun resolveRestaurant(
        request: GroupedDishReviewSubmissionRequest,
        imageUrl: String?
    ): Result<SubmissionRestaurant> {
        val selected = request.selectedRestaurant
        if (selected != null) {
            return databaseRepository.ensureRestaurantExists(selected, dishImageUrl = imageUrl)
                .map { SubmissionRestaurant(it.id, it.name) }
        }
        return databaseRepository.getRestaurantById(request.restaurantId)
            .mapCatching { restaurant ->
                val existing = restaurant ?: throw IllegalArgumentException("Selected restaurant no longer exists")
                SubmissionRestaurant(existing.id, existing.name)
            }
    }

    private fun receiptPayload(request: GroupedDishReviewSubmissionRequest): String? {
        if (request.receiptAnalysisSummary == null && request.receiptRawItems.isEmpty()) return null
        return json.encodeToString(
            ReceiptPayload(
                summary = request.receiptAnalysisSummary,
                items = request.receiptRawItems
            )
        )
    }

    private suspend fun submitGroupedToBackend(
        request: GroupedDishReviewSubmissionRequest,
        restaurantId: String,
        uploadedImageUrls: List<String>,
        receiptImageUrl: String?
    ): Result<BackendGroupedReviewResult> {
        val accessToken = currentAccessToken()
            ?: return Result.failure(IllegalStateException("User session expired"))

        return try {
            val response = httpClient.post("${SupabaseConfig.BACKEND_URL.trimEnd('/')}/api/ratings/grouped") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody(json.encodeToString(
                    BackendGroupedReviewRequest(
                        restaurantId = restaurantId,
                        rating = request.rating,
                        comment = request.comment,
                        tags = request.tags,
                        receiptImageUrl = receiptImageUrl,
                        receiptExtractedData = receiptPayload(request),
                        latitude = request.latitude,
                        longitude = request.longitude,
                        items = request.items.mapIndexed { index, item ->
                            BackendGroupedReviewItem(
                                dishName = item.dishName,
                                imageUrl = uploadedImageUrls[index],
                                price = item.price,
                                aiConfidence = item.aiConfidence,
                                sortOrder = index
                            )
                        }
                    )
                ))
            }
            val responseText = response.body<String>()
            if (response.status.value !in 200..299) {
                return Result.failure(IllegalStateException("Backend grouped review failed (${response.status.value}): ${responseText.take(300)}"))
            }

            val parsed = json.decodeFromString<BackendGroupedReviewResponse>(responseText)
            val groupId = parsed.groupId
            val primaryRatingId = parsed.primaryRatingId
            if (groupId.isBlank() || primaryRatingId.isBlank()) {
                return Result.failure(IllegalStateException("Backend grouped review response missing ids"))
            }
            Result.success(BackendGroupedReviewResult(groupId, primaryRatingId))
        } catch (e: Exception) {
            Logger.e("GroupedDishReviewSubmissionService", "Backend grouped review failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun currentAccessToken(): String? {
        val status = SupabaseClientProvider.client.auth.sessionStatus.value
        return (status as? SessionStatus.Authenticated)?.session?.accessToken
    }

    private fun calculateXp(request: GroupedDishReviewSubmissionRequest): Int {
        val baseXp = 10
        val photoBonus = request.items.size * 5
        val commentBonus = if (request.comment.length > 50) 10 else 0
        val tagBonus = request.tags.size * 2
        return baseXp + photoBonus + commentBonus + tagBonus
    }

    private fun trackAnalytics(request: GroupedDishReviewSubmissionRequest, xpEarned: Int) {
        Analytics.track("grouped_post_created", mapOf(
            "rating" to request.rating,
            "dish_count" to request.items.size,
            "has_receipt" to (request.receiptImageBytes != null),
            "has_comment" to request.comment.isNotEmpty(),
            "xp_earned" to xpEarned
        ))
    }

    private fun runSideEffects(
        userId: String,
        groupId: String,
        primaryRatingId: String,
        restaurantId: String,
        imageUrl: String,
        xpEarned: Int
    ) {
        sideEffectScope.launch {
            databaseRepository.refreshRestaurantRating(restaurantId)
                .onFailure { Logger.e("GroupedDishReviewSubmissionService", "Failed to refresh restaurant rating: ${it.message}", it) }
            socialRepository.uploadStory(userId, imageUrl)
                .onFailure { Logger.e("GroupedDishReviewSubmissionService", "Failed to auto-publish grouped story: ${it.message}", it) }
            databaseRepository.addXpToUser(userId, xpEarned)
                .onFailure { Logger.e("GroupedDishReviewSubmissionService", "Failed to award XP: ${it.message}", it) }
            databaseRepository.updateUserStreak(userId)
                .onFailure { Logger.e("GroupedDishReviewSubmissionService", "Failed to update streak: ${it.message}", it) }
            achievementService.checkAndAwardAchievements(userId)
                .onFailure { Logger.e("GroupedDishReviewSubmissionService", "Failed to check achievements: ${it.message}", it) }
            Logger.d("GroupedDishReviewSubmissionService", "Submitted grouped review $groupId with primary rating $primaryRatingId")
        }
    }
}

@Serializable
private data class ReceiptPayload(
    val summary: String? = null,
    val items: List<String> = emptyList()
)

@Serializable
private data class BackendGroupedReviewRequest(
    @SerialName("restaurant_id")
    val restaurantId: String,
    val rating: Float,
    val comment: String,
    val tags: List<String>,
    @SerialName("receipt_image_url")
    val receiptImageUrl: String?,
    @SerialName("receipt_extracted_data")
    val receiptExtractedData: String?,
    val latitude: Double?,
    val longitude: Double?,
    val items: List<BackendGroupedReviewItem>
)

@Serializable
private data class BackendGroupedReviewItem(
    @SerialName("dish_name")
    val dishName: String,
    @SerialName("image_url")
    val imageUrl: String,
    val price: Double?,
    @SerialName("ai_confidence")
    val aiConfidence: Float?,
    @SerialName("sort_order")
    val sortOrder: Int
)

@Serializable
private data class BackendGroupedReviewResponse(
    @SerialName("group_id")
    val groupId: String = "",
    @SerialName("primary_rating_id")
    val primaryRatingId: String = ""
)

private data class BackendGroupedReviewResult(
    val groupId: String,
    val primaryRatingId: String
)
