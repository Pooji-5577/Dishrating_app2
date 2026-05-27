package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.data.repository.ReceiptAnalysisRepository
import com.example.smackcheck2.model.CapturedDishDraft
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.service.GroupedDishReviewItemRequest
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.components.StarRating
import com.example.smackcheck2.ui.theme.BrandRed
import com.example.smackcheck2.ui.theme.BrandRedDark
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private val GroupDeepMaroon = Color(0xFF3B1011)
private val GroupWarmMaroon = BrandRedDark
private val GroupCrimson = BrandRed
private val GroupCream = Color(0xFFFFF8F0)
private val GroupBlush = Color(0xFFFDE8E8)

private data class EditableDishDraft(
    val image: com.example.smackcheck2.model.CapturedImage,
    val initialName: String,
    val confidence: Float,
    var name: String,
    var price: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DarkGroupedDishReviewScreen(
    dishDrafts: List<CapturedDishDraft>,
    imagePicker: ImagePicker?,
    restaurants: List<Restaurant>,
    nearbyRestaurants: List<Restaurant>,
    searchedRestaurants: List<Restaurant>,
    isLoadingRestaurants: Boolean,
    isSearchingRestaurants: Boolean,
    isSubmitting: Boolean,
    showSuccess: Boolean,
    xpEarned: Int?,
    errorMessage: String?,
    onNavigateBack: () -> Unit,
    onRatingComplete: () -> Unit,
    onSubmit: (
        rating: Float,
        comment: String,
        tags: List<String>,
        restaurant: Restaurant?,
        items: List<GroupedDishReviewItemRequest>,
        receiptBytes: ByteArray?,
        receiptSummary: String?,
        receiptItems: List<String>
    ) -> Unit,
    onDismissError: () -> Unit,
    onSearchRestaurants: (String) -> Unit,
    currencySymbol: String = "\u20B9 "
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val receiptAnalysisRepository = remember { ReceiptAnalysisRepository() }

    val editableDishes = remember(dishDrafts) {
        mutableStateListOf<EditableDishDraft>().also { list ->
            list.addAll(dishDrafts.map {
                EditableDishDraft(
                    image = it.image,
                    initialName = it.dishName,
                    confidence = it.confidence,
                    name = it.dishName
                )
            })
        }
    }

    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showRestaurantPicker by remember { mutableStateOf(false) }
    var restaurantSearchQuery by remember { mutableStateOf("") }
    var receiptBytes by remember { mutableStateOf<ByteArray?>(null) }
    var receiptSummary by remember { mutableStateOf<String?>(null) }
    var receiptItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAnalyzingReceipt by remember { mutableStateOf(false) }

    val tags = listOf("Highly Recommended", "Authentic", "Must Try", "Spicy", "Comfort Food", "Good Presentation", "Value for Money")

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    if (showSuccess) {
        GroupedReviewSuccessScreen(
            dishCount = editableDishes.size,
            imageBytes = editableDishes.firstOrNull()?.image?.bytes,
            xpEarned = xpEarned ?: 0,
            onContinue = onRatingComplete
        )
        return
    }

    Scaffold(
        containerColor = GroupCream,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { SmackCheckWordmark(fontFamily = PlusJakartaSans(), fontSize = 18.sp, letterSpacing = 0.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GroupDeepMaroon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GroupCream)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Review ${editableDishes.size} dishes",
                color = GroupDeepMaroon,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            editableDishes.forEachIndexed { index, draft ->
                DishDraftCard(
                    index = index,
                    draft = draft,
                    currencySymbol = currencySymbol,
                    onNameChange = { draft.name = it },
                    onPriceChange = { draft.price = cleanPrice(it) }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showRestaurantPicker = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GroupBlush)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = GroupCrimson, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedRestaurant?.name ?: "Where did you eat this?",
                            color = GroupDeepMaroon,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        selectedRestaurant?.city?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = GroupWarmMaroon, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = GroupWarmMaroon)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Shared visit rating", color = GroupWarmMaroon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                StarRating(rating = rating, onRatingChange = { rating = it }, starSize = 42.dp, isEditable = true, allowHalfRating = true)
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Shared review") },
                placeholder = { Text("How was this meal?") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = reviewTextFieldColors()
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val selected = tag in selectedTags
                    FilterChip(
                        selected = selected,
                        onClick = { selectedTags = if (selected) selectedTags - tag else selectedTags + tag },
                        label = { Text(tag, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GroupCrimson,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val receipt = imagePicker?.pickFromGallery()
                        if (receipt != null) {
                            receiptBytes = receipt.bytes
                            isAnalyzingReceipt = true
                            receiptAnalysisRepository.analyzeReceipt(
                                receiptBytes = receipt.bytes,
                                dishNames = editableDishes.map { it.name },
                                mimeType = receipt.mimeType
                            ).onSuccess { analysis ->
                                receiptSummary = analysis.summary
                                receiptItems = analysis.rawItems
                                analysis.suggestions.forEach { suggestion ->
                                    editableDishes.firstOrNull { it.name.equals(suggestion.dishName, ignoreCase = true) }
                                        ?.let { it.price = formatSuggestedPrice(suggestion.price) }
                                }
                            }
                            isAnalyzingReceipt = false
                        }
                    }
                },
                enabled = imagePicker != null && !isAnalyzingReceipt,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(99.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GroupCrimson)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when {
                        isAnalyzingReceipt -> "Reading receipt..."
                        receiptBytes != null -> "Receipt added. Prices are editable"
                        else -> "Add receipt for price suggestions"
                    },
                    color = GroupDeepMaroon,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    onSubmit(
                        rating,
                        comment,
                        selectedTags.toList(),
                        selectedRestaurant,
                        editableDishes.map {
                            GroupedDishReviewItemRequest(
                                dishName = it.name,
                                image = it.image,
                                price = it.price.toDoubleOrNull(),
                                aiConfidence = it.confidence
                            )
                        },
                        receiptBytes,
                        receiptSummary,
                        receiptItems
                    )
                },
                enabled = !isSubmitting && rating > 0f && selectedRestaurant != null && editableDishes.all { it.name.isNotBlank() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GroupCrimson, contentColor = Color.White)
            ) {
                if (isSubmitting) Text("Posting...") else Text("Post grouped review", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (showRestaurantPicker) {
            ModalBottomSheet(
                onDismissRequest = { showRestaurantPicker = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                RestaurantPickerContent(
                    query = restaurantSearchQuery,
                    restaurants = restaurants,
                    nearbyRestaurants = nearbyRestaurants,
                    searchedRestaurants = searchedRestaurants,
                    isLoading = isLoadingRestaurants || isSearchingRestaurants,
                    onQueryChange = {
                        restaurantSearchQuery = it
                        onSearchRestaurants(it)
                    },
                    onSelect = {
                        selectedRestaurant = it
                        restaurantSearchQuery = it.name
                        showRestaurantPicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GroupedReviewSuccessScreen(
    dishCount: Int,
    imageBytes: ByteArray?,
    xpEarned: Int,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(GroupCream).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                modifier = Modifier.size(132.dp).clip(CircleShape).background(GroupBlush),
                contentAlignment = Alignment.Center
            ) {
                if (imageBytes != null) {
                    ByteArrayImage(
                        imageBytes = imageBytes,
                        contentDescription = "Grouped review",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = GroupCrimson, modifier = Modifier.size(56.dp))
                }
            }
            Text(
                text = "Posted $dishCount dishes",
                color = GroupDeepMaroon,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "+$xpEarned XP",
                color = GroupCrimson,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GroupCrimson, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DishDraftCard(
    index: Int,
    draft: EditableDishDraft,
    currencySymbol: String,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5EDE3)),
                contentAlignment = Alignment.Center
            ) {
                ByteArrayImage(
                    imageBytes = draft.image.bytes,
                    contentDescription = draft.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(22.dp).background(GroupCrimson, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    label = { Text("Dish name") },
                    leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null, tint = GroupCrimson) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = reviewTextFieldColors()
                )
                OutlinedTextField(
                    value = draft.price,
                    onValueChange = onPriceChange,
                    singleLine = true,
                    label = { Text("Price") },
                    prefix = { Text(currencySymbol) },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GroupCrimson) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = reviewTextFieldColors()
                )
            }
        }
    }
}

@Composable
private fun RestaurantPickerContent(
    query: String,
    restaurants: List<Restaurant>,
    nearbyRestaurants: List<Restaurant>,
    searchedRestaurants: List<Restaurant>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (Restaurant) -> Unit
) {
    val shown = remember(query, restaurants, nearbyRestaurants, searchedRestaurants) {
        val localMatches = if (query.isBlank()) nearbyRestaurants else restaurants.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true) ||
                it.cuisine.contains(query, ignoreCase = true)
        }
        (searchedRestaurants + localMatches).distinctBy { it.id }.take(12)
    }
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search restaurant") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = reviewTextFieldColors()
        )
        if (isLoading) Text("Searching...", color = GroupWarmMaroon)
        shown.forEach { restaurant ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onSelect(restaurant) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GroupCrimson)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(restaurant.name, color = GroupDeepMaroon, fontWeight = FontWeight.SemiBold)
                    if (restaurant.city.isNotBlank()) Text(restaurant.city, color = GroupWarmMaroon, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun reviewTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GroupCrimson,
    unfocusedBorderColor = GroupWarmMaroon.copy(alpha = 0.25f),
    focusedLabelColor = GroupCrimson,
    unfocusedLabelColor = GroupWarmMaroon,
    cursorColor = GroupCrimson,
    focusedTextColor = GroupDeepMaroon,
    unfocusedTextColor = GroupDeepMaroon
)

private fun cleanPrice(value: String): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot == -1) filtered else {
        filtered.take(firstDot + 1) + filtered.drop(firstDot + 1).replace(".", "")
    }
}

private fun formatSuggestedPrice(price: Double): String =
    if (price % 1.0 == 0.0) price.toInt().toString() else price.toString()
