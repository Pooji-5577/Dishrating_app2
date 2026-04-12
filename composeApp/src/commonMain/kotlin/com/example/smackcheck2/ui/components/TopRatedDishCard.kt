package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.ui.theme.StarColor
import com.example.smackcheck2.ui.theme.appColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun TopRatedDishCard(
    dish: Dish,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors()

    Card(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Photo with overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (dish.imageUrl != null) {
                    KamelImage(
                        resource = asyncPainterResource(dish.imageUrl!!),
                        contentDescription = dish.name,
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
                                    modifier = Modifier.size(32.dp),
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
                            modifier = Modifier.size(32.dp),
                            tint = colors.TextTertiary
                        )
                    }
                }

                // Bookmark button top-right
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) colors.Primary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Rating chip bottom-left
                if (dish.rating > 0f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = StarColor
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", dish.rating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Dish name and price
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dish.restaurantName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
