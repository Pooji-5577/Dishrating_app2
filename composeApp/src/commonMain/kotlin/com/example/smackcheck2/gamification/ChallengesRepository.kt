package com.example.smackcheck2.gamification

import com.example.smackcheck2.data.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * Repository for managing challenge definitions and per-user challenge progress.
 *
 * Reads challenge definitions from the `challenges` table (seeded via SQL)
 * and tracks progress in `user_challenges`.
 */
object ChallengesRepository {

    private val client get() = SupabaseClient.client

    // ── Fetch all active challenge definitions ─────────────────────

    /**
     * Load all active challenges (daily + weekly) from Supabase.
     */
    suspend fun fetchChallenges(): List<ChallengeRow> {
        return try {
            client.from("challenges")
                .select { filter { eq("is_active", true) } }
                .decodeList<ChallengeRow>()
        } catch (e: Exception) {
            println("ChallengesRepository.fetchChallenges error: ${e.message}")
            emptyList()
        }
    }

    // ── User challenge progress ────────────────────────────────────

    /**
     * Get or create a `user_challenges` row for the given challenge + period.
     * If the row doesn't exist yet, inserts a new one with count 0.
     */
    suspend fun getOrCreateProgress(
        userId: String,
        challengeId: String,
        periodStart: String
    ): UserChallengeRow? {
        return try {
            // Try to fetch existing
            val existing = client.from("user_challenges")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("challenge_id", challengeId)
                        eq("period_start", periodStart)
                    }
                }
                .decodeSingleOrNull<UserChallengeRow>()

            if (existing != null) return existing

            // Create new
            val newRow = UserChallengeRow(
                userId = userId,
                challengeId = challengeId,
                periodStart = periodStart
            )
            client.from("user_challenges").insert(newRow)

            // Re-fetch to get the generated id
            client.from("user_challenges")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("challenge_id", challengeId)
                        eq("period_start", periodStart)
                    }
                }
                .decodeSingleOrNull<UserChallengeRow>()
        } catch (e: Exception) {
            println("ChallengesRepository.getOrCreateProgress error: ${e.message}")
            null
        }
    }

    /**
     * Increment the progress counter for challenges that match [actionType].
     * Automatically marks challenges as completed when target_count is reached
     * and awards the bonus XP.
     *
     * @return Total bonus XP earned from completed challenges
     */
    suspend fun incrementChallengeProgress(
        userId: String,
        actionType: String
    ): Int {
        var bonusXp = 0

        try {
            val allChallenges = fetchChallenges()
            val matching = allChallenges.filter { it.actionType == actionType }

            for (challenge in matching) {
                val periodStart = when (challenge.challengeType) {
                    "daily"  -> PointsRepository.currentDateString()
                    "weekly" -> PointsRepository.currentWeekStartString()
                    else     -> continue
                }

                val progress = getOrCreateProgress(userId, challenge.id, periodStart) ?: continue

                // Already completed — skip
                if (progress.isCompleted) continue

                val newCount = progress.currentCount + 1
                val completed = newCount >= challenge.targetCount

                // Update the row
                @kotlinx.serialization.Serializable
                data class ProgressUpdate(
                    @kotlinx.serialization.SerialName("current_count")
                    val currentCount: Int,
                    @kotlinx.serialization.SerialName("is_completed")
                    val isCompleted: Boolean,
                    @kotlinx.serialization.SerialName("completed_at")
                    val completedAt: String? = null
                )

                val update = ProgressUpdate(
                    currentCount = newCount,
                    isCompleted = completed,
                    completedAt = if (completed) kotlinx.datetime.Clock.System.now().toString() else null
                )

                client.from("user_challenges").update(update) {
                    filter {
                        eq("user_id", userId)
                        eq("challenge_id", challenge.id)
                        eq("period_start", periodStart)
                    }
                }

                // If just completed, award the bonus XP
                if (completed) {
                    bonusXp += challenge.xpReward
                    // Record a bonus action for the challenge completion
                    client.from("user_actions").insert(
                        UserActionRow(
                            userId = userId,
                            actionType = "challenge_complete",
                            pointsEarned = challenge.xpReward,
                            metadata = """{"challenge_id":"${challenge.id}","title":"${challenge.title}"}"""
                        )
                    )
                }
            }

            // If bonus XP was earned, update the profile total
            if (bonusXp > 0) {
                val profile = PointsRepository.fetchCurrentUserProfile()
                val newTotal = (profile?.totalPoints ?: 0) + bonusXp
                val today = PointsRepository.currentDateString()

                client.from("profiles").update(
                    ProfilePointsUpdate(
                        totalPoints = newTotal,
                        currentStreak = profile?.currentStreak ?: 0,
                        longestStreak = 0,
                        lastActiveDate = today
                    )
                ) { filter { eq("id", userId) } }
            }
        } catch (e: Exception) {
            println("ChallengesRepository.incrementChallengeProgress error: ${e.message}")
        }

        return bonusXp
    }

    /**
     * Load all challenge progress for the current user for today/this week.
     * Returns a list of (ChallengeRow, UserChallengeRow?) pairs.
     */
    suspend fun fetchCurrentProgress(userId: String): List<Pair<ChallengeRow, UserChallengeRow?>> {
        return try {
            val challenges = fetchChallenges()
            challenges.map { challenge ->
                val periodStart = when (challenge.challengeType) {
                    "daily"  -> PointsRepository.currentDateString()
                    "weekly" -> PointsRepository.currentWeekStartString()
                    else     -> ""
                }
                val progress = if (periodStart.isNotEmpty()) {
                    getOrCreateProgress(userId, challenge.id, periodStart)
                } else null

                challenge to progress
            }
        } catch (e: Exception) {
            println("ChallengesRepository.fetchCurrentProgress error: ${e.message}")
            emptyList()
        }
    }
}
