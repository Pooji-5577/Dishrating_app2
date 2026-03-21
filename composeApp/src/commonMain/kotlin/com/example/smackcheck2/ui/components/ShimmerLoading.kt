package com.example.smackcheck2.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.appColors

/**
 * Shimmer effect brush for skeleton loading
 */
@Composable
fun shimmerBrush(): Brush {
    val colors = appColors()
    val shimmerColors = listOf(
        colors.Surface,
        colors.Surface.copy(alpha = 0.5f),
        colors.Surface
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

/**
 * A single shimmer box placeholder
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * Skeleton for a restaurant card in the home screen
 */
@Composable
fun RestaurantCardSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                // Restaurant name
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Cuisine type
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Rating row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Skeleton for a featured dish card (horizontal scroll item)
 */
@Composable
fun FeaturedDishSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    Card(
        modifier = modifier.width(160.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            Column(modifier = Modifier.padding(10.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                )
            }
        }
    }
}

/**
 * Skeleton for a social feed card
 */
@Composable
fun SocialFeedCardSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User profile row
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    ShimmerBox(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dish image placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dish name + rating row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .padding(end = 16.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Actions row
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(20.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(20.dp)
                )
            }
        }
    }
}

/**
 * Full skeleton for the Home Screen (DarkHomeScreen layout)
 */
@Composable
fun HomeScreenSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.Background),
        contentPadding = PaddingValues(bottom = 16.dp),
        userScrollEnabled = false
    ) {
        // Search bar skeleton
        item {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Category tabs skeleton
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(36.dp),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // Section header skeleton
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(140.dp)
                        .height(20.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                )
            }
        }

        // Featured dishes horizontal scroll skeleton
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(3) {
                    FeaturedDishSkeleton()
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // "All Restaurants" header skeleton
        item {
            ShimmerBox(
                modifier = Modifier
                    .width(150.dp)
                    .height(20.dp)
                    .padding(start = 16.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Nearby restaurants banner skeleton
        item {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Filter chips skeleton
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(90.dp)
                            .height(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // Restaurant card skeletons
        items(3) {
            RestaurantCardSkeleton(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Full skeleton for the Social Feed Screen
 */
@Composable
fun SocialFeedSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(4) {
            SocialFeedCardSkeleton()
        }
    }
}
