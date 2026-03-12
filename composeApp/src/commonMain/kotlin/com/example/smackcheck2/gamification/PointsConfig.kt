package com.example.smackcheck2.gamification

/**
 * Central configuration for all point values in the SmackCheck gamification system.
 *
 * ── Core Actions ──────────────────────────────────────────────────
 *   Upload Photo       → +10
 *   Rate Dish           → +5
 *   Write Review        → +10
 *   Add Restaurant      → +20
 *   First Profile Pic   → +50  (one-time)
 *
 * ── Streak Rewards ────────────────────────────────────────────────
 *   3-day streak        → +20
 *   7-day streak        → +50
 *
 * ── Daily Challenges (bonus on completion) ────────────────────────
 *   Rate 3 dishes       → +20
 *   Upload 1 photo      → +15
 *
 * ── Weekly Challenges (bonus on completion) ───────────────────────
 *   Visit 3 restaurants → +40
 *   Upload 5 dishes     → +50
 */
object PointsConfig {

    // ── Core action points ─────────────────────────────────────────
    const val UPLOAD_PHOTO       = 10
    const val RATE_DISH          = 5
    const val WRITE_REVIEW       = 10
    const val ADD_RESTAURANT     = 20
    const val FIRST_PROFILE_PIC  = 50

    // ── Streak milestone rewards ───────────────────────────────────
    const val STREAK_3_DAY       = 20
    const val STREAK_7_DAY       = 50

    // ── Action type keys (match Supabase action_type column) ───────
    const val ACTION_UPLOAD_PHOTO      = "upload_photo"
    const val ACTION_RATE_DISH         = "rate_dish"
    const val ACTION_WRITE_REVIEW      = "write_review"
    const val ACTION_ADD_RESTAURANT    = "add_restaurant"
    const val ACTION_FIRST_PROFILE_PIC = "first_profile_pic"

    /**
     * Look up how many points a given action type earns.
     */
    fun pointsFor(actionType: String): Int = when (actionType) {
        ACTION_UPLOAD_PHOTO      -> UPLOAD_PHOTO
        ACTION_RATE_DISH         -> RATE_DISH
        ACTION_WRITE_REVIEW      -> WRITE_REVIEW
        ACTION_ADD_RESTAURANT    -> ADD_RESTAURANT
        ACTION_FIRST_PROFILE_PIC -> FIRST_PROFILE_PIC
        else -> 0
    }

    // ── Level thresholds ───────────────────────────────────────────
    /** XP needed to reach each level (index = level).  Level 0 unused. */
    val LEVEL_THRESHOLDS = listOf(
        0,      // Level 0 (unused)
        0,      // Level 1 — start
        50,     // Level 2
        150,    // Level 3
        300,    // Level 4
        500,    // Level 5
        750,    // Level 6
        1050,   // Level 7
        1400,   // Level 8
        1800,   // Level 9
        2300,   // Level 10
        2900,   // Level 11
        3600,   // Level 12
        4500,   // Level 13
        5500,   // Level 14
        7000    // Level 15
    )

    /** Calculate level from total XP. */
    fun levelForXp(totalXp: Int): Int {
        var level = 1
        for (i in LEVEL_THRESHOLDS.indices) {
            if (totalXp >= LEVEL_THRESHOLDS[i]) level = i
        }
        return level.coerceAtLeast(1)
    }

    /** XP needed to reach the *next* level. */
    fun xpForNextLevel(totalXp: Int): Int {
        val currentLevel = levelForXp(totalXp)
        val nextIndex = (currentLevel + 1).coerceAtMost(LEVEL_THRESHOLDS.lastIndex)
        return LEVEL_THRESHOLDS[nextIndex]
    }

    /** Progress fraction (0f–1f) within the current level. */
    fun levelProgress(totalXp: Int): Float {
        val currentLevel = levelForXp(totalXp)
        val currentThreshold = LEVEL_THRESHOLDS[currentLevel.coerceAtMost(LEVEL_THRESHOLDS.lastIndex)]
        val nextIndex = (currentLevel + 1).coerceAtMost(LEVEL_THRESHOLDS.lastIndex)
        val nextThreshold = LEVEL_THRESHOLDS[nextIndex]
        val range = nextThreshold - currentThreshold
        if (range <= 0) return 1f
        return ((totalXp - currentThreshold).toFloat() / range).coerceIn(0f, 1f)
    }
}
