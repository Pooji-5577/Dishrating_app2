package com.example.smackcheck2.gamification

import com.example.smackcheck2.data.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.smackcheck2.util.Logger

/**
 * Manages daily activity streaks and streak milestone rewards.
 *
 * Delegates streak computation entirely to the backend via
 * POST /api/gamification/update-streak, which applies the rules:
 *   • Consecutive day → streak + 1
 *   • Gap → streak resets to 1
 *   • Milestones: 3-day (+20 XP), 7-day (+50 XP)
 */
object StreakManager {

    /**
     * Call this once per session (or per action) to update the user's streak.
     *
     * @return A [StreakResult] describing what happened.
     */
    suspend fun updateStreak(userId: String): StreakResult {
        return try {
            val response: StreakUpdateResponse = ApiClient.postEmpty("gamification/update-streak")
            StreakResult(
                newStreak = response.newStreak ?: 0,
                milestoneBonus = response.milestoneBonus ?: 0,
                milestonesReached = response.milestonesReached ?: emptyList(),
                totalPoints = response.totalPoints ?: 0
            )
        } catch (e: Exception) {
            Logger.e("StreakManager", "StreakManager.updateStreak error: ${e.message}", e)
            StreakResult(newStreak = 0, milestoneBonus = 0, milestonesReached = emptyList(), totalPoints = 0)
        }
    }
}

/**
 * Result of a streak update.
 */
data class StreakResult(
    val newStreak: Int,
    val milestoneBonus: Int,
    val milestonesReached: List<Int>,
    val totalPoints: Int
)

// ── Internal response DTO ──────────────────────────────────────────

@Serializable
private data class StreakUpdateResponse(
    @SerialName("new_streak")
    val newStreak: Int? = null,
    @SerialName("milestone_bonus")
    val milestoneBonus: Int? = null,
    @SerialName("milestones_reached")
    val milestonesReached: List<Int>? = null,
    @SerialName("total_points")
    val totalPoints: Int? = null
)
