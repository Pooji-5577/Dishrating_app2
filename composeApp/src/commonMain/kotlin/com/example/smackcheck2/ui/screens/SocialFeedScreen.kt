package com.example.smackcheck2.ui.screens

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.model.FeedFilter
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.SocialFeedUiState
import com.example.smackcheck2.ui.components.EmptyState
import com.example.smackcheck2.ui.components.SocialFeedSkeleton
import com.example.smackcheck2.ui.components.StarRatingDisplay
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
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
    onScrollComplete: () -> Unit = {}
) {
    val colors = appColors()
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
        topBar = {
            TopAppBar(
                title = { Text("Social Feed", color = colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = colors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.Background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    FeedFilter.ALL -> "All"
                                    FeedFilter.FOLLOWING -> "Following"
                                    FeedFilter.NEARBY -> "Nearby"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.Primary,
                            selectedLabelColor = colors.Background
                        )
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    SocialFeedSkeleton()
                }

                uiState.feedItems.isEmpty() -> {
                    EmptyState(
                        title = "No Posts Yet",
                        message = when (uiState.filter) {
                            FeedFilter.FOLLOWING -> "Follow friends to see their dish ratings here"
                            FeedFilter.NEARBY -> "No ratings from nearby restaurants yet"
                            FeedFilter.ALL -> "Be the first to rate a dish!"
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.feedItems, key = { it.id }) { item ->
                            SocialFeedCard(
                                item = item,
                                onLikeClick = { onLikeClick(item.id) },
                                onCommentClick = { onCommentClick(item.id) },
                                onShareClick = { onShareClick(item) },
                                onUserClick = { onUserClick(item.userId) }
                            )
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
                        } else if (!uiState.hasMoreItems) {
                            item(key = "end_of_feed") {
                                Text(
                                    text = "You're all caught up!",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
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
fun SocialFeedCard(
    item: FeedItem,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onUserClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    // Driven by ViewModel state — no local copy to avoid triple-update bugs
    val isLiked = item.isLiked
    val likesCount = item.likesCount

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User profile row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onUserClick != null) Modifier.clickable { onUserClick() } else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.userName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.Primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.userName,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.TextPrimary
                    )
                    Text(
                        text = "at ${item.restaurantName}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = colors.TextSecondary
                    )
                }

                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = colors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dish image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(androidx.compose.material3.MaterialTheme.shapes.medium)
                    .background(colors.Surface),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = item.dishImageUrl?.takeIf { it.isNotBlank() }
                    ?: item.imageUrls.firstOrNull()
                println("SocialFeedCard: dish='${item.dishName}' imageUrl=$imageUrl")
                if (imageUrl != null) {
                    com.example.smackcheck2.ui.components.NetworkImage(
                        imageUrl = imageUrl,
                        contentDescription = item.dishName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = colors.TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dish name and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.dishName,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = colors.TextPrimary
                )

                StarRatingDisplay(
                    rating = item.rating,
                    starSize = 20.dp
                )
            }

            // Comment preview
            if (item.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.comment,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = colors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) colors.Error else colors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = likesCount.toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = colors.TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onCommentClick)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = colors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.commentsCount.toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = colors.TextSecondary
                    )
                }
            }
        }
    }
}
