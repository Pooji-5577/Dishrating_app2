package com.example.smackcheck2.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import com.example.smackcheck2.ui.screens.Challenge

/**
 * Repository for challenge management with real user stats
 */
class ChallengeRepository {
    private val databaseRepository = DatabaseRepository()
    private val authRepository = AuthRepository()

    // Store previously completed challenge IDs to detect new completions
    private val completedChallenges = mutableSetOf<String>()

    /**
     * Get user challenges with real progress data
     * Returns Pair of (dailyChallenges, weeklyChallenges)
     */
    suspend fun getUserChallenges(userId: String): Result<Pair<List<Challenge>, List<Challenge>>> {
        return try {
            // Query real user stats
            val ratingsToday = databaseRepository.getRatingsCountToday(userId).getOrDefault(0)
            val photosCount = databaseRepository.getRatingsWithPhotosCount(userId).getOrDefault(0)
            val uniqueRestaurants = databaseRepository.getUniqueRestaurantsRated(userId).getOrDefault(0)
            val uniqueCuisines = databaseRepository.getUniqueCuisinesTried(userId).getOrDefault(emptySet()).size
            val user = authRepository.getCurrentUser()
            val streak = user?.streakCount ?: 0

            // Build daily challenges with real progress
            val dailyChallenges = listOf(
                Challenge(
                    id = "daily_rate_3",
                    title = "Daily Explorer",
                    description = "Rate 3 dishes today",
                    icon = Icons.Filled.Restaurant,
                    xpReward = 50,
                    progress = (ratingsToday / 3f).coerceIn(0f, 1f),
                    isCompleted = ratingsToday >= 3
                ),
                Challenge(
                    id = "daily_photos_5",
                    title = "Photo Pro",
                    description = "Upload 5 dish photos",
                    icon = Icons.Filled.CameraAlt,
                    xpReward = 30,
                    progress = (photosCount / 5f).coerceIn(0f, 1f),
                    isCompleted = photosCount >= 5
                ),
                Challenge(
                    id = "daily_new_cuisine",
                    title = "Taste Tester",
                    description = "Try a new cuisine today",
                    icon = Icons.Filled.Fastfood,
                    xpReward = 40,
                    progress = if (uniqueCuisines > 0) 1f else 0f,
                    isCompleted = uniqueCuisines > 0
                )
            )

            // Build weekly challenges with real progress
            val weeklyChallenges = listOf(
                Challenge(
                    id = "weekly_restaurants_5",
                    title = "Restaurant Hopper",
                    description = "Visit 5 different restaurants",
                    icon = Icons.Filled.Explore,
                    xpReward = 200,
                    progress = (uniqueRestaurants / 5f).coerceIn(0f, 1f),
                    isCompleted = uniqueRestaurants >= 5
                ),
                Challenge(
                    id = "weekly_reviews_10",
                    title = "Review Master",
                    description = "Write 10 detailed reviews",
                    icon = Icons.Filled.RateReview,
                    xpReward = 250,
                    progress = (ratingsToday / 10f).coerceIn(0f, 1f), // Could track detailed reviews separately
                    isCompleted = ratingsToday >= 10
                ),
                Challenge(
                    id = "weekly_streak_7",
                    title = "Streak Champion",
                    description = "Maintain a 7-day rating streak",
                    icon = Icons.Filled.LocalFireDepartment,
                    xpReward = 300,
                    progress = (streak / 7f).coerceIn(0f, 1f),
                    isCompleted = streak >= 7
                ),
                Challenge(
                    id = "weekly_share_5",
                    title = "Social Foodie",
                    description = "Share 5 dishes on social media",
                    icon = Icons.Filled.Share,
                    xpReward = 150,
                    progress = 0f, // Feature not implemented yet
                    isCompleted = false
                )
            )

            // Check for newly completed challenges and award XP
            val allChallenges = dailyChallenges + weeklyChallenges
            val newlyCompleted = allChallenges.filter { challenge ->
                challenge.isCompleted && !completedChallenges.contains(challenge.id)
            }

            // Award XP for newly completed challenges
            for (challenge in newlyCompleted) {
                val xpResult = databaseRepository.addXpToUser(userId, challenge.xpReward)
                xpResult.fold(
                    onSuccess = {
                        println("ChallengeRepository: ✓ Awarded ${challenge.xpReward} XP for completing '${challenge.title}'")
                        completedChallenges.add(challenge.id)
                    },
                    onFailure = { error ->
                        println("ChallengeRepository: ✗ Failed to award XP for challenge: ${error.message}")
                    }
                )
            }

            Result.success(Pair(dailyChallenges, weeklyChallenges))
        } catch (e: Exception) {
            println("ChallengeRepository: Error loading challenges: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Mark a challenge as completed by ID (for manual completion)
     */
    suspend fun markChallengeCompleted(userId: String, challengeId: String, xpReward: Int): Result<Unit> {
        return try {
            if (!completedChallenges.contains(challengeId)) {
                val xpResult = databaseRepository.addXpToUser(userId, xpReward)
                xpResult.fold(
                    onSuccess = {
                        completedChallenges.add(challengeId)
                        println("ChallengeRepository: ✓ Challenge $challengeId completed, awarded $xpReward XP")
                    },
                    onFailure = { error ->
                        return Result.failure(error)
                    }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset daily challenges (should be called daily)
     */
    fun resetDailyChallenges() {
        completedChallenges.removeAll { it.startsWith("daily_") }
    }

    /**
     * Reset weekly challenges (should be called weekly)
     */
    fun resetWeeklyChallenges() {
        completedChallenges.removeAll { it.startsWith("weekly_") }
    }
}
