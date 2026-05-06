package com.example.smackcheck2.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.ui.components.RestaurantHeroSection
import com.example.smackcheck2.ui.components.StarRatingDisplay
import com.example.smackcheck2.ui.components.TopRatedDishCard
import com.example.smackcheck2.ui.components.LoadingState
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.util.formatRelativeTime
import com.example.smackcheck2.viewmodel.PhotoState
import com.example.smackcheck2.viewmodel.RestaurantDetailViewModel
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

private data class DetailNavItem(
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
)

private val detailNavItems = listOf(
    DetailNavItem(
        "Home",
        { Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(24.dp)) },
        { Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(24.dp)) }
    ),
    DetailNavItem(
        "Map",
        { Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(24.dp)) },
        { Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(24.dp)) }
    ),
    DetailNavItem(
        "Rate",
        { Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp)) },
        { Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp)) }
    ),
    DetailNavItem(
        "Feed",
        { Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(24.dp)) },
        { Icon(Icons.Outlined.People, contentDescription = null, modifier = Modifier.size(24.dp)) }
    ),
    DetailNavItem(
        "Profile",
        { Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp)) },
        { Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp)) }
    )
)

@Composable
fun RestaurantDetailScreen(
    viewModel: RestaurantDetailViewModel,
    photoViewModel: RestaurantPhotoViewModel? = null,
    restaurantId: String,
    onNavigateBack: () -> Unit,
    onNavItemClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val photoStates by photoViewModel?.photoStates?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(emptyMap<String, PhotoState>()) }
    val colors = appColors()

    LaunchedEffect(restaurantId) {
        viewModel.loadRestaurant(restaurantId)
    }

    when {
        uiState.isLoading -> {
            LoadingState(message = "Loading restaurant...")
        }
        uiState.errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.Error
                    )
                    Text(
                        text = uiState.errorMessage ?: "An error occurred",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.TextPrimary
                    )
                    Button(onClick = { viewModel.retry(restaurantId) }) {
                        Text("Retry")
                    }
                }
            }
        }
        uiState.restaurant != null -> {
            val restaurant = uiState.restaurant!!
            val listState = rememberLazyListState()

            // Trigger Google Places full photo load
            LaunchedEffect(restaurant.id) {
                photoViewModel?.loadFullPhotos(
                    restaurantId = restaurant.id,
                    placeId = restaurant.googlePlaceId,
                    name = restaurant.name,
                    city = restaurant.city
                )
            }

            // Collapsing toolbar: compute header alpha based on scroll
            val headerAlpha by remember {
                derivedStateOf {
                    val firstItem = listState.firstVisibleItemIndex
                    if (firstItem > 0) 1f
                    else {
                        val offset = listState.firstVisibleItemScrollOffset.toFloat()
                        (offset / 300f).coerceIn(0f, 1f)
                    }
                }
            }
            val animatedAlpha by animateFloatAsState(targetValue = headerAlpha)

            Scaffold(
                containerColor = colors.Background,
                bottomBar = {
                    NavigationBar(
                        containerColor = colors.Surface,
                        tonalElevation = 0.dp
                    ) {
                        detailNavItems.forEachIndexed { index, item ->
                            val isSelected = index == 1 // Map tab active
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { onNavItemClick(index) },
                                icon = {
                                    if (isSelected) item.selectedIcon() else item.unselectedIcon()
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.Primary,
                                    selectedTextColor = colors.Primary,
                                    unselectedIconColor = colors.TextSecondary,
                                    unselectedTextColor = colors.TextSecondary,
                                    indicatorColor = colors.Primary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Hero section
                        item(key = "hero") {
                            RestaurantHeroSection(
                                restaurant = restaurant
                            )
                        }

                        // Top Rated Dishes section
                        if (uiState.topDishes.isNotEmpty()) {
                            item(key = "top_dishes_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Top Rated Dishes",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.TextPrimary
                                    )
                                    Text(
                                        text = "See All",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.Primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            item(key = "top_dishes_row") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(uiState.topDishes, key = { it.id }) { dish ->
                                        TopRatedDishCard(
                                            dish = dish,
                                            isBookmarked = false,
                                            onBookmarkClick = { /* TODO */ },
                                            onClick = { /* TODO: navigate to dish detail */ }
                                        )
                                    }
                                }
                            }
                        }

                        // All Dishes section (if there are dishes not in top)
                        if (uiState.dishes.isNotEmpty()) {
                            item(key = "menu_header") {
                                Text(
                                    text = "Menu",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            items(uiState.dishes, key = { "dish_${it.id}" }) { dish ->
                                DishListItem(
                                    dish = dish,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Recent Reviews section
                        item(key = "reviews_header") {
                            Text(
                                text = "Recent Reviews",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        if (uiState.reviews.isEmpty()) {
                            item(key = "no_reviews") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = CardShape,
                                    colors = CardDefaults.cardColors(containerColor = colors.SurfaceVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Restaurant,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = colors.TextTertiary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No reviews yet. Be the first!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(uiState.reviews.take(5), key = { "review_${it.id}" }) { review ->
                                DetailReviewCard(
                                    review = review,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }

                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Collapsing top bar overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .graphicsLayer { alpha = 1f }
                    ) {
                        // Filled background that fades in
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = animatedAlpha }
                                .background(colors.Background)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (animatedAlpha > 0.5f) colors.TextPrimary else Color.White
                                )
                            }

                            // Restaurant name fades in as header fills
                            Text(
                                text = restaurant.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = animatedAlpha }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Redesigned review card for restaurant detail — italic quoted text style
 */
@Composable
private fun DetailReviewCard(
    review: Review,
    modifier: Modifier = Modifier
) {
    val colors = appColors()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (review.userProfileUrl != null) {
                        KamelImage(
                            resource = asyncPainterResource(review.userProfileUrl!!),
                            contentDescription = review.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onFailure = {
                                Text(
                                    text = review.userName.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.Primary,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    } else {
                        Text(
                            text = review.userName.firstOrNull()?.uppercase() ?: "?",
                            fontWeight = FontWeight.Bold,
                            color = colors.Primary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.TextPrimary
                    )
                    Text(
                        text = formatRelativeTime(review.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.TextTertiary
                    )
                }

                StarRatingDisplay(
                    rating = review.rating,
                    starSize = 14.dp
                )
            }

            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\u201C${review.comment}\u201D",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = colors.TextSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Simple dish list item for the full menu section
 */
@Composable
private fun DishListItem(
    dish: Dish,
    modifier: Modifier = Modifier
) {
    val colors = appColors()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dish image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!dish.imageUrl.isNullOrEmpty()) {
                    KamelImage(
                        resource = asyncPainterResource(dish.imageUrl!!),
                        contentDescription = dish.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onFailure = {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = colors.TextTertiary
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colors.TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.TextPrimary
                )
                if (dish.rating > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = com.example.smackcheck2.ui.theme.StarColor
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", dish.rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = "No ratings yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.TextTertiary
                    )
                }
            }
        }
    }
}
