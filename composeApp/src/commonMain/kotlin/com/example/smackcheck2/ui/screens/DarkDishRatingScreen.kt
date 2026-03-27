package com.example.smackcheck2.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.theme.appColors

/**
 * Dark themed Dish Rating Screen
 * Allows users to rate dishes with stars, tags, and comments
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DarkDishRatingScreen(
    dishName: String,
    imageUri: String,
    imageBytes: ByteArray? = null,
    restaurants: List<Restaurant> = emptyList(),
    nearbyRestaurants: List<Restaurant> = emptyList(),
    searchedRestaurants: List<Restaurant> = emptyList(),
    isLoadingRestaurants: Boolean = false,
    isSearchingRestaurants: Boolean = false,
    isSubmitting: Boolean = false,
    showSuccess: Boolean = false,
    xpEarned: Int? = null,
    errorMessage: String? = null,
    onNavigateBack: () -> Unit,
    onSubmitRating: (rating: Float, comment: String, tags: List<String>, restaurant: Restaurant?) -> Unit,
    onDismissError: () -> Unit = {},
    onAddRestaurantManually: (() -> Unit)? = null,
    onSearchRestaurants: ((String) -> Unit)? = null
) {
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showRestaurantPicker by remember { mutableStateOf(false) }
    var restaurantSearchQuery by remember { mutableStateOf("") }

    // Skip / Add New Place state
    var showSkipInput by remember { mutableStateOf(false) }
    var skipRestaurantName by remember { mutableStateOf("") }
    var showAddNewPlaceForm by remember { mutableStateOf(false) }
    var newPlaceName by remember { mutableStateOf("") }
    var newPlaceCity by remember { mutableStateOf("") }
    var newPlaceCuisine by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message in Snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    val tags = listOf(
        "Spicy", "Tasty", "Healthy", "Value for Money",
        "Good Presentation", "Quick Service", "Large Portion",
        "Fresh Ingredients", "Chef's Special", "Must Try"
    )

    Scaffold(
        containerColor = appColors().Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rate Dish",
                        color = appColors().TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = appColors().TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appColors().Background
                )
            )
        }
    ) { paddingValues ->
        if (showSuccess) {
            // Success screen
            RatingSuccessScreen(
                dishName = dishName,
                rating = rating,
                xpEarned = xpEarned ?: calculateXP(rating, comment, selectedTags, imageBytes != null),
                onContinue = onNavigateBack
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dish preview card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors().Surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dish image - shows captured photo
                        if (imageBytes != null) {
                            ByteArrayImage(
                                imageBytes = imageBytes,
                                contentDescription = dishName,
                                modifier = Modifier
                                    .width(80.dp)
                                    .heightIn(max = 100.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Fallback to placeholder if no image
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF3D3D3D),
                                                Color(0xFF252525)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = appColors().TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = appColors().Primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI Detected",
                                    color = appColors().Primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = dishName,
                                color = appColors().TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Restaurant Selection
                Text(
                    text = "Where did you eat this?",
                    color = appColors().TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Restaurant picker button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showRestaurantPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors().Surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (selectedRestaurant != null) appColors().Primary else appColors().TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (selectedRestaurant != null) {
                                Text(
                                    text = selectedRestaurant!!.name,
                                    color = appColors().TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${selectedRestaurant!!.cuisine} • ${selectedRestaurant!!.city}",
                                    color = appColors().TextSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "Select a restaurant",
                                    color = appColors().TextSecondary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = appColors().TextSecondary
                        )
                    }
                }

                // Skip / Add New Place options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        showSkipInput = !showSkipInput
                        showAddNewPlaceForm = false
                        if (!showSkipInput) {
                            skipRestaurantName = ""
                            if (selectedRestaurant?.id?.startsWith("skip_") == true) {
                                selectedRestaurant = null
                            }
                        }
                    }) {
                        Text(
                            text = if (showSkipInput) "✕ Cancel skip" else "Skip — type name",
                            color = appColors().TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    TextButton(onClick = {
                        showRestaurantPicker = true
                        showAddNewPlaceForm = true
                        showSkipInput = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = appColors().Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add New Place",
                            color = appColors().Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Inline skip input — quick name entry without opening the picker
                if (showSkipInput) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors().Surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Type the restaurant name",
                                color = appColors().TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = skipRestaurantName,
                                onValueChange = { name ->
                                    skipRestaurantName = name
                                    selectedRestaurant = if (name.isNotBlank()) {
                                        Restaurant(
                                            id = "skip_${name.trim().hashCode()}",
                                            name = name.trim(),
                                            city = "",
                                            cuisine = ""
                                        )
                                    } else null
                                },
                                placeholder = { Text("e.g. Joe's Burger Bar") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appColors().Primary,
                                    unfocusedBorderColor = appColors().TextSecondary.copy(alpha = 0.3f),
                                    cursorColor = appColors().Primary,
                                    focusedTextColor = appColors().TextPrimary,
                                    unfocusedTextColor = appColors().TextPrimary,
                                    focusedPlaceholderColor = appColors().TextSecondary,
                                    unfocusedPlaceholderColor = appColors().TextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Star rating section
                Text(
                    text = "How would you rate it?",
                    color = appColors().TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive star rating
                StarRatingInput(
                    rating = rating,
                    onRatingChange = { rating = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Rating label
                Text(
                    text = when {
                        rating >= 4.5f -> "Amazing!"
                        rating >= 4f -> "Great!"
                        rating >= 3f -> "Good"
                        rating >= 2f -> "Okay"
                        rating >= 1f -> "Not Good"
                        else -> "Tap to rate"
                    },
                    color = if (rating > 0) appColors().Primary else appColors().TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tags section
                Text(
                    text = "Add tags (optional)",
                    color = appColors().TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) appColors().Primary.copy(alpha = 0.2f)
                                    else appColors().Surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) appColors().Primary
                                           else appColors().TextSecondary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    selectedTags = if (isSelected) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tag,
                                color = if (isSelected) appColors().Primary
                                       else appColors().TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Comment section
                Text(
                    text = "Add a comment (optional)",
                    color = appColors().TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Share your experience...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appColors().Primary,
                        unfocusedBorderColor = appColors().TextSecondary.copy(alpha = 0.3f),
                        cursorColor = appColors().Primary,
                        focusedTextColor = appColors().TextPrimary,
                        unfocusedTextColor = appColors().TextPrimary,
                        focusedPlaceholderColor = appColors().TextSecondary,
                        unfocusedPlaceholderColor = appColors().TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // XP Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors().Primary.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = appColors().Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "You'll earn",
                                color = appColors().TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "+${calculateXP(rating, comment, selectedTags, imageBytes != null)} XP",
                                color = appColors().Primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = {
                        onSubmitRating(rating, comment, selectedTags.toList(), selectedRestaurant)
                    },
                    enabled = rating > 0 && (selectedRestaurant != null || showSkipInput && skipRestaurantName.isNotBlank()) && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors().Primary,
                        contentColor = Color.White,
                        disabledContainerColor = appColors().TextSecondary.copy(alpha = 0.3f),
                        disabledContentColor = appColors().TextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = when {
                                selectedRestaurant != null -> "Submit Rating"
                                showSkipInput && skipRestaurantName.isBlank() -> "Enter a restaurant name"
                                else -> "Select or add a restaurant"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Restaurant picker bottom sheet
    if (showRestaurantPicker) {
        ModalBottomSheet(
            onDismissRequest = { showRestaurantPicker = false },
            sheetState = sheetState,
            containerColor = appColors().Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Select Restaurant",
                    color = appColors().TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search field
                OutlinedTextField(
                    value = restaurantSearchQuery,
                    onValueChange = { query ->
                        restaurantSearchQuery = query
                        // Trigger search when user types 3+ characters
                        if (query.length >= 3 && onSearchRestaurants != null) {
                            onSearchRestaurants(query)
                        }
                    },
                    placeholder = { Text("Search restaurants...") },
                    leadingIcon = {
                        if (isSearchingRestaurants) {
                            CircularProgressIndicator(
                                color = appColors().Primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = appColors().TextSecondary
                            )
                        }
                    },
                    trailingIcon = {
                        if (restaurantSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                restaurantSearchQuery = "" 
                                // Clear search results when clearing the query
                                onSearchRestaurants?.invoke("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = appColors().TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appColors().Primary,
                        unfocusedBorderColor = appColors().TextSecondary.copy(alpha = 0.3f),
                        cursorColor = appColors().Primary,
                        focusedTextColor = appColors().TextPrimary,
                        unfocusedTextColor = appColors().TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingRestaurants) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = appColors().Primary)
                    }
                } else {
                    // Combine nearby restaurants with search results from Places API
                    val searchResults = if (restaurantSearchQuery.length >= 3 && searchedRestaurants.isNotEmpty()) {
                        searchedRestaurants
                    } else {
                        emptyList()
                    }
                    
                    // When nearby restaurants are available (location selected), show only those
                    // Otherwise fall back to all restaurants
                    val primaryRestaurants = if (nearbyRestaurants.isNotEmpty()) {
                        nearbyRestaurants
                    } else {
                        restaurants
                    }

                    // Filter based on search query (searches name, cuisine, and city)
                    val filteredNearby = primaryRestaurants.filter {
                        restaurantSearchQuery.isEmpty() ||
                        it.name.contains(restaurantSearchQuery, ignoreCase = true) ||
                        it.cuisine.contains(restaurantSearchQuery, ignoreCase = true) ||
                        it.city.contains(restaurantSearchQuery, ignoreCase = true)
                    }
                    
                    // Combine filtered nearby with search results
                    val allFilteredNearby = (filteredNearby + searchResults).distinctBy { it.id }

                    // Only show "All Restaurants" section if no location is selected
                    val nearbyIds = (nearbyRestaurants + searchResults).map { it.id }.toSet()
                    val filteredOthers = if (nearbyRestaurants.isEmpty() && searchResults.isEmpty()) {
                        // No location selected and no search results, don't show separate "All" section
                        emptyList()
                    } else {
                        // Location selected, show other restaurants from database as fallback
                        restaurants.filter { restaurant ->
                            restaurant.id !in nearbyIds && (
                                restaurantSearchQuery.isEmpty() ||
                                restaurant.name.contains(restaurantSearchQuery, ignoreCase = true) ||
                                restaurant.cuisine.contains(restaurantSearchQuery, ignoreCase = true) ||
                                restaurant.city.contains(restaurantSearchQuery, ignoreCase = true)
                            )
                        }
                    }

                    val hasAnyResults = allFilteredNearby.isNotEmpty() || filteredOthers.isNotEmpty()

                    // Show "no results" only if we're not currently searching
                    if (!hasAnyResults && !isSearchingRestaurants) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = appColors().TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (restaurantSearchQuery.isNotEmpty()) 
                                        "No restaurants found for \"$restaurantSearchQuery\"" 
                                    else 
                                        "No restaurants found in this area",
                                    color = appColors().TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (onAddRestaurantManually != null) {
                                    Button(
                                        onClick = {
                                            showRestaurantPicker = false
                                            onAddRestaurantManually()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = appColors().Primary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Restaurant Manually")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(400.dp)
                        ) {
                            // Show loading indicator when searching
                            if (isSearchingRestaurants) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = appColors().Primary,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Searching restaurants...",
                                                color = appColors().TextSecondary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Nearby restaurants section (uses allFilteredNearby which includes search results)
                            if (allFilteredNearby.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = appColors().Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (searchResults.isNotEmpty()) "Search Results" else "Nearby Restaurants",
                                            color = appColors().Primary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                items(allFilteredNearby) { restaurant ->
                                    RestaurantPickerItem(
                                        restaurant = restaurant,
                                        isSelected = selectedRestaurant?.id == restaurant.id,
                                        isNearby = true,
                                        onClick = {
                                            selectedRestaurant = restaurant
                                            showRestaurantPicker = false
                                        }
                                    )
                                }
                            }

                            // All other restaurants section (only show if no nearby restaurants)
                            if (filteredOthers.isNotEmpty()) {
                                item {
                                    if (allFilteredNearby.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    Text(
                                        text = if (nearbyRestaurants.isEmpty()) "All Restaurants" else "Other Nearby",
                                        color = appColors().TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(filteredOthers) { restaurant ->
                                    RestaurantPickerItem(
                                        restaurant = restaurant,
                                        isSelected = selectedRestaurant?.id == restaurant.id,
                                        isNearby = false,
                                        onClick = {
                                            selectedRestaurant = restaurant
                                            showRestaurantPicker = false
                                        }
                                    )
                                }
                            }
                        // Add New Place — always visible at the bottom of the list
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = appColors().TextSecondary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (showAddNewPlaceForm) {
                                // Full form for adding a new restaurant
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = appColors().Primary.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Add New Place",
                                            color = appColors().Primary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        OutlinedTextField(
                                            value = newPlaceName,
                                            onValueChange = { newPlaceName = it },
                                            placeholder = { Text("Restaurant name *") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = appColors().Primary,
                                                unfocusedBorderColor = appColors().TextSecondary.copy(alpha = 0.3f),
                                                cursorColor = appColors().Primary,
                                                focusedTextColor = appColors().TextPrimary,
                                                unfocusedTextColor = appColors().TextPrimary,
                                                focusedPlaceholderColor = appColors().TextSecondary,
                                                unfocusedPlaceholderColor = appColors().TextSecondary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = newPlaceCity,
                                            onValueChange = { newPlaceCity = it },
                                            placeholder = { Text("City (optional)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = appColors().Primary,
                                                unfocusedBorderColor = appColors().TextSecondary.copy(alpha = 0.3f),
                                                cursorColor = appColors().Primary,
                                                focusedTextColor = appColors().TextPrimary,
                                                unfocusedTextColor = appColors().TextPrimary,
                                                focusedPlaceholderColor = appColors().TextSecondary,
                                                unfocusedPlaceholderColor = appColors().TextSecondary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = newPlaceCuisine,
                                            onValueChange = { newPlaceCuisine = it },
                                            placeholder = { Text("Cuisine (optional)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = appColors().Primary,
                                                unfocusedBorderColor = appColors().TextSecondary.copy(alpha = 0.3f),
                                                cursorColor = appColors().Primary,
                                                focusedTextColor = appColors().TextPrimary,
                                                unfocusedTextColor = appColors().TextPrimary,
                                                focusedPlaceholderColor = appColors().TextSecondary,
                                                unfocusedPlaceholderColor = appColors().TextSecondary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    showAddNewPlaceForm = false
                                                    newPlaceName = ""
                                                    newPlaceCity = ""
                                                    newPlaceCuisine = ""
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancel", color = appColors().TextSecondary)
                                            }
                                            Button(
                                                onClick = {
                                                    val newRestaurant = Restaurant(
                                                        id = "new_${newPlaceName.trim().hashCode()}",
                                                        name = newPlaceName.trim(),
                                                        city = newPlaceCity.trim(),
                                                        cuisine = newPlaceCuisine.trim()
                                                    )
                                                    selectedRestaurant = newRestaurant
                                                    showRestaurantPicker = false
                                                    showAddNewPlaceForm = false
                                                    showSkipInput = false
                                                    newPlaceName = ""
                                                    newPlaceCity = ""
                                                    newPlaceCuisine = ""
                                                },
                                                enabled = newPlaceName.isNotBlank(),
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = appColors().Primary,
                                                    contentColor = Color.White,
                                                    disabledContainerColor = appColors().TextSecondary.copy(alpha = 0.2f)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Add Place")
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Button to open the Add New Place form
                                TextButton(
                                    onClick = { showAddNewPlaceForm = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = appColors().Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Add New Place",
                                        color = appColors().Primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
}

@Composable
private fun RestaurantPickerItem(
    restaurant: Restaurant,
    isSelected: Boolean,
    isNearby: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> appColors().Primary.copy(alpha = 0.15f)
                isNearby -> appColors().Primary.copy(alpha = 0.05f)
                else -> appColors().Surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isNearby) appColors().Primary.copy(alpha = 0.1f)
                        else appColors().SurfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isNearby) Icons.Default.LocationOn else Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = if (isNearby) appColors().Primary else appColors().TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restaurant.name,
                        color = appColors().TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isNearby) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    appColors().Primary.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Nearby",
                                color = appColors().Primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restaurant.cuisine,
                        color = appColors().TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = " • ",
                        color = appColors().TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = restaurant.city,
                        color = appColors().TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = " • ",
                        color = appColors().TextSecondary,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = " ${restaurant.averageRating}",
                        color = appColors().TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = appColors().Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StarRatingInput(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val isSelected = i <= rating
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = tween(150)
            )
            val starColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFFD700) else appColors().TextSecondary.copy(alpha = 0.4f),
                animationSpec = tween(150)
            )

            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star $i",
                tint = starColor,
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .clickable { onRatingChange(i.toFloat()) }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun RatingSuccessScreen(
    dishName: String,
    rating: Float,
    xpEarned: Int,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors().Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    appColors().Primary.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = appColors().Primary,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Rating Submitted!",
            color = appColors().TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Thanks for rating $dishName",
            color = appColors().TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // XP earned card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = appColors().Primary.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = appColors().Primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "You earned",
                        color = appColors().TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "+$xpEarned XP",
                        color = appColors().Primary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = appColors().Primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Unified XP formula matching DishRatingViewModel:
 * base 10 + photo 5 + comment(>50 chars) 10 + tags * 2
 */
private fun calculateXP(rating: Float, comment: String, tags: Set<String>, hasPhoto: Boolean = true): Int {
    var xp = 10 // Base XP for submitting a rating
    if (hasPhoto) xp += 5 // Bonus for including a photo
    if (comment.length > 50) xp += 10 // Bonus for detailed comment
    xp += tags.size * 2 // 2 XP per tag
    return xp
}
