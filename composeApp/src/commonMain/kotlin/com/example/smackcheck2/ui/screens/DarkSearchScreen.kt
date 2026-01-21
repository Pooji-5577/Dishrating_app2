package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkSearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onRestaurantClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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
            
            // Results
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
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No restaurants found",
                        color = themeColors.TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Try a different search",
                        color = themeColors.TextTertiary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.results) { restaurant ->
                        DarkRestaurantSearchCard(
                            restaurant = restaurant,
                            onClick = { onRestaurantClick(restaurant.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkRestaurantSearchCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    val themeColors = appColors()
    
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            
            // Rating
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
