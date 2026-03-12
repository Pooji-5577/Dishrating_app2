package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.components.CategoryChip
import com.example.smackcheck2.ui.components.DarkSearchBar
import com.example.smackcheck2.ui.components.FavoriteButton
import com.example.smackcheck2.ui.components.FeaturedDishCard
import com.example.smackcheck2.ui.components.FilterChipDark
import com.example.smackcheck2.ui.components.LargeDishCard
import com.example.smackcheck2.ui.components.LocationHeader
import com.example.smackcheck2.ui.components.RestaurantCardDark
import com.example.smackcheck2.ui.theme.appColors

data class NavItem(
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
)

@Composable
fun DarkHomeScreen(
    currentLocation: String,
    allRestaurants: List<com.example.smackcheck2.model.Restaurant> = emptyList(),
    allDishes: List<com.example.smackcheck2.model.Dish> = emptyList(),
    noRestaurantsFound: Boolean = false,
    onLocationClick: () -> Unit,
    onDishClick: (String) -> Unit,
    onRestaurantClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMapClick: () -> Unit = {},
    onProfileClick: () -> Unit,
    onGameClick: () -> Unit,
    onCameraClick: () -> Unit = {},
    onTopDishesClick: () -> Unit = {},
    onTopRestaurantsClick: () -> Unit = {},
    onNearbyRestaurantsClick: () -> Unit = {},
    onSocialFeedClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddRestaurantClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeColors = appColors()
    
    var selectedNavItem by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("Healthy") }
    var searchQuery by remember { mutableStateOf("") }
    
    val categories = listOf("Healthy", "Gourmet", "Chef's Special", "Quick Bites", "Desserts")
    val filters = listOf("Great Offers", "Nearest", "Rating 4.0+", "Pure Veg")
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }

    // Convert real data to UI models with category filtering
    val featuredDishes = remember(allDishes, selectedCategory) {
        allDishes
            .filter { dish ->
                // Filter by category using name and comment
                val searchText = "${dish.name} ${dish.comment}".lowercase()
                when (selectedCategory) {
                    "Healthy" -> searchText.contains("healthy") || searchText.contains("salad") ||
                                searchText.contains("grilled") || searchText.contains("steamed")
                    "Gourmet" -> searchText.contains("gourmet") || searchText.contains("premium") ||
                                searchText.contains("fine") || dish.rating >= 4.5f
                    "Chef's Special" -> searchText.contains("special") || searchText.contains("signature") ||
                                       searchText.contains("chef") || dish.rating >= 4.5f
                    "Quick Bites" -> searchText.contains("quick") || searchText.contains("snack") ||
                                    searchText.contains("bite") || searchText.contains("appetizer")
                    "Desserts" -> searchText.contains("dessert") || searchText.contains("sweet") ||
                                 searchText.contains("cake") || searchText.contains("ice cream") ||
                                 searchText.contains("pastry")
                    else -> true
                }
            }
            .sortedByDescending { it.rating }
            .take(10)
            .map { dish ->
                DishInfo(
                    id = dish.id,
                    name = dish.name,
                    restaurant = dish.restaurantName,
                    rating = dish.rating,
                    calories = 0 // Default calories, not in model
                )
            }
    }

    val tryThisOut = remember<TryDishInfo?>(allDishes, selectedCategory) {
        allDishes
            .filter { dish ->
                val searchText = "${dish.name} ${dish.comment}".lowercase()
                when (selectedCategory) {
                    "Healthy" -> searchText.contains("healthy") || searchText.contains("salad") ||
                                searchText.contains("grilled") || searchText.contains("steamed")
                    "Gourmet" -> searchText.contains("gourmet") || searchText.contains("premium") ||
                                searchText.contains("fine") || dish.rating >= 4.5f
                    "Chef's Special" -> searchText.contains("special") || searchText.contains("signature") ||
                                       searchText.contains("chef") || dish.rating >= 4.5f
                    "Quick Bites" -> searchText.contains("quick") || searchText.contains("snack") ||
                                    searchText.contains("bite") || searchText.contains("appetizer")
                    "Desserts" -> searchText.contains("dessert") || searchText.contains("sweet") ||
                                 searchText.contains("cake") || searchText.contains("ice cream") ||
                                 searchText.contains("pastry")
                    else -> true
                }
            }
            .maxByOrNull { it.rating }
            ?.let { dish ->
                TryDishInfo(
                    name = dish.name,
                    restaurant = dish.restaurantName,
                    rating = dish.rating,
                    reviewCount = 0, // Not in model
                    calories = 0,
                    isBestseller = true
                )
            }
    }

    val restaurants = remember(allRestaurants, selectedFilters) {
        allRestaurants
            .filter { restaurant ->
                // Apply filters
                val matchesRating = if (selectedFilters.contains("Rating 4.0+")) {
                    restaurant.averageRating >= 4.0f
                } else true

                val matchesVeg = if (selectedFilters.contains("Pure Veg")) {
                    restaurant.cuisine.contains("veg", ignoreCase = true) ||
                    restaurant.name.contains("veg", ignoreCase = true)
                } else true

                val matchesOffers = if (selectedFilters.contains("Great Offers")) {
                    // Show restaurants with good ratings (likely to have offers)
                    restaurant.averageRating >= 3.5f
                } else true

                matchesRating && matchesVeg && matchesOffers
            }
            .let { filtered ->
                // Apply "Nearest" sorting if selected
                if (selectedFilters.contains("Nearest")) {
                    // Sort by those with lat/long first (actual nearby restaurants)
                    filtered.sortedByDescending { it.latitude != null && it.longitude != null }
                } else {
                    filtered
                }
            }
            .map { restaurant ->
                RestaurantInfo(
                    id = restaurant.id,
                    name = restaurant.name,
                    cuisine = restaurant.cuisine,
                    rating = restaurant.averageRating,
                    reviewCount = restaurant.reviewCount,
                    deliveryTime = "30-40 min" // Default delivery time
                )
            }
    }
    
    val navItems = listOf(
        NavItem(
            "Home",
            { Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(24.dp)) },
            { Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(24.dp)) }
        ),
        NavItem(
            "Map",
            { Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(24.dp)) },
            { Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(24.dp)) }
        ),
        NavItem(
            "Rate",
            { Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp)) },
            { Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp)) }
        ),
        NavItem(
            "Feed",
            { Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(24.dp)) },
            { Icon(Icons.Outlined.People, contentDescription = null, modifier = Modifier.size(24.dp)) }
        ),
        NavItem(
            "Profile",
            { Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp)) },
            { Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp)) }
        )
    )
    
    Scaffold(
        containerColor = themeColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = themeColors.Surface,
                tonalElevation = 0.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedNavItem == index,
                        onClick = {
                            selectedNavItem = index
                            when (index) {
                                1 -> onMapClick()
                                2 -> onCameraClick()
                                3 -> onSocialFeedClick()
                                4 -> onProfileClick()
                            }
                        },
                        icon = {
                            if (selectedNavItem == index) item.selectedIcon() else item.unselectedIcon()
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = themeColors.Primary,
                            selectedTextColor = themeColors.Primary,
                            unselectedIconColor = themeColors.TextSecondary,
                            unselectedTextColor = themeColors.TextSecondary,
                            indicatorColor = themeColors.Primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(themeColors.Background),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Location Header + Notifications Bell
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocationHeader(
                        locationType = "Location",
                        address = currentLocation,
                        onLocationClick = onLocationClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = themeColors.TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            
            // Search Bar
            item {
                DarkSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search dishes, restaurants...",
                    onClick = onSearchClick,
                    onMicrophoneClick = {
                        // TODO: Implement voice search
                        onSearchClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Category Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    categories.forEach { category ->
                        CategoryChip(
                            text = category,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            
            // Featured Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Picks for You",
                        color = themeColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See All >",
                        color = themeColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onTopDishesClick() }
                    )
                }
            }
            
            // Featured Dishes Horizontal Scroll
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    featuredDishes.forEach { dish ->
                        var isFavorite by remember { mutableStateOf(false) }
                        FeaturedDishCard(
                            dishName = dish.name,
                            restaurantName = dish.restaurant,
                            rating = dish.rating,
                            calories = dish.calories,
                            isFavorite = isFavorite,
                            onClick = { onDishClick(dish.id) },
                            onFavoriteClick = { isFavorite = !isFavorite }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            
            // Try This Out Section
            tryThisOut?.let { dish ->
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Try This Out",
                            color = themeColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LargeDishCard(
                            dishName = dish.name,
                            restaurantName = dish.restaurant,
                            rating = dish.rating,
                            reviewCount = dish.reviewCount,
                            calories = dish.calories,
                            isBestseller = dish.isBestseller,
                            onClick = { onDishClick("bestseller") }
                        )
                    }
                }
            }
            
            // All Restaurants Section Header
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "All Restaurants",
                    color = themeColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Nearby Restaurants Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNearbyRestaurantsClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = themeColors.Primary.copy(alpha = 0.15f)
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.Primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = themeColors.Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Find Nearby Restaurants",
                                    color = themeColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Discover restaurants around you",
                                    color = themeColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = themeColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Restaurant Filters
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    filters.forEach { filter ->
                        FilterChipDark(
                            text = filter,
                            isSelected = selectedFilters.contains(filter),
                            onClick = {
                                selectedFilters = if (selectedFilters.contains(filter)) {
                                    selectedFilters - filter
                                } else {
                                    selectedFilters + filter
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            
            // No Restaurants Found Message
            if (noRestaurantsFound || restaurants.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.Surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = themeColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No restaurants found in $currentLocation",
                                color = themeColors.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try selecting a different location or be the first to add a restaurant here!",
                                color = themeColors.TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .clickable { onLocationClick() },
                                    colors = CardDefaults.cardColors(
                                        containerColor = themeColors.Primary.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Change Location",
                                        color = themeColors.Primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                                Card(
                                    modifier = Modifier
                                        .clickable { onAddRestaurantClick() },
                                    colors = CardDefaults.cardColors(
                                        containerColor = themeColors.Primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Add Restaurant",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Restaurant Cards
            items(restaurants) { restaurant ->
                var isFavorite by remember { mutableStateOf(false) }
                RestaurantCardDark(
                    restaurantName = restaurant.name,
                    cuisine = restaurant.cuisine,
                    rating = restaurant.rating,
                    reviewCount = restaurant.reviewCount,
                    deliveryTime = restaurant.deliveryTime,
                    isFavorite = isFavorite,
                    onClick = { onRestaurantClick(restaurant.id) },
                    onFavoriteClick = { isFavorite = !isFavorite },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Data classes for the screen
private data class DishInfo(
    val id: String,
    val name: String,
    val restaurant: String,
    val rating: Float,
    val calories: Int
)

private data class TryDishInfo(
    val name: String,
    val restaurant: String,
    val rating: Float,
    val reviewCount: Int,
    val calories: Int,
    val isBestseller: Boolean
)

private data class RestaurantInfo(
    val id: String,
    val name: String,
    val cuisine: String,
    val rating: Float,
    val reviewCount: Int,
    val deliveryTime: String
)
