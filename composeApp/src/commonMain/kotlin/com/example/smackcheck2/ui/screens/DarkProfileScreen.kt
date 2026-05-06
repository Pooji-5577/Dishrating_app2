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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.gamification.PointsConfig
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.User
import com.example.smackcheck2.ui.components.ReviewPostCard
import com.example.smackcheck2.ui.theme.LocalThemeState
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.util.formatOneDecimal
import com.example.smackcheck2.viewmodel.ProfileViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

// ── Brand palette ────────────────────────────────────────────────────────────
private val ProfileBg       = Color(0xFFF6F6F6)
private val CardWhite       = Color(0xFFFFFFFF)
private val DeepMaroon      = Color(0xFF3B1011)
private val WarmMaroon      = Color(0xFF642223)
private val CrimsonRed      = Color(0xFF9B2335)
private val LightBlush      = Color(0xFFFDE8E8)
private val MutedGrey       = Color(0xFF767777)
private val DividerGrey     = Color(0xFFEAE0D8)
private val StreakRed       = Color(0xFF9B2335)
private const val DefaultBioText = "Just getting started on my food journey.\nTrying every dish I can find — one plate at a time."

// ── Level title mapping ───────────────────────────────────────────────────────
private fun levelTitle(level: Int): String = when {
    level >= 10 -> "Grand Connoisseur"
    level >= 8  -> "Master Taster"
    level >= 6  -> "Flavor Expert"
    level >= 5  -> "Food Critic"
    level >= 4  -> "Flavor Chaser"
    level >= 3  -> "Rising Foodie"
    level >= 2  -> "Food Explorer"
    else        -> "Fresh Taster"
}

private fun nextLevelTitle(level: Int) = levelTitle(level + 1)
private fun xpProgress(xp: Int): Float = PointsConfig.levelProgress(xp)
private fun xpToGo(xp: Int): Int = maxOf(0, PointsConfig.xpForNextLevel(xp) - xp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkProfileScreen(
    viewModel: ProfileViewModel,
    preferencesRepository: PreferencesRepository? = null,
    onNavigateBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToGames: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToHelpFaq: () -> Unit = {},
    onNavigateToContactSupport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavHome: () -> Unit = {},
    onNavMap: () -> Unit = {},
    onNavCamera: () -> Unit = {},
    onNavExplore: () -> Unit = {},
    onNavProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState = LocalThemeState.current

    // Profile tab state
    var selectedTab by remember { mutableStateOf(0) } // 0=My Ratings, 1=Saved, 2=Reviews

    // Load user ratings
    var userRatings by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    val user = uiState.user

    // Wire selected cuisines to user profile preference tags
    LaunchedEffect(user?.id) {
        val uid = user?.id ?: return@LaunchedEffect
        try {
            val repo = SocialRepository()
            repo.getUserRatings(uid).onSuccess { items -> userRatings = items }
        } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBg),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = DeepMaroon, modifier = Modifier.size(22.dp))
                }
                Text("SmackCheck", color = DeepMaroon, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = DeepMaroon, modifier = Modifier.size(22.dp))
                }
            }
        }

        // ── Avatar + name + bio ──────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with level badge
                Box(modifier = Modifier.size(90.dp)) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(DeepMaroon, WarmMaroon))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user?.profilePhotoUrl != null) {
                            KamelImage(
                                resource = asyncPainterResource(user.profilePhotoUrl),
                                contentDescription = user.name,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                onFailure = {
                                    Text(
                                        text = user.name.firstOrNull()?.uppercase() ?: "?",
                                        color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        } else {
                            Text(
                                text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Level badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(DeepMaroon, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LVL ${user?.level ?: 1}",
                            color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name
                Text(user?.name ?: "", color = DeepMaroon, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                // @username + location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${user?.username ?: ""}",
                        color = MutedGrey, fontSize = 13.sp
                    )
                    if (!user?.lastLocation.isNullOrBlank()) {
                        Text(" • ", color = MutedGrey, fontSize = 13.sp)
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = WarmMaroon, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(user!!.lastLocation!!, color = MutedGrey, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFF642223).copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = levelTitle(user?.level ?: 1).uppercase(),
                        color = WarmMaroon, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bio
                val bioText = user?.bio?.takeIf { it.isNotBlank() } ?: DefaultBioText
                Text(
                    text = "\u201C$bioText\u201D",
                    color = Color(0xFF171717), fontSize = 18.sp, textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 30.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Edit Profile button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onEditProfile,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmMaroon, contentColor = Color.White)
                    ) {
                        Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9D2D2))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Edit profile",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ── Stats row ────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "${userRatings.size}", label = "DISHES")
                    StatDivider()
                    StatItem(value = "${user?.followersCount ?: 0}", label = "FOLLOWERS", onClick = {})
                    StatDivider()
                    StatItem(value = "${user?.followingCount ?: 0}", label = "FOLLOWING", onClick = {})
                    StatDivider()
                    val avgScore = if (userRatings.isNotEmpty())
                        formatOneDecimal(userRatings.map { it.rating }.average())
                    else "–"
                    StatItem(value = avgScore, label = "AVG SCORE")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Current Status card ──────────────────────────────────────────────
        item {
            val lvl   = user?.level ?: 1
            val xp    = user?.xp ?: 0
            val progress = xpProgress(xp)
            val toGo  = xpToGo(xp)
            val levelBase = PointsConfig.LEVEL_THRESHOLDS.getOrElse(lvl) { (lvl - 1) * 300 }
            val earned = maxOf(0, xp - levelBase)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable { onNavigateToProgress() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("CURRENT STATUS", color = MutedGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${levelTitle(lvl)} → ${nextLevelTitle(lvl)}",
                            color = CrimsonRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("$earned XP", color = DeepMaroon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = CrimsonRed,
                        trackColor = Color(0xFFE0DADA),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Level $lvl", color = MutedGrey, fontSize = 12.sp)
                        Text("$toGo xp to go", color = MutedGrey, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Streak card ──────────────────────────────────────────────────────
        item {
            val streak = user?.streakCount ?: 0
            val displayStreak = if (streak > 0) streak else 3
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        Brush.horizontalGradient(listOf(WarmMaroon, Color(0xFFB2001D))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("$displayStreak-day streak!", color = Color.White, fontSize = 44.sp / 2, fontWeight = FontWeight.Bold)
                        Text("Rate a dish today to keep it alive", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp / 1.2f)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Achievements ────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Achievements", color = DeepMaroon, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "View All",
                    color = CrimsonRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToProgress() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Achievement chips
        item {
            val badges = user?.badges ?: emptyList()
            if (badges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Start rating dishes to earn your first achievement!",
                        color = MutedGrey,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(badges.filter { it.isEarned }.take(6)) { badge ->
                        AchievementChip(icon = Icons.Default.Star, name = badge.name, xpLabel = "")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Tabs: My Ratings | Saved | Reviews ───────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                listOf("My Ratings", "Saved", "Reviews").forEachIndexed { i, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = i }
                    ) {
                        Text(
                            label,
                            color = if (selectedTab == i) DeepMaroon else MutedGrey,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(if (selectedTab == i) 40.dp else 0.dp)
                                .background(CrimsonRed)
                        )
                    }
                }
            }
            HorizontalDivider(color = DividerGrey, modifier = Modifier.padding(top = 0.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Ratings grid ─────────────────────────────────────────────────────
        if (selectedTab == 0) {
            if (userRatings.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = MutedGrey, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No ratings yet", color = MutedGrey, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                // 2-column grid of dish images
                val chunked = userRatings.chunked(2)
                items(chunked) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { item ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardWhite)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(Color(0xFFEEE5DC))
                                ) {
                                    if (item.dishImageUrl != null) {
                                        KamelImage(
                                            resource = asyncPainterResource(item.dishImageUrl),
                                            contentDescription = item.dishName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = MutedGrey, modifier = Modifier.size(32.dp).align(Alignment.Center))
                                    }
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(formatOneDecimal(item.rating.toDouble()), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    Text(item.dishName, color = DeepMaroon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.restaurantName, color = MutedGrey, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        // Filler if odd count
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = MutedGrey, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nothing here yet", color = MutedGrey, fontSize = 14.sp)
                }
            }
        }

        // ── ACCOUNT section ──────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("ACCOUNT")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                SettingsRow(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    subtitle = "Name, photo, bio, location",
                    onClick = onEditProfile
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Log Out button ──────────────────────────────────────────────────
        item {
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE1DBDB),
                    contentColor = WarmMaroon
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = WarmMaroon, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WarmMaroon)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    com.example.smackcheck2.ui.components.BottomNavBar(
        selectedItem = com.example.smackcheck2.ui.components.NavItem.PROFILE,
        onHomeClick = onNavHome,
        onMapClick = onNavMap,
        onCameraClick = onNavCamera,
        onExploreClick = onNavExplore,
        onProfileClick = onNavProfile,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    } // end Box
}

// ── Shared helper composables ─────────────────────────────────────────────────

@Composable
private fun StatItem(
    value: String,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(value, color = DeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = MutedGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color(0xFFEAE0D8))
    )
}

@Composable
private fun AchievementChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    xpLabel: String
) {
    Card(
        modifier = Modifier
            .width(132.dp)
            .height(140.dp)
            .border(1.dp, Color(0xFFE4DFDF), RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFE4DFDF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WarmMaroon,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, color = DeepMaroon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (xpLabel.isNotBlank()) {
                Text(xpLabel, color = WarmMaroon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color = MutedGrey) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = DeepMaroon, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DeepMaroon, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = MutedGrey, fontSize = 12.sp)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedGrey, modifier = Modifier.size(20.dp))
    }
}
