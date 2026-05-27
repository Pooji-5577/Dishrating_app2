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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.smackcheck2.ui.components.ReviewPostCard
import com.example.smackcheck2.ui.components.SocialFeedSkeleton
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.components.TopDishesCarousel
import com.example.smackcheck2.ui.theme.BrandRed
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
    onNotificationsClick: () -> Unit = {},
    onUserSearchChange: (String) -> Unit = {},
    onUserSearchSubmit: (String) -> Unit = {},
    onUserSuggestionClick: (UserSummary) -> Unit = {}
) {
    val colors = appColors()
    val jakartaSans = PlusJakartaSans()
    val listState = rememberLazyListState()
    var userSearch by remember { mutableStateOf("") }

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

            item(key = "user_search") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    val searchShape = RoundedCornerShape(32.dp)

                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = {
                            userSearch = it
                            onUserSearchChange(it)
                        },
                        singleLine = true,
                        shape = searchShape,
                        placeholder = { Text("Search user id (e.g. @simhateja)") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = BrandRed,
                                modifier = Modifier.clickable {
                                    if (userSearch.isNotBlank()) onUserSearchSubmit(userSearch)
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, BrandRed, searchShape),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = BrandRed,
                            focusedTextColor = colors.TextPrimary,
                            unfocusedTextColor = colors.TextPrimary
                        )
                    )

                    if (uiState.isUserSearchLoading || uiState.userSearchSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            if (uiState.isUserSearchLoading) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = BrandRed,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Searching usernames...",
                                        color = colors.TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                uiState.userSearchSuggestions.forEachIndexed { index, user ->
                                    UserSearchSuggestionRow(
                                        user = user,
                                        onClick = {
                                            userSearch = user.username?.let { "@$it" } ?: user.name
                                            onUserSuggestionClick(user)
                                        }
                                    )
                                    if (index < uiState.userSearchSuggestions.lastIndex) {
                                        HorizontalDivider(color = BrandRed.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }

                    val normalizedSearch = userSearch.trim().removePrefix("@")
                    if (
                        normalizedSearch.length >= 2 &&
                        !uiState.isUserSearchLoading &&
                        uiState.userSearchSuggestions.isEmpty()
                    ) {
                        Text(
                            text = "No matching user found. Please check the username and try again.",
                            color = colors.TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                            containerColor = BrandRed.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = BrandRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Something went wrong — tap to retry",
                                color = BrandRed,
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

                    // Show first 3 items
                    items(
                        feedItems.take(3),
                        key = { it.id }
                    ) { item ->
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

                    // Remaining items
                    if (feedItems.size > 3) {
                        items(
                            feedItems.drop(3),
                            key = { it.id }
                        ) { item ->
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

@Composable
private fun UserSearchSuggestionRow(
    user: UserSummary,
    onClick: () -> Unit
) {
    val colors = appColors()
    val handle = user.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "@${user.name}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BrandRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (!user.profilePhotoUrl.isNullOrBlank()) {
                KamelImage(
                    resource = asyncPainterResource(user.profilePhotoUrl),
                    contentDescription = user.name,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onFailure = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = handle,
                color = colors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.name.isNotBlank()) {
                Text(
                    text = user.name,
                    color = colors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
