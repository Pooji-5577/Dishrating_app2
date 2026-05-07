package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Surface
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.model.UserSummary
import com.example.smackcheck2.ui.components.TopDishesCarousel
import com.example.smackcheck2.ui.components.BottomNavBar
import com.example.smackcheck2.ui.components.NavItem
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.theme.NewsreaderFontFamily
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.*

// ─── Design tokens ───────────────────────────────────────────────────────────
private val Maroon      = Color(0xFF642223)
private val MaroonLight = Color(0xFFBB5B5C)
private val MaroonAlpha = Color(0x33642223)   // rgba(100,34,35,0.20)
private val PageBg      = Color(0xFFF6F6F6)
private val CardBg      = Color.White
private val CreamAlpha  = Color(0xE6E4E2DF)   // rgba(228,226,223,0.90)
private val TextBlack   = Color(0xFF2D2F2F)
private val TextGray    = Color(0xFF5C5B5B)
private val MapDark     = Color(0xFF0C0F0F)

// ─── Haversine distance (km) ─────────────────────────────────────────────────
private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatDist(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else "${"%.1f".format(km)} km"

// ─── Greeting helper ─────────────────────────────────────────────────────────
private fun greeting(): String {
    val hour = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else      -> "Good night"
    }
}

// ─── Cuisine filter buckets ──────────────────────────────────────────────────
private val ASIAN_CUISINES = setOf(
    "asian","chinese","japanese","korean","thai","vietnamese","indian","sushi","ramen","dim sum"
)
private val DESSERT_CUISINES = setOf(
    "dessert","desserts","bakery","ice cream","sweet","café","cafe","patisserie","gelato"
)
private val ITALIAN_CUISINES = setOf("italian","pizza","pasta")

private fun Restaurant.matchesChip(chip: String): Boolean = when (chip) {
    "All"      -> true
    "Near Me"  -> true          // sorted separately, all included
    "Italian"  -> ITALIAN_CUISINES.any { cuisine.lowercase().contains(it) }
    "Asian"    -> ASIAN_CUISINES.any { cuisine.lowercase().contains(it) }
    "Desserts" -> DESSERT_CUISINES.any { cuisine.lowercase().contains(it) }
    else       -> true
}

// ═════════════════════════════════════════════════════════════════════════════
// Main composable
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun DarkHomeScreen(
    currentLocation: String,
    userName: String = "",
    userProfilePhotoUrl: String? = null,
    isLoading: Boolean = false,
    allRestaurants: List<Restaurant> = emptyList(),
    allDishes: List<Dish> = emptyList(),
    topDishFeedItems: List<FeedItem> = emptyList(),
    followingUsers: List<UserSummary> = emptyList(),
    currentUserHasStory: Boolean = false,
    noRestaurantsFound: Boolean = false,
    photoViewModel: RestaurantPhotoViewModel? = null,
    currentLatitude: Double? = null,
    currentLongitude: Double? = null,
    hasUnreadNotifications: Boolean = false,
    onLocationClick: () -> Unit = {},
    onDishClick: (String) -> Unit = {},
    onFeedItemDishClick: (String) -> Unit = {},
    onRestaurantClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onGameClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onCurrentUserStoryClick: () -> Unit = {},
    onStoryClick: (String) -> Unit = {},
    onTopDishesClick: () -> Unit = {},
    onTopRestaurantsClick: () -> Unit = {},
    onNearbyRestaurantsClick: () -> Unit = {},
    onChipSelected: (String) -> Unit = {},
    onSocialFeedClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddRestaurantClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val jakartaSans   = PlusJakartaSans()
    val newsreader    = NewsreaderFontFamily()

    var selectedChip by remember { mutableStateOf("All") }
    val chips = listOf("All", "Near Me", "Italian", "Asian", "Desserts")

    // Filter + sort restaurants by selected chip
    val filteredRestaurants = remember(allRestaurants, selectedChip, currentLatitude, currentLongitude) {
        val base = allRestaurants.filter { it.matchesChip(selectedChip) }
        if (selectedChip == "Near Me" && currentLatitude != null && currentLongitude != null) {
            base.sortedBy { r ->
                if (r.latitude != null && r.longitude != null)
                    distanceKm(currentLatitude, currentLongitude, r.latitude, r.longitude)
                else Double.MAX_VALUE
            }
        } else base
    }

    val topDishes   = allDishes.take(6)
    val nearbyCards = filteredRestaurants.take(4)
    val rankingList = filteredRestaurants
        .sortedByDescending { it.averageRating }
        .take(3)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 0.dp, end = 0.dp,
                top = 0.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Top Nav Bar ───────────────────────────────────────────────
            item {
                TopNavBar(
                    userProfilePhotoUrl = userProfilePhotoUrl,
                    jakartaSans = jakartaSans,
                    hasUnreadNotifications = hasUnreadNotifications,
                    onNotificationsClick = onNotificationsClick,
                    onProfileClick = onProfileClick
                )
            }

            // ── Section 1: Greeting + Search + Chips ─────────────────────
            item {
                Spacer(Modifier.height(8.dp))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Greeting text block
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${greeting()}, ${userName.ifBlank { "there" }} 👋",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = TextBlack
                        )
                        Text(
                            text = "Find your next meal.",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            letterSpacing = (-0.75).sp,
                            color = Maroon
                        )
                    }

                    // Search + filter button row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaroonAlpha)
                                .clickable { onSearchClick() }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = Maroon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Search dishes, restaurants...",
                                fontFamily = jakartaSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = Maroon
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaroonAlpha)
                                .clickable { onSearchClick() }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Filter",
                                tint = Maroon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Category chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chips) { chip ->
                            val active = chip == selectedChip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (active) Maroon else MaroonLight)
                                    .clickable {
                                        selectedChip = chip
                                        onChipSelected(chip)
                                    }
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chip,
                                    fontFamily = jakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 2: Following stories ─────────────────────────────
            item { Spacer(Modifier.height(40.dp)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Following",
                        fontFamily = jakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TextBlack,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // "Your Story" add button
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                val maroonColor = Maroon
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .drawBehind {
                                            drawCircle(
                                                color = maroonColor,
                                                radius = size.minDimension / 2 - 3.dp.toPx(),
                                                style = Stroke(
                                                    width = 2.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(
                                                        floatArrayOf(8f, 5f), 0f
                                                    )
                                                )
                                            )
                                        }
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(MaroonLight)
                                        .clickable {
                                            if (currentUserHasStory) onCurrentUserStoryClick() else onAddStoryClick()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentUserHasStory && userProfilePhotoUrl != null) {
                                        KamelImage(
                                            resource = asyncPainterResource(userProfilePhotoUrl),
                                            contentDescription = "Your Story",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (currentUserHasStory) {
                                        Text(
                                            text = userName.take(1).ifBlank { "?" }.uppercase(),
                                            fontFamily = jakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Add story",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Your Story",
                                    fontFamily = jakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Maroon,
                                    maxLines = 1
                                )
                            }
                        }
                        items(followingUsers.take(6), key = { it.id }) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .border(2.dp, Maroon, CircleShape)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .clickable { onStoryClick(user.id) }
                                ) {
                                    if (!user.profilePhotoUrl.isNullOrBlank()) {
                                        NetworkImage(
                                            imageUrl = user.profilePhotoUrl,
                                            contentDescription = user.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaroonLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                fontFamily = jakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = user.name,
                                    fontFamily = jakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 64.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 3: Top Dishes Today ───────────────────────────────
            item { Spacer(Modifier.height(40.dp)) }
            item {
                // Prefer FeedItem-based carousel (same as Explore page); fall back to Dish-based cards
                val feedDishes = topDishFeedItems.ifEmpty {
                    // Convert allDishes (Dish) to FeedItem so TopDishesCarousel can render them
                    topDishes.map { d ->
                        FeedItem(
                            id = d.id,
                            userId = d.userId,
                            userProfileImageUrl = d.uploaderProfileUrl,
                            userName = d.uploaderName,
                            dishImageUrl = d.imageUrl,
                            dishName = d.name,
                            dishId = d.id,
                            restaurantName = d.restaurantName,
                            restaurantCity = d.restaurantCity,
                            rating = d.rating,
                            likesCount = 0,
                            commentsCount = 0,
                            isLiked = false,
                            timestamp = d.createdAt
                        )
                    }
                }

                if (feedDishes.isEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Dishes Today",
                                fontFamily = jakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = TextBlack
                            )
                            Text(
                                text = "See all",
                                fontFamily = jakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Maroon,
                                modifier = Modifier.clickable { onTopDishesClick() }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(313.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(CardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No dishes yet",
                                fontFamily = jakartaSans,
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
                } else {
                    TopDishesCarousel(
                        dishes = feedDishes,
                        onDishClick = { id ->
                            // id is FeedItem.id (rating id); resolve to dishId when available
                            val feedItem = feedDishes.find { it.id == id }
                            val navId = feedItem?.dishId?.takeIf { it.isNotBlank() } ?: id
                            if (topDishFeedItems.isNotEmpty()) onFeedItemDishClick(navId)
                            else onDishClick(navId)
                        },
                        onSeeAllClick = onTopDishesClick,
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                }
            }

            // ── Section 4: Nearby Restaurants ────────────────────────────
            item { Spacer(Modifier.height(40.dp)) }
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nearby Restaurants",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = TextBlack
                        )
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Filter",
                            tint = TextBlack,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onNearbyRestaurantsClick() }
                        )
                    }

                    nearbyCards.take(1).forEach { restaurant ->
                        val distText = if (currentLatitude != null && currentLongitude != null &&
                            restaurant.latitude != null && restaurant.longitude != null)
                            formatDist(distanceKm(currentLatitude, currentLongitude, restaurant.latitude, restaurant.longitude))
                        else currentLocation

                        NearbyRestaurantCard(
                            restaurant = restaurant,
                            distanceText = distText,
                            jakartaSans = jakartaSans,
                            newsreader = newsreader,
                            onClick = { onRestaurantClick(restaurant.id) }
                        )
                    }

                    // Map banner
                    MapBanner(
                        count = filteredRestaurants.size,
                        jakartaSans = jakartaSans,
                        onExploreClick = onMapClick
                    )

                    nearbyCards.drop(1).take(1).forEach { restaurant ->
                        val distText = if (currentLatitude != null && currentLongitude != null &&
                            restaurant.latitude != null && restaurant.longitude != null)
                            formatDist(distanceKm(currentLatitude, currentLongitude, restaurant.latitude, restaurant.longitude))
                        else currentLocation

                        NearbyRestaurantCard(
                            restaurant = restaurant,
                            distanceText = distText,
                            jakartaSans = jakartaSans,
                            newsreader = newsreader,
                            onClick = { onRestaurantClick(restaurant.id) }
                        )
                    }
                }
            }

            // ── Section 5: Top Restaurants This Week ─────────────────────
            item { Spacer(Modifier.height(40.dp)) }
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Restaurants\nThis Week",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = TextBlack
                        )
                        Text(
                            text = "View\nRankings",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Maroon,
                            modifier = Modifier.clickable { onTopRestaurantsClick() }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        rankingList.forEach { restaurant ->
                            val distText = if (currentLatitude != null && currentLongitude != null &&
                                restaurant.latitude != null && restaurant.longitude != null)
                                formatDist(distanceKm(currentLatitude, currentLongitude, restaurant.latitude, restaurant.longitude))
                            else ""

                            RankingRow(
                                restaurant = restaurant,
                                distanceText = distText,
                                jakartaSans = jakartaSans,
                                onClick = { onRestaurantClick(restaurant.id) }
                            )
                        }
                    }
                }
            }
        }

        BottomNavBar(
            selectedItem = NavItem.HOME,
            onHomeClick = {},
            onMapClick = onMapClick,
            onCameraClick = onCameraClick,
            onExploreClick = onSocialFeedClick,
            onProfileClick = onProfileClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─── Top Dish Card (280dp wide × 313dp tall, matching Figma) ─────────────────
@Composable
private fun TopDishCard(
    dish: Dish,
    onClick: () -> Unit,
    jakartaSans: androidx.compose.ui.text.font.FontFamily,
    newsreader: androidx.compose.ui.text.font.FontFamily
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(313.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(CardBg)
            .clickable { onClick() }
    ) {
        Column {
            // Image area 192dp
            Box(modifier = Modifier.fillMaxWidth().height(192.dp)) {
                NetworkImage(
                    imageUrl = dish.imageUrl ?: "",
                    contentDescription = dish.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Rating pill top-right with location-specific count
                if (dish.rating > 0f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(CreamAlpha)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Maroon,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "%.1f".format(dish.rating),
                                    fontFamily = jakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Maroon
                                )
                            }
                        }
                        if (dish.ratingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(CreamAlpha)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${dish.ratingCount} local",
                                    fontFamily = jakartaSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }

            // Text area
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = dish.name,
                        fontFamily = newsreader,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Maroon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dish.restaurantName,
                        fontFamily = jakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = TextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Location city row
                    if (dish.restaurantCity.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Maroon,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = dish.restaurantCity,
                                fontFamily = jakartaSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = TextGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Uploader info row
                if (dish.uploaderName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaroonLight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!dish.uploaderProfileUrl.isNullOrBlank()) {
                                NetworkImage(
                                    imageUrl = dish.uploaderProfileUrl,
                                    contentDescription = dish.uploaderName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = dish.uploaderName.take(1).uppercase(),
                                    fontFamily = jakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "by ${dish.uploaderName}",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = TextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─── Nearby Restaurant Card (full-width, 256dp image, Figma style) ────────────
@Composable
private fun NearbyRestaurantCard(
    restaurant: Restaurant,
    distanceText: String,
    jakartaSans: androidx.compose.ui.text.font.FontFamily,
    newsreader: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(CardBg)
            .clickable { onClick() }
    ) {
        Column {
            // Image 256dp
            Box(modifier = Modifier.fillMaxWidth().height(256.dp)) {
                NetworkImage(
                    imageUrl = restaurant.photoUrl ?: restaurant.imageUrls.firstOrNull() ?: "",
                    contentDescription = restaurant.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Distance pill top-left
                if (distanceText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(CreamAlpha)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Maroon,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = distanceText,
                                fontFamily = jakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Maroon
                            )
                        }
                    }
                }
                // Bookmark button top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CreamAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = Maroon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Info area
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = restaurant.name,
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.6).sp,
                            color = Maroon
                        )
                        val priceHint = if (restaurant.cuisine.isNotBlank()) restaurant.cuisine else "Restaurant"
                        Text(
                            text = priceHint,
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = TextBlack
                        )
                    }
                    if (restaurant.averageRating > 0f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaroonAlpha)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Maroon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "%.1f".format(restaurant.averageRating),
                                    fontFamily = jakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Maroon
                                )
                            }
                        }
                    }
                }

                if (!restaurant.tagline.isNullOrBlank()) {
                    Text(
                        text = restaurant.tagline,
                        fontFamily = newsreader,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = TextBlack,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Maroon)
                            .clickable { onClick() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "View Details",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFF6F6F6)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1E3E3))
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint = TextBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Map Banner ───────────────────────────────────────────────────────────────
@Composable
private fun MapBanner(
    count: Int,
    jakartaSans: androidx.compose.ui.text.font.FontFamily,
    onExploreClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MapDark)
            .clickable { onExploreClick() },
        contentAlignment = Alignment.Center
    ) {
        // Blurred dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$count restaurants near you",
                    fontFamily = jakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = "READY TO EXPLORE?",
                    fontFamily = jakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Maroon, Color(0xFFFF7669))
                        )
                    )
                    .clickable { onExploreClick() }
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Explore Map",
                    fontFamily = jakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ─── Ranking Row (Top Restaurants This Week) ──────────────────────────────────
@Composable
private fun RankingRow(
    restaurant: Restaurant,
    distanceText: String,
    jakartaSans: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 80dp image
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            NetworkImage(
                imageUrl = restaurant.photoUrl ?: restaurant.imageUrls.firstOrNull() ?: "",
                contentDescription = restaurant.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = restaurant.name,
                    fontFamily = jakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Maroon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (distanceText.isNotBlank()) {
                    Text(
                        text = distanceText,
                        fontFamily = jakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextBlack
                    )
                }
            }

            if (restaurant.averageRating > 0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "%.1f".format(restaurant.averageRating),
                        fontFamily = jakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextBlack
                    )
                    if (restaurant.reviewCount > 0) {
                        Text(
                            text = "(${restaurant.reviewCount} ratings)",
                            fontFamily = jakartaSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = TextBlack
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (restaurant.cuisine.isNotBlank()) {
                    CuisineChip(label = restaurant.cuisine, jakartaSans = jakartaSans)
                }
            }
        }
    }
}

// ─── Top Navigation Bar (SmackCheck logo + bell + avatar) ────────────────────
@Composable
private fun TopNavBar(
    userProfilePhotoUrl: String?,
    jakartaSans: androidx.compose.ui.text.font.FontFamily,
    hasUnreadNotifications: Boolean,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCCFFFFFF))  // rgba(255,255,255,0.80) frosted look
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmackCheckWordmark(
            fontFamily = jakartaSans,
            fontSize = 24.sp,
            letterSpacing = (-1.2).sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification bell with red dot
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onNotificationsClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = TextBlack,
                    modifier = Modifier.size(20.dp)
                )
                // Red notification dot — only when unread notifications exist
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Maroon)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            // Profile avatar circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, Maroon, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            ) {
                if (!userProfilePhotoUrl.isNullOrBlank()) {
                    NetworkImage(
                        imageUrl = userProfilePhotoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaroonLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CuisineChip(
    label: String,
    jakartaSans: androidx.compose.ui.text.font.FontFamily
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaroonAlpha)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = jakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Maroon
        )
    }
}
