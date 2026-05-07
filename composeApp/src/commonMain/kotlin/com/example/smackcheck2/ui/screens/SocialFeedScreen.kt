package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.SocialFeedUiState
import com.example.smackcheck2.model.UserSummary
import com.example.smackcheck2.ui.components.BottomNavBar
import com.example.smackcheck2.ui.components.NavItem
import com.example.smackcheck2.ui.components.EmptyState
import com.example.smackcheck2.ui.components.FeedTabRow
import com.example.smackcheck2.ui.components.FollowingStoriesRow
import com.example.smackcheck2.ui.components.NearbyMapBanner
import com.example.smackcheck2.ui.components.ReviewPostCard
import com.example.smackcheck2.ui.components.SocialFeedSkeleton
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.components.TopDishesCarousel
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.ui.theme.appColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialFeedScreen(
    uiState: SocialFeedUiState,
    onNavigateBack: () -> Unit,
    onFilterSelected: (FeedFilter) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (FeedItem) -> Unit,
    onUserClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit = {},
    onScrollComplete: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onFindFriendsClick: () -> Unit = {},
    onBookmarkClick: (String) -> Unit = {},
    onMapBannerClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    currentUserAvatarUrl: String? = null,
    onStoryClick: (String) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onCurrentUserStoryClick: () -> Unit = {},
    onTopDishClick: (String) -> Unit = {},
    onSeeAllTopDishes: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onNavExploreClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val colors = appColors()
    val jakartaSans = PlusJakartaSans()
    val listState = rememberLazyListState()

    // Auto-scroll to newly created post
    LaunchedEffect(uiState.scrollToIndex) {
        val index = uiState.scrollToIndex ?: return@LaunchedEffect
        listState.animateScrollToItem(index)
        onScrollComplete()
    }

    // Trigger load-more when within 3 items of the bottom
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) onLoadMore()
    }

    Scaffold(
        containerColor = colors.Background,
        bottomBar = {
            BottomNavBar(
                selectedItem = NavItem.EXPLORE,
                onHomeClick = onHomeClick,
                onMapClick = onMapClick,
                onCameraClick = onCameraClick,
                onExploreClick = onNavExploreClick,
                onProfileClick = onProfileClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Top bar: SmackCheck logo + bell (no dot) + user photo
            item(key = "top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmackCheckWordmark(
                        fontFamily = jakartaSans,
                        fontSize = 22.sp,
                        letterSpacing = 0.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onNotificationsClick() }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = colors.TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.Primary.copy(alpha = 0.15f))
                                .clickable { onAvatarClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentUserAvatarUrl.isNullOrBlank()) {
                                KamelImage(
                                    resource = asyncPainterResource(currentUserAvatarUrl),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onFailure = {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = colors.Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = colors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Following Stories Row
            item(key = "stories") {
                FollowingStoriesRow(
                    storyUsers = uiState.storyUsers,
                    currentUserAvatarUrl = currentUserAvatarUrl,
                    currentUserHasStory = uiState.stories.any { it.userId == uiState.currentUserId },
                    onAddStoryClick = onAddStoryClick,
                    onCurrentUserStoryClick = onCurrentUserStoryClick,
                    onStoryClick = onStoryClick
                )
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Top Dishes Carousel
            if (uiState.topDishes.isNotEmpty()) {
                item(key = "top_dishes") {
                    TopDishesCarousel(
                        dishes = uiState.topDishes,
                        onDishClick = { id ->
                            val feedItem = uiState.topDishes.find { it.id == id }
                            val navId = feedItem?.dishId?.takeIf { it.isNotBlank() } ?: id
                            onTopDishClick(navId)
                        },
                        onSeeAllClick = onSeeAllTopDishes
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // "From Your Network" header + sticky tab row
            stickyHeader(key = "tab_row") {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    Text(
                        text = "From Your Network",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = jakartaSans,
                        color = Color(0xFF2D2F2F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    FeedTabRow(
                        selectedTab = uiState.filter,
                        onTabSelected = onFilterSelected
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Error banner
            if (!uiState.errorMessage.isNullOrBlank()) {
                item(key = "error_banner") {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clickable { onRefresh() },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color(0xFF9B2335).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Color(0xFF9B2335),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Something went wrong — tap to retry",
                                color = Color(0xFF9B2335),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Feed content
            when {
                uiState.isLoading -> {
                    item(key = "skeleton") {
                        SocialFeedSkeleton()
                    }
                }

                uiState.feedItems.isEmpty() -> {
                    item(key = "empty") {
                        EmptyState(
                            title = "No Posts Yet",
                            message = when (uiState.filter) {
                                FeedFilter.FOLLOWING -> "Follow friends to see their dish ratings here"
                                FeedFilter.TRENDING -> "No trending posts yet"
                                FeedFilter.NEARBY -> "No ratings from nearby restaurants yet"
                                FeedFilter.MY_RATINGS -> "You haven't rated any dishes yet"
                            },
                            icon = when (uiState.filter) {
                                FeedFilter.FOLLOWING -> Icons.Outlined.People
                                FeedFilter.TRENDING -> Icons.AutoMirrored.Outlined.TrendingUp
                                FeedFilter.NEARBY -> Icons.Outlined.LocationOn
                                FeedFilter.MY_RATINGS -> Icons.Outlined.RssFeed
                            },
                            action = {
                                Button(
                                    onClick = {
                                        when (uiState.filter) {
                                            FeedFilter.FOLLOWING -> onFindFriendsClick()
                                            FeedFilter.NEARBY -> onMapBannerClick()
                                            else -> onExploreClick()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.Primary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        when (uiState.filter) {
                                            FeedFilter.FOLLOWING -> "Find Friends"
                                            FeedFilter.TRENDING -> "Rate a Dish"
                                            FeedFilter.NEARBY -> "Explore Map"
                                            FeedFilter.MY_RATINGS -> "Rate a Dish"
                                        }
                                    )
                                }
                            }
                        )
                    }
                }

                else -> {
                    val feedItems = uiState.feedItems
                    feedItems.forEachIndexed { index, item ->
                        item(key = item.id) {
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                ReviewPostCard(
                                    feedItem = item,
                                    onLikeClick = { onLikeClick(item.id) },
                                    onCommentClick = { onCommentClick(item.id) },
                                    onShareClick = { onShareClick(item) },
                                    onBookmarkClick = { onBookmarkClick(item.id) },
                                    onUserClick = { onUserClick(item.userId) },
                                    onDishClick = {
                                        val navId = item.dishId.takeIf { it.isNotBlank() } ?: item.id
                                        onTopDishClick(navId)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Insert map banner after 3rd item
                        if (index == 2) {
                            item(key = "map_banner") {
                                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    NearbyMapBanner(
                                        restaurantCount = uiState.nearbyRestaurantCount,
                                        onClick = onMapBannerClick
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    if (uiState.isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    } else if (!uiState.hasMoreItems && feedItems.isNotEmpty()) {
                        item(key = "end_of_feed") {
                            Text(
                                text = "You're all caught up!",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        }
    }
}
