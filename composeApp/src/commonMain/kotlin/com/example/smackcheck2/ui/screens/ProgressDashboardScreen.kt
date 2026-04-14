package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.data.repository.AuthRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.gamification.GamificationViewModel
import com.example.smackcheck2.model.Badge
import com.example.smackcheck2.viewmodel.UserProgressViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

// ── Palette ───────────────────────────────────────────────────────────────────
private val PBg         = Color(0xFFF6F6F6)
private val PCardWhite  = Color(0xFFFFFFFF)
private val PDeepMaroon = Color(0xFF3B1011)
private val PWarmMaroon = Color(0xFF642223)
private val PCrimsonRed = Color(0xFF9B2335)
private val PLightBlush = Color(0xFFFDE8E8)
private val PMutedGrey  = Color(0xFF767777)
private val PDivider    = Color(0xFFEAE0D8)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun membershipTier(level: Int) = when {
    level >= 8 -> "DIAMOND MEMBER"
    level >= 5 -> "PLATINUM MEMBER"
    level >= 3 -> "GOLD MEMBER"
    level >= 2 -> "SILVER MEMBER"
    else       -> "BRONZE MEMBER"
}

private fun progLevelTitle(level: Int): String = when {
    level >= 10 -> "Grand Connoisseur"
    level >= 8  -> "Master Taster"
    level >= 6  -> "Flavor Expert"
    level >= 5  -> "Food Critic"
    level >= 4  -> "Flavor Chaser"
    level >= 3  -> "Rising Foodie"
    level >= 2  -> "Food Explorer"
    else        -> "Fresh Taster"
}

private fun rankPerk(level: Int): Pair<String, String> {
    val title = "${progLevelTitle(level)} Rank"
    val body  = when {
        level >= 8 -> "You are a legend! Enjoy **unlimited XP boosts** on any review and exclusive Grand Tasting invitations."
        level >= 6 -> "You are a flavor expert! Get **20% bonus XP** on every review plus early access to new features."
        level >= 5 -> "You are a food critic! Earn **15% more XP** on restaurant discoveries and unlock weekly bonus challenges."
        level >= 4 -> "Your flavors chase is paying off! Enjoy **12% more XP** on rated dishes and access to curated dish lists."
        level >= 3 -> "You are rising fast! Enjoy **10% more XP** on all weekend reviews and early access to \"Secret Menu\" journals."
        level >= 2 -> "You're discovering new flavors! Get **10% bonus XP** on first reviews of any restaurant."
        else       -> "Your palate is evolving! Enjoy **10% more XP** on all weekend reviews and early access to \"Secret Menu\" journals."
    }
    return title to body
}

private fun iconForBadgeProg(badgeId: String): ImageVector = when (badgeId.lowercase()) {
    "first_bite"        -> Icons.Filled.Restaurant
    "foodie_explorer"   -> Icons.Filled.Star
    "rating_streak"     -> Icons.Filled.LocalFireDepartment
    "cuisine_master"    -> Icons.Filled.Star
    "photo_pro"         -> Icons.Filled.CameraAlt
    "restaurant_hopper" -> Icons.Filled.Star
    "snap_rate"         -> Icons.Filled.CameraAlt
    "quality_critic"    -> Icons.Filled.Star
    else                -> Icons.Filled.EmojiEvents
}

private fun badgeXpProgress(badge: Badge, xp: Int, streak: Int, level: Int): Pair<Int, Int>? = when (badge.id.lowercase()) {
    "rating_streak"     -> streak to 7
    "cuisine_master"    -> (level * 2) to 15
    "photo_pro"         -> (xp / 50).coerceAtMost(20) to 20
    "restaurant_hopper" -> (xp / 10).coerceAtMost(5) to 5
    "foodie_explorer"   -> level to 5
    else                -> null
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ProgressDashboardScreen(
    progressViewModel: UserProgressViewModel,
    gamificationViewModel: GamificationViewModel,
    onNavigateBack: () -> Unit,
    onViewAllAchievements: () -> Unit
) {
    val progressState  by progressViewModel.uiState.collectAsState()
    val gameState      by gamificationViewModel.uiState.collectAsState()

    val isLoading = progressState.isLoading

    val level       = progressState.level.coerceAtLeast(gameState.level)
    val xp          = progressState.currentXp.coerceAtLeast(gameState.totalXp)
    val streak      = progressState.streakCount.coerceAtLeast(gameState.streakDays)
    val maxXp       = (level * 300).coerceAtLeast(100)
    val xpProgress  = ((xp - (level - 1) * 300).toFloat() / (level * 300 - (level - 1) * 300).toFloat()).coerceIn(0f, 1f)
    val xpBase      = (level - 1) * 300
    val xpCap       = level * 300

    val earnedBadges   = progressState.badges.filter { it.isEarned }
    val unearnedBadges = progressState.badges.filter { !it.isEarned }
    val gAchievements  = gameState.achievements
    val dailyChallenges = gameState.dailyChallenges
    val weeklyChallenges = gameState.weeklyChallenges

    val milestones = unearnedBadges + gAchievements.filter { !it.isUnlocked }.map { ach ->
        Badge(id = ach.id, name = ach.title, description = ach.description, isEarned = false)
    }
    val milestonesRemaining = milestones.size

    // Load user info for name, photo, followers, dishes count
    var userName    by remember { mutableStateOf("") }
    var photoUrl    by remember { mutableStateOf<String?>(null) }
    var followers   by remember { mutableIntStateOf(0) }
    var dishesCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val user = AuthRepository().getCurrentUser()
            userName = user?.name ?: ""
            photoUrl = user?.profilePhotoUrl
            followers = user?.followersCount ?: 0
            val uid = user?.id ?: return@LaunchedEffect
            SocialRepository().getUserRatings(uid).onSuccess { dishesCount = it.size }
        } catch (_: Exception) {}
    }

    val (rankTitle, rankBody) = rankPerk(level)

    Box(modifier = Modifier.fillMaxSize().background(PBg)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PBg),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PDeepMaroon)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("SmackCheck", color = PDeepMaroon, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Notifications, null, tint = PDeepMaroon, modifier = Modifier.size(22.dp))
                }
                // Avatar
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(PWarmMaroon),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl != null) {
                        KamelImage(
                            resource = asyncPainterResource(photoUrl!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onFailure = {
                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        // Loading
        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PCrimsonRed)
                }
            }
            return@LazyColumn
        }

        // ── Summary card (level + name/next + XP + stats) ─────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .drawBehind {
                                val strokeWidth = 8.dp.toPx()
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        listOf(PWarmMaroon, Color(0xFFDB7065), PWarmMaroon)
                                    ),
                                    radius = size.minDimension / 2f - strokeWidth / 2f,
                                    center = size.center,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LEVEL", color = PWarmMaroon, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("$level", color = Color.Black, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 68.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(userName.ifBlank { "SmackChecker" }, color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.Black, fontSize = 20.sp / 1.6f, fontWeight = FontWeight.Bold)) { append("Next: ") }
                                withStyle(SpanStyle(color = PWarmMaroon, fontSize = 20.sp / 1.6f, fontWeight = FontWeight.Bold)) { append(progLevelTitle(level + 1)) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp / 1.8f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PDivider)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(xpProgress)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(PWarmMaroon, Color(0xFFDB7065))))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$xp XP", color = Color.Black, fontSize = 34.sp / 2.2f, fontWeight = FontWeight.Bold)
                        Text("$xpCap XP", color = Color.Black, fontSize = 34.sp / 2.2f, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PStatItem("$dishesCount", "DISHES", valueColor = Color.Black)
                        PStatDivider()
                        PStatItem("$followers", "FOLLOWERS", valueColor = Color.Black)
                        PStatDivider()
                        PStatItem(
                            value = if (streak > 0) "${streak}-Day" else "0-Day",
                            label = "STREAK",
                            valueColor = PWarmMaroon
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Rank perk card ────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F1F1))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(84.dp).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Restaurant, null, tint = PCrimsonRed, modifier = Modifier.size(34.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rankTitle, color = Color(0xFF3E3E3E), fontSize = 48.sp / 2, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                val parts = rankBody.split("**")
                                parts.forEachIndexed { i, part ->
                                    if (i % 2 == 0) {
                                        withStyle(SpanStyle(color = Color(0xFF575757), fontSize = 17.sp, fontWeight = FontWeight.Normal)) { append(part) }
                                    } else {
                                        withStyle(SpanStyle(color = PCrimsonRed, fontSize = 17.sp, fontWeight = FontWeight.Bold)) { append(part) }
                                    }
                                }
                            },
                            lineHeight = 26.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Recent Achievements header ────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT ACHIEVEMENTS", color = PDeepMaroon, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                Text(
                    "VIEW ALL", color = PCrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewAllAchievements() }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Achievement chips ─────────────────────────────────────────────────
        item {
            // Combine DB earned badges + unlocked gamification achievements
            val recentList = buildList {
                earnedBadges.take(2).forEach { b -> add(Triple(iconForBadgeProg(b.id), b.name, true)) }
                gAchievements.filter { it.isUnlocked }.take(3 - size).forEach { a ->
                    add(Triple(a.icon, a.title, true))
                }
                // Pad with locked items if not enough
                if (size < 3) {
                    gAchievements.filter { !it.isUnlocked }.take(3 - size).forEach { a ->
                        add(Triple(a.icon, a.title, false))
                    }
                }
            }.take(3)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentList, key = { it.second }) { (icon, label, isEarned) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(PLightBlush, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = if (isEarned) PWarmMaroon else PMutedGrey, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(label, color = PDeepMaroon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Milestones header ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Milestones", color = PDeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                if (milestonesRemaining > 0) {
                    Text("$milestonesRemaining remaining", color = PMutedGrey, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Milestone progress items ──────────────────────────────────────────
        items(milestones.take(5), key = { "ms_${it.id}" }) { badge ->
            val (achIcon, achIsGam) = if (badge.id.startsWith("a")) {
                val gAch = gAchievements.find { it.id == badge.id }
                (gAch?.icon ?: iconForBadgeProg(badge.id)) to true
            } else {
                iconForBadgeProg(badge.id) to false
            }

            val progressPair: Pair<Int, Int>? = if (achIsGam) {
                when (badge.id) {
                    "a1" -> xp.coerceAtMost(1) to 1
                    "a2" -> level.coerceAtMost(5) to 5
                    "a3" -> streak.coerceAtMost(3) to 3
                    "a4" -> streak.coerceAtMost(7) to 7
                    "a5" -> xp.coerceAtMost(1000) to 1000
                    "a6" -> level.coerceAtMost(10) to 10
                    "a7" -> xp.coerceAtMost(500) to 500
                    else -> null
                }
            } else {
                badgeXpProgress(badge, xp, streak, level)
            }

            MilestoneRow(
                icon = achIcon,
                title = badge.name,
                progress = progressPair
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (milestones.isEmpty()) {
            item {
                Text(
                    "🎉 All milestones completed!",
                    color = PMutedGrey, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Daily Challenges header ───────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily challenges", color = PDeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEAECEC))
                        .clickable { gamificationViewModel.loadAll() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = PMutedGrey, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", color = PMutedGrey, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Challenge items ───────────────────────────────────────────────────
        val allChallenges = (dailyChallenges + weeklyChallenges).take(6)

        if (allChallenges.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PCardWhite)
                ) {
                    Text(
                        "No active challenges right now. Check back later!",
                        color = PMutedGrey, fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(allChallenges, key = { "ch_${it.id}" }) { challenge ->
                ChallengeRow(challenge = challenge)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
    com.example.smackcheck2.ui.components.BottomNavBar(
        selectedItem = com.example.smackcheck2.ui.components.NavItem.PROFILE,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    } // end Box
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun PStatItem(value: String, label: String, valueColor: Color = PDeepMaroon) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = PMutedGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun PStatDivider() {
    Box(modifier = Modifier.width(1.dp).height(28.dp).background(PDivider))
}

@Composable
private fun MilestoneRow(icon: ImageVector, title: String, progress: Pair<Int, Int>?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PCardWhite)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFF4EEEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = PWarmMaroon, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = PDeepMaroon, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (progress != null) {
                    val (cur, max) = progress
                    val label = if (max >= 100) "$cur/$max XP" else "$cur/$max Reviews"
                    Text(label, color = PMutedGrey, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val frac = if (progress != null) (progress.first.toFloat() / progress.second.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = PCrimsonRed,
                trackColor = PDivider,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ChallengeRow(challenge: Challenge) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PCardWhite)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier.size(42.dp).background(PLightBlush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(challenge.icon, null, tint = PWarmMaroon, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(challenge.title, color = PDeepMaroon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("+${challenge.xpReward} XP", color = PCrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            // Status indicator
            when {
                challenge.isCompleted -> {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(PCrimsonRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                challenge.progress > 0f -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val filledDots = (challenge.progress * 3).toInt().coerceIn(1, 3)
                        repeat(3) { i ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(if (i < filledDots) PCrimsonRed else PDivider, CircleShape)
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .border(1.5.dp, PCrimsonRed, CircleShape)
                    )
                }
            }
        }
    }
}
