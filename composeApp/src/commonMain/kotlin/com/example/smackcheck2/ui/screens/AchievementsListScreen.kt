package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.gamification.GamificationViewModel
import com.example.smackcheck2.viewmodel.UserProgressViewModel

// ── Brand palette (same as DarkProfileScreen) ────────────────────────────────
private val ABg         = Color(0xFFF5EDE3)
private val ACardWhite  = Color(0xFFFFFFFF)
private val ADeepMaroon = Color(0xFF3B1011)
private val AWarmMaroon = Color(0xFF642223)
private val ACrimsonRed = Color(0xFF9B2335)
private val ALightBlush = Color(0xFFFDE8E8)
private val AMutedGrey  = Color(0xFF767777)
private val ADivider    = Color(0xFFEAE0D8)

// ── Level title helper ────────────────────────────────────────────────────────
private fun achLevelTitle(level: Int): String = when {
    level >= 10 -> "Grand Connoisseur"
    level >= 8  -> "Master Taster"
    level >= 6  -> "Flavor Expert"
    level >= 5  -> "Food Critic"
    level >= 4  -> "Flavor Chaser"
    level >= 3  -> "Rising Foodie"
    level >= 2  -> "Food Explorer"
    else        -> "Fresh Taster"
}

// ── Icon helper ───────────────────────────────────────────────────────────────
private fun iconForBadge(badgeId: String): ImageVector = when (badgeId.lowercase()) {
    "first_bite"        -> Icons.Filled.Restaurant
    "foodie_explorer"   -> Icons.Filled.Explore
    "rating_streak"     -> Icons.Filled.LocalFireDepartment
    "cuisine_master"    -> Icons.Filled.Fastfood
    "photo_pro"         -> Icons.Filled.CameraAlt
    "restaurant_hopper" -> Icons.Filled.TrendingUp
    "snap_rate"         -> Icons.Filled.CameraAlt
    "quality_critic"    -> Icons.Filled.Star
    "weekly_regular"    -> Icons.Filled.CalendarToday
    else                -> Icons.Filled.EmojiEvents
}

private fun earnedDateLabel(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis == 0L) return ""
    val months = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
    // Simple epoch → date (approximate, no timezone)
    val days = epochMillis / 86_400_000L
    val year  = 1970 + (days / 365).toInt()
    val month = ((days % 365) / 30).toInt().coerceIn(0, 11)
    val day   = ((days % 365) % 30 + 1).toInt()
    return "UNLOCKED ${months[month]} $day"
}

// ── Screen entry point ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsListScreen(
    progressViewModel: UserProgressViewModel,
    gamificationViewModel: GamificationViewModel,
    onNavigateBack: () -> Unit
) {
    val progressState  by progressViewModel.uiState.collectAsState()
    val gameState      by gamificationViewModel.uiState.collectAsState()

    val isLoading = progressState.isLoading || gameState.isLoading

    // Real badge data from DB
    val earnedBadges   = progressState.badges.filter { it.isEarned }
    val unearnedBadges = progressState.badges.filter { !it.isEarned }
    val allBadges      = progressState.badges

    // Gamification achievements (rule-based)
    val gAchievements  = gameState.achievements
    val dailyChallenges   = gameState.dailyChallenges
    val weeklyChallenges  = gameState.weeklyChallenges
    val allChallenges     = dailyChallenges + weeklyChallenges

    val totalEarned   = earnedBadges.size + gAchievements.count { it.isUnlocked }
    val totalMilestones = allBadges.size + gAchievements.size
    val milestonesEarned = earnedBadges.size + gAchievements.count { it.isUnlocked }

    val activityEarned = allChallenges.count { it.isCompleted }
    val activityTotal  = allChallenges.size

    val level   = progressState.level
    val streak  = progressState.streakCount.coerceAtLeast(gameState.streakDays)
    val xp      = progressState.currentXp.coerceAtLeast(gameState.totalXp)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ABg),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ADeepMaroon)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("SmackCheck", color = ADeepMaroon, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = ADeepMaroon, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AWarmMaroon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        // ── Page title ───────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    "Achievements List",
                    color = ADeepMaroon, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Your culinary journey, tracked and celebrated.",
                    color = AMutedGrey, fontSize = 14.sp, fontStyle = FontStyle.Italic
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Loading spinner ───────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ACrimsonRed)
                }
            }
            return@LazyColumn
        }

        // ── Summary cards ────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    icon = Icons.Filled.EmojiEvents,
                    label = "TOTAL EARNED",
                    value = "$totalEarned Badges"
                )
                SummaryCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "CURRENT STREAK",
                    value = "$streak Days"
                )
                SummaryCard(
                    icon = Icons.Filled.Star,
                    label = "REPUTATION",
                    value = achLevelTitle(level)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── Milestones header ────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(4.dp).height(22.dp).background(ACrimsonRed, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Milestones", color = ADeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(ALightBlush, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("$milestonesEarned/$totalMilestones Earned", color = ACrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Earned badges from DB ─────────────────────────────────────────────
        items(earnedBadges, key = { "badge_earned_${it.id}" }) { badge ->
            MilestoneCard(
                icon = iconForBadge(badge.id),
                title = badge.name,
                description = badge.description,
                isUnlocked = true,
                unlockedDateLabel = earnedDateLabel(badge.earnedDate),
                progress = null
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Unlocked gamification achievements ───────────────────────────────
        items(gAchievements.filter { it.isUnlocked }, key = { "gach_${it.id}" }) { ach ->
            MilestoneCard(
                icon = ach.icon,
                title = ach.title,
                description = ach.description,
                isUnlocked = true,
                unlockedDateLabel = "",
                progress = null
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Locked/in-progress badges (unearned) ─────────────────────────────
        items(unearnedBadges, key = { "badge_unearned_${it.id}" }) { badge ->
            // Show with progress for known badges that have progress data
            val progressValue = badgeProgress(badge.id, xp, streak, level)
            MilestoneCard(
                icon = iconForBadge(badge.id),
                title = badge.name,
                description = badge.description,
                isUnlocked = false,
                unlockedDateLabel = "",
                progress = progressValue
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Locked gamification achievements ─────────────────────────────────
        items(gAchievements.filter { !it.isUnlocked }, key = { "gach_locked_${it.id}" }) { ach ->
            val progress = achProgress(ach.id, xp, streak, level)
            MilestoneCard(
                icon = ach.icon,
                title = ach.title,
                description = ach.description,
                isUnlocked = false,
                unlockedDateLabel = "",
                progress = progress
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Spacer
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // ── Activity header ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(4.dp).height(22.dp).background(ACrimsonRed, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Activity", color = ADeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(ALightBlush, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${activityEarned}/${activityTotal.coerceAtLeast(1)} Earned",
                        color = ACrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Activity chips row (streak + weekly) ──────────────────────────────
        item {
            val activityItems = buildList {
                if (streak > 0) add(Triple(Icons.Filled.LocalFireDepartment, "${streak}-Day Streak", "EARNED TODAY!"))
                weeklyChallenges.filter { it.isCompleted }.forEach { c ->
                    add(Triple(c.icon, c.title, "COMPLETE"))
                }
                if (streak == 0 && weeklyChallenges.none { it.isCompleted }) {
                    add(Triple(Icons.Filled.CalendarToday, "Weekly Regular", ""))
                }
            }

            if (activityItems.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activityItems, key = { it.second }) { (icon, label, sublabel) ->
                        ActivityChip(icon = icon, title = label, earnedLabel = sublabel)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Daily challenges ──────────────────────────────────────────────────
        items(dailyChallenges, key = { "daily_${it.id}" }) { challenge ->
            ChallengeCard(challenge = challenge)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Weekly challenges ─────────────────────────────────────────────────
        items(weeklyChallenges, key = { "weekly_${it.id}" }) { challenge ->
            ChallengeCard(challenge = challenge)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Empty state for activity
        if (allChallenges.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ACardWhite)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AMutedGrey, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active challenges", color = AMutedGrey, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ── Badge/achievement progress helpers ───────────────────────────────────────

private data class ProgressInfo(val current: Int, val target: Int)

private fun badgeProgress(badgeId: String, xp: Int, streak: Int, level: Int): ProgressInfo? = when (badgeId.lowercase()) {
    "first_bite"        -> ProgressInfo(minOf(xp / 10, 1), 1)
    "foodie_explorer"   -> ProgressInfo(minOf(xp / 10, 10), 10)
    "rating_streak"     -> ProgressInfo(minOf(streak, 7), 7)
    "cuisine_master"    -> ProgressInfo(minOf(level, 15), 15)
    "photo_pro"         -> ProgressInfo(minOf(xp / 50, 20), 20)
    "restaurant_hopper" -> ProgressInfo(minOf(xp / 10, 5), 5)
    else                -> null
}

private fun achProgress(achId: String, xp: Int, streak: Int, level: Int): ProgressInfo? = when (achId) {
    "a1" -> ProgressInfo(if (xp > 0) 1 else 0, 1)
    "a2" -> ProgressInfo(minOf(level, 5), 5)
    "a3" -> ProgressInfo(minOf(streak, 3), 3)
    "a4" -> ProgressInfo(minOf(streak, 7), 7)
    "a5" -> ProgressInfo(minOf(xp, 1000), 1000)
    "a6" -> ProgressInfo(minOf(level, 10), 10)
    "a7" -> ProgressInfo(minOf(xp, 500), 500)
    else -> null
}

// ── Composable building blocks ────────────────────────────────────────────────

@Composable
private fun SummaryCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ACardWhite)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(ALightBlush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AWarmMaroon, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, color = AMutedGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, color = ADeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    icon: ImageVector,
    title: String,
    description: String,
    isUnlocked: Boolean,
    unlockedDateLabel: String,
    progress: ProgressInfo?
) {
    val modifier = if (isUnlocked) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .border(1.5.dp, ADivider, RoundedCornerShape(16.dp))
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ACardWhite)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        if (isUnlocked) Brush.linearGradient(listOf(ADeepMaroon, AWarmMaroon))
                        else Brush.linearGradient(listOf(ADivider, ADivider)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isUnlocked) Color.White else AMutedGrey,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = ADeepMaroon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    description,
                    color = AMutedGrey, fontSize = 13.sp, fontStyle = FontStyle.Italic,
                    lineHeight = 18.sp
                )
                if (isUnlocked && unlockedDateLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(unlockedDateLabel, color = ACrimsonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                if (!isUnlocked && progress != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PROGRESS", color = AMutedGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text("${progress.current}/${progress.target}", color = AMutedGrey, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (progress.current.toFloat() / progress.target.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = ACrimsonRed,
                        trackColor = ALightBlush,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            // Checkmark for unlocked, lock icon for locked
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (isUnlocked) ACrimsonRed else ADivider,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ActivityChip(icon: ImageVector, title: String, earnedLabel: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ACardWhite)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).width(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ALightBlush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ACrimsonRed, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = ADeepMaroon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (earnedLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(earnedLabel, color = ACrimsonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: Challenge) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(
                if (challenge.isCompleted)
                    Brush.horizontalGradient(listOf(AWarmMaroon, ACrimsonRed))
                else
                    Brush.horizontalGradient(listOf(ADeepMaroon, AWarmMaroon)),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(challenge.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(challenge.description, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
                }
                Text(
                    "+${challenge.xpReward} XP",
                    color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
            if (!challenge.isCompleted && challenge.progress > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { challenge.progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
            }
            if (challenge.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COMPLETE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
