package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import com.example.smackcheck2.model.CapturedImage
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.RequestCameraPermission
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.DishCaptureViewModel
import kotlinx.coroutines.launch

/**
 * Dark themed Dish Capture Screen with AI detection
 * Allows users to capture/upload dish photos and auto-detect dish name
 * Supports multiple image capture (up to 5 images)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkDishCaptureScreen(
    viewModel: DishCaptureViewModel,
    imagePicker: ImagePicker?,
    onNavigateBack: () -> Unit,
    onImageCaptured: (imageUri: String, dishName: String, imageBytes: ByteArray?, allImages: List<CapturedImage>) -> Unit,
    onAddManually: ((String) -> Unit)? = null
) {
    val themeColors = appColors()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // "Not a Dish" error modal
    if (uiState.showNotDishError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNotDishError() },
            containerColor = themeColors.Surface,
            iconContentColor = Color(0xFFE53935),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFE53935)
                )
            },
            title = {
                Text(
                    text = "Not a Dish",
                    color = themeColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "This image doesn't appear to be a food dish. Please take or select a photo of a dish to rate.",
                    color = themeColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissNotDishError() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.Primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        )
    }

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
                    allImages = uiState.allImages,
                    selectedImageIndex = uiState.selectedImageIndex,
                    imageBytes = uiState.imageBytes,
                    isAnalyzing = uiState.isAnalyzing,
                    detectedDishName = if (uiState.isEditingName) uiState.editedName else uiState.detectedDishName,
                    showConfirmation = uiState.showConfirmation,
                    isEditingName = uiState.isEditingName,
                    editedName = uiState.editedName,
                    isAIDetected = uiState.isAIDetected,
                    itemType = uiState.itemType,
                    confidence = uiState.detectionConfidence,
                    cuisine = uiState.detectedCuisine,
                    debugInfo = uiState.debugInfo,
                    errorMessage = uiState.errorMessage,
                    canAddMoreImages = viewModel.canAddMoreImages(),
                    remainingSlots = viewModel.remainingImageSlots(),
                    onSelectImage = { viewModel.selectImage(it) },
                    onRemoveImage = { viewModel.removeImage(it) },
                    onAddMoreFromGallery = {
                        coroutineScope.launch {
                            val remaining = viewModel.remainingImageSlots()
                            imagePicker?.pickMultipleFromGallery(remaining)?.let { results ->
                                viewModel.onAdditionalImagesSelected(results)
                            }
                        }
                    },
                    onAddMoreFromCamera = {
                        // This will be handled by permission request
                    },
                    onEditedNameChange = { viewModel.onDishNameEdited(it) },
                    onEditClick = { viewModel.onEditClick() },
                    onConfirmEdit = { viewModel.confirmDishName() },
                    onCancelEdit = { viewModel.cancelEdit() },
                    onRetake = { viewModel.retake() },
                    onConfirm = {
                        onImageCaptured(
                            uiState.imageUri!!,
                            viewModel.getFinalDishName(),
                            viewModel.getImageBytes(),
                            viewModel.getAllImages()
                        )
                    },
                    imagePicker = imagePicker
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
    allImages: List<CapturedImage>,
    selectedImageIndex: Int,
    imageBytes: ByteArray?,
    isAnalyzing: Boolean,
    detectedDishName: String?,
    showConfirmation: Boolean,
    isEditingName: Boolean,
    editedName: String,
    isAIDetected: Boolean,
    itemType: String,
    confidence: Float,
    cuisine: String?,
    debugInfo: String?,
    errorMessage: String?,
    canAddMoreImages: Boolean,
    remainingSlots: Int,
    onSelectImage: (Int) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onAddMoreFromGallery: () -> Unit,
    onAddMoreFromCamera: () -> Unit,
    onEditedNameChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    imagePicker: ImagePicker?
) {
    val themeColors = appColors()
    val coroutineScope = rememberCoroutineScope()
    
    // Get currently displayed image
    val displayedImage = allImages.getOrNull(selectedImageIndex)
    val displayedImageBytes = displayedImage?.bytes ?: imageBytes
    
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
            if (displayedImageBytes != null && displayedImageBytes.isNotEmpty()) {
                ByteArrayImage(
                    imageBytes = displayedImageBytes,
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
            
            // Image count badge (top right)
            if (allImages.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${selectedImageIndex + 1}/${allImages.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Image thumbnails strip (horizontal scrollable)
        if (allImages.isNotEmpty() && showConfirmation && !isAnalyzing) {
            ImageThumbnailStrip(
                images = allImages,
                selectedIndex = selectedImageIndex,
                canAddMore = canAddMoreImages,
                onSelectImage = onSelectImage,
                onRemoveImage = onRemoveImage,
                onAddMoreFromGallery = onAddMoreFromGallery,
                imagePicker = imagePicker
            )
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
                    // Outage / error message banner
                    if (errorMessage != null && !isAIDetected) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFFFF3E0),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFE65100),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

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
                            imageVector = if (isAIDetected) Icons.Default.AutoAwesome else Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isAIDetected) themeColors.Primary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                !isAIDetected -> "Manual Entry"
                                itemType == "beverage" -> {
                                    val confidencePercent = (confidence * 100).toInt()
                                    "Beverage ($confidencePercent%)"
                                }
                                else -> {
                                    val confidencePercent = (confidence * 100).toInt()
                                    "Food ($confidencePercent%)"
                                }
                            },
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
                                text = detectedDishName ?: "Unknown",
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
                            enabled = (!detectedDishName.isNullOrBlank() && detectedDishName != "Unknown") ||
                                    (isEditingName && editedName.isNotBlank()),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.Primary,
                                contentColor = Color.White,
                                disabledContainerColor = themeColors.Primary.copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
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

/**
 * Horizontal scrollable strip of image thumbnails with add more button
 */
@Composable
private fun ImageThumbnailStrip(
    images: List<CapturedImage>,
    selectedIndex: Int,
    canAddMore: Boolean,
    onSelectImage: (Int) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onAddMoreFromGallery: () -> Unit,
    imagePicker: ImagePicker?
) {
    val themeColors = appColors()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Photos (${images.size}/5)",
                color = themeColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (canAddMore) {
                Text(
                    text = "Tap + to add more",
                    color = themeColors.Primary,
                    fontSize = 11.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Thumbnails row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Image thumbnails
            images.forEachIndexed { index, image ->
                ImageThumbnail(
                    image = image,
                    isSelected = index == selectedIndex,
                    onSelect = { onSelectImage(index) },
                    onRemove = { onRemoveImage(index) }
                )
            }
            
            // Add more button
            if (canAddMore && imagePicker != null) {
                AddMoreImageButton(
                    onClick = onAddMoreFromGallery
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * Single image thumbnail with selection indicator and remove button
 */
@Composable
private fun ImageThumbnail(
    image: CapturedImage,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    val themeColors = appColors()
    
    Box(
        modifier = Modifier.size(64.dp)
    ) {
        // Thumbnail image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) themeColors.Primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onSelect() }
        ) {
            ByteArrayImage(
                imageBytes = image.bytes,
                contentDescription = "Dish photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Remove button (top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Button to add more images
 */
@Composable
private fun AddMoreImageButton(
    onClick: () -> Unit
) {
    val themeColors = appColors()
    
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(themeColors.Surface)
            .border(
                width = 1.dp,
                color = themeColors.Primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add more photos",
                tint = themeColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Add",
                color = themeColors.Primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
