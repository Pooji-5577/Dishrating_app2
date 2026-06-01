package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
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
import com.example.smackcheck2.model.GroupedReviewFormDraft
import com.example.smackcheck2.model.Restaurant
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.service.GroupedDishReviewItemRequest
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.components.StarRating
import com.example.smackcheck2.ui.theme.BrandRed
import com.example.smackcheck2.ui.theme.BrandRedDark
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.util.CurrencyHelper
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
    val initialPrice: String = "",
    val initialRating: Float = 0f
) {
    var name by mutableStateOf(initialName)
    var price by mutableStateOf(initialPrice)
    var dishRating by mutableFloatStateOf(initialRating)
}

private data class ReceiptPriceRow(
    val dishName: String,
    val price: Double?,
    val applied: Boolean
)

private enum class ReceiptSheet {
    Prompt,
    Applied,
    Review
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
    initialDraft: GroupedReviewFormDraft? = null,
    onNavigateBack: () -> Unit,
    onRatingComplete: () -> Unit,
    onSubmit: (GroupedReviewFormDraft) -> Unit,
    onDismissError: () -> Unit,
    onSearchRestaurants: (String) -> Unit,
    currencySymbol: String = "\u20B9 ",
    currencyCode: String? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val receiptAnalysisRepository = remember { ReceiptAnalysisRepository() }

    val editableDishes = remember(dishDrafts, initialDraft) {
        mutableStateListOf<EditableDishDraft>().also { list ->
            val restoredItems = initialDraft?.items.orEmpty()
            list.addAll(dishDrafts.mapIndexed { index, draft ->
                val restored = restoredItems.getOrNull(index)
                EditableDishDraft(
                    image = draft.image,
                    initialName = restored?.dishName ?: draft.dishName,
                    confidence = draft.confidence,
                    initialPrice = restored?.price?.toString().orEmpty(),
                    initialRating = restored?.rating ?: 0f
                )
            })
        }
    }

    var rating by remember { mutableFloatStateOf(initialDraft?.rating ?: 0f) }
    var comment by remember { mutableStateOf(initialDraft?.comment.orEmpty()) }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(initialDraft?.restaurant) }
    var selectedTags by remember { mutableStateOf(initialDraft?.tags?.toSet() ?: emptySet()) }
    var showRestaurantPicker by remember { mutableStateOf(false) }
    var restaurantSearchQuery by remember { mutableStateOf("") }
    var receiptBytes by remember { mutableStateOf<ByteArray?>(initialDraft?.receiptBytes) }
    var receiptSummary by remember { mutableStateOf<String?>(initialDraft?.receiptSummary) }
    var receiptItems by remember { mutableStateOf<List<String>>(initialDraft?.receiptItems ?: emptyList()) }
    var isAnalyzingReceipt by remember { mutableStateOf(false) }
    var activeCurrencySymbol by remember(currencySymbol) { mutableStateOf(initialDraft?.currencySymbol ?: currencySymbol) }
    var activeCurrencyCode by remember(currencyCode) { mutableStateOf(initialDraft?.currencyCode ?: currencyCode) }
    var receiptSheet by remember { mutableStateOf<ReceiptSheet?>(ReceiptSheet.Prompt) }
    var receiptPriceRows by remember { mutableStateOf<List<ReceiptPriceRow>>(emptyList()) }
    var reviewSuggestions by remember { mutableStateOf<List<ReceiptPriceSuggestion>>(emptyList()) }

    val tags = listOf("Highly Recommended", "Authentic", "Must Try", "Spicy", "Comfort Food", "Good Presentation", "Value for Money")

    fun applyDetectedCurrency(analysisCurrencyCode: String?, analysisCurrencySymbol: String?) {
        val detected = CurrencyHelper.forCode(analysisCurrencyCode)
        activeCurrencyCode = detected?.code ?: analysisCurrencyCode
        activeCurrencySymbol = detected?.symbol ?: analysisCurrencySymbol?.takeIf { it.isNotBlank() } ?: activeCurrencySymbol
    }

    suspend fun pickAndAnalyzeReceipt() {
        val receipt = imagePicker?.pickFromGallery() ?: return
        receiptBytes = receipt.bytes
        isAnalyzingReceipt = true
        receiptSheet = null
        try {
            receiptAnalysisRepository.analyzeReceipt(
                receiptBytes = receipt.bytes,
                dishNames = editableDishes.map { it.name },
                mimeType = receipt.mimeType
            ).onSuccess { analysis ->
                receiptSummary = analysis.summary
                receiptItems = analysis.rawItems
                applyDetectedCurrency(analysis.currencyCode, analysis.currencySymbol)
                val suggestions = mergeReceiptPriceSuggestions(
                    aiSuggestions = analysis.suggestions,
                    inferredSuggestions = inferReceiptPriceSuggestions(editableDishes, analysis.rawItems)
                )
                val autoSuggestions = suggestions.filter { it.confidence >= 0.60f }
                val appliedRows = applyReceiptPriceSuggestions(editableDishes, autoSuggestions)
                val appliedDishNames = appliedRows.filter { it.applied }.map { normalizedReceiptName(it.dishName) }.toSet()
                reviewSuggestions = suggestions.filter { normalizedReceiptName(it.dishName) !in appliedDishNames }
                receiptPriceRows = appliedRows

                receiptSheet = when {
                    appliedRows.any { it.applied } -> ReceiptSheet.Applied
                    reviewSuggestions.isNotEmpty() -> ReceiptSheet.Review
                    else -> {
                        snackbarHostState.showSnackbar("Receipt added, but no prices were detected")
                        null
                    }
                }
            }.onFailure {
                snackbarHostState.showSnackbar("Receipt added, but prices could not be read")
            }
        } finally {
            isAnalyzingReceipt = false
        }
    }

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
                    currencySymbol = activeCurrencySymbol,
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
                        pickAndAnalyzeReceipt()
                    }
                },
                enabled = imagePicker != null && !isAnalyzingReceipt,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (receiptBytes != null) GroupCrimson.copy(alpha = 0.28f) else GroupWarmMaroon.copy(alpha = 0.35f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (receiptBytes != null) Color.White else GroupCream,
                    disabledContainerColor = GroupCream.copy(alpha = 0.7f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (receiptBytes != null) GroupBlush else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnalyzingReceipt) {
                        CircularProgressIndicator(
                            color = GroupCrimson,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (receiptBytes != null) Icons.Default.Check else Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = GroupCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isAnalyzingReceipt -> "Reading receipt"
                            receiptBytes != null -> "Receipt added"
                            else -> "Add receipt"
                        },
                        color = GroupDeepMaroon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isAnalyzingReceipt -> "Finding item prices"
                            receiptBytes != null -> "Prices can be suggested from it"
                            else -> "Auto-fill dish prices"
                        },
                        color = GroupWarmMaroon,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (receiptBytes != null && !isAnalyzingReceipt) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Replace",
                        color = GroupCrimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    val currencyForSubmit = activeCurrencyCode
                    val items = editableDishes.map {
                        GroupedDishReviewItemRequest(
                            dishName = it.name,
                            image = it.image,
                            price = it.price.toDoubleOrNull(),
                            aiConfidence = it.confidence,
                            rating = it.dishRating,
                            currencyCode = currencyForSubmit
                        )
                    }
                    onSubmit(
                        GroupedReviewFormDraft(
                            dishDrafts = dishDrafts,
                            rating = rating,
                            comment = comment,
                            tags = selectedTags.toList(),
                            restaurant = selectedRestaurant,
                            items = items,
                            receiptBytes = receiptBytes,
                            receiptSummary = receiptSummary,
                            receiptItems = receiptItems,
                            currencySymbol = activeCurrencySymbol,
                            currencyCode = currencyForSubmit
                        )
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

        when (receiptSheet) {
            ReceiptSheet.Prompt -> {
                ReceiptPromptSheet(
                    onUpload = { coroutineScope.launch { pickAndAnalyzeReceipt() } },
                    onSkip = { receiptSheet = null },
                    enabled = imagePicker != null && !isAnalyzingReceipt
                )
            }
            ReceiptSheet.Applied -> {
                ReceiptAppliedSheet(
                    rows = receiptPriceRows,
                    currencySymbol = activeCurrencySymbol,
                    onLooksGood = { receiptSheet = null },
                    onEditPrices = { receiptSheet = null }
                )
            }
            ReceiptSheet.Review -> {
                ReceiptReviewSheet(
                    suggestions = reviewSuggestions,
                    currencySymbol = activeCurrencySymbol,
                    onApply = {
                        receiptPriceRows = applyReceiptPriceSuggestions(editableDishes, reviewSuggestions)
                        receiptSheet = ReceiptSheet.Applied
                    },
                    onSkip = { receiptSheet = null }
                )
            }
            null -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptPromptSheet(
    onUpload: () -> Unit,
    onSkip: () -> Unit,
    enabled: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onSkip,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Got the receipt?", color = GroupDeepMaroon, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Upload it once and SmackCheck will fill dish prices for you.",
                color = GroupWarmMaroon,
                fontSize = 14.sp
            )
            Button(
                onClick = onUpload,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GroupCrimson, contentColor = Color.White)
            ) {
                if (!enabled) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Upload receipt", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, GroupCrimson)
            ) {
                Text("Skip for now", color = GroupCrimson, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptAppliedSheet(
    rows: List<ReceiptPriceRow>,
    currencySymbol: String,
    onLooksGood: () -> Unit,
    onEditPrices: () -> Unit
) {
    val applied = rows.count { it.applied }
    ModalBottomSheet(
        onDismissRequest = onLooksGood,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (rows.size <= 1) "Price applied" else "$applied of ${rows.size} prices applied",
                color = GroupDeepMaroon,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.dishName, color = GroupDeepMaroon, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        row.price?.let { formatDisplayPrice(it, currencySymbol) } ?: "Not found",
                        color = if (row.applied) GroupCrimson else GroupWarmMaroon,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text("You can still edit anything before posting.", color = GroupWarmMaroon, fontSize = 13.sp)
            Button(
                onClick = onLooksGood,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GroupCrimson, contentColor = Color.White)
            ) {
                Text("Looks good", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onEditPrices,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, GroupCrimson)
            ) {
                Text("Edit prices", color = GroupCrimson, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptReviewSheet(
    suggestions: List<ReceiptPriceSuggestion>,
    currencySymbol: String,
    onApply: () -> Unit,
    onSkip: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onSkip,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Review matches", color = GroupDeepMaroon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("We found prices, but need your help matching them.", color = GroupWarmMaroon, fontSize = 14.sp)
            suggestions.forEach { suggestion ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(suggestion.dishName, color = GroupDeepMaroon, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDisplayPrice(suggestion.price, currencySymbol), color = GroupCrimson, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = onApply,
                enabled = suggestions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GroupCrimson, contentColor = Color.White)
            ) {
                Text("Apply selected prices", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip prices", color = GroupCrimson, fontWeight = FontWeight.SemiBold)
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

private fun formatDisplayPrice(price: Double, currencySymbol: String): String =
    currencySymbol + formatSuggestedPrice(price)

private fun applyReceiptPriceSuggestions(
    dishes: List<EditableDishDraft>,
    suggestions: List<ReceiptPriceSuggestion>
): List<ReceiptPriceRow> {
    val pricesByDish = mutableMapOf<Int, Double>()
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
            pricesByDish[matchIndex] = suggestion.price
        }
    }

    if (pricesByDish.isEmpty() && suggestions.isNotEmpty()) {
        suggestions.take(dishes.size).forEachIndexed { index, suggestion ->
            dishes[index].price = formatSuggestedPrice(suggestion.price)
            pricesByDish[index] = suggestion.price
        }
    }

    return dishes.mapIndexed { index, dish ->
        ReceiptPriceRow(
            dishName = dish.name,
            price = pricesByDish[index],
            applied = pricesByDish.containsKey(index)
        )
    }
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
