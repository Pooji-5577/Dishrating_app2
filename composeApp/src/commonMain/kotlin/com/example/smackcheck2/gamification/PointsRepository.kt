package com.example.smackcheck2.gamification

import com.example.smackcheck2.data.ApiClient
import com.example.smackcheck2.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import com.example.smackcheck2.util.Logger

/**
 * Repository that handles all point-earning operations against the custom backend.
 *
 * Responsibilities:
 *  • Record user actions via POST /api/gamification/record-action
 *  • Fetch leaderboard via GET /api/gamification/leaderboard
 *  • Query action counts via GET /api/gamification/actions/count
 */
object PointsRepository {

    // ── Current user helper ────────────────────────────────────────
    /** Returns the authenticated user's UUID, or null if not signed in. */
    suspend fun currentUserId(): String? {
        return try {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    // ── Record an action & award points ────────────────────────────

    /**
     * Record a point-earning action.
     *
     * POSTs to /api/gamification/record-action with {action_type, points_earned, metadata}.
     *
     * @param actionType One of [PointsConfig] ACTION_* constants
     * @param metadata   Optional metadata (e.g. dish name)
     * @return The number of points earned, or 0 on failure
     */
    suspend fun recordAction(
        actionType: String,
        metadata: Map<String, String> = emptyMap()
    ): Int {
        currentUserId() ?: return 0
        val points = PointsConfig.pointsFor(actionType)
        if (points == 0) return 0

        return try {
            val response: RecordActionResponse = ApiClient.post(
                path = "gamification/record-action",
                body = RecordActionRequest(
                    actionType = actionType,
                    pointsEarned = points,
                    metadata = metadata
                )
            )
            response.pointsEarned ?: points
        } catch (e: Exception) {
            Logger.e("PointsRepository", "PointsRepository.recordAction error: ${e.message}", e)
            0
        }
    }

    // ── One-time: first profile picture bonus ──────────────────────

    /**
     * Award the one-time +50 XP bonus for uploading a profile picture.
     * The backend checks the flag to prevent double-awarding.
     *
     * @return points earned (50 or 0)
     */
    suspend fun awardFirstProfilePicBonus(): Int {
        currentUserId() ?: return 0
        return try {
            recordAction(PointsConfig.ACTION_FIRST_PROFILE_PIC)
        } catch (e: Exception) {
            Logger.e("PointsRepository", "PointsRepository.awardFirstProfilePicBonus error: ${e.message}", e)
            0
        }
    }

    // ── Leaderboard ────────────────────────────────────────────────

    /**
     * Fetch the top N users by total_points for the leaderboard.
     */
    suspend fun fetchLeaderboard(limit: Int = 20): List<LeaderboardProfileRow> {
        return try {
            val response = ApiClient.getJsonElement(
                path = "gamification/leaderboard",
                params = mapOf("limit" to limit.toString())
            )
            val items = when (response) {
                is JsonArray -> response
                is JsonObject -> {
                    response["leaderboard"] as? JsonArray
                        ?: response["profiles"] as? JsonArray
                        ?: response["users"] as? JsonArray
                        ?: response["items"] as? JsonArray
                        ?: response["data"] as? JsonArray
                        ?: JsonArray(emptyList())
                }
                else -> JsonArray(emptyList())
            }
            ApiClient.json.decodeFromJsonElement<List<LeaderboardProfileRow>>(items)
        } catch (e: Exception) {
            Logger.e("PointsRepository", "PointsRepository.fetchLeaderboard error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get the current user's rank (1-based) on the leaderboard.
     */
    suspend fun fetchCurrentUserRank(): Int {
        currentUserId() ?: return 0
        return try {
            val response: RankResponse = ApiClient.get("gamification/my-rank")
            response.rank ?: 0
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Get the current user's profile (for points, streak, level display).
     */
    suspend fun fetchCurrentUserProfile(): LeaderboardProfileRow? {
        currentUserId() ?: return null
        return try {
            val leaderboard = fetchLeaderboard(limit = 200)
            val userId = currentUserId() ?: return null
            leaderboard.firstOrNull { it.id == userId }
        } catch (e: Exception) {
            Logger.e("PointsRepository", "PointsRepository.fetchCurrentUserProfile error: ${e.message}", e)
            null
        }
    }

    // ── Action count queries (for challenge progress) ──────────────

    /**
     * Count how many actions of [actionType] the current user performed
     * on or after [sinceDate] (inclusive, "YYYY-MM-DD").
     */
    suspend fun countActionsSince(actionType: String, sinceDate: String): Int {
        currentUserId() ?: return 0
        return try {
            val response: ActionCountResponse = ApiClient.get(
                path = "gamification/actions/count",
                params = mapOf(
                    "actionType" to actionType,
                    "since" to sinceDate
                )
            )
            response.count ?: 0
        } catch (e: Exception) {
            Logger.e("PointsRepository", "PointsRepository.countActionsSince error: ${e.message}", e)
            0
        }
    }

    // ── Date helpers ────────────────────────────────────────────────
    /**
     * Returns today's date as "YYYY-MM-DD" in a platform-agnostic way.
     */
    fun currentDateString(): String {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.toString() // "YYYY-MM-DD"
    }

    /**
     * Returns the start of the current ISO week (Monday) as "YYYY-MM-DD".
     */
    fun currentWeekStartString(): String {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfWeek = today.dayOfWeek.ordinal // Mon=0 .. Sun=6
        val monday = today.minus(DatePeriod(days = dayOfWeek))
        return monday.toString()
    }
}

// ── Internal request/response DTOs ────────────────────────────────

@Serializable
private data class RecordActionRequest(
    @SerialName("action_type")
    val actionType: String,
    @SerialName("points_earned")
    val pointsEarned: Int,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
private data class RecordActionResponse(
    @SerialName("points_earned")
    val pointsEarned: Int? = null
)

@Serializable
private data class RankResponse(
    val rank: Int? = null
)

@Serializable
private data class ActionCountResponse(
    val count: Int? = null
)
