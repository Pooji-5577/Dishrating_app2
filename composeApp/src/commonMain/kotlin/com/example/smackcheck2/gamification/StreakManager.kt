package com.example.smackcheck2.gamification

import com.example.smackcheck2.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Manages daily activity streaks and streak milestone rewards.
 *
 * ── Rules ──────────────────────────────────────────────────────────
 *   • If the user was active yesterday, streak continues (+1).
 *   • If the user was NOT active yesterday, streak resets to 1.
 *   • Streak milestones:
 *       3-day streak  → +20 XP (once per streak)
 *       7-day streak  → +50 XP (once per streak)
 */
object StreakManager {

    private val client get() = SupabaseClient.client

    /**
     * Call this once per session (or per action) to update the user's streak.
     *
     * @return A [StreakResult] describing what happened.
     */
    suspend fun updateStreak(userId: String): StreakResult {
        return try {
            val profile = PointsRepository.fetchCurrentUserProfile()
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val todayStr = today.toString()

            val lastActiveStr = profile?.let {
                // Fetch full profile with last_active_date
                @kotlinx.serialization.Serializable
                data class StreakProfile(
                    @kotlinx.serialization.SerialName("current_streak")
                    val currentStreak: Int? = 0,
                    @kotlinx.serialization.SerialName("longest_streak")
                    val longestStreak: Int? = 0,
                    @kotlinx.serialization.SerialName("last_active_date")
                    val lastActiveDate: String? = null,
                    @kotlinx.serialization.SerialName("total_points")
                    val totalPoints: Int? = 0
                )
                client.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<StreakProfile>()
            }

            // Parse dates
            val currentStreak = profile?.currentStreak ?: 0
            val totalPoints = profile?.totalPoints ?: 0

            // Determine new streak
            val newStreak: Int
            val lastDate = try {
                lastActiveStr?.lastActiveDate?.let { LocalDate.parse(it) }
            } catch (_: Exception) { null }

            val yesterday = today.minus(DatePeriod(days = 1))

            newStreak = when {
                lastDate == today -> currentStreak // Already active today, no change
                lastDate == yesterday -> currentStreak + 1 // Consecutive day
                else -> 1 // Reset
            }

            // Calculate longest streak
            val longestStreak = maxOf(lastActiveStr?.longestStreak ?: 0, newStreak)

            // Check for milestone rewards
            var milestoneBonus = 0
            val milestonesEarned = mutableListOf<Int>()

            if (newStreak >= 3 && !hasStreakRewardToday(userId, 3, todayStr)) {
                milestoneBonus += PointsConfig.STREAK_3_DAY
                milestonesEarned.add(3)
                recordStreakReward(userId, 3, PointsConfig.STREAK_3_DAY)
            }
            if (newStreak >= 7 && !hasStreakRewardToday(userId, 7, todayStr)) {
                milestoneBonus += PointsConfig.STREAK_7_DAY
                milestonesEarned.add(7)
                recordStreakReward(userId, 7, PointsConfig.STREAK_7_DAY)
            }

            // Update profile
            val updatedTotal = totalPoints + milestoneBonus
            client.from("profiles").update(
                ProfilePointsUpdate(
                    totalPoints = updatedTotal,
                    currentStreak = newStreak,
                    longestStreak = longestStreak,
                    lastActiveDate = todayStr
                )
            ) { filter { eq("id", userId) } }

            StreakResult(
                newStreak = newStreak,
                milestoneBonus = milestoneBonus,
                milestonesReached = milestonesEarned,
                totalPoints = updatedTotal
            )
        } catch (e: Exception) {
            println("StreakManager.updateStreak error: ${e.message}")
            StreakResult(newStreak = 0, milestoneBonus = 0, milestonesReached = emptyList(), totalPoints = 0)
        }
    }

    /**
     * Check if a streak reward for [days] was already awarded today.
     */
    private suspend fun hasStreakRewardToday(userId: String, days: Int, todayStr: String): Boolean {
        return try {
            val rows = client.from("streak_rewards")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("streak_days", days)
                        gte("awarded_at", "${todayStr}T00:00:00Z")
                    }
                }
                .decodeList<StreakRewardRow>()
            rows.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Record a streak milestone reward.
     */
    private suspend fun recordStreakReward(userId: String, days: Int, points: Int) {
        try {
            client.from("streak_rewards").insert(
                StreakRewardRow(
                    userId = userId,
                    streakDays = days,
                    pointsEarned = points
                )
            )
        } catch (e: Exception) {
            println("StreakManager.recordStreakReward error: ${e.message}")
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
