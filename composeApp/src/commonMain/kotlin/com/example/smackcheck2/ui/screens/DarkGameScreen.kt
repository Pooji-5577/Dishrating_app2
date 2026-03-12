package com.example.smackcheck2.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.ui.theme.LocalThemeState
import com.example.smackcheck2.ui.screens.Achievement
import com.example.smackcheck2.ui.screens.Challenge
import com.example.smackcheck2.ui.screens.LeaderboardEntry
import com.example.smackcheck2.gamification.GamificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkGameScreen(
    viewModel: GamificationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Challenges", "Leaderboard", "Achievements")
    val themeColors = appColors()
    
    Scaffold(
        containerColor = themeColors.Background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Games & Challenges",
                        color = themeColors.TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = themeColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.Background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(themeColors.Background)
        ) {
            // Player stats card
            DarkPlayerStatsCard(
                xp = uiState.totalXp,
                level = uiState.level,
                rank = uiState.rank,
                streak = uiState.streakDays,
                badges = uiState.achievements.count { it.isUnlocked },
                modifier = Modifier.padding(16.dp)
            )
            
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = themeColors.Background,
                contentColor = themeColors.Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = themeColors.Primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title,
                                color = if (selectedTab == index) themeColors.Primary else themeColors.TextSecondary
                            ) 
                        }
                    )
                }
            }
            
            // Tab content
            when (selectedTab) {
                0 -> DarkChallengesTab(
                    dailyChallenges = uiState.dailyChallenges,
                    weeklyChallenges = uiState.weeklyChallenges
                )
                1 -> DarkLeaderboardTab(
                    leaderboard = uiState.leaderboard,
                    currentUserRank = uiState.rank
                )
                2 -> DarkAchievementsTab(
                    achievements = uiState.achievements
                )
            }
        }
    }
}

@Composable
private fun DarkPlayerStatsCard(
    xp: Int,
    level: Int,
    rank: Int,
    streak: Int,
    badges: Int,
    modifier: Modifier = Modifier
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4A1E1E),
                                Color(0xFF2D1212),
                                Color(0xFF1A0808),
                                Color(0xFF2D1212),
                                Color(0xFF4A1E1E)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFE5E5),
                                Color(0xFFFFD5D5),
                                Color(0xFFFFC5C5),
                                Color(0xFFFFD5D5),
                                Color(0xFFFFE5E5)
                            )
                        )
                    }
                )
        ) {
            // Gaming pattern overlay - diagonal stripes effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFE53935).copy(alpha = 0.15f),
                                Color.Transparent,
                                Color(0xFFFF6B35).copy(alpha = 0.1f),
                                Color.Transparent,
                                Color(0xFFE53935).copy(alpha = 0.15f)
                            )
                        )
                    )
            )
            
            // Radial glow effect from center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                themeColors.Primary.copy(alpha = 0.2f),
                                themeColors.Primary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )
            
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Level $level",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.TextPrimary
                        )
                        Text(
                            text = "Food Explorer",
                            fontSize = 14.sp,
                            color = themeColors.TextSecondary
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = themeColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$xp XP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.Primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(icon = Icons.Filled.EmojiEvents, value = "#$rank", label = "Rank")
                    StatItem(icon = Icons.Filled.LocalFireDepartment, value = "$streak", label = "Day Streak")
                    StatItem(icon = Icons.Filled.Star, value = "$badges", label = "Badges")
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = themeColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = themeColors.TextSecondary
        )
    }
}

@Composable
private fun DarkChallengesTab(
    dailyChallenges: List<Challenge>,
    weeklyChallenges: List<Challenge>
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Daily Challenges",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(dailyChallenges) { challenge ->
            DarkChallengeCard(challenge = challenge)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Weekly Challenges",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(weeklyChallenges) { challenge ->
            DarkChallengeCard(challenge = challenge)
        }
    }
}

@Composable
private fun DarkChallengeCard(
    challenge: Challenge
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    val progress by animateFloatAsState(
        targetValue = challenge.progress,
        animationSpec = tween(1000)
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isCompleted) 
                themeColors.Primary.copy(alpha = 0.15f) 
            else 
                themeColors.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (challenge.isCompleted) 
                            themeColors.Primary.copy(alpha = 0.2f)
                        else 
                            themeColors.SurfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (challenge.isCompleted) Icons.Filled.Check else challenge.icon,
                    contentDescription = null,
                    tint = themeColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeColors.TextPrimary
                )
                Text(
                    text = challenge.description,
                    fontSize = 13.sp,
                    color = themeColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = themeColors.Primary,
                        trackColor = themeColors.SurfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = themeColors.TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // XP reward
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${challenge.xpReward}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.Primary
                )
                Text(
                    text = "XP",
                    fontSize = 12.sp,
                    color = themeColors.Primary
                )
            }
        }
    }
}

@Composable
private fun DarkLeaderboardTab(
    leaderboard: List<LeaderboardEntry>,
    currentUserRank: Int
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(leaderboard) { index, entry ->
            DarkLeaderboardItem(
                rank = index + 1,
                entry = entry,
                isCurrentUser = index + 1 == currentUserRank
            )
        }
    }
}

@Composable
private fun DarkLeaderboardItem(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) 
                themeColors.Primary.copy(alpha = 0.15f) 
            else 
                themeColors.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "#$rank",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> themeColors.TextPrimary
                },
                modifier = Modifier.width(40.dp)
            )
            
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themeColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.userName.first().toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.Primary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name and level
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeColors.TextPrimary
                )
                Text(
                    text = "Level ${entry.level}",
                    fontSize = 12.sp,
                    color = themeColors.TextSecondary
                )
            }
            
            // XP
            Text(
                text = "${entry.xp} XP",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.Primary
            )
        }
    }
}

@Composable
private fun DarkAchievementsTab(
    achievements: List<Achievement>
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(achievements) { achievement ->
            DarkAchievementCard(achievement = achievement)
        }
    }
}

@Composable
private fun DarkAchievementCard(
    achievement: Achievement
) {
    val isDark = LocalThemeState.current.isDarkMode
    val themeColors = appColors()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) 
                themeColors.Primary.copy(alpha = 0.15f) 
            else 
                themeColors.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) 
                            themeColors.Primary.copy(alpha = 0.2f)
                        else 
                            themeColors.SurfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (achievement.isUnlocked) achievement.icon else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) themeColors.Primary else themeColors.TextTertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (achievement.isUnlocked) themeColors.TextPrimary else themeColors.TextSecondary
                )
                Text(
                    text = achievement.description,
                    fontSize = 13.sp,
                    color = themeColors.TextSecondary
                )
                
                if (achievement.isUnlocked) {
                    Text(
                        text = "Unlocked!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = themeColors.Primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
