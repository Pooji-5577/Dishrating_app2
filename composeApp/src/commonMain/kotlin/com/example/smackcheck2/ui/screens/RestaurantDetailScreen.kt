package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.model.Review
import com.example.smackcheck2.ui.components.FoodImages
import com.example.smackcheck2.ui.components.LoadingState
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.components.StarRatingDisplay
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.viewmodel.PhotoState
import com.example.smackcheck2.viewmodel.RestaurantDetailViewModel
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel

/**
 * Restaurant Detail Screen composable
 * Displays detailed information about a restaurant including reviews
 * 
 * @param viewModel RestaurantDetailViewModel instance
 * @param restaurantId ID of the restaurant
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    viewModel: RestaurantDetailViewModel,
    photoViewModel: RestaurantPhotoViewModel? = null,
    restaurantId: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val photoStates by photoViewModel?.photoStates?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap()) }
    
    LaunchedEffect(restaurantId) {
        viewModel.loadRestaurant(restaurantId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.restaurant?.name ?: "Restaurant") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues),
                    message = "Loading restaurant..."
                )
            }
            uiState.restaurant != null -> {
                val restaurant = uiState.restaurant!!

                // ── Trigger Google Places full photo load ──
                val photoState = photoStates[restaurant.id]
                LaunchedEffect(restaurant.id) {
                    photoViewModel?.loadFullPhotos(
                        restaurantId = restaurant.id,
                        placeId = restaurant.googlePlaceId,
                        name = restaurant.name,
                        city = restaurant.city
                    )
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Image gallery / carousel
                    item {
                        Text(
                            text = "Photos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // ── Google Places photos carousel ──
                        val googlePhotoUrls = (photoState as? PhotoState.FullPhotosLoaded)?.urls
                        
                        // Build the final image URL list: Google Places → Supabase → Unsplash
                        val imageUrls = when {
                            !googlePhotoUrls.isNullOrEmpty() -> googlePhotoUrls
                            restaurant.imageUrls.isNotEmpty() -> restaurant.imageUrls
                            else -> listOf(
                                FoodImages.getRestaurantImageByName(restaurant.name),
                                FoodImages.getRestaurantImage(restaurant.name.hashCode().let { if (it < 0) -it else it } + 1),
                                FoodImages.getRestaurantImage(restaurant.name.hashCode().let { if (it < 0) -it else it } + 2)
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(imageUrls) { imageUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 200.dp, height = 150.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    NetworkImage(
                                        imageUrl = imageUrl,
                                        contentDescription = "${restaurant.name} photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    
                    // Restaurant info
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = restaurant.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${restaurant.city} • ${restaurant.cuisine}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${restaurant.averageRating}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${restaurant.reviewCount} reviews)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Reviews section
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Reviews",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(uiState.reviews) { review ->
                        ReviewCard(
                            review = review,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Review Card composable
 */
@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.userName.first().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = review.dishName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StarRatingDisplay(
                    rating = review.rating,
                    starSize = 16.dp
                )
            }
            
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
