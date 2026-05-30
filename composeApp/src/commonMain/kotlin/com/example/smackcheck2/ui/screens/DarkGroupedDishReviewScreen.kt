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
import com.example.smackcheck2.data.repository.ReceiptPriceSuggestion
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
) {
    var name by mutableStateOf(initialName)
    var price by mutableStateOf("")
    var dishRating by mutableFloatStateOf(0f)
}

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
                    confidence = it.confidence
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
                    onPriceChange = { draft.price = cleanPrice(it) },
                    onRatingChange = { editableDishes[index].dishRating = it }
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
                            try {
                                receiptAnalysisRepository.analyzeReceipt(
                                    receiptBytes = receipt.bytes,
                                    dishNames = editableDishes.map { it.name },
                                    mimeType = receipt.mimeType
                                ).onSuccess { analysis ->
                                    receiptSummary = analysis.summary
                                    receiptItems = analysis.rawItems
                                    val suggestions = mergeReceiptPriceSuggestions(
                                        aiSuggestions = analysis.suggestions,
                                        inferredSuggestions = inferReceiptPriceSuggestions(editableDishes, analysis.rawItems)
                                    )
                                    val appliedCount = applyReceiptPriceSuggestions(editableDishes, suggestions)
                                    when {
                                        suggestions.isEmpty() -> snackbarHostState.showSnackbar("Receipt added, but no prices were detected")
                                        appliedCount == 0 -> snackbarHostState.showSnackbar("Receipt found prices, but none matched these dishes")
                                        else -> snackbarHostState.showSnackbar("Applied receipt prices to $appliedCount ${if (appliedCount == 1) "dish" else "dishes"}")
                                    }
                                }
                            } finally {
                                isAnalyzingReceipt = false
                            }
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
                                aiConfidence = it.confidence,
                                rating = it.dishRating
                            )
                        },
                        receiptBytes,
                        receiptSummary,
                        receiptItems
                    )
                },
                enabled = !isSubmitting && selectedRestaurant != null && editableDishes.all { it.name.isNotBlank() && it.dishRating > 0f },
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
    onPriceChange: (String) -> Unit,
    onRatingChange: (Float) -> Unit
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Rate dish", color = GroupWarmMaroon, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    StarRating(
                        rating = draft.dishRating,
                        onRatingChange = onRatingChange,
                        starSize = 28.dp,
                        isEditable = true,
                        allowHalfRating = true
                    )
                }
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

private fun applyReceiptPriceSuggestions(
    dishes: List<EditableDishDraft>,
    suggestions: List<ReceiptPriceSuggestion>
): Int {
    var applied = 0
    val usedDishIndexes = mutableSetOf<Int>()

    suggestions.forEach { suggestion ->
        val matchIndex = bestReceiptMatchIndex(
            dishes = dishes,
            usedDishIndexes = usedDishIndexes,
            suggestionName = suggestion.dishName
        )
        if (matchIndex != null) {
            dishes[matchIndex].price = formatSuggestedPrice(suggestion.price)
            usedDishIndexes += matchIndex
            applied++
        }
    }

    if (applied == 0 && suggestions.isNotEmpty()) {
        suggestions.take(dishes.size).forEachIndexed { index, suggestion ->
            if (dishes[index].price.isBlank()) {
                dishes[index].price = formatSuggestedPrice(suggestion.price)
                applied++
            }
        }
    }

    return applied
}

private fun mergeReceiptPriceSuggestions(
    aiSuggestions: List<ReceiptPriceSuggestion>,
    inferredSuggestions: List<ReceiptPriceSuggestion>
): List<ReceiptPriceSuggestion> {
    if (aiSuggestions.isEmpty()) return inferredSuggestions
    if (inferredSuggestions.isEmpty()) return aiSuggestions

    val merged = mutableListOf<ReceiptPriceSuggestion>()
    (aiSuggestions + inferredSuggestions).forEach { suggestion ->
        val normalized = normalizedReceiptName(suggestion.dishName)
        val existingIndex = merged.indexOfFirst {
            val existing = normalizedReceiptName(it.dishName)
            existing == normalized || existing.contains(normalized) || normalized.contains(existing)
        }
        if (existingIndex == -1) {
            merged += suggestion
        } else if (suggestion.confidence > merged[existingIndex].confidence) {
            merged[existingIndex] = suggestion
        }
    }
    return merged
}

private fun inferReceiptPriceSuggestions(
    dishes: List<EditableDishDraft>,
    receiptItems: List<String>
): List<ReceiptPriceSuggestion> {
    val lines = receiptItems.mapNotNull { line ->
        val price = parseReceiptLinePrice(line) ?: return@mapNotNull null
        normalizedReceiptName(line).takeIf { it.isNotBlank() }?.let { normalized ->
            ReceiptLine(line = line, normalized = normalized, price = price)
        }
    }
    if (lines.isEmpty()) return emptyList()

    val usedLineIndexes = mutableSetOf<Int>()
    val matched = dishes.mapNotNull { dish ->
        val dishName = normalizedReceiptName(dish.name)
        if (dishName.isBlank()) return@mapNotNull null
        val best = lines.mapIndexedNotNull { index, line ->
            if (index in usedLineIndexes) return@mapIndexedNotNull null
            val score = receiptLineMatchScore(dishName, line.normalized)
            if (score > 0) index to (line to score) else null
        }.maxByOrNull { it.second.second }

        if (best != null && best.second.second >= 18) {
            usedLineIndexes += best.first
            ReceiptPriceSuggestion(
                dishName = dish.name,
                price = best.second.first.price,
                confidence = (best.second.second / 100f).coerceIn(0f, 1f)
            )
        } else {
            null
        }
    }

    if (matched.isNotEmpty()) return matched

    return lines.take(dishes.size).mapIndexed { index, line ->
        ReceiptPriceSuggestion(
            dishName = dishes[index].name,
            price = line.price,
            confidence = 0.35f
        )
    }
}

private fun bestReceiptMatchIndex(
    dishes: List<EditableDishDraft>,
    usedDishIndexes: Set<Int>,
    suggestionName: String
): Int? {
    val suggestion = normalizedReceiptName(suggestionName)
    if (suggestion.isBlank()) return null

    val candidates = dishes.mapIndexedNotNull { index, draft ->
        if (index in usedDishIndexes) return@mapIndexedNotNull null
        val dish = normalizedReceiptName(draft.name)
        if (dish.isBlank()) return@mapIndexedNotNull null

        val score = when {
            dish == suggestion -> 100
            dish.contains(suggestion) || suggestion.contains(dish) -> 80
            else -> {
                val dishTokens = dish.split(" ").filter { it.length >= 3 }.toSet()
                val suggestionTokens = suggestion.split(" ").filter { it.length >= 3 }.toSet()
                val categoryScore = foodCategoryMatchScore(dishTokens, suggestionTokens)
                val overlap = dishTokens.intersect(suggestionTokens).size
                categoryScore + overlap * 20
            }
        }
        if (score > 0) index to score else null
    }

    return candidates.maxByOrNull { it.second }
        ?.takeIf { it.second >= 20 }
        ?.first
}

private fun receiptLineMatchScore(dishName: String, receiptLine: String): Int = when {
    dishName == receiptLine -> 100
    dishName.contains(receiptLine) || receiptLine.contains(dishName) -> 80
    else -> {
        val dishTokens = dishName.split(" ").filter { it.length >= 3 }.toSet()
        val lineTokens = receiptLine.split(" ").filter { it.length >= 3 }.toSet()
        foodCategoryMatchScore(dishTokens, lineTokens) + dishTokens.intersect(lineTokens).size * 25
    }
}

private fun foodCategoryMatchScore(leftTokens: Set<String>, rightTokens: Set<String>): Int =
    foodCategoryTokens.maxOfOrNull { category ->
        val leftMatches = leftTokens.any { it in category }
        val rightMatches = rightTokens.any { it in category }
        if (leftMatches && rightMatches) 35 else 0
    } ?: 0

private fun parseReceiptLinePrice(line: String): Double? {
    val matches = Regex("""(?:[$₹]\s*)?(\d+(?:\.\d{1,2})?)""")
        .findAll(line)
        .mapNotNull { it.groupValues.getOrNull(1)?.toDoubleOrNull() }
        .filter { it > 0.0 }
        .toList()
    return matches.lastOrNull()
}

private fun normalizedReceiptName(value: String): String =
    value.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .split(" ")
        .filter { token -> token.isNotBlank() && token !in receiptStopWords }
        .joinToString(" ")

private val receiptStopWords = setOf(
    "dish",
    "food",
    "with",
    "and",
    "the",
    "plate",
    "item"
)

private val foodCategoryTokens = listOf(
    setOf("bbq", "barbecue", "pizza", "pizzas"),
    setOf("tofu", "salad", "bowl", "greens"),
    setOf("burger", "cheeseburger", "hamburger", "sandwich"),
    setOf("pasta", "spaghetti", "penne", "farfalle", "noodle", "noodles"),
    setOf("biryani", "rice", "pulav", "pulao"),
    setOf("fries", "chips", "potato")
)

private data class ReceiptLine(
    val line: String,
    val normalized: String,
    val price: Double
)
