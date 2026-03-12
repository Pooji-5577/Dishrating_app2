package com.example.smackcheck2.gamification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════
// Supabase DTOs for the gamification tables
// ═══════════════════════════════════════════════════════════════════

/**
 * Row in the `user_actions` table.
 */
@Serializable
data class UserActionRow(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("action_type")
    val actionType: String,
    @SerialName("points_earned")
    val pointsEarned: Int,
    val metadata: String? = null,           // JSON string
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Row in the `challenges` table (read-only seed data).
 */
@Serializable
data class ChallengeRow(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("challenge_type")
    val challengeType: String,              // "daily" | "weekly"
    @SerialName("action_type")
    val actionType: String,
    @SerialName("target_count")
    val targetCount: Int,
    @SerialName("xp_reward")
    val xpReward: Int,
    @SerialName("icon_name")
    val iconName: String? = "Star",
    @SerialName("is_active")
    val isActive: Boolean = true
)

/**
 * Row in the `user_challenges` table.
 */
@Serializable
data class UserChallengeRow(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("challenge_id")
    val challengeId: String,
    @SerialName("period_start")
    val periodStart: String,                // "YYYY-MM-DD"
    @SerialName("current_count")
    val currentCount: Int = 0,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Row in the `streak_rewards` table.
 */
@Serializable
data class StreakRewardRow(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("streak_days")
    val streakDays: Int,
    @SerialName("points_earned")
    val pointsEarned: Int,
    @SerialName("awarded_at")
    val awardedAt: String? = null
)

/**
 * Lightweight profile row for the leaderboard query.
 */
@Serializable
data class LeaderboardProfileRow(
    val id: String,
    val name: String? = null,
    @SerialName("total_points")
    val totalPoints: Int? = 0,
    val level: Int? = 1,
    @SerialName("current_streak")
    val currentStreak: Int? = 0,
    @SerialName("profile_photo_url")
    val profilePhotoUrl: String? = null
)

/**
 * Profile columns we update when awarding points / streaks.
 */
@Serializable
data class ProfilePointsUpdate(
    @SerialName("total_points")
    val totalPoints: Int,
    @SerialName("current_streak")
    val currentStreak: Int,
    @SerialName("longest_streak")
    val longestStreak: Int,
    @SerialName("last_active_date")
    val lastActiveDate: String              // "YYYY-MM-DD"
)

/**
 * For the one-time first profile pic update.
 */
@Serializable
data class ProfilePicFlagUpdate(
    @SerialName("profile_picture_uploaded")
    val profilePictureUploaded: Boolean = true
)
