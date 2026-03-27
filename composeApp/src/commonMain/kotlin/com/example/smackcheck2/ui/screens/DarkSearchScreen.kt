package com.example.smackcheck2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.components.FoodImages
import com.example.smackcheck2.ui.components.SmartRestaurantImage
import com.example.smackcheck2.ui.components.EmptyState
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import com.example.smackcheck2.viewmodel.SearchViewModel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkSearchScreen(
    viewModel: SearchViewModel,
    photoViewModel: RestaurantPhotoViewModel? = null,
    onNavigateBack: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onAddRestaurantClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val photoStates by photoViewModel?.photoStates?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap()) }
    val themeColors = appColors()
    
    Scaffold(
        containerColor = themeColors.Background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Search",
                        color = themeColors.TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = themeColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddRestaurantClick) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Restaurant",
                            tint = themeColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.Background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(themeColors.Background)
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { 
                    Text(
                        "Search restaurants...",
                        color = themeColors.TextTertiary
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = themeColors.Primary
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = themeColors.TextSecondary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.Primary,
                    unfocusedBorderColor = themeColors.Surface,
                    focusedContainerColor = themeColors.CardBackground,
                    unfocusedContainerColor = themeColors.CardBackground,
                    cursorColor = themeColors.Primary,
                    focusedTextColor = themeColors.TextPrimary,
                    unfocusedTextColor = themeColors.TextPrimary
                ),
                singleLine = true
            )
            
            // ── "Restaurants & Cafes Only" Filter ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isFilterActive = uiState.restaurantsAndCafesOnly
                FilterChip(
                    selected = isFilterActive,
                    onClick = { viewModel.toggleRestaurantsAndCafesFilter() },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFilterActive) Icons.Default.Check else Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isFilterActive) themeColors.Primary else themeColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restaurants & Cafes Only")
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = themeColors.CardBackground,
                        labelColor = themeColors.TextSecondary,
                        selectedContainerColor = themeColors.Primary.copy(alpha = 0.2f),
                        selectedLabelColor = themeColors.Primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = themeColors.Surface,
                        selectedBorderColor = themeColors.Primary,
                        enabled = true,
                        selected = isFilterActive
                    )
                )
            }
            
            // Active filter banner
            AnimatedVisibility(visible = uiState.restaurantsAndCafesOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            themeColors.Primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            themeColors.Primary.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = themeColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Showing Restaurants & Cafes only",
                        color = themeColors.Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Clear",
                        color = themeColors.Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            viewModel.toggleRestaurantsAndCafesFilter()
                        }
                    )
                }
            }
            
            // Cuisine filters
            Text(
                text = "Cuisine",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeColors.Primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Italian", "Japanese", "Indian", "Mexican", "Chinese", "Thai").forEach { cuisine ->
                    val isSelected = uiState.selectedCuisines.contains(cuisine)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCuisineToggle(cuisine) },
                        label = { Text(cuisine) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = themeColors.CardBackground,
                            labelColor = themeColors.TextSecondary,
                            selectedContainerColor = themeColors.Primary.copy(alpha = 0.2f),
                            selectedLabelColor = themeColors.Primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = themeColors.Surface,
                            selectedBorderColor = themeColors.Primary,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rating filters
            Text(
                text = "Minimum Rating",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeColors.Primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3.0f, 3.5f, 4.0f, 4.5f).forEach { rating ->
                    val isSelected = uiState.selectedRating == rating
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onRatingSelect(if (isSelected) null else rating) },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) themeColors.Primary else themeColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$rating+")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = themeColors.CardBackground,
                            labelColor = themeColors.TextSecondary,
                            selectedContainerColor = themeColors.Primary.copy(alpha = 0.2f),
                            selectedLabelColor = themeColors.Primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = themeColors.Surface,
                            selectedBorderColor = themeColors.Primary,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results - use weight(1f) to fill remaining space and enable smooth scrolling
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (uiState.isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Searching...",
                            color = themeColors.TextSecondary
                        )
                    }
                } else if (uiState.results.isEmpty() && uiState.query.isNotEmpty()) {
                    EmptyState(
                        title = "No restaurants found",
                        message = "We couldn't find any results for \"${uiState.query}\". Try a different search term.",
                        icon = Icons.Outlined.SearchOff,
                        action = {
                            Button(
                                onClick = { viewModel.clearSearch() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColors.Primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Clear Search")
                            }
                        }
                    )
                } else if (uiState.results.isEmpty() && uiState.query.isEmpty()) {
                    EmptyState(
                        title = "Search for Restaurants",
                        message = "Find restaurants by name, cuisine, or location",
                        icon = Icons.Outlined.Search
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.results) { restaurant ->
                            DarkRestaurantSearchCard(
                                restaurant = restaurant,
                                photoViewModel = photoViewModel,
                                onClick = { onRestaurantClick(restaurant.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkRestaurantSearchCard(
    restaurant: Restaurant,
    photoViewModel: RestaurantPhotoViewModel? = null,
    onClick: () -> Unit
) {
    val themeColors = appColors()

    // ── Trigger Google Places thumbnail fetch when card appears ──
    val photoStates by photoViewModel?.photoStates?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap()) }
    val photoState = photoStates[restaurant.id]

    // Request thumbnail on first composition
    androidx.compose.runtime.LaunchedEffect(restaurant.id) {
        photoViewModel?.loadThumbnail(
            restaurantId = restaurant.id,
            placeId = restaurant.googlePlaceId,
            name = restaurant.name,
            city = restaurant.city
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.CardBackground
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Thumbnail Image: Google Places → Supabase → Unsplash fallback ──
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(themeColors.Surface)
            ) {
                // Try Google Places photo first
                if (photoState != null && photoViewModel != null) {
                    SmartRestaurantImage(
                        photoState = photoState,
                        restaurantName = restaurant.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback: Supabase photo_urls → Unsplash
                    val imageUrl = restaurant.imageUrls.firstOrNull()
                        ?: FoodImages.getRestaurantImageByName(restaurant.name)
                    NetworkImage(
                        imageUrl = imageUrl,
                        contentDescription = restaurant.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // ── Info ──
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurant.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = restaurant.cuisine,
                    fontSize = 13.sp,
                    color = themeColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = themeColors.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = restaurant.city,
                        fontSize = 12.sp,
                        color = themeColors.TextTertiary
                    )
                }
            }
            
            // ── Rating ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = themeColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${restaurant.averageRating}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.TextPrimary
                )
            }
        }
    }
}
