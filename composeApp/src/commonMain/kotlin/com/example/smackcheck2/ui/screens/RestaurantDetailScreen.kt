package com.example.smackcheck2.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.ui.components.BottomNavBar
import com.example.smackcheck2.ui.components.LoadingState
import com.example.smackcheck2.ui.components.NavItem
import com.example.smackcheck2.util.formatOneDecimal
import com.example.smackcheck2.util.formatRelativeTime
import com.example.smackcheck2.viewmodel.RestaurantDetailViewModel
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlin.math.roundToInt

private val DetailBackground = Color(0xFFF6F6F6)
private val DetailPrimary = Color(0xFF642223)
private val DetailPrimaryLight = Color(0xFFA95858)
private val DetailText = Color(0xFF151515)
private val DetailMuted = Color(0xFF6F6F6F)

@Composable
fun RestaurantDetailScreen(
    viewModel: RestaurantDetailViewModel,
    photoViewModel: RestaurantPhotoViewModel? = null,
    restaurantId: String,
    onNavigateBack: () -> Unit,
    onNavItemClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(restaurantId) {
        viewModel.loadRestaurant(restaurantId)
    }

    when {
        uiState.isLoading -> LoadingState(message = "Loading restaurant...")
        uiState.errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DetailBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage ?: "Unable to load restaurant",
                        color = DetailText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.retry(restaurantId) },
                        colors = ButtonDefaults.buttonColors(containerColor = DetailPrimary)
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

            Scaffold(
                containerColor = DetailBackground,
                topBar = {
                    DetailTopBar(onNavigateBack = onNavigateBack)
                },
                bottomBar = {
                    BottomNavBar(
                        selectedItem = NavItem.MAP,
                        onHomeClick = { onNavItemClick(0) },
                        onMapClick = { onNavItemClick(1) },
                        onCameraClick = { onNavItemClick(2) },
                        onExploreClick = { onNavItemClick(3) },
                        onProfileClick = { onNavItemClick(4) },
                        modifier = Modifier.background(DetailBackground)
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(DetailBackground),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item("hero") {
                        RestaurantHero(
                            restaurant = restaurant,
                            imageUrl = heroImage
                        )
                    }

                    item("chips") {
                        DetailChipRow(restaurant = restaurant)
                    }

                    item("top-dishes-title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Rated Dishes",
                                color = DetailText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "VIEW ALL",
                                color = DetailPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (uiState.topDishes.isNotEmpty()) {
                        item("top-dishes-list") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.topDishes, key = { it.id }) { dish ->
                                    FeaturedDishCard(
                                        dish = dish,
                                        estimatedPrice = uiState.reviews.firstOrNull { it.dishId == dish.id }?.price
                                    )
                                }
                            }
                        }
                    }

                    item("recent-title") {
                        Text(
                            text = "Recent Reviews",
                            color = DetailText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                        )
                    }

                    if (uiState.reviews.isEmpty()) {
                        item("no-reviews") {
                            Text(
                                text = "No reviews yet.",
                                color = DetailMuted,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        items(uiState.reviews.take(4), key = { it.id }) { review ->
                            DetailReviewRow(
                                review = review,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DetailPrimary
            )
        }
        Text(
            text = "SmackCheck",
            color = DetailText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Filled.NotificationsNone,
                contentDescription = "Notifications",
                tint = DetailText
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D2D2D)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RestaurantHero(
    restaurant: Restaurant,
    imageUrl: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        if (imageUrl != null) {
            KamelImage(
                resource = asyncPainterResource(imageUrl),
                contentDescription = restaurant.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE9E4E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = Color(0xFF9B9B9B),
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Transparent,
                            DetailBackground
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (restaurant.averageRating >= 4.5f) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(DetailPrimary)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatOneDecimal(restaurant.averageRating)} Premium Choice",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = restaurant.name,
                fontFamily = FontFamily.Serif,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            if (!restaurant.tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = restaurant.tagline,
                    color = Color.Black.copy(alpha = 0.88f),
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun DetailChipRow(restaurant: Restaurant) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (restaurant.cuisine.isNotBlank()) {
                InfoChip(text = restaurant.cuisine)
            }
            if (restaurant.isOpenNow == true) {
                InfoChip(text = "Open Now", showDot = true)
            }
        }
        if (restaurant.city.isNotBlank()) {
            InfoChip(text = "${restaurant.city} • Nearby", leadingPin = true)
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    showDot: Boolean = false,
    leadingPin: Boolean = false
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(DetailPrimaryLight)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot || leadingPin) {
            Text(
                text = if (leadingPin) "◦" else "•",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun FeaturedDishCard(
    dish: Dish,
    estimatedPrice: Double?
) {
    Card(
        modifier = Modifier.width(230.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFDFDFDF))
        ) {
            if (dish.imageUrl != null) {
                KamelImage(
                    resource = asyncPainterResource(dish.imageUrl),
                    contentDescription = dish.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = Color(0xFF9B9B9B),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DetailPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1E8E8))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = DetailPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatOneDecimal(dish.rating.toDouble()),
                    color = DetailPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dish.name,
            color = DetailPrimary,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatPrice(estimatedPrice),
            color = DetailText,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun DetailReviewRow(
    review: Review,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            if (review.userProfileUrl != null) {
                KamelImage(
                    resource = asyncPainterResource(review.userProfileUrl),
                    contentDescription = review.userName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = review.userName.firstOrNull()?.uppercase() ?: "?",
                    color = DetailPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    color = DetailText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = formatRelativeTime(review.createdAt),
                    color = DetailMuted,
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (index < review.rating.roundToInt()) DetailPrimary else Color(0xFFD0C7C7),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (review.comment.isNotBlank()) {
                Text(
                    text = "\"${review.comment}\"",
                    color = Color(0xFF2D2D2D),
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatPrice(price: Double?): String {
    if (price == null || price <= 0.0) return "Price unavailable"
    val rounded = price.roundToInt()
    return "$$rounded"
}
