package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors

/**
 * Location Header with dropdown
 */
@Composable
fun LocationHeader(
    locationType: String,
    address: String,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onLocationClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(appColors().Primary)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = locationType,
                    color = appColors().TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = appColors().TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = address,
                color = appColors().TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Dark Search Bar
 */
@Composable
fun DarkSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search \"biryani\"",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(appColors().SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = appColors().TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = appColors().TextPrimary,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(appColors().Primary),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = appColors().TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Voice search",
            tint = appColors().Primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Category Tab Chip
 */
@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isSpecial -> appColors().Primary
                    isSelected -> appColors().SurfaceVariant
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (!isSelected && !isSpecial) 1.dp else 0.dp,
                color = if (!isSelected && !isSpecial) appColors().Divider else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSpecial || isSelected) appColors().TextPrimary else appColors().TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Calorie Badge
 */
@Composable
fun CalorieBadge(
    calories: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(appColors().CalorieBadge)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$calories Kcal",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Bestseller Badge
 */
@Composable
fun BestsellerBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(appColors().BestsellerBadge)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "BESTSELLER",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Rating Badge with star
 */
@Composable
fun RatingBadge(
    rating: Float,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true
) {
    Row(
        modifier = modifier
            .then(
                if (showBackground) {
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(appColors().SurfaceVariant.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = appColors().StarYellow,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = rating.toString(),
            color = appColors().TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Favorite Heart Button
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(size + 8.dp)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) appColors().Primary else appColors().TextPrimary,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Filter Chip for restaurant filters
 */
@Composable
fun FilterChipDark(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) appColors().Primary else appColors().SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingIcon?.invoke()
        Text(
            text = text,
            color = appColors().TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Price Tag
 */
@Composable
fun PriceTag(
    price: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 18
) {
    Text(
        text = price,
        color = appColors().TextPrimary,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * Add Button (Red)
 */
@Composable
fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Add"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(appColors().Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Available Badge
 */
@Composable
fun AvailableBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(appColors().AvailableBadge)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "AVAILABLE",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
