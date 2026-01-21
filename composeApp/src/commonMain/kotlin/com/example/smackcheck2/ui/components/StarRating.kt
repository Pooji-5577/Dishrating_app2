package com.example.smackcheck2.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.ui.theme.StarColor
import com.example.smackcheck2.ui.theme.StarColorEmpty

/**
 * Reusable Star Rating composable
 * 
 * @param rating Current rating value (0-5)
 * @param onRatingChange Callback when rating changes
 * @param modifier Modifier for the row
 * @param starSize Size of each star
 * @param starColor Color of filled/half stars
 * @param emptyStarColor Color of empty stars
 * @param isEditable Whether the rating can be changed
 * @param allowHalfRating Whether half ratings are allowed
 */
@Composable
fun StarRating(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 32.dp,
    starColor: Color = StarColor,
    emptyStarColor: Color = StarColorEmpty,
    isEditable: Boolean = true,
    allowHalfRating: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (starIndex in 1..5) {
            val starValue = starIndex.toFloat()
            
            val icon = when {
                rating >= starValue -> Icons.Filled.Star
                rating >= starValue - 0.5f && allowHalfRating -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            
            val tint = when {
                rating >= starValue - 0.5f -> starColor
                else -> emptyStarColor
            }
            
            Icon(
                imageVector = icon,
                contentDescription = "Star $starIndex",
                tint = tint,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (isEditable) {
                            Modifier.clickable {
                                val newRating = if (allowHalfRating) {
                                    // Toggle between half and full star
                                    when {
                                        rating == starValue -> starValue - 0.5f
                                        rating == starValue - 0.5f -> starValue
                                        else -> starValue
                                    }
                                } else {
                                    starValue
                                }
                                onRatingChange(newRating)
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

/**
 * Read-only star rating display
 */
@Composable
fun StarRatingDisplay(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
    starColor: Color = StarColor,
    emptyStarColor: Color = StarColorEmpty
) {
    StarRating(
        rating = rating,
        onRatingChange = {},
        modifier = modifier,
        starSize = starSize,
        starColor = starColor,
        emptyStarColor = emptyStarColor,
        isEditable = false,
        allowHalfRating = true
    )
}
