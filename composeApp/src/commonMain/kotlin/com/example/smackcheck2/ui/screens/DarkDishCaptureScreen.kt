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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.RequestCameraPermission
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.DishCaptureViewModel
import kotlinx.coroutines.launch

/**
 * Dark themed Dish Capture Screen with AI detection
 * Allows users to capture/upload dish photos and auto-detect dish name
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkDishCaptureScreen(
    viewModel: DishCaptureViewModel,
    imagePicker: ImagePicker?,
    onNavigateBack: () -> Unit,
    onImageCaptured: (imageUri: String, dishName: String, imageBytes: ByteArray?) -> Unit
) {
    val themeColors = appColors()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Wrap with camera permission request
    RequestCameraPermission(
        onPermissionResult = { granted ->
            if (granted) {
                // Permission granted, launch camera
                coroutineScope.launch {
                    imagePicker?.captureImage()?.let { result ->
                        viewModel.onImageCaptured(result)
                    }
                }
            }
        }
    ) { requestCameraPermission ->
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
                if (uiState.imageUri == null) {
                    // Camera View / Capture Mode
                    CameraCaptureView(
                        onCaptureClick = {
                            // Request camera permission - camera will launch when granted
                            requestCameraPermission()
                        },
                        onGalleryClick = {
                            coroutineScope.launch {
                                imagePicker?.pickFromGallery()?.let { result ->
                                    viewModel.onImageCaptured(result)
                                }
                            }
                        },
                        imagePickerAvailable = imagePicker != null
                    )
                } else {
                // Image Preview with AI Detection
                ImagePreviewWithAI(
                    imageBytes = uiState.imageBytes,
                    isAnalyzing = uiState.isAnalyzing,
                    detectedDishName = if (uiState.isEditingName) uiState.editedName else uiState.detectedDishName,
                    showConfirmation = uiState.showConfirmation,
                    isEditingName = uiState.isEditingName,
                    editedName = uiState.editedName,
                    isAIDetected = uiState.isAIDetected,
                    confidence = uiState.detectionConfidence,
                    cuisine = uiState.detectedCuisine,
                    debugInfo = uiState.debugInfo,
                    onEditedNameChange = { viewModel.onDishNameEdited(it) },
                    onEditClick = { viewModel.onEditClick() },
                    onConfirmEdit = { viewModel.confirmDishName() },
                    onCancelEdit = { viewModel.cancelEdit() },
                    onRetake = { viewModel.retake() },
                    onConfirm = {
                        onImageCaptured(
                            uiState.imageUri!!,
                            viewModel.getFinalDishName(),
                            viewModel.getImageBytes()
                        )
                    }
                )
            }
        }
    }
    } // End RequestCameraPermission
}

@Composable
private fun CameraCaptureView(
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit,
    imagePickerAvailable: Boolean
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
                            text = if (imagePickerAvailable) "Point camera at your dish" else "Camera not available",
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
                    enabled = imagePickerAvailable,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (imagePickerAvailable) themeColors.Surface else themeColors.Surface.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = if (imagePickerAvailable) themeColors.TextPrimary else themeColors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Capture button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (imagePickerAvailable) themeColors.Primary else themeColors.Primary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = imagePickerAvailable) { onCaptureClick() },
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
    imageBytes: ByteArray?,
    isAnalyzing: Boolean,
    detectedDishName: String?,
    showConfirmation: Boolean,
    isEditingName: Boolean,
    editedName: String,
    isAIDetected: Boolean,
    confidence: Float,
    cuisine: String?,
    debugInfo: String?,
    onEditedNameChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
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
            // Display image from bytes
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                ByteArrayImage(
                    imageBytes = imageBytes,
                    contentDescription = "Captured dish",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback placeholder
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

        // Detection result and actions
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
                                if (isAIDetected) themeColors.Primary.copy(alpha = 0.15f)
                                else Color.Gray.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAIDetected) themeColors.Primary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAIDetected) {
                                val confidencePercent = (confidence * 100).toInt()
                                "AI Detected ($confidencePercent%)"
                            } else "Manual Entry",
                            color = if (isAIDetected) themeColors.Primary else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Show cuisine if detected
                    if (cuisine != null && isAIDetected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cuisine,
                            color = themeColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Show debug info when detection fails
                    if (!isAIDetected && debugInfo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Debug: $debugInfo",
                            color = Color(0xFFFF6B6B),
                            fontSize = 10.sp,
                            maxLines = 3
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
