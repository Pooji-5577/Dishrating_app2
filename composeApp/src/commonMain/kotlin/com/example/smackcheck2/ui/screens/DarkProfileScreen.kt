package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.User
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.ProfileViewModel
import kotlin.math.roundToInt

private enum class ProfileTab {
    MyRatings, Saved, Reviews
}

@Composable
fun DarkProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToGames: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToHelpFaq: () -> Unit = {},
    onNavigateToContactSupport: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = appColors()
    val user = uiState.user

    var selectedTab by remember { mutableStateOf(ProfileTab.MyRatings) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(true) }
    var showCuisinePreferences by remember { mutableStateOf(true) }

    Scaffold(containerColor = Color(0xFFF5F5F5)) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF642223))
            }
            return@Scaffold
        }

        if (uiState.errorMessage != null || user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Unable to load profile",
                    color = colors.Error
                )
            }
            return@Scaffold
        }

        val ratings = uiState.userRatings
        val dishesCount = ratings.size
        val avgScore = if (ratings.isNotEmpty()) ratings.map { it.rating }.average() else 0.0
        val currentLevel = user.level.coerceAtLeast(1)
        val levelFloorXp = (currentLevel - 1) * 100
        val levelCapXp = currentLevel * 100
        val xpProgress = ((user.xp - levelFloorXp).coerceAtLeast(0)).toFloat() /
            (levelCapXp - levelFloorXp).coerceAtLeast(1).toFloat()
        val remainingXp = (levelCapXp - user.xp).coerceAtLeast(0)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileTopBar(
                onSearchClick = onNavigateBack,
                onSettingsClick = onNavigateToAccount
            )

            ProfileHeader(
                user = user,
                onEditProfile = onEditProfile
            )

            StatsGridCard(
                dishes = dishesCount,
                followers = user.followersCount,
                following = user.followingCount,
                avgScore = formatOneDecimal(avgScore)
            )

            ProgressCard(
                currentLevel = currentLevel,
                currentXp = user.xp,
                progress = xpProgress,
                remainingXp = remainingXp,
                onClick = onNavigateToProgress
            )

            StreakBanner(streakCount = user.streakCount, onClick = onNavigateToGames)

            AchievementsSection(
                badgeNames = user.badges.map { it.name },
                onViewAllClick = onNavigateToGames
            )

            RatingsTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            RatingsSection(
                selectedTab = selectedTab,
                ratings = ratings,
                isLoading = uiState.isRatingsLoading
            )

            LabelHeading(text = "Account", color = Color.Black)
            AccountSection(
                onEditProfile = onEditProfile,
                onNotifications = onNavigateToNotifications
            )

            LabelHeading(text = "Preferences")
            PreferencesSection(
                pushEnabled = pushNotificationsEnabled,
                onPushToggle = { pushNotificationsEnabled = it },
                locationEnabled = locationEnabled,
                onLocationToggle = { locationEnabled = it },
                showCuisinePreferences = showCuisinePreferences,
                onCuisineExpandToggle = { showCuisinePreferences = !showCuisinePreferences },
                onSavePreferences = onNavigateToPrivacy
            )

            LabelHeading(text = "Support")
            SupportSection(
                onHelpFaq = onNavigateToHelpFaq,
                onContactSupport = onNavigateToContactSupport
            )

            DangerZoneSection(onDeleteAccount = onNavigateToAccount)

            LogoutButton(onClick = onSignOut)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Back",
                tint = Color(0xFF2D2F2F)
            )
        }
        Text(
            text = "SmackCheck",
            color = Color(0xFF2D2F2F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF2D2F2F)
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User,
    onEditProfile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!user.profilePhotoUrl.isNullOrBlank()) {
            NetworkImage(
                imageUrl = user.profilePhotoUrl,
                contentDescription = user.name,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                showGradientOnFailure = false
            )
        } else {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE7E8E8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "",
                    color = Color(0xFF642223),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = user.name,
            color = Color(0xFF2D2F2F),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = user.lastLocation ?: "Unknown location",
            color = Color(0xFF5A5C5C),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = user.bio ?: "Just getting started on my food journey.",
            color = Color(0xFF2D2F2F),
            fontSize = 18.sp,
            lineHeight = 26.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onEditProfile,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE7DCDD),
                contentColor = Color(0xFF642223)
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "Edit Profile",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatsGridCard(
    dishes: Int,
    followers: Int,
    following: Int,
    avgScore: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(32.dp))
            .padding(horizontal = 10.dp, vertical = 20.dp)
    ) {
        StatsItem(value = dishes.toString(), label = "DISHES", modifier = Modifier.weight(1f))
        VerticalDivider()
        StatsItem(value = followers.toString(), label = "FOLLOWERS", modifier = Modifier.weight(1f))
        VerticalDivider()
        StatsItem(value = following.toString(), label = "FOLLOWING", modifier = Modifier.weight(1f))
        VerticalDivider()
        StatsItem(value = avgScore, label = "AVG SCORE", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatsItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color(0xFF642223),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(43.dp)
            .width(1.dp)
            .background(Color(0xFFE7E8E8))
    )
}

@Composable
private fun ProgressCard(
    currentLevel: Int,
    currentXp: Int,
    progress: Float,
    remainingXp: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "CURRENT STATUS",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Level $currentLevel Foodie",
                    color = Color(0xFF642223),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$currentXp XP",
                color = Color(0xFF642223),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color(0xFFDBDDDD), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(12.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF642223), Color(0xFFFF7669))
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Level $currentLevel",
                color = Color(0xFF5A5C5C),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$remainingXp XP to go",
                color = Color(0xFF642223),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StreakBanner(
    streakCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF8C111A), Color(0xFFBB000E))
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "${streakCount}-day streak! Keep rating daily to climb",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AchievementsSection(
    badgeNames: List<String>,
    onViewAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Achievements",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "View All",
                color = Color(0xFF642223),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (badgeNames.isEmpty()) {
                LockedAchievementCard()
            } else {
                badgeNames.take(4).forEach { badgeName ->
                    AchievementCard(
                        title = badgeName,
                        subtitle = "Unlocked"
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.width(140.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7E8E8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0x33642223), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF642223),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = Color(0xFF642223),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LockedAchievementCard() {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x80DBDDDD)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFACADAD)),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE7E8E8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF5A5C5C),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "On Fire",
                color = Color(0xFF2D2F2F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Locked",
                color = Color(0xFF5A5C5C),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RatingsTabs(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFFE7E8E8), shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TabButton(
            title = "My Ratings",
            active = selectedTab == ProfileTab.MyRatings,
            onClick = { onTabSelected(ProfileTab.MyRatings) }
        )
        TabButton(
            title = "Saved",
            active = selectedTab == ProfileTab.Saved,
            onClick = { onTabSelected(ProfileTab.Saved) }
        )
        TabButton(
            title = "Reviews",
            active = selectedTab == ProfileTab.Reviews,
            onClick = { onTabSelected(ProfileTab.Reviews) }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(top = 2.dp)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (active) Color(0xFF642223) else Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active) 2.dp else 1.dp)
                .background(if (active) Color(0xFF642223) else Color.Transparent)
        )
    }
}

@Composable
private fun RatingsSection(
    selectedTab: ProfileTab,
    ratings: List<FeedItem>,
    isLoading: Boolean
) {
    when (selectedTab) {
        ProfileTab.MyRatings -> {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF642223),
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                ratings.isEmpty() -> {
                    Text(
                        text = "No ratings yet.",
                        color = Color(0xFF5A5C5C),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ratings.take(8).forEach { item ->
                            RatingCard(item = item)
                        }
                    }
                }
            }
        }
        ProfileTab.Saved -> {
            Text(
                text = "Saved dishes will appear here.",
                color = Color(0xFF5A5C5C),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
        ProfileTab.Reviews -> {
            Text(
                text = "Review history will appear here.",
                color = Color(0xFF5A5C5C),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RatingCard(item: FeedItem) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.width(160.dp)
    ) {
        Column {
            if (!item.dishImageUrl.isNullOrBlank()) {
                NetworkImage(
                    imageUrl = item.dishImageUrl,
                    contentDescription = item.dishName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentScale = ContentScale.Crop,
                    showGradientOnFailure = false
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .background(Color(0xFFE7E8E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = Color(0xFF642223),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.dishName,
                    color = Color(0xFF642223),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.restaurantName,
                    color = Color(0xFF642223),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LabelHeading(
    text: String,
    color: Color = Color(0xFF5A5C5C)
) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.2.sp,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun AccountSection(
    onEditProfile: () -> Unit,
    onNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {
        SectionRow(
            icon = Icons.Filled.Edit,
            title = "Edit Profile",
            subtitle = "Name, photo, bio, location",
            onClick = onEditProfile
        )
        HorizontalDivider(color = Color(0xFFE7E8E8))
        SectionRow(
            icon = Icons.Filled.NotificationsNone,
            title = "Notifications",
            subtitle = "Manage alerts",
            onClick = onNotifications
        )
    }
}

@Composable
private fun PreferencesSection(
    pushEnabled: Boolean,
    onPushToggle: (Boolean) -> Unit,
    locationEnabled: Boolean,
    onLocationToggle: (Boolean) -> Unit,
    showCuisinePreferences: Boolean,
    onCuisineExpandToggle: () -> Unit,
    onSavePreferences: () -> Unit
) {
    val accent = Color(0xFFBB000E)
    val selectedChip = Color(0x33642223)
    val mutedChip = Color(0x33A59596)
    val selectedCuisines = listOf("Japanese", "Italian", "Indian", "Asian", "Mexican", "French")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {
        ToggleRow(
            icon = Icons.Filled.NotificationsNone,
            title = "Push Notifications",
            checked = pushEnabled,
            onCheckedChange = onPushToggle,
            checkedColor = accent
        )
        HorizontalDivider(color = Color(0xFFE7E8E8))
        ToggleRow(
            icon = Icons.Filled.LocationOn,
            title = "Location Access",
            checked = locationEnabled,
            onCheckedChange = onLocationToggle,
            checkedColor = accent
        )
        HorizontalDivider(color = Color(0xFFE7E8E8))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCuisineExpandToggle)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Cuisine Preferences",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF5A5C5C),
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (showCuisinePreferences) 90f else 0f)
            )
        }

        if (showCuisinePreferences) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rowIndex in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val rowItems = selectedCuisines.drop(rowIndex * 2).take(2)
                        rowItems.forEachIndexed { index, cuisine ->
                            val isSelected = rowIndex < 2
                            val itemModifier = if (index == 1 || rowItems.size == 1) Modifier.weight(1f) else Modifier.weight(1f)
                            Box(
                                modifier = itemModifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(48.dp))
                                    .background(if (isSelected) selectedChip else mutedChip)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = Color(0xFF642223),
                                        shape = RoundedCornerShape(48.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cuisine,
                                    color = Color(0xFF642223),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onSavePreferences,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF642223),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save Preferences",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = checkedColor
            )
        )
    }
}

@Composable
private fun SupportSection(
    onHelpFaq: () -> Unit,
    onContactSupport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {
        PlainSectionRow(title = "Help & FAQ", onClick = onHelpFaq)
        HorizontalDivider(color = Color(0xFFE7E8E8))
        PlainSectionRow(title = "Contact Support", onClick = onContactSupport)
    }
}

@Composable
private fun PlainSectionRow(
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFF2D2F2F),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF5A5C5C),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun DangerZoneSection(onDeleteAccount: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelHeading(text = "Danger Zone", color = Color(0xFF642223))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .border(width = 1.dp, color = Color(0xFF642223), shape = RoundedCornerShape(32.dp))
                .clickable(onClick = onDeleteAccount)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Delete Account",
                color = Color(0xFF642223),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = Color(0xFF642223),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .widthIn(min = 220.dp),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0x33642223),
            contentColor = Color(0xFF642223)
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            tint = Color(0xFF642223),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Log Out",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = Color.Black.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
