package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.util.formatOneDecimal
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun RestaurantHeroSection(
    restaurant: Restaurant,
    distance: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    val heroImageUrl = restaurant.photoUrl
        ?: restaurant.imageUrls.firstOrNull()

    // Dark maroon matching Figma #642223
    val maroon = Color(0xFF642223)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(397.dp)
    ) {
        // Background image
        if (heroImageUrl != null) {
            KamelImage(
                resource = asyncPainterResource(heroImageUrl),
                contentDescription = restaurant.name,
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
                            modifier = Modifier.size(64.dp),
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
                    modifier = Modifier.size(64.dp),
                    tint = colors.TextTertiary
                )
            }
        }

        // Gradient scrim: fades from transparent at top-half to page background at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            1.0f to colors.Background
                        )
                    )
                )
        )

        // Content positioned at bottom-start over the fade
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            // Rating / Premium Choice badge
            if (restaurant.averageRating > 0f) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = maroon
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (restaurant.averageRating >= 4.5f)
                                "${String.format("%.1f", restaurant.averageRating)} Premium Choice"
                            else
                                String.format("%.1f", restaurant.averageRating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Restaurant name — large serif style
            Text(
                text = restaurant.name,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.TextPrimary,
                lineHeight = 40.sp
            )

            // Tagline
            if (!restaurant.tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = restaurant.tagline!!,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    color = colors.TextPrimary.copy(alpha = 0.75f),
                    lineHeight = 25.sp
                )
            }
        }
    }
}
