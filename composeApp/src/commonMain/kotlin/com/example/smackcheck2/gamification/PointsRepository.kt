package com.example.smackcheck2.gamification

import com.example.smackcheck2.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repository that handles all point-earning operations against Supabase.
 *
 * Responsibilities:
 *  • Record user actions in `user_actions`
 *  • Increment `total_points` on the `profiles` row
 *  • Fetch leaderboard (top N profiles by total_points)
 *  • Query action counts for challenge progress
 */
object PointsRepository {

    private val client get() = SupabaseClient.client

    // ── Current user helper ────────────────────────────────────────
    /** Returns the authenticated user's UUID, or null if not signed in. */
    suspend fun currentUserId(): String? {
        return try {
            client.auth.currentUserOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    // ── Record an action & award points ────────────────────────────

    /**
     * Record a point-earning action.
     *
     * 1. Inserts a row into `user_actions`
     * 2. Increments `profiles.total_points`
     *
     * @param actionType One of [PointsConfig] ACTION_* constants
     * @param metadata   Optional JSON metadata (e.g. dish name)
     * @return The number of points earned, or 0 on failure
     */
    suspend fun recordAction(
        actionType: String,
        metadata: Map<String, String> = emptyMap()
    ): Int {
        val userId = currentUserId() ?: return 0
        val points = PointsConfig.pointsFor(actionType)
        if (points == 0) return 0

        return try {
            // Build metadata JSON string
            val metaJson = buildJsonObject {
                metadata.forEach { (k, v) -> put(k, v) }
            }.toString()

            // 1. Insert action row
            client.from("user_actions").insert(
                UserActionRow(
                    userId = userId,
                    actionType = actionType,
                    pointsEarned = points,
                    metadata = metaJson
                )
            )

            // 2. Fetch current total_points, increment, and update
            val profile = client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<LeaderboardProfileRow>()

            val newTotal = (profile?.totalPoints ?: 0) + points
            val today = currentDateString()

            client.from("profiles").update(
                ProfilePointsUpdate(
                    totalPoints = newTotal,
                    currentStreak = profile?.currentStreak ?: 0,
                    longestStreak = 0,  // will be fixed by StreakManager
                    lastActiveDate = today
                )
            ) { filter { eq("id", userId) } }

            points
        } catch (e: Exception) {
            println("PointsRepository.recordAction error: ${e.message}")
            0
        }
    }

    // ── One-time: first profile picture bonus ──────────────────────

    /**
     * Award the one-time +50 XP bonus for uploading a profile picture.
     * Checks `profile_picture_uploaded` flag to prevent double-awarding.
     *
     * @return points earned (50 or 0)
     */
    suspend fun awardFirstProfilePicBonus(): Int {
        val userId = currentUserId() ?: return 0

        return try {
            // Check if already awarded
            @kotlinx.serialization.Serializable
            data class PicFlag(
                @kotlinx.serialization.SerialName("profile_picture_uploaded")
                val profilePictureUploaded: Boolean? = false
            )
            val flag = client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<PicFlag>()

            if (flag?.profilePictureUploaded == true) return 0

            // Record the action
            val pts = recordAction(PointsConfig.ACTION_FIRST_PROFILE_PIC)

            // Set the flag so it's not awarded again
            client.from("profiles").update(
                ProfilePicFlagUpdate(profilePictureUploaded = true)
            ) { filter { eq("id", userId) } }

            pts
        } catch (e: Exception) {
            println("PointsRepository.awardFirstProfilePicBonus error: ${e.message}")
            0
        }
    }

    // ── Leaderboard ────────────────────────────────────────────────

    /**
     * Fetch the top N users by total_points for the leaderboard.
     */
    suspend fun fetchLeaderboard(limit: Int = 20): List<LeaderboardProfileRow> {
        return try {
            client.from("profiles")
                .select()  {
                    order("total_points", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<LeaderboardProfileRow>()
        } catch (e: Exception) {
            println("PointsRepository.fetchLeaderboard error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get the current user's rank (1-based) on the leaderboard.
     */
    suspend fun fetchCurrentUserRank(): Int {
        val userId = currentUserId() ?: return 0
        return try {
            val leaderboard = fetchLeaderboard(limit = 200)
            val index = leaderboard.indexOfFirst { it.id == userId }
            if (index >= 0) index + 1 else 0
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Get the current user's profile (for points, streak, level display).
     */
    suspend fun fetchCurrentUserProfile(): LeaderboardProfileRow? {
        val userId = currentUserId() ?: return null
        return try {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<LeaderboardProfileRow>()
        } catch (e: Exception) {
            println("PointsRepository.fetchCurrentUserProfile error: ${e.message}")
            null
        }
    }

    // ── Action count queries (for challenge progress) ──────────────

    /**
     * Count how many actions of [actionType] the current user performed
     * on or after [sinceDate] (inclusive, "YYYY-MM-DD").
     */
    suspend fun countActionsSince(actionType: String, sinceDate: String): Int {
        val userId = currentUserId() ?: return 0
        return try {
            val rows = client.from("user_actions")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("action_type", actionType)
                        gte("created_at", "${sinceDate}T00:00:00Z")
                    }
                }
                .decodeList<UserActionRow>()
            rows.size
        } catch (e: Exception) {
            println("PointsRepository.countActionsSince error: ${e.message}")
            0
        }
    }

    // ── Date helper ────────────────────────────────────────────────
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
