package com.example.smackcheck2.gamification

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.ui.screens.Achievement
import com.example.smackcheck2.ui.screens.Challenge
import com.example.smackcheck2.ui.screens.LeaderboardEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// UI State
// ═══════════════════════════════════════════════════════════════════

data class GamificationUiState(
    val isLoading: Boolean = false,
    val totalXp: Int = 0,
    val level: Int = 1,
    val rank: Int = 0,
    val streakDays: Int = 0,
    val levelProgress: Float = 0f,
    val dailyChallenges: List<Challenge> = emptyList(),
    val weeklyChallenges: List<Challenge> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val error: String? = null
)

/**
 * Event emitted when points are earned, consumed by the UI popup.
 */
data class PointsEarnedEvent(
    val points: Int,
    val actionLabel: String,
    val bonusPoints: Int = 0,      // challenge/streak bonus
    val bonusLabel: String? = null  // e.g. "Challenge Complete!" or "3-Day Streak!"
)

// ═══════════════════════════════════════════════════════════════════
// ViewModel
// ═══════════════════════════════════════════════════════════════════

/**
 * ViewModel that replaces the old mock [GameViewModel].
 *
 * Backed by Supabase via [PointsRepository], [ChallengesRepository],
 * and [StreakManager]. Provides:
 *   • Real-time XP / level / streak
 *   • Daily & weekly challenge progress
 *   • Leaderboard
 *   • Static achievements (server-based in future)
 *   • Point-earned events for popup animation
 */
class GamificationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    /** One-shot events for the points-earned popup. */
    private val _pointsEarned = MutableSharedFlow<PointsEarnedEvent>(extraBufferCapacity = 5)
    val pointsEarned: SharedFlow<PointsEarnedEvent> = _pointsEarned.asSharedFlow()

    init {
        loadAll()
    }

    // ── Load everything from Supabase ──────────────────────────────

    fun loadAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val userId = PointsRepository.currentUserId()

                // Profile
                val profile = PointsRepository.fetchCurrentUserProfile()
                val totalXp = profile?.totalPoints ?: 0
                val streak = profile?.currentStreak ?: 0
                val level = PointsConfig.levelForXp(totalXp)
                val progress = PointsConfig.levelProgress(totalXp)

                // Rank
                val rank = PointsRepository.fetchCurrentUserRank()

                // Leaderboard
                val leaderboardRows = PointsRepository.fetchLeaderboard(20)
                val leaderboard = leaderboardRows.map {
                    LeaderboardEntry(
                        userId = it.id,
                        userName = it.name ?: "Anonymous",
                        xp = it.totalPoints ?: 0,
                        level = PointsConfig.levelForXp(it.totalPoints ?: 0)
                    )
                }

                // Challenges
                val dailyChallenges: List<Challenge>
                val weeklyChallenges: List<Challenge>

                if (userId != null) {
                    val progressPairs = ChallengesRepository.fetchCurrentProgress(userId)
                    val allChallenges = progressPairs.map { (def, prog) ->
                        val currentCount = prog?.currentCount ?: 0
                        val progressFraction = if (def.targetCount > 0) {
                            (currentCount.toFloat() / def.targetCount).coerceIn(0f, 1f)
                        } else 0f
                        Challenge(
                            id = def.id,
                            title = def.title,
                            description = def.description,
                            icon = iconForName(def.iconName ?: "Star"),
                            xpReward = def.xpReward,
                            progress = progressFraction,
                            isCompleted = prog?.isCompleted ?: false
                        )
                    }
                    dailyChallenges = allChallenges.filter { c ->
                        progressPairs.any { it.first.id == c.id && it.first.challengeType == "daily" }
                    }
                    weeklyChallenges = allChallenges.filter { c ->
                        progressPairs.any { it.first.id == c.id && it.first.challengeType == "weekly" }
                    }
                } else {
                    dailyChallenges = emptyList()
                    weeklyChallenges = emptyList()
                }

                // Achievements (static for now — will be dynamic later)
                val achievements = buildAchievements(totalXp, streak, leaderboardRows.size)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalXp = totalXp,
                        level = level,
                        rank = rank,
                        streakDays = streak,
                        levelProgress = progress,
                        dailyChallenges = dailyChallenges,
                        weeklyChallenges = weeklyChallenges,
                        leaderboard = leaderboard,
                        achievements = achievements,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Public action methods (called from other screens) ──────────

    /**
     * Record a point-earning action. This is the single entry point
     * called by dish capture, rating, review, and restaurant screens.
     *
     * @param actionType  One of [PointsConfig].ACTION_* constants
     * @param actionLabel Human-readable label for the popup (e.g. "Photo Uploaded")
     * @param metadata    Optional context (dish name, etc.)
     */
    fun recordAction(
        actionType: String,
        actionLabel: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val userId = PointsRepository.currentUserId() ?: return@launch

            // 1. Record the action → earn base points
            val basePoints = PointsRepository.recordAction(actionType, metadata)
            if (basePoints == 0) return@launch

            // 2. Update challenge progress → may earn bonus XP
            val challengeBonus = ChallengesRepository.incrementChallengeProgress(userId, actionType)

            // 3. Update streak → may earn milestone bonus
            val streakResult = StreakManager.updateStreak(userId)

            // 4. Build bonus label
            val totalBonus = challengeBonus + streakResult.milestoneBonus
            val bonusLabel = buildString {
                if (challengeBonus > 0) append("Challenge Complete! ")
                streakResult.milestonesReached.forEach { days ->
                    append("${days}-Day Streak! ")
                }
            }.trimEnd()

            // 5. Emit points event for popup
            _pointsEarned.emit(
                PointsEarnedEvent(
                    points = basePoints,
                    actionLabel = actionLabel,
                    bonusPoints = totalBonus,
                    bonusLabel = bonusLabel.ifEmpty { null }
                )
            )

            // 6. Refresh UI state
            loadAll()
        }
    }

    /**
     * Award first-profile-pic bonus.
     */
    fun awardFirstProfilePicBonus() {
        viewModelScope.launch {
            val pts = PointsRepository.awardFirstProfilePicBonus()
            if (pts > 0) {
                _pointsEarned.emit(
                    PointsEarnedEvent(
                        points = pts,
                        actionLabel = "First Profile Photo!"
                    )
                )
                loadAll()
            }
        }
    }

    // ── Achievement builder (static rules) ─────────────────────────

    private fun buildAchievements(totalXp: Int, streak: Int, leaderboardSize: Int): List<Achievement> {
        return listOf(
            Achievement(
                id = "a1",
                title = "First Bite",
                description = "Earn your first points",
                icon = Icons.Filled.Star,
                isUnlocked = totalXp > 0
            ),
            Achievement(
                id = "a2",
                title = "Foodie Explorer",
                description = "Reach Level 5",
                icon = Icons.Filled.Explore,
                isUnlocked = PointsConfig.levelForXp(totalXp) >= 5
            ),
            Achievement(
                id = "a3",
                title = "Streak Starter",
                description = "Maintain a 3-day streak",
                icon = Icons.Filled.LocalFireDepartment,
                isUnlocked = streak >= 3
            ),
            Achievement(
                id = "a4",
                title = "Streak Champion",
                description = "Maintain a 7-day streak",
                icon = Icons.Filled.LocalFireDepartment,
                isUnlocked = streak >= 7
            ),
            Achievement(
                id = "a5",
                title = "Point Master",
                description = "Earn 1,000 XP total",
                icon = Icons.Filled.EmojiEvents,
                isUnlocked = totalXp >= 1000
            ),
            Achievement(
                id = "a6",
                title = "Cuisine Connoisseur",
                description = "Reach Level 10",
                icon = Icons.Filled.Fastfood,
                isUnlocked = PointsConfig.levelForXp(totalXp) >= 10
            ),
            Achievement(
                id = "a7",
                title = "Rising Star",
                description = "Earn 500 XP",
                icon = Icons.Filled.TrendingUp,
                isUnlocked = totalXp >= 500
            )
        )
    }

    // ── Icon mapping ───────────────────────────────────────────────

    private fun iconForName(name: String): ImageVector = when (name) {
        "Star"                -> Icons.Filled.Star
        "CameraAlt"           -> Icons.Filled.CameraAlt
        "Restaurant"          -> Icons.Filled.Restaurant
        "RateReview"          -> Icons.Filled.RateReview
        "Explore"             -> Icons.Filled.Explore
        "LocalFireDepartment" -> Icons.Filled.LocalFireDepartment
        "EmojiEvents"         -> Icons.Filled.EmojiEvents
        "Fastfood"            -> Icons.Filled.Fastfood
        "TrendingUp"          -> Icons.Filled.TrendingUp
        else                  -> Icons.Filled.Star
    }
}
