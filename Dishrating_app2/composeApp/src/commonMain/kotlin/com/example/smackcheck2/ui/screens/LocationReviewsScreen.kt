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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.ui.components.LoadingState
import com.example.smackcheck2.ui.components.StarRatingDisplay
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.LocationReviewsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Location Reviews Screen - Shows all reviews filtered by selected location
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationReviewsScreen(
    viewModel: LocationReviewsViewModel,
    location: String?,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeColors = appColors()
    var showFilterMenu by remember { mutableStateOf(false) }

    // Load reviews when screen is launched
    LaunchedEffect(location) {
        viewModel.loadReviews(location, sortByRating = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "All Reviews",
                            color = themeColors.TextPrimary
                        )
                        if (location != null) {
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.TextSecondary
                            )
                        }
                    }
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
                actions = {
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filter",
                            tint = themeColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.Surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.Background)
                .padding(paddingValues)
        ) {
            // Sort Filter Chips
            if (showFilterMenu) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.sortByRating,
                        onClick = { viewModel.changeSortOrder(true) },
                        label = {
                            Text(
                                "Highest Rated",
                                color = if (uiState.sortByRating) themeColors.OnPrimary else themeColors.TextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (uiState.sortByRating) themeColors.OnPrimary else themeColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.Primary,
                            selectedLabelColor = themeColors.OnPrimary,
                            selectedLeadingIconColor = themeColors.OnPrimary
                        )
                    )

                    FilterChip(
                        selected = !uiState.sortByRating,
                        onClick = { viewModel.changeSortOrder(false) },
                        label = {
                            Text(
                                "Most Recent",
                                color = if (!uiState.sortByRating) themeColors.OnPrimary else themeColors.TextPrimary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.Primary,
                            selectedLabelColor = themeColors.OnPrimary
                        )
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    LoadingState(
                        message = "Loading reviews..."
                    )
                }
                uiState.reviews.isEmpty() -> {
                    EmptyReviewsState(
                        location = location,
                        themeColors = themeColors
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.reviews) { review ->
                            ReviewCard(
                                review = review,
                                onLikeClick = { viewModel.toggleLike(review.id) },
                                themeColors = themeColors
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no reviews are available
 */
@Composable
fun EmptyReviewsState(
    location: String?,
    themeColors: com.example.smackcheck2.ui.theme.ThemeColors
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = themeColors.TextTertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No reviews available for this location yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = themeColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            if (location != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Be the first to review a restaurant in $location!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Review Card - Displays a single review with user info, dish, and rating
 */
@Composable
fun ReviewCard(
    review: Review,
    onLikeClick: () -> Unit,
    themeColors: com.example.smackcheck2.ui.theme.ThemeColors,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = themeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(themeColors.Primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.TextPrimary
                    )
                    Text(
                        text = review.restaurantName,
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Date
                if (review.createdAt > 0) {
                    Text(
                        text = formatTimestamp(review.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dish image placeholder (if available)
            if (review.dishImageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(themeColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = themeColors.TextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Dish name and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.dishName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = themeColors.StarYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", review.rating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.TextPrimary
                    )
                }
            }

            // Comment
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onLikeClick)
                ) {
                    Icon(
                        imageVector = if (review.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (review.isLiked) themeColors.Error else themeColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = review.likesCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp to relative time
 */
fun formatTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val daysDiff = (now.date.toEpochDays() - dateTime.date.toEpochDays())

        when {
            daysDiff == 0 -> "Today"
            daysDiff == 1 -> "Yesterday"
            daysDiff < 7 -> "${daysDiff}d ago"
            daysDiff < 30 -> "${daysDiff / 7}w ago"
            daysDiff < 365 -> "${daysDiff / 30}mo ago"
            else -> "${daysDiff / 365}y ago"
        }
    } catch (e: Exception) {
        ""
    }
}
