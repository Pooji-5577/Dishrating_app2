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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.ui.components.BottomNavBar
import com.example.smackcheck2.ui.components.NavItem
import com.example.smackcheck2.ui.components.RestaurantHeroSection
import com.example.smackcheck2.ui.components.TopRatedDishCard
import com.example.smackcheck2.ui.components.LoadingState
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.util.formatRelativeTime
import com.example.smackcheck2.viewmodel.PhotoState
import com.example.smackcheck2.viewmodel.RestaurantDetailViewModel
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch

// Figma design maroon accent
private val FigmaMaroon = Color(0xFF642223)
private val FigmaMaroonDot = Color(0xFFBB5B5C)

@Composable
fun RestaurantDetailScreen(
    viewModel: RestaurantDetailViewModel,
    photoViewModel: RestaurantPhotoViewModel? = null,
    preferencesRepository: PreferencesRepository? = null,
    restaurantId: String,
    userAvatarUrl: String? = null,
    onNavigateBack: () -> Unit,
    onNotificationClick: () -> Unit = {},
    onNavItemClick: (Int) -> Unit = {},
    onNavHome: () -> Unit = {},
    onNavMap: () -> Unit = {},
    onNavCamera: () -> Unit = {},
    onNavExplore: () -> Unit = {},
    onNavProfile: () -> Unit = {},
    currencySymbol: String = "$"
) {
    val uiState by viewModel.uiState.collectAsState()
    val photoStates by photoViewModel?.photoStates?.collectAsState()
        ?: remember { mutableStateOf(emptyMap<String, PhotoState>()) }
    val colors = appColors()
    val errorBackground = colors.SurfaceVariant
    val errorText = colors.TextSecondary
    val errorButtonColor = FigmaMaroon

    var bookmarkedDishIds by remember { mutableStateOf(emptySet<String>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(restaurantId) {
        viewModel.loadRestaurant(restaurantId)
        preferencesRepository?.let { repo ->
            bookmarkedDishIds = repo.getBookmarks()
        }
    }

    when {
        uiState.isLoading -> LoadingState(message = "Loading restaurant...")
        uiState.errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(errorBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage ?: "Unable to load restaurant",
                        color = errorText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.retry(restaurantId) },
                        colors = ButtonDefaults.buttonColors(containerColor = errorButtonColor)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        uiState.restaurant != null -> {
            val restaurant = uiState.restaurant!!
            val heroImage = restaurant.photoUrl ?: restaurant.imageUrls.firstOrNull()

            LaunchedEffect(restaurant.id) {
                photoViewModel?.loadFullPhotos(
                    restaurantId = restaurant.id,
                    placeId = restaurant.googlePlaceId,
                    name = restaurant.name,
                    city = restaurant.city
                )
            }

            // Toolbar background fades in as user scrolls past the hero image
            val listState = rememberLazyListState()
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

            // Resolve hero photo from photoStates — photoStates is collected but hero
            // previously read only restaurant.photoUrl; this wires them together.
            val heroPhotoUrl = when (val ps = photoStates[restaurant.id]) {
                is PhotoState.FullPhotosLoaded -> ps.urls.firstOrNull()
                is PhotoState.ThumbnailLoaded -> ps.url
                else -> null
            } ?: restaurant.photoUrl ?: restaurant.imageUrls.firstOrNull()

            Scaffold(
                containerColor = Color(0xFFF6F6F6),
                bottomBar = {
                    BottomNavBar(
                        selectedItem = NavItem.MAP,
                        onHomeClick = onNavHome,
                        onMapClick = onNavMap,
                        onCameraClick = onNavCamera,
                        onExploreClick = onNavExplore,
                        onProfileClick = onNavProfile
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        // Only apply bottom padding — hero must fill edge-to-edge under status bar
                        .padding(bottom = paddingValues.calculateBottomPadding())
                ) {
                    // Hero goes full edge-to-edge from top of screen (behind status bar)
                    // The floating overlay bar handles its own statusBarsPadding
                    item(key = "hero") {
                        RestaurantHeroSection(restaurant = restaurant.copy(photoUrl = heroPhotoUrl))
                    }

                    // Status pills: cuisine · open-now · location city
                    item(key = "status_tags") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (restaurant.cuisine.isNotBlank()) {
                                StatusPill(text = restaurant.cuisine, background = FigmaMaroon)
                            }
                            if (restaurant.isOpenNow == true) {
                                StatusPill(
                                    text = "Open Now",
                                    background = FigmaMaroonDot,
                                    dot = true
                                )
                            }
                            if (restaurant.city.isNotBlank()) {
                                LocationPill(city = restaurant.city)
                            }
                        }
                    }

                    // ── Top Rated Dishes ── always shown (with empty state when no data)
                    item(key = "top_dishes_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "Top Rated Dishes",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.TextPrimary,
                                letterSpacing = (-0.6).sp
                            )
                            Text(
                                text = "VIEW ALL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FigmaMaroon,
                                letterSpacing = 1.4.sp
                            )
                        }
                    }

                    item(key = "top_dishes_content") {
                        if (uiState.topDishes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No dishes yet. Be the first to rate one!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.topDishes, key = { it.id }) { dish ->
                                    TopRatedDishCard(
                                        dish = dish,
                                        isBookmarked = bookmarkedDishIds.contains(dish.id),
                                        onBookmarkClick = {
                                            coroutineScope.launch {
                                                preferencesRepository?.let { repo ->
                                                    val newState = repo.toggleBookmark(dish.id)
                                                    bookmarkedDishIds = if (newState)
                                                        bookmarkedDishIds + dish.id
                                                    else
                                                        bookmarkedDishIds - dish.id
                                                }
                                            }
                                        },
                                        onClick = {},
                                        currencySymbol = currencySymbol
                                    )
                                }
                            }
                        }
                    }

                    // ── Recent Reviews ── always shown
                    item(key = "reviews_header") {
                        Text(
                            text = "Recent Reviews",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.TextPrimary,
                            letterSpacing = (-0.6).sp,
                            modifier = Modifier.padding(
                                start = 24.dp, end = 24.dp,
                                top = 32.dp, bottom = 0.dp
                            )
                        )
                    }

                    if (uiState.reviews.isEmpty()) {
                        item(key = "no_reviews") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No reviews yet. Be the first!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(uiState.reviews.take(5), key = { _, review -> "review_${review.id}" }) { index, review ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(40.dp))
                            }
                            FigmaReviewItem(
                                review = review,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // ── Figma top bar overlay ──
                // Outer Box: fills (statusBarHeight + 56dp) by using a Spacer as a
                // height anchor. Background and Row each apply statusBarsPadding()
                // independently so both have 56dp of usable space below the status bar.
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Height anchor: statusBarHeight + 56dp
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                    )

                    // Fading background covers the full anchor area
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = animatedAlpha }
                            .background(colors.Background)
                    )

                    // Icons row — sits below status bar with full 56dp height
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (animatedAlpha.compareTo(0.5f) > 0) colors.TextPrimary else Color.White
                            )
                        }

                        // "SmackCheck" title — centred in available space
                        Text(
                            text = "SmackCheck",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (animatedAlpha.compareTo(0.5f) > 0) colors.TextPrimary else Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        // Notification bell
                        IconButton(onClick = onNotificationClick) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = if (animatedAlpha.compareTo(0.5f) > 0) colors.TextPrimary else Color.White
                            )
                        }

                        // User avatar (36dp circle)
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FigmaMaroon),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userAvatarUrl.isNullOrBlank()) {
                                KamelImage(
                                    resource = asyncPainterResource(userAvatarUrl),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onFailure = {
                                        Icon(
                                            imageVector = Icons.Filled.Restaurant,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Restaurant,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cuisine / open-now status pill
 */
@Composable
private fun StatusPill(
    text: String,
    background: Color,
    dot: Boolean = false
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/**
 * Location pill — #BB5B5C background with white pin icon and white city text (Figma spec)
 */
@Composable
private fun LocationPill(city: String) {
    Box(
        modifier = Modifier
            .background(FigmaMaroonDot, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Text(
                text = city,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/**
 * Review item matching the Figma design:
 * 56dp circular avatar | name + timestamp row | star row | italic quoted comment
 */
@Composable
private fun FigmaReviewItem(
    review: Review,
    modifier: Modifier = Modifier
) {
    val colors = appColors()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar — 56dp circle
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!review.userProfileUrl.isNullOrEmpty()) {
                KamelImage(
                    resource = asyncPainterResource(review.userProfileUrl!!),
                    contentDescription = review.userName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onFailure = {
                        AvatarInitial(review.userName, colors.TextPrimary)
                    }
                )
            } else {
                AvatarInitial(review.userName, colors.TextPrimary)
            }
        }

        // Right column
        Column(modifier = Modifier.weight(1f)) {
            // Name + timestamp on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary
                )
                Text(
                    text = formatRelativeTime(review.createdAt),
                    fontSize = 12.sp,
                    color = FigmaMaroon
                )
            }

            // Stars directly below name
            Spacer(modifier = Modifier.height(2.dp))
            StarRow(rating = review.rating)

            // Italic quoted comment — Newsreader 18sp per Figma spec
            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\u201C${review.comment}\u201D",
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    color = colors.TextPrimary,
                    lineHeight = 28.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AvatarInitial(name: String, textColor: Color) {
    Text(
        text = name.firstOrNull()?.uppercase() ?: "?",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
}

@Composable
private fun StarRow(rating: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val full = rating.toInt().coerceIn(0, 5)
        repeat(full) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = FigmaMaroon
            )
        }
        repeat((5 - full).coerceAtLeast(0)) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = FigmaMaroon.copy(alpha = 0.25f)
            )
        }
    }
}
