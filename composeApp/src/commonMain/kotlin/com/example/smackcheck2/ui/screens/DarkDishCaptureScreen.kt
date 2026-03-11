package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
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
import kotlinx.coroutines.delay

/**
 * Dark themed Dish Capture Screen with AI detection and manual-entry fallback.
 *
 * When the simulated AI returns a low-confidence result (< 0.5) or no dish
 * is detected, a fallback card is shown with:
 *   • "Add Dish Manually" button → navigates to ManualDishEntryScreen
 *   • "Skip" button             → navigates back
 *
 * A high-confidence result shows the standard confirmation card where the
 * user can edit the detected name and proceed to rate the dish.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkDishCaptureScreen(
    onNavigateBack: () -> Unit,
    onImageCaptured: (imageUri: String, dishName: String) -> Unit,
    onAddManually: (imageUri: String) -> Unit = {}   // Fallback: open manual entry
) {
    val themeColors = appColors()
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var detectedDishName by remember { mutableStateOf<String?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var flashMode by remember { mutableStateOf(FlashMode.Auto) }
    var showConfirmation by remember { mutableStateOf(false) }

    // ── AI confidence tracking ──────────────────────────────────────────
    var aiConfidence by remember { mutableStateOf(0f) }
    var showFallback by remember { mutableStateOf(false) }  // true when AI fails
    
    // Simulate AI detection when image is captured
    LaunchedEffect(selectedImageUri) {
        if (selectedImageUri != null && detectedDishName == null && !showFallback) {
            isAnalyzing = true
            delay(2000) // Simulate AI processing

            // ── Simulate confidence score (0.0 – 1.0) ──
            // In a real app this comes from the ML model output
            val confidence = (0..100).random() / 100f
            aiConfidence = confidence

            if (confidence >= 0.5f) {
                // High confidence → show detected dish name
                detectedDishName = listOf(
                    "Butter Chicken",
                    "Margherita Pizza",
                    "Caesar Salad",
                    "Grilled Salmon",
                    "Pad Thai",
                    "Beef Burger",
                    "Sushi Platter",
                    "Pasta Carbonara"
                ).random()
                editedName = detectedDishName ?: ""
                showConfirmation = true
            } else {
                // Low confidence or no match → show fallback UI
                showFallback = true
            }
            isAnalyzing = false
        }
    }
    
    Scaffold(
        containerColor = themeColors.Background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Capture Dish",
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
                actions = {
                    if (selectedImageUri == null) {
                        IconButton(onClick = { 
                            flashMode = when (flashMode) {
                                FlashMode.Auto -> FlashMode.On
                                FlashMode.On -> FlashMode.Off
                                FlashMode.Off -> FlashMode.Auto
                            }
                        }) {
                            Icon(
                                imageVector = when (flashMode) {
                                    FlashMode.Auto -> Icons.Default.FlashAuto
                                    FlashMode.On -> Icons.Default.FlashOn
                                    FlashMode.Off -> Icons.Default.FlashOff
                                },
                                contentDescription = "Flash",
                                tint = themeColors.TextPrimary
                            )
                        }
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
            if (selectedImageUri == null) {
                // Camera View / Capture Mode
                CameraCaptureView(
                    onCaptureClick = {
                        // Simulate image capture
                        selectedImageUri = "captured_image_${(0..999999).random()}"
                    },
                    onGalleryClick = {
                        // Simulate gallery selection
                        selectedImageUri = "gallery_image_${(0..999999).random()}"
                    }
                )
            } else {
                // Image Preview with AI Detection or Fallback
                ImagePreviewWithAI(
                    imageUri = selectedImageUri!!,
                    isAnalyzing = isAnalyzing,
                    detectedDishName = if (isEditingName) editedName else detectedDishName,
                    showConfirmation = showConfirmation,
                    showFallback = showFallback,
                    aiConfidence = aiConfidence,
                    isEditingName = isEditingName,
                    editedName = editedName,
                    onEditedNameChange = { editedName = it },
                    onEditClick = { isEditingName = true },
                    onConfirmEdit = {
                        isEditingName = false
                        detectedDishName = editedName
                    },
                    onCancelEdit = {
                        isEditingName = false
                        editedName = detectedDishName ?: ""
                    },
                    onRetake = {
                        selectedImageUri = null
                        detectedDishName = null
                        showConfirmation = false
                        showFallback = false
                        aiConfidence = 0f
                        isEditingName = false
                    },
                    onConfirm = {
                        onImageCaptured(selectedImageUri!!, detectedDishName ?: editedName)
                    },
                    onAddManually = {
                        // Navigate to the manual dish entry screen with the captured photo
                        onAddManually(selectedImageUri!!)
                    },
                    onSkip = onNavigateBack
                )
            }
        }
    }
}

private enum class FlashMode { Auto, On, Off }

@Composable
private fun CameraCaptureView(
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val themeColors = appColors()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Camera preview placeholder (dark gradient simulating camera)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            themeColors.Background,
                            themeColors.Surface,
                            themeColors.Background
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera frame indicator
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .border(
                            width = 2.dp,
                            color = themeColors.Primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = themeColors.TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Point camera at your dish",
                            color = themeColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // AI Detection badge
                Row(
                    modifier = Modifier
                        .background(
                            themeColors.Primary.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = themeColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI will detect dish name",
                        color = themeColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Bottom controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            themeColors.Background.copy(alpha = 0.9f),
                            themeColors.Background
                        )
                    )
                )
                .padding(vertical = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                IconButton(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(themeColors.Surface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = themeColors.TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Capture button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(themeColors.Primary, CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable { onCaptureClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Placeholder for symmetry
                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }
}

@Composable
private fun ImagePreviewWithAI(
    imageUri: String,
    isAnalyzing: Boolean,
    detectedDishName: String?,
    showConfirmation: Boolean,
    showFallback: Boolean,
    aiConfidence: Float,
    isEditingName: Boolean,
    editedName: String,
    onEditedNameChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    onAddManually: () -> Unit,
    onSkip: () -> Unit
) {
    val themeColors = appColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.Background)
    ) {
        // Image preview area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(themeColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            // Simulated image preview
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
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = themeColors.TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(120.dp)
                )
            }
            
            // AI analyzing overlay
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = themeColors.Primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = themeColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Detecting Dish...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // ── FALLBACK UI: shown when AI detection fails ────────────────────
        if (showFallback && !isAnalyzing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.Surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Warning icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                themeColors.Warning.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = themeColors.Warning,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fallback message
                    Text(
                        text = "We couldn't recognize this dish",
                        color = themeColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You can add the dish details manually\nor try taking another photo.",
                        color = themeColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Action buttons ─────────────────────────────────────
                    // Primary: Add Dish Manually
                    Button(
                        onClick = onAddManually,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Add Dish Manually",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary row: Retake + Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Retake photo
                        Button(
                            onClick = onRetake,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.SurfaceVariant,
                                contentColor = themeColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake", fontSize = 14.sp)
                        }

                        // Skip / close
                        Button(
                            onClick = onSkip,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.SurfaceVariant,
                                contentColor = themeColors.TextSecondary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skip", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Detection result and actions (high-confidence path)
        if (showConfirmation && !isAnalyzing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.Surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // AI detected badge
                    Row(
                        modifier = Modifier
                            .background(
                                themeColors.Primary.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = themeColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Detected",
                            color = themeColors.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Dish name display or edit
                    if (isEditingName) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = onEditedNameChange,
                            label = { Text("Dish Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColors.Primary,
                                unfocusedBorderColor = themeColors.TextSecondary.copy(alpha = 0.3f),
                                focusedLabelColor = themeColors.Primary,
                                unfocusedLabelColor = themeColors.TextSecondary,
                                cursorColor = themeColors.Primary,
                                focusedTextColor = themeColors.TextPrimary,
                                unfocusedTextColor = themeColors.TextPrimary
                            ),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = onConfirmEdit) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Confirm",
                                            tint = themeColors.Primary
                                        )
                                    }
                                    IconButton(onClick = onCancelEdit) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel",
                                            tint = themeColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onEditClick() }
                        ) {
                            Text(
                                text = detectedDishName ?: "Unknown Dish",
                                color = themeColors.TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = themeColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    if (!isEditingName) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to edit if incorrect",
                            color = themeColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRetake,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.Surface,
                                contentColor = themeColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retake", modifier = Modifier.padding(vertical = 4.dp))
                        }
                        
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.Primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Rate This Dish", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
