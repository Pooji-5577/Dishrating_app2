package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Dish
import com.example.smackcheck2.ui.theme.appColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun TopRatedDishCard(
    dish: Dish,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$"
) {
    val colors = appColors()
    // Dark maroon matching Figma #642223
    val maroon = Color(0xFF642223)
    // Blurred badge background matching Figma rgba(228,226,223,0.9)
    val badgeBg = Color(0xE6E4E2DF)

    Column(
        modifier = modifier
            .width(256.dp)
            .clickable { onClick() }
    ) {
        // Image card with overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(288.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(colors.SurfaceVariant)
        ) {
            if (!dish.imageUrl.isNullOrEmpty()) {
                KamelImage(
                    resource = asyncPainterResource(dish.imageUrl!!),
                    contentDescription = dish.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onFailure = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = colors.TextTertiary
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = colors.TextTertiary
                    )
                }
            }

            // + / bookmark button — top right circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(maroon)
                    .clickable { onBookmarkClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Bookmark",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Rating badge — bottom left, cream blurred-look background
            if (dish.rating > 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = maroon
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", dish.rating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = maroon
                        )
                    }
                }
            }
        }

        // Dish name — serif maroon
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = dish.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = maroon,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Price — Plus Jakarta Sans 16sp Bold Black per Figma spec
        if (dish.price != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currencySymbol${String.format("%.0f", dish.price)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
