package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.DishDetailViewModel

/**
 * Dish Detail Screen - displays real dish data loaded from Supabase
 *
 * @param viewModel DishDetailViewModel that loads dish info, reviews, and related dishes
 * @param dishId The ID of the dish to display
 * @param onBackClick Navigate back
 * @param onRelatedDishClick Navigate to another dish
 */
@Composable
fun DishDetailScreen(
    viewModel: DishDetailViewModel,
    dishId: String,
    onBackClick: () -> Unit,
    onRelatedDishClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dishId) {
        viewModel.loadDish(dishId)
    }

    Scaffold(
        containerColor = appColors().Background,
        modifier = modifier
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = appColors().Primary)
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            color = appColors().TextSecondary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry(dishId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.dish != null -> {
                val dish = uiState.dish!!
                val restaurant = uiState.restaurant

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Hero Image with overlays
                    item {
                        val heroImageUrl = uiState.featuredReview?.dishImageUrl
                            ?: dish.imageUrl

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            // Actual dish image
                            if (!heroImageUrl.isNullOrBlank()) {
                                NetworkImage(
                                    imageUrl = heroImageUrl,
                                    contentDescription = dish.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Gradient overlay for readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.25f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.55f)
                                                )
                                            )
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(appColors().SurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = appColors().TextTertiary
                                    )
                                }
                            }

                            // Top bar with back, favorite, share
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (uiState.isFavorite) Color(0xFFFF6B6B) else Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = { /* Share */ },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // Uploader info chip at the bottom of the image
                            val featured = uiState.featuredReview
                            if (featured != null) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF642223)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!featured.userProfileUrl.isNullOrBlank()) {
                                            NetworkImage(
                                                imageUrl = featured.userProfileUrl,
                                                contentDescription = featured.userName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.Black.copy(alpha = 0.55f)
                                    ) {
                                        Text(
                                            text = "Posted by ${featured.userName}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dish Info Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Name
                            Text(
                                text = dish.name,
                                color = appColors().TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Rating row with location-specific count
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = appColors().StarYellow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (dish.rating > 0f) formatRatingValue(dish.rating) else "No ratings",
                                        color = appColors().TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (uiState.reviews.isNotEmpty()) {
                                    Text(text = "·", color = appColors().TextSecondary)
                                    Text(
                                        text = "${uiState.reviews.size} ${if (uiState.reviews.size == 1) "rating" else "ratings"} in this area",
                                        color = appColors().TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Location info row
                            val locationLine = buildString {
                                if (restaurant != null) {
                                    append(restaurant.name)
                                    if (restaurant.city.isNotBlank()) append(" · ${restaurant.city}")
                                } else if (dish.restaurantName.isNotBlank()) {
                                    append(dish.restaurantName)
                                    if (dish.restaurantCity.isNotBlank()) append(" · ${dish.restaurantCity}")
                                }
                            }
                            if (locationLine.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF642223),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = locationLine,
                                        color = appColors().TextSecondary,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Restaurant Card (if available)
                    if (restaurant != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = appColors().CardBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Restaurant avatar
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(appColors().SurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = restaurant.name.firstOrNull()?.toString() ?: "R",
                                                color = appColors().Primary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = restaurant.name,
                                                color = appColors().TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (restaurant.cuisine.isNotBlank()) {
                                                Text(
                                                    text = restaurant.cuisine,
                                                    color = appColors().TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }

                                    // Restaurant rating
                                    if (restaurant.averageRating > 0f) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = appColors().StarYellow,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = formatRatingValue(restaurant.averageRating),
                                                color = appColors().TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Reviews Section
                    if (uiState.reviews.isNotEmpty()) {
                        item {
                            Text(
                                text = "Reviews",
                                color = appColors().TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(uiState.reviews) { review ->
                            ReviewItem(review = review)
                            HorizontalDivider(
                                color = appColors().Surface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else if (!uiState.isLoading) {
                        item {
                            Text(
                                text = "No reviews yet. Be the first to rate this dish!",
                                color = appColors().TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Related Dishes Section
                    if (uiState.relatedDishes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "More from ${restaurant?.name ?: "this restaurant"}",
                                color = appColors().TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(uiState.relatedDishes) { relatedDish ->
                            RelatedDishItem(
                                dish = relatedDish,
                                onClick = { onRelatedDishClick(relatedDish.id) }
                            )
                            HorizontalDivider(
                                color = appColors().Surface,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

/**
 * Single review item in the reviews list
 */
@Composable
private fun ReviewItem(review: Review) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(appColors().SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!review.userProfileUrl.isNullOrBlank()) {
                NetworkImage(
                    imageUrl = review.userProfileUrl,
                    contentDescription = review.userName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = review.userName.firstOrNull()?.toString() ?: "?",
                    color = appColors().Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    color = appColors().TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Star rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = appColors().StarYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatRatingValue(review.rating),
                        color = appColors().TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = review.comment,
                    color = appColors().TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Single related dish item
 */
@Composable
private fun RelatedDishItem(
    dish: Dish,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dish image placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(appColors().SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = appColors().TextTertiary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dish.name,
                color = appColors().TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (dish.rating > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = appColors().StarYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatRatingValue(dish.rating),
                        color = appColors().TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Format a rating value to one decimal place (Kotlin/Native compatible)
 */
private fun formatRatingValue(value: Float): String {
    val intPart = value.toInt()
    val decimalPart = ((value - intPart) * 10).toInt()
    return "$intPart.$decimalPart"
}
