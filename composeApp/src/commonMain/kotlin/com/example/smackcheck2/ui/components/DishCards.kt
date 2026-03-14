package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.smackcheck2.ui.theme.appColors

/**
 * Featured Dish Card - Square with image, badges, and info overlay
 */
@Composable
fun FeaturedDishCard(
    dishName: String,
    restaurantName: String,
    rating: Float,
    calories: Int,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors().CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Image with overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                // Real dish image from network
                DishImage(
                    dishName = dishName,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Calorie badge - bottom left
                CalorieBadge(
                    calories = calories,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
                
                // Favorite button - top right
                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    size = 20.dp
                )
            }
            
            // Info section
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dishName,
                        color = appColors().TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    RatingBadge(rating = rating, showBackground = false)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = restaurantName,
                    color = appColors().TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Large Horizontal Dish Card with Chef info
 */
@Composable
fun LargeDishCard(
    dishName: String,
    restaurantName: String,
    rating: Float,
    reviewCount: Int,
    calories: Int,
    isBestseller: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors().CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left side - Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = dishName,
                    color = appColors().TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = restaurantName,
                    color = appColors().TextSecondary,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = appColors().StarYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$rating",
                            color = appColors().TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "$reviewCount ratings",
                        color = appColors().TextSecondary,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Try This Out",
                        color = appColors().Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " >",
                        color = appColors().Primary,
                        fontSize = 13.sp
                    )
                }
            }
            
            // Right side - Image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                DishImage(
                    dishName = dishName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                )
                
                // Bestseller badge
                if (isBestseller) {
                    BestsellerBadge(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Restaurant Card for All Restaurants section
 */
@Composable
fun RestaurantCardDark(
    restaurantId: String = "",
    restaurantName: String,
    cuisine: String,
    rating: Float,
    reviewCount: Int,
    deliveryTime: String,
    googlePlaceId: String? = null,
    city: String = "",
    photoViewModel: com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel? = null,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoStatesMap = photoViewModel?.photoStates?.collectAsState()
    val photoState = photoStatesMap?.value?.get(restaurantId)

    if (photoViewModel != null && restaurantId.isNotEmpty()) {
        LaunchedEffect(restaurantId) {
            photoViewModel.loadThumbnail(
                restaurantId = restaurantId,
                placeId = googlePlaceId,
                name = restaurantName,
                city = city
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors().CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                if (photoViewModel != null) {
                    SmartRestaurantImage(
                        photoState = photoState,
                        restaurantName = restaurantName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    RestaurantImage(
                        restaurantName = restaurantName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Favorite button
                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    size = 24.dp
                )
            }
            
            // Info
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = restaurantName,
                    color = appColors().TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = cuisine,
                    color = appColors().TextSecondary,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Only show rating if there are SmackCheck reviews
                    if (reviewCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = appColors().StarYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$rating ($reviewCount)",
                                color = appColors().TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Empty spacer to maintain layout when no rating
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = deliveryTime,
                        color = appColors().TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Dish Detail List Item (for "More Dishes" section)
 */
@Composable
fun DishListItem(
    dishName: String,
    rating: Float,
    reviewCount: Int,
    description: String,
    price: String,
    calories: Int,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = dishName,
                color = appColors().TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = appColors().StarYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$rating",
                        color = appColors().TextPrimary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "$reviewCount ratings",
                    color = appColors().TextSecondary,
                    fontSize = 11.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                color = appColors().TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = price,
                color = appColors().TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Image and Add button
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Calorie badge
            Text(
                text = "$calories Kcal",
                color = appColors().Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Image placeholder
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(appColors().SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = appColors().TextTertiary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AddButton(onClick = onAddClick)
        }
    }
}
