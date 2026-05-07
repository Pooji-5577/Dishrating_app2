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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.ui.theme.appColors

private val DeepMaroon = Color(0xFF3B1011)
private val WarmMaroon = Color(0xFF642223)
private val CrimsonRed = Color(0xFF9B2335)
private val RosePink = Color(0xFFBB5B5C)
private val CreamWhite = Color(0xFFFFF8F0)
private val WarmBeige = Color(0xFFF5EDE3)
private val LightBlush = Color(0xFFFDE8E8)

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
    onRatingComplete: () -> Unit = onNavigateBack,
    onSubmitRating: (rating: Float, comment: String, tags: List<String>, restaurant: Restaurant?) -> Unit,
    onPriceChange: (String) -> Unit = {},
    onDismissError: () -> Unit = {},
    onAddRestaurantManually: (() -> Unit)? = null,
    onSearchRestaurants: ((String) -> Unit)? = null,
    detectedChain: String? = null,
    detectedType: String? = null,
    currencySymbol: String = "\u20B9 "
) {
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showRestaurantPicker by remember { mutableStateOf(false) }
    var restaurantSearchQuery by remember { mutableStateOf("") }

    var showEditorialSheet by remember { mutableStateOf(false) }
    var editorialSuggestion by remember { mutableStateOf("") }
    var isEditorialLoading by remember { mutableStateOf(false) }

    var showSkipInput by remember { mutableStateOf(false) }
    var skipRestaurantName by remember { mutableStateOf("") }
    var showAddNewPlaceForm by remember { mutableStateOf(false) }
    var newPlaceName by remember { mutableStateOf("") }
    var newPlaceCity by remember { mutableStateOf("") }
    var newPlaceCuisine by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    // When AI detected a restaurant chain, seed the picker search so we surface that chain first.
    LaunchedEffect(showRestaurantPicker, detectedChain) {
        if (showRestaurantPicker && !detectedChain.isNullOrBlank() && restaurantSearchQuery.isBlank()) {
            restaurantSearchQuery = detectedChain
            onSearchRestaurants?.invoke(detectedChain)
        }
    }

    // Pre-fill the "Add New Place" form with AI-detected chain/type when opened.
    LaunchedEffect(showAddNewPlaceForm) {
        if (showAddNewPlaceForm) {
            if (newPlaceName.isBlank() && !detectedChain.isNullOrBlank()) {
                newPlaceName = detectedChain
            }
            if (newPlaceCuisine.isBlank() && !detectedType.isNullOrBlank()) {
                newPlaceCuisine = detectedType
            }
        }
    }

    // Preserve rating for success screen before clearing form
    var savedRating by remember { mutableFloatStateOf(0f) }
    var savedComment by remember { mutableStateOf("") }
    var savedTags by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            savedRating = rating
            savedComment = comment
            savedTags = selectedTags
            rating = 0f
            comment = ""
            selectedRestaurant = null
            selectedTags = setOf()
            showRestaurantPicker = false
            restaurantSearchQuery = ""
            showSkipInput = false
            skipRestaurantName = ""
            showAddNewPlaceForm = false
            newPlaceName = ""
            newPlaceCity = ""
            newPlaceCuisine = ""
        }
    }

    val tags = listOf(
        "Highly Recommended", "Authentic", "Seasonal",
        "Must Try", "Spicy", "Comfort Food",
        "Good Presentation", "Value for Money"
    )

    if (showSuccess) {
        RatingSuccessScreen(
            dishName = dishName,
            imageBytes = imageBytes,
            xpEarned = xpEarned ?: calculateXP(savedRating, savedComment, savedTags, imageBytes != null),
            onContinue = onRatingComplete
        )
        return
    }

    Scaffold(
        containerColor = CreamWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmackCheckWordmark(
                            fontFamily = PlusJakartaSans(),
                            fontSize = 18.sp,
                            letterSpacing = 0.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepMaroon)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = DeepMaroon, modifier = Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero dish image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmBeige),
                contentAlignment = Alignment.Center
            ) {
                if (imageBytes != null) {
                    ByteArrayImage(
                        imageBytes = imageBytes,
                        contentDescription = dishName,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = RosePink.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                }

                // Camera re-shoot overlay badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(CrimsonRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Re-shoot", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Currently Reviewing" label
            Text(
                text = "CURRENTLY REVIEWING",
                color = CrimsonRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dish name
            Text(
                text = dishName,
                color = DeepMaroon,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Restaurant selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable { showRestaurantPicker = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightBlush)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (selectedRestaurant != null) CrimsonRed else RosePink, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedRestaurant != null) {
                            Text(selectedRestaurant!!.name, color = DeepMaroon, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (selectedRestaurant!!.cuisine.isNotBlank()) {
                                Text("${selectedRestaurant!!.cuisine} • ${selectedRestaurant!!.city}", color = WarmMaroon, fontSize = 12.sp)
                            }
                        } else {
                            Text("Where did you eat this?", color = WarmMaroon, fontSize = 15.sp)
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = WarmMaroon)
                }
            }

            // Skip / Add options
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    showSkipInput = !showSkipInput
                    showAddNewPlaceForm = false
                    if (!showSkipInput) {
                        skipRestaurantName = ""
                        if (selectedRestaurant?.id?.startsWith("skip_") == true) selectedRestaurant = null
                    }
                }) {
                    Text(
                        text = if (showSkipInput) "Cancel" else "Type name instead",
                        color = WarmMaroon,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = {
                    showRestaurantPicker = true
                    showAddNewPlaceForm = true
                    showSkipInput = false
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add New Place", color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Inline skip input
            if (showSkipInput) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBlush)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = skipRestaurantName,
                            onValueChange = { name ->
                                skipRestaurantName = name
                                selectedRestaurant = if (name.isNotBlank()) {
                                    Restaurant(id = "skip_${name.trim().hashCode()}", name = name.trim(), city = "", cuisine = "")
                                } else null
                            },
                            placeholder = { Text("e.g. Joe's Burger Bar", color = RosePink) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonRed,
                                unfocusedBorderColor = RosePink.copy(alpha = 0.3f),
                                cursorColor = CrimsonRed,
                                focusedTextColor = DeepMaroon,
                                unfocusedTextColor = DeepMaroon
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Star rating section
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LightBlush)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How was your culinary experience?",
                        color = DeepMaroon,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StarRatingInput(
                        rating = rating,
                        onRatingChange = { rating = it }
                    )

                    if (rating > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                rating >= 5f -> "Exceptional!"
                                rating >= 4f -> "Excellent"
                                rating >= 3f -> "Good"
                                rating >= 2f -> "Fair"
                                else -> "Poor"
                            },
                            color = CrimsonRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Comment section
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LightBlush)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        placeholder = {
                            Text(
                                "Describe the textures, the aromas, and the symphony of flavors...",
                                color = RosePink,
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonRed,
                            unfocusedBorderColor = RosePink.copy(alpha = 0.2f),
                            cursorColor = CrimsonRed,
                            focusedTextColor = DeepMaroon,
                            unfocusedTextColor = DeepMaroon,
                            focusedContainerColor = Color.White.copy(alpha = 0.5f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✦ Editorial Style",
                        color = CrimsonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                if (comment.isNotBlank()) {
                                    isEditorialLoading = true
                                    editorialSuggestion = ""
                                    showEditorialSheet = true
                                    // Simulate AI editorial enhancement of the draft text
                                    editorialSuggestion = buildString {
                                        append("A dish that commands attention — ")
                                        append(comment.trimEnd('.', '!', '?'))
                                        append(". The interplay of flavors lingers well beyond the last bite.")
                                    }
                                    isEditorialLoading = false
                                } else {
                                    showEditorialSheet = true
                                    editorialSuggestion = ""
                                    isEditorialLoading = false
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tags section
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val isSelected = selectedTags.contains(tag)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CrimsonRed else LightBlush)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CrimsonRed else RosePink.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tag,
                            color = if (isSelected) Color.White else WarmMaroon,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Price field
            OutlinedTextField(
                value = price,
                onValueChange = { newPrice ->
                    val filtered = newPrice.filter { it.isDigit() || it == '.' }
                        .let { if (it.count { c -> c == '.' } > 1) it.dropLast(1) else it }
                    price = filtered
                    onPriceChange(filtered)
                },
                placeholder = { Text("Price (optional)", color = RosePink) },
                prefix = { Text(currencySymbol, color = DeepMaroon, fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonRed,
                    unfocusedBorderColor = RosePink.copy(alpha = 0.2f),
                    cursorColor = CrimsonRed,
                    focusedTextColor = DeepMaroon,
                    unfocusedTextColor = DeepMaroon,
                    focusedContainerColor = LightBlush,
                    unfocusedContainerColor = LightBlush
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button - gradient style
            Button(
                onClick = { onSubmitRating(rating, comment, selectedTags.toList(), selectedRestaurant) },
                enabled = rating > 0 && (selectedRestaurant != null || (showSkipInput && skipRestaurantName.isNotBlank())) && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (rating > 0 && (selectedRestaurant != null || (showSkipInput && skipRestaurantName.isNotBlank())) && !isSubmitting)
                                Brush.horizontalGradient(listOf(WarmMaroon, CrimsonRed, WarmMaroon))
                            else
                                Brush.horizontalGradient(listOf(RosePink.copy(alpha = 0.3f), RosePink.copy(alpha = 0.3f))),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Post Review", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your curation will be shared with the SmackCheck community",
                color = RosePink,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Editorial Style AI assist bottom sheet
    if (showEditorialSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditorialSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CreamWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Editorial Style",
                        color = DeepMaroon, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "AI-enhanced version of your review",
                    color = WarmMaroon, fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                if (isEditorialLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CrimsonRed, modifier = Modifier.size(32.dp))
                    }
                } else if (editorialSuggestion.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBlush, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = editorialSuggestion,
                            color = DeepMaroon, fontSize = 14.sp,
                            fontStyle = FontStyle.Italic, lineHeight = 22.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showEditorialSheet = false },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Dismiss", color = CrimsonRed, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                comment = editorialSuggestion
                                showEditorialSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Use This", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Text(
                        "Write something in your review first, then tap Editorial Style for an AI-enhanced version.",
                        color = WarmMaroon, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { showEditorialSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = CrimsonRed)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Restaurant picker bottom sheet
    if (showRestaurantPicker) {
        ModalBottomSheet(
            onDismissRequest = { showRestaurantPicker = false },
            sheetState = sheetState,
            containerColor = CreamWhite
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("Select Restaurant", color = DeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = restaurantSearchQuery,
                    onValueChange = { query ->
                        restaurantSearchQuery = query
                        if (query.length >= 3 && onSearchRestaurants != null) onSearchRestaurants(query)
                    },
                    placeholder = { Text("Search restaurants...", color = RosePink) },
                    leadingIcon = {
                        if (isSearchingRestaurants) {
                            CircularProgressIndicator(color = CrimsonRed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = WarmMaroon)
                        }
                    },
                    trailingIcon = {
                        if (restaurantSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                restaurantSearchQuery = ""
                                onSearchRestaurants?.invoke("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = WarmMaroon)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonRed,
                        unfocusedBorderColor = RosePink.copy(alpha = 0.3f),
                        cursorColor = CrimsonRed,
                        focusedTextColor = DeepMaroon,
                        unfocusedTextColor = DeepMaroon
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingRestaurants) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CrimsonRed)
                    }
                } else {
                    val searchResults = if (restaurantSearchQuery.length >= 3 && searchedRestaurants.isNotEmpty()) searchedRestaurants else emptyList()
                    val primaryRestaurants = if (nearbyRestaurants.isNotEmpty()) nearbyRestaurants else restaurants

                    val filteredNearby = primaryRestaurants.filter {
                        restaurantSearchQuery.isEmpty() ||
                        it.name.contains(restaurantSearchQuery, ignoreCase = true) ||
                        it.cuisine.contains(restaurantSearchQuery, ignoreCase = true) ||
                        it.city.contains(restaurantSearchQuery, ignoreCase = true)
                    }
                    val allFilteredNearby = (filteredNearby + searchResults).distinctBy { it.id }

                    val nearbyIds = (nearbyRestaurants + searchResults).map { it.id }.toSet()
                    val filteredOthers = if (nearbyRestaurants.isEmpty() && searchResults.isEmpty()) {
                        emptyList()
                    } else {
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

                    if (!hasAnyResults && !isSearchingRestaurants) {
                        Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = RosePink, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (restaurantSearchQuery.isNotEmpty()) "No restaurants found for \"$restaurantSearchQuery\"" else "No restaurants found in this area",
                                    color = WarmMaroon,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (onAddRestaurantManually != null) {
                                    Button(
                                        onClick = { showRestaurantPicker = false; onAddRestaurantManually() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Restaurant Manually")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(400.dp)) {
                            if (isSearchingRestaurants) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                            CircularProgressIndicator(color = CrimsonRed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Searching restaurants...", color = WarmMaroon, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }

                            if (allFilteredNearby.isNotEmpty()) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (searchResults.isNotEmpty()) "Search Results" else "Nearby Restaurants",
                                            color = CrimsonRed,
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
                                        onClick = { selectedRestaurant = restaurant; showRestaurantPicker = false }
                                    )
                                }
                            }

                            if (filteredOthers.isNotEmpty()) {
                                item {
                                    if (allFilteredNearby.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (nearbyRestaurants.isEmpty()) "All Restaurants" else "Other Nearby",
                                        color = WarmMaroon,
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
                                        onClick = { selectedRestaurant = restaurant; showRestaurantPicker = false }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = RosePink.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(8.dp))

                                if (showAddNewPlaceForm) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = LightBlush)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text("Add New Place", color = CrimsonRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            OutlinedTextField(
                                                value = newPlaceName, onValueChange = { newPlaceName = it },
                                                placeholder = { Text("Restaurant name *", color = RosePink) },
                                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CrimsonRed, unfocusedBorderColor = RosePink.copy(alpha = 0.3f), cursorColor = CrimsonRed, focusedTextColor = DeepMaroon, unfocusedTextColor = DeepMaroon),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            OutlinedTextField(
                                                value = newPlaceCity, onValueChange = { newPlaceCity = it },
                                                placeholder = { Text("City (optional)", color = RosePink) },
                                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CrimsonRed, unfocusedBorderColor = RosePink.copy(alpha = 0.3f), cursorColor = CrimsonRed, focusedTextColor = DeepMaroon, unfocusedTextColor = DeepMaroon),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            OutlinedTextField(
                                                value = newPlaceCuisine, onValueChange = { newPlaceCuisine = it },
                                                placeholder = { Text("Cuisine (optional)", color = RosePink) },
                                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CrimsonRed, unfocusedBorderColor = RosePink.copy(alpha = 0.3f), cursorColor = CrimsonRed, focusedTextColor = DeepMaroon, unfocusedTextColor = DeepMaroon),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(
                                                    onClick = { showAddNewPlaceForm = false; newPlaceName = ""; newPlaceCity = ""; newPlaceCuisine = "" },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Cancel", color = WarmMaroon) }
                                                Button(
                                                    onClick = {
                                                        selectedRestaurant = Restaurant(id = "new_${newPlaceName.trim().hashCode()}", name = newPlaceName.trim(), city = newPlaceCity.trim(), cuisine = newPlaceCuisine.trim())
                                                        showRestaurantPicker = false; showAddNewPlaceForm = false; showSkipInput = false
                                                        newPlaceName = ""; newPlaceCity = ""; newPlaceCuisine = ""
                                                    },
                                                    enabled = newPlaceName.isNotBlank(),
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White, disabledContainerColor = RosePink.copy(alpha = 0.2f)),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) { Text("Add Place") }
                                            }
                                        }
                                    }
                                } else {
                                    TextButton(onClick = { showAddNewPlaceForm = true }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add New Place", color = CrimsonRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> CrimsonRed.copy(alpha = 0.1f)
                isNearby -> LightBlush
                else -> Color.White
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (isNearby) CrimsonRed.copy(alpha = 0.1f) else WarmBeige),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isNearby) Icons.Default.LocationOn else Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = if (isNearby) CrimsonRed else WarmMaroon,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(restaurant.name, color = DeepMaroon, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (isNearby) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.background(CrimsonRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Nearby", color = CrimsonRed, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(restaurant.cuisine, color = WarmMaroon, fontSize = 12.sp)
                    if (restaurant.city.isNotBlank()) {
                        Text(" • ", color = WarmMaroon, fontSize = 12.sp)
                        Text(restaurant.city, color = WarmMaroon, fontSize = 12.sp)
                    }
                    if (restaurant.averageRating > 0) {
                        Text(" • ", color = WarmMaroon, fontSize = 12.sp)
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(13.dp))
                        Text(" ${restaurant.averageRating}", color = WarmMaroon, fontSize = 12.sp)
                    }
                }
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = CrimsonRed, modifier = Modifier.size(22.dp))
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
                targetValue = if (isSelected) 1.15f else 1f,
                animationSpec = tween(200)
            )
            val starColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFFD700) else RosePink.copy(alpha = 0.35f),
                animationSpec = tween(200)
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star $i",
                tint = starColor,
                modifier = Modifier
                    .size(44.dp)
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
    imageBytes: ByteArray?,
    xpEarned: Int,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CreamWhite, LightBlush, CreamWhite)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(CrimsonRed.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(64.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text("Review Posted!", color = DeepMaroon, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Text("Thanks for reviewing $dishName", color = WarmMaroon, fontSize = 15.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("You earned", color = WarmMaroon, fontSize = 14.sp)
                    Text("+$xpEarned XP", color = CrimsonRed, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(WarmMaroon, CrimsonRed, WarmMaroon)),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun calculateXP(rating: Float, comment: String, tags: Set<String>, hasPhoto: Boolean = true): Int {
    var xp = 10
    if (hasPhoto) xp += 5
    if (comment.length > 50) xp += 10
    xp += tags.size * 2
    return xp
}
