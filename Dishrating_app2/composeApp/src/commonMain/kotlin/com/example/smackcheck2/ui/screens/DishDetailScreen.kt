package com.example.smackcheck2.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.components.AddButton
import com.example.smackcheck2.ui.components.AvailableBadge
import com.example.smackcheck2.ui.components.BestsellerBadge
import com.example.smackcheck2.ui.components.DishListItem
import com.example.smackcheck2.ui.components.FavoriteButton
import com.example.smackcheck2.ui.components.PriceTag
import com.example.smackcheck2.ui.theme.appColors

@Composable
fun DishDetailScreen(
    dishId: String,
    onBackClick: () -> Unit,
    onAddToCart: () -> Unit,
    onRelatedDishClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Sample dish data - in real app, fetch from ViewModel
    val dish = remember {
        DishDetailInfo(
            id = dishId,
            name = "Truffle Mushroom Risotto",
            restaurant = "La Cucina Italiana",
            chefName = "Chef Marco Rossi",
            price = "$24.99",
            rating = 4.7f,
            reviewCount = 856,
            calories = 553,
            description = "Creamy Arborio rice slow-cooked to perfection with wild mushrooms, black truffle oil, and aged Parmesan cheese. Garnished with fresh herbs and truffle shavings.",
            isBestseller = true
        )
    }
    
    val relatedDishes = remember {
        listOf(
            RelatedDish("1", "Pesto Pasta", 4.5f, 180, "Fresh basil pesto with pine nuts", "$18.99", 420),
            RelatedDish("2", "Carbonara", 4.6f, 220, "Classic Roman style with pancetta", "$19.99", 580),
            RelatedDish("3", "Margherita Pizza", 4.4f, 340, "Wood-fired with fresh mozzarella", "$16.99", 680)
        )
    }
    
    var isFavorite by remember { mutableStateOf(false) }
    var isCaloriesExpanded by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = appColors().Background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hero Image with overlays
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // Image placeholder with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        appColors().SurfaceVariant,
                                        appColors().Background
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = appColors().TextTertiary
                        )
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
                                    appColors().Surface.copy(alpha = 0.8f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = appColors().TextPrimary
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { isFavorite = !isFavorite },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        appColors().Surface.copy(alpha = 0.8f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) appColors().Primary else appColors().TextPrimary
                                )
                            }
                            
                            IconButton(
                                onClick = { /* Share */ },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        appColors().Surface.copy(alpha = 0.8f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = appColors().TextPrimary
                                )
                            }
                        }
                    }
                    
                    // Bestseller badge at bottom
                    if (dish.isBestseller) {
                        BestsellerBadge(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
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
                    // Price
                    Text(
                        text = dish.price,
                        color = appColors().TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Name
                    Text(
                        text = dish.name,
                        color = appColors().TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Rating row
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
                                text = "${dish.rating}",
                                color = appColors().TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Text(
                            text = "•",
                            color = appColors().TextSecondary
                        )
                        
                        Text(
                            text = "${dish.reviewCount} ratings",
                            color = appColors().TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Description
                    Text(
                        text = dish.description,
                        color = appColors().TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    
                    Text(
                        text = "read more",
                        color = appColors().Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { /* Expand */ }
                    )
                }
            }
            
            // Calories Expandable Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { isCaloriesExpanded = !isCaloriesExpanded },
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
                        Column {
                            Text(
                                text = "Calorie Details",
                                color = appColors().TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${dish.calories} Kcal per serving",
                                color = appColors().TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = appColors().TextSecondary
                        )
                    }
                    
                    if (isCaloriesExpanded) {
                        HorizontalDivider(color = appColors().Surface)
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CalorieRow("Carbohydrates", "45g")
                            CalorieRow("Protein", "12g")
                            CalorieRow("Fat", "28g")
                            CalorieRow("Fiber", "3g")
                            CalorieRow("Sodium", "680mg")
                        }
                    }
                }
            }
            
            // Chef/Restaurant Card
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
                            // Chef avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(appColors().SurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dish.chefName.first().toString(),
                                    color = appColors().Primary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Column {
                                Text(
                                    text = dish.chefName,
                                    color = appColors().TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = dish.restaurant,
                                    color = appColors().TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        
                        Button(
                            onClick = onAddToCart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = appColors().Primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Add",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // More Dishes Section
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "More Dishes by ${dish.restaurant}",
                        color = appColors().TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AvailableBadge()
                }
            }
            
            // Related Dishes List
            items(relatedDishes) { relatedDish ->
                DishListItem(
                    dishName = relatedDish.name,
                    rating = relatedDish.rating,
                    reviewCount = relatedDish.reviewCount,
                    description = relatedDish.description,
                    price = relatedDish.price,
                    calories = relatedDish.calories,
                    onAddClick = { /* Add to cart */ },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                HorizontalDivider(
                    color = appColors().Surface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CalorieRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = appColors().TextSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = appColors().TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Data classes
private data class DishDetailInfo(
    val id: String,
    val name: String,
    val restaurant: String,
    val chefName: String,
    val price: String,
    val rating: Float,
    val reviewCount: Int,
    val calories: Int,
    val description: String,
    val isBestseller: Boolean
)

private data class RelatedDish(
    val id: String,
    val name: String,
    val rating: Float,
    val reviewCount: Int,
    val description: String,
    val price: String,
    val calories: Int
)
