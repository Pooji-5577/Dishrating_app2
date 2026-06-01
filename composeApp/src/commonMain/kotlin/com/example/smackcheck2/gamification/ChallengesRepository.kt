package com.example.smackcheck2.gamification

import com.example.smackcheck2.data.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.smackcheck2.util.Logger

/**
 * Repository for managing challenge definitions and per-user challenge progress.
 *
 * Reads challenge definitions via GET /api/gamification/challenges
 * and tracks progress via GET /api/gamification/challenges/progress
 * and POST /api/gamification/challenges/increment.
 */
object ChallengesRepository {

    // ── Fetch all active challenge definitions ─────────────────────

    /**
     * Load all active challenges (daily + weekly) from the backend.
     */
    suspend fun fetchChallenges(): List<ChallengeRow> {
        return try {
            ApiClient.get("gamification/challenges")
        } catch (e: Exception) {
            Logger.e("ChallengesRepository", "ChallengesRepository.fetchChallenges error: ${e.message}", e)
            emptyList()
        }
    }

    // ── User challenge progress ────────────────────────────────────

    /**
     * Get or create a user_challenges row for the given challenge + period.
     * Delegates to the backend which handles upsert logic server-side.
     */
    suspend fun getOrCreateProgress(
        userId: String,
        challengeId: String,
        periodStart: String
    ): UserChallengeRow? {
        return try {
            val allProgress = fetchCurrentProgress(userId)
            allProgress
                .firstOrNull { (challenge, progress) ->
                    challenge.id == challengeId && progress?.periodStart == periodStart
                }
                ?.second
        } catch (e: Exception) {
            Logger.e("ChallengesRepository", "ChallengesRepository.getOrCreateProgress error: ${e.message}", e)
            null
        }
    }

    /**
     * Increment the progress counter for challenges that match [actionType].
     * The backend handles completion detection and bonus XP awarding.
     *
     * @return Total bonus XP earned from completed challenges
     */
    suspend fun incrementChallengeProgress(
        userId: String,
        actionType: String
    ): Int {
        return try {
            val response: IncrementChallengeResponse = ApiClient.post(
                path = "gamification/challenges/increment",
                body = IncrementChallengeRequest(actionType = actionType)
            )
            response.bonusXp ?: 0
        } catch (e: Exception) {
            Logger.e("ChallengesRepository", "ChallengesRepository.incrementChallengeProgress error: ${e.message}", e)
            0
        }
    }

    /**
     * Load all challenge progress for the current user for today/this week.
     * Returns a list of (ChallengeRow, UserChallengeRow?) pairs.
     */
    suspend fun fetchCurrentProgress(userId: String): List<Pair<ChallengeRow, UserChallengeRow?>> {
        return try {
            val response: List<ChallengeProgressResponse> = ApiClient.get("gamification/challenges/progress")
            response.mapNotNull { item ->
                val challenge = item.challenge ?: return@mapNotNull null
                challenge to item.toUserChallengeRow()
            }
        } catch (e: Exception) {
            Logger.e("ChallengesRepository", "ChallengesRepository.fetchCurrentProgress error: ${e.message}", e)
            emptyList()
        }
    }
}

// ── Internal request/response DTOs ────────────────────────────────

@Serializable
private data class IncrementChallengeRequest(
    @SerialName("action_type")
    val actionType: String
)

@Serializable
private data class IncrementChallengeResponse(
    @SerialName("bonus_xp")
    val bonusXp: Int? = null
)

@Serializable
private data class ChallengeProgressResponse(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("challenge_id")
    val challengeId: String,
    @SerialName("started_at")
    val startedAt: String? = null,
    val progress: Int = 0,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("challenges")
    val challenge: ChallengeRow? = null
) {
    fun toUserChallengeRow(): UserChallengeRow = UserChallengeRow(
        id = id,
        userId = userId,
        challengeId = challengeId,
        periodStart = startedAt?.substringBefore("T").orEmpty(),
        currentCount = progress,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt
    )
}
