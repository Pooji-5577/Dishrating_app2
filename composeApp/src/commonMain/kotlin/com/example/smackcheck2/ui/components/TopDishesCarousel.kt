package com.example.smackcheck2.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.ui.theme.NewsreaderFontFamily
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.util.formatLikeCountCompact
import com.example.smackcheck2.util.formatOneDecimal
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun TopDishesCarousel(
    dishes: List<FeedItem>,
    onDishClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    val jakartaSans = PlusJakartaSans()
    val newsreader = NewsreaderFontFamily()

    Column(modifier = modifier) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top Dishes Today",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = jakartaSans,
                color = Color.Black
            )
            Text(
                text = "See all",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = jakartaSans,
                color = Color(0xFF642223),
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(dishes, key = { it.id }) { dish ->
                TopDishCard(
                    dish = dish,
                    onClick = { onDishClick(dish.id) },
                    jakartaSans = jakartaSans,
                    newsreader = newsreader
                )
            }
        }
    }
}

@Composable
private fun TopDishCard(
    dish: FeedItem,
    onClick: () -> Unit,
    jakartaSans: androidx.compose.ui.text.font.FontFamily,
    newsreader: androidx.compose.ui.text.font.FontFamily,
    modifier: Modifier = Modifier
) {
    val colors = appColors()

    Surface(
        modifier = modifier
            .width(280.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column {
            // Dish image with rating overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                if (dish.dishImageUrl != null) {
                    KamelImage(
                        resource = asyncPainterResource(dish.dishImageUrl),
                        contentDescription = dish.dishName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onFailure = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colors.SurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = colors.TextTertiary
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colors.TextTertiary
                        )
                    }
                }

                // Rating badge overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFFE4E2DF).copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFF642223)
                        )
                        Text(
                            text = formatOneDecimal(dish.rating.toDouble()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = jakartaSans,
                            color = Color(0xFF642223)
                        )
                    }
                }
            }

            // Info section
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dish name
                Text(
                    text = dish.dishName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = newsreader,
                    color = Color(0xFF642223),
                    maxLines = 1
                )

                // Restaurant + city
                Text(
                    text = dish.restaurantName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = jakartaSans,
                    color = Color.Black,
                    maxLines = 1
                )
                if (dish.restaurantCity.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = Color(0xFF642223)
                        )
                        Text(
                            text = dish.restaurantCity,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = jakartaSans,
                            color = Color(0xFF5C5B5B),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Likes and comments
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF642223)
                        )
                        Text(
                            text = formatLikeCount(dish.likesCount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = jakartaSans,
                            color = Color(0xFF642223)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF642223)
                        )
                        Text(
                            text = "${dish.commentsCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = jakartaSans,
                            color = Color(0xFF642223)
                        )
                    }
                }
            }
        }
    }
}

private fun formatLikeCount(count: Int): String {
    return formatLikeCountCompact(count)
}
