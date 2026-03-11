package com.example.smackcheck2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.ManualDishEntryViewModel

/**
 * Manual Dish Entry Screen — AI Fallback
 *
 * Shown when the AI dish recognition fails or has low confidence.
 * The captured photo is pre-filled and the user can type:
 *   • Dish name (required)
 *   • Restaurant name (required)
 *   • Optional description
 *   • Star rating (1–5)
 *
 * On submit the entry is saved to the Supabase "dishes" table.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualDishEntryScreen(
    viewModel: ManualDishEntryViewModel,
    imageUri: String,
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val themeColors = appColors()
    val uiState by viewModel.uiState.collectAsState()
    var showSuccess by remember { mutableStateOf(false) }

    // Initialize the ViewModel with the captured image
    LaunchedEffect(imageUri) {
        viewModel.initialize(imageUri)
    }

    // When the save succeeds, show a brief success animation then navigate
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            showSuccess = true
            kotlinx.coroutines.delay(1500)
            onSubmitSuccess()
        }
    }

    Scaffold(
        containerColor = themeColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Dish Manually",
                        color = themeColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Success overlay ────────────────────────────────────────────
            if (showSuccess) {
                SuccessOverlay(themeColors)
            } else {
                // ── Scrollable form ────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Captured photo preview card ────────────────────────
                    CapturedPhotoPreview(imageUri, themeColors)

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Info banner ────────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.Primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = themeColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "We couldn't recognize this dish.\nYou can add it manually below.",
                                color = themeColors.TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Form fields ───────────────────────────────────────
                    // Dish name (required)
                    FormField(
                        value = uiState.dishName,
                        onValueChange = { viewModel.onDishNameChange(it) },
                        label = "Dish Name *",
                        placeholder = "e.g. Butter Chicken",
                        error = uiState.dishNameError,
                        themeColors = themeColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Restaurant name (required)
                    FormField(
                        value = uiState.restaurantName,
                        onValueChange = { viewModel.onRestaurantNameChange(it) },
                        label = "Restaurant Name *",
                        placeholder = "e.g. Spice Garden",
                        error = uiState.restaurantNameError,
                        themeColors = themeColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description (optional)
                    FormField(
                        value = uiState.description,
                        onValueChange = { viewModel.onDescriptionChange(it) },
                        label = "Description (optional)",
                        placeholder = "Any details about the dish…",
                        singleLine = false,
                        maxLines = 3,
                        themeColors = themeColors
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Star rating ───────────────────────────────────────
                    Text(
                        text = "Rating",
                        color = themeColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StarRatingBar(
                        currentRating = uiState.rating,
                        onRatingChange = { viewModel.onRatingChange(it) },
                        themeColors = themeColors
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Error message ─────────────────────────────────────
                    if (uiState.errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = themeColors.Error.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = themeColors.Error,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Submit button ─────────────────────────────────────
                    Button(
                        onClick = { viewModel.submitDish(onSuccess = { /* handled via LaunchedEffect */ }) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.Primary,
                            contentColor = Color.White,
                            disabledContainerColor = themeColors.Primary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Saving…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Save Dish", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Reusable composables ───────────────────────────────────────────────────

/**
 * Displays the captured photo preview (placeholder since this is KMP).
 */
@Composable
private fun CapturedPhotoPreview(
    imageUri: String,
    themeColors: com.example.smackcheck2.ui.theme.ThemeColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.Surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = themeColors.TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Captured Photo",
                    color = themeColors.TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = imageUri,
                    color = themeColors.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Themed form text field matching the app's dark design.
 */
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    error: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    themeColors: com.example.smackcheck2.ui.theme.ThemeColors
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = themeColors.TextTertiary) },
            singleLine = singleLine,
            maxLines = maxLines,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.Primary,
                unfocusedBorderColor = themeColors.TextSecondary.copy(alpha = 0.3f),
                errorBorderColor = themeColors.Error,
                focusedLabelColor = themeColors.Primary,
                unfocusedLabelColor = themeColors.TextSecondary,
                errorLabelColor = themeColors.Error,
                cursorColor = themeColors.Primary,
                focusedTextColor = themeColors.TextPrimary,
                unfocusedTextColor = themeColors.TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        )
        // Inline validation error
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = themeColors.Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Interactive 5-star rating bar.
 */
@Composable
private fun StarRatingBar(
    currentRating: Float,
    onRatingChange: (Float) -> Unit,
    themeColors: com.example.smackcheck2.ui.theme.ThemeColors
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val isSelected = i <= currentRating
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) themeColors.StarYellow.copy(alpha = 0.2f)
                        else themeColors.Surface
                    )
                    .clickable { onRatingChange(i.toFloat()) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star $i",
                    tint = if (isSelected) themeColors.StarYellow else themeColors.TextTertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (currentRating > 0) "${currentRating.toInt()}/5" else "Tap to rate",
            color = if (currentRating > 0) themeColors.TextPrimary else themeColors.TextTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Full-screen success overlay shown after a dish is saved.
 */
@Composable
private fun SuccessOverlay(
    themeColors: com.example.smackcheck2.ui.theme.ThemeColors
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.Background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 2 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = themeColors.Success,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Dish Saved!",
                    color = themeColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your dish has been added successfully.",
                    color = themeColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
