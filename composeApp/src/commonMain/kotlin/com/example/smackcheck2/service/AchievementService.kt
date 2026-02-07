package com.example.smackcheck2.service

import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.DatabaseRepository

/**
 * Service for automatically checking and awarding achievements
 */
class AchievementService {
    private val databaseRepository = DatabaseRepository()
    private val authRepository = AuthRepository()

    /**
     * Check user stats and award achievements that have been unlocked
     * Returns list of newly unlocked achievement IDs
     */
    suspend fun checkAndAwardAchievements(userId: String): Result<List<String>> {
        return try {
            val newlyUnlocked = mutableListOf<String>()

            // Get user stats
            val ratingsCount = databaseRepository.getUniqueRestaurantsRated(userId).getOrDefault(0)
            val user = authRepository.getCurrentUser()
            val streak = user?.streakCount ?: 0
            val cuisines = databaseRepository.getUniqueCuisinesTried(userId).getOrDefault(emptySet())
            val photosCount = databaseRepository.getRatingsWithPhotosCount(userId).getOrDefault(0)

            // Define achievement criteria
            val achievements = mapOf(
                "first_bite" to (ratingsCount >= 1),
                "foodie_explorer" to (ratingsCount >= 10),
                "rating_streak" to (streak >= 7),
                "cuisine_master" to (cuisines.size >= 15),
                "photo_pro" to (photosCount >= 20),
                "restaurant_hopper" to (ratingsCount >= 5)
            )

            // Check each achievement
            for ((badgeId, isUnlocked) in achievements) {
                if (isUnlocked) {
                    val hasAlready = databaseRepository.hasUserBadge(userId, badgeId).getOrDefault(false)
                    if (!hasAlready) {
                        val awardResult = databaseRepository.awardBadge(userId, badgeId)
                        if (awardResult.isSuccess) {
                            println("AchievementService: ✓ Awarded badge: $badgeId")
                            newlyUnlocked.add(badgeId)
                        } else {
                            println("AchievementService: ✗ Failed to award badge: $badgeId - ${awardResult.exceptionOrNull()?.message}")
                        }
                    }
                }
            }

            Result.success(newlyUnlocked)
        } catch (e: Exception) {
            println("AchievementService: Error checking achievements: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
