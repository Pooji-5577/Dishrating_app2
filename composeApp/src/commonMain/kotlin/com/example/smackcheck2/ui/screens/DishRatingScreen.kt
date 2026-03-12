package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.ui.components.StarRating
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.TextFieldShape
import com.example.smackcheck2.viewmodel.DishRatingViewModel

/**
 * Dish Rating Screen composable (light theme)
 * Allows users to rate a dish with stars, select a restaurant, add tags, and add a comment.
 *
 * @param viewModel DishRatingViewModel instance
 * @param dishName Name of the dish being rated
 * @param imageUri URI of the dish image
 * @param restaurants List of all restaurants to choose from
 * @param onNavigateBack Callback to navigate back
 * @param onSubmitSuccess Callback when rating is submitted successfully
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DishRatingScreen(
    viewModel: DishRatingViewModel,
    dishName: String,
    imageUri: String,
    restaurants: List<Restaurant> = emptyList(),
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Local state for restaurant picker and tags
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var showRestaurantDropdown by remember { mutableStateOf(false) }
    var restaurantSearchQuery by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    val tags = listOf(
        "Spicy", "Tasty", "Healthy", "Value for Money",
        "Good Presentation", "Quick Service", "Large Portion",
        "Fresh Ingredients", "Chef's Special", "Must Try"
    )

    LaunchedEffect(dishName, imageUri) {
        viewModel.initialize(dishName, imageUri)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Calculate XP preview
    val xpPreview = remember(uiState.rating, uiState.comment, selectedTags, imageUri) {
        var xp = 10 // base
        if (imageUri.isNotBlank()) xp += 5 // photo bonus
        if (uiState.comment.length > 50) xp += 10 // comment bonus
        xp += selectedTags.size * 2 // tag bonus
        xp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate Dish") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dish image card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dish name
            Text(
                text = dishName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Restaurant picker
            Text(
                text = "Select Restaurant",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedRestaurant?.name ?: restaurantSearchQuery,
                    onValueChange = { query ->
                        restaurantSearchQuery = query
                        selectedRestaurant = null
                        showRestaurantDropdown = query.isNotBlank()
                    },
                    placeholder = { Text("Search for a restaurant...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = TextFieldShape,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown for filtered restaurants (searches name, cuisine, and city)
                val filteredRestaurants = restaurants.filter {
                    restaurantSearchQuery.isNotBlank() && (
                        it.name.contains(restaurantSearchQuery, ignoreCase = true) ||
                        it.cuisine.contains(restaurantSearchQuery, ignoreCase = true) ||
                        it.city.contains(restaurantSearchQuery, ignoreCase = true)
                    )
                }.take(5)

                DropdownMenu(
                    expanded = showRestaurantDropdown && filteredRestaurants.isNotEmpty(),
                    onDismissRequest = { showRestaurantDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    filteredRestaurants.forEach { restaurant ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = restaurant.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (restaurant.city.isNotBlank()) {
                                        Text(
                                            text = restaurant.city,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedRestaurant = restaurant
                                restaurantSearchQuery = restaurant.name
                                showRestaurantDropdown = false
                                viewModel.setRestaurantId(restaurant.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rating label
            Text(
                text = "How would you rate this dish?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Star rating
            StarRating(
                rating = uiState.rating,
                onRatingChange = viewModel::onRatingChange,
                starSize = 48.dp,
                isEditable = true,
                allowHalfRating = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating value text
            Text(
                text = when {
                    uiState.rating == 0f -> "Tap to rate"
                    uiState.rating <= 1f -> "Poor"
                    uiState.rating <= 2f -> "Fair"
                    uiState.rating <= 3f -> "Good"
                    uiState.rating <= 4f -> "Very Good"
                    else -> "Excellent"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (uiState.rating > 0f)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tags
            Text(
                text = "Tags (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val isSelected = tag in selectedTags
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                            viewModel.onTagsChange(selectedTags.toList())
                        },
                        label = { Text(tag, style = MaterialTheme.typography.bodySmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Comment field
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = viewModel::onCommentChange,
                label = { Text("Add a comment (optional)") },
                placeholder = { Text("What did you think about this dish?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = TextFieldShape,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // XP Preview
            if (uiState.rating > 0f) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "XP you'll earn",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "+${xpPreview} XP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Submit button
            Button(
                onClick = {
                    viewModel.submitRating(onSubmitSuccess)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSubmitting && uiState.rating > 0f && selectedRestaurant != null
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (selectedRestaurant == null) "Select a Restaurant" else "Submit Rating",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
