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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
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

private val DeepMaroon = Color(0xFF3B1011)
private val WarmMaroon = Color(0xFF642223)
private val CrimsonRed = Color(0xFF9B2335)
private val RosePink = Color(0xFFBB5B5C)
private val CreamWhite = Color(0xFFFFF8F0)
private val WarmBeige = Color(0xFFF5EDE3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkDishCaptureScreen(
    viewModel: DishCaptureViewModel,
    imagePicker: ImagePicker?,
    onNavigateBack: () -> Unit,
    onImageCaptured: (imageUri: String, dishName: String, imageBytes: ByteArray?, allImages: List<CapturedImage>) -> Unit,
    onAddManually: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (uiState.showNotDishError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNotDishError() },
            containerColor = CreamWhite,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = CrimsonRed
                )
            },
            title = {
                Text(
                    text = "Not a Dish",
                    color = DeepMaroon,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "This image doesn't appear to be a food dish. Please take or select a photo of a dish to rate.",
                    color = WarmMaroon,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissNotDishError() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonRed,
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

    RequestCameraPermission(
        onPermissionResult = { granted ->
            if (granted) {
                coroutineScope.launch {
                    imagePicker?.captureImage()?.let { result ->
                        viewModel.onImageCaptured(result)
                    }
                }
            }
        }
    ) { requestCameraPermission ->

        Box(modifier = Modifier.fillMaxSize().background(DeepMaroon)) {
            if (uiState.imageUri == null) {
                CameraCaptureView(
                    onCaptureClick = { requestCameraPermission() },
                    onGalleryClick = {
                        coroutineScope.launch {
                            imagePicker?.pickFromGallery()?.let { result ->
                                viewModel.onImageCaptured(result)
                            }
                        }
                    },
                    onNavigateBack = onNavigateBack,
                    imagePickerAvailable = imagePicker != null
                )
            } else {
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
                    errorMessage = uiState.errorMessage,
                    canAddMoreImages = viewModel.canAddMoreImages(),
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
                    onEditedNameChange = { viewModel.onDishNameEdited(it) },
                    onEditClick = { viewModel.onEditClick() },
                    onConfirmEdit = { viewModel.confirmDishName() },
                    onCancelEdit = { viewModel.cancelEdit() },
                    onRetake = { viewModel.retake() },
                    onNavigateBack = onNavigateBack,
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
}

@Composable
private fun CameraCaptureView(
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onNavigateBack: () -> Unit,
    imagePickerAvailable: Boolean
) {
    var flashEnabled by remember { mutableStateOf(false) }

    // Infinite pulse animation for the bounding-box glow
    val infiniteTransition = rememberInfiniteTransition(label = "box_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Dark gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepMaroon, Color(0xFF1A0708), DeepMaroon)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Animated camera frame (pulsing dashed-style border)
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .drawWithContent {
                            drawContent()
                            // Simulated dashed bounding box using four corner brackets
                            val stroke = 4.dp.toPx()
                            val cornerLen = 40.dp.toPx()
                            val radius = 24.dp.toPx()
                            val color = androidx.compose.ui.graphics.Color(0xFF9B2335).copy(alpha = pulseAlpha)
                            val w = size.width
                            val h = size.height
                            // Top-left bracket
                            drawLine(color, androidx.compose.ui.geometry.Offset(radius, 0f), androidx.compose.ui.geometry.Offset(cornerLen, 0f), stroke)
                            drawLine(color, androidx.compose.ui.geometry.Offset(0f, radius), androidx.compose.ui.geometry.Offset(0f, cornerLen), stroke)
                            // Top-right bracket
                            drawLine(color, androidx.compose.ui.geometry.Offset(w - cornerLen, 0f), androidx.compose.ui.geometry.Offset(w - radius, 0f), stroke)
                            drawLine(color, androidx.compose.ui.geometry.Offset(w, radius), androidx.compose.ui.geometry.Offset(w, cornerLen), stroke)
                            // Bottom-left bracket
                            drawLine(color, androidx.compose.ui.geometry.Offset(radius, h), androidx.compose.ui.geometry.Offset(cornerLen, h), stroke)
                            drawLine(color, androidx.compose.ui.geometry.Offset(0f, h - cornerLen), androidx.compose.ui.geometry.Offset(0f, h - radius), stroke)
                            // Bottom-right bracket
                            drawLine(color, androidx.compose.ui.geometry.Offset(w - cornerLen, h), androidx.compose.ui.geometry.Offset(w - radius, h), stroke)
                            drawLine(color, androidx.compose.ui.geometry.Offset(w, h - cornerLen), androidx.compose.ui.geometry.Offset(w, h - radius), stroke)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = RosePink.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (imagePickerAvailable) "Point camera at your dish" else "Camera not available",
                            color = RosePink.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // AI badge
                Row(
                    modifier = Modifier
                        .background(CrimsonRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CrimsonRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI VISION ENABLED", color = CrimsonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        // Top bar: X (close) | AI badge | Flash toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(CrimsonRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI VISION ENABLED", color = CrimsonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Flash toggle top-right
            IconButton(onClick = { flashEnabled = !flashEnabled }) {
                Icon(
                    imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (flashEnabled) "Flash On" else "Flash Off",
                    tint = if (flashEnabled) Color(0xFFFFD700) else Color.White
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, DeepMaroon.copy(alpha = 0.95f), DeepMaroon)
                    )
                )
                .padding(bottom = 32.dp, top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onGalleryClick,
                        enabled = imagePickerAvailable,
                        modifier = Modifier
                            .size(52.dp)
                            .background(WarmMaroon.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("GALLERY", color = RosePink, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                }

                // Capture button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                        .clickable(enabled = imagePickerAvailable) { onCaptureClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, CircleShape)
                            .border(3.dp, Color(0xFFE0E0E0), CircleShape)
                    )
                }

                // Auto mode
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(WarmMaroon.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Auto", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AUTO", color = RosePink, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                }
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
    errorMessage: String?,
    canAddMoreImages: Boolean,
    onSelectImage: (Int) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onAddMoreFromGallery: () -> Unit,
    onEditedNameChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onRetake: () -> Unit,
    onNavigateBack: () -> Unit,
    onConfirm: () -> Unit,
    imagePicker: ImagePicker?
) {
    val displayedImage = allImages.getOrNull(selectedImageIndex)
    val displayedImageBytes = displayedImage?.bytes ?: imageBytes

    Box(modifier = Modifier.fillMaxSize().background(DeepMaroon)) {
        // Full-bleed dish photo with corner bracket accents
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayedImageBytes != null && displayedImageBytes.isNotEmpty()) {
                ByteArrayImage(
                    imageBytes = displayedImageBytes,
                    contentDescription = "Captured dish",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1A0708)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = RosePink.copy(alpha = 0.3f), modifier = Modifier.size(120.dp))
                }
            }
            // Decorative corner bracket accents on the photo
            if (!isAnalyzing && isAIDetected) {
                val bracketColor = CrimsonRed.copy(alpha = 0.7f)
                Box(modifier = Modifier.fillMaxSize().drawWithContent {
                    drawContent()
                    val s = 28.dp.toPx()
                    val stroke = 3.dp.toPx()
                    val inset = 16.dp.toPx()
                    // Top-left
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(inset, inset + s), androidx.compose.ui.geometry.Offset(inset, inset), stroke)
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset + s, inset), stroke)
                    // Top-right
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width - inset - s, inset), androidx.compose.ui.geometry.Offset(size.width - inset, inset), stroke)
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width - inset, inset), androidx.compose.ui.geometry.Offset(size.width - inset, inset + s), stroke)
                    // Bottom-left
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(inset, size.height - inset - s), androidx.compose.ui.geometry.Offset(inset, size.height - inset), stroke)
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(inset, size.height - inset), androidx.compose.ui.geometry.Offset(inset + s, size.height - inset), stroke)
                    // Bottom-right
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width - inset - s, size.height - inset), androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset), stroke)
                    drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset - s), androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset), stroke)
                })
            }
        }

        // Analyzing overlay
        if (isAnalyzing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CrimsonRed, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Detecting Dish...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Top bar overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            // AROMA PROFILE chip — top-left after close, shown when dish is detected
            if (isAIDetected && !isAnalyzing) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "AROMA PROFILE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isAIDetected) {
                Box(
                    modifier = Modifier
                        .background(CrimsonRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI VISION ENABLED", color = CrimsonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Image count badge
        if (allImages.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("${selectedImageIndex + 1}/${allImages.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Bottom card overlay
        if (showConfirmation && !isAnalyzing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Thumbnail strip
                if (allImages.size > 1) {
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

                // Detection result card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamWhite)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Error banner
                        if (errorMessage != null && !isAIDetected) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMessage, color = Color(0xFFE65100), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Status row: DISH RECOGNIZED + confidence badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAIDetected) "DISH RECOGNIZED" else "MANUAL ENTRY",
                                color = DeepMaroon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (isAIDetected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.horizontalGradient(listOf(CrimsonRed, WarmMaroon)),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${(confidence * 100).toInt()}% MATCH",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dish name or editing field
                        if (isEditingName) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = onEditedNameChange,
                                label = { Text("Dish Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonRed,
                                    unfocusedBorderColor = RosePink.copy(alpha = 0.3f),
                                    focusedLabelColor = CrimsonRed,
                                    unfocusedLabelColor = WarmMaroon,
                                    cursorColor = CrimsonRed,
                                    focusedTextColor = DeepMaroon,
                                    unfocusedTextColor = DeepMaroon
                                ),
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = onConfirmEdit) {
                                            Icon(Icons.Default.Check, contentDescription = "Confirm", tint = CrimsonRed)
                                        }
                                        IconButton(onClick = onCancelEdit) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = WarmMaroon)
                                        }
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = detectedDishName ?: "Unknown Dish",
                                color = DeepMaroon,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Cuisine and AI analysis
                        if (cuisine != null && isAIDetected && !isEditingName) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GEMINI AI ANALYSIS", color = DeepMaroon, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cuisine,
                                color = WarmMaroon,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = if (isEditingName) onCancelEdit else onEditClick,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarmMaroon.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = DeepMaroon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Name", color = DeepMaroon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f).height(48.dp),
                                enabled = (!detectedDishName.isNullOrBlank() && detectedDishName != "Unknown") ||
                                        (isEditingName && editedName.isNotBlank()),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonRed,
                                    contentColor = Color.White,
                                    disabledContainerColor = CrimsonRed.copy(alpha = 0.3f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Rate Now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Save to Journal — ghost button
                        OutlinedButton(
                            onClick = { /* Save to Journal – future feature */ },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarmMaroon.copy(alpha = 0.25f))
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = WarmMaroon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Journal", color = WarmMaroon, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom retake strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepMaroon)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onRetake() }
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(WarmMaroon.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("TAKE ANOTHER", color = RosePink, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            // handled by add more from gallery
                            onAddMoreFromGallery()
                        }
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(WarmMaroon.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("FROM GALLERY", color = RosePink, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    }
                }
            }
        }

        // AI IDENTIFIED banner — centered below top bar when dish is identified
        if (isAIDetected && !isAnalyzing && !detectedDishName.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 112.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        buildString {
                            append("AI IDENTIFIED  ")
                            append("\u201C")
                            append(detectedDishName)
                            append("\u201D")
                        },
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ENHANCED BY SMACKCHECK AI badge at bottom when confirmed
        if (isAIDetected && !isAnalyzing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showConfirmation) 8.dp else 96.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "ENHANCED BY SMACKCHECK AI",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // If still analyzing, show bottom controls for retake
        if (!showConfirmation && !isAnalyzing && uiState_imageExists(allImages)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(DeepMaroon)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onRetake() }
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(WarmMaroon.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("TAKE ANOTHER", color = RosePink, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onAddMoreFromGallery() }
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(WarmMaroon.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("FROM GALLERY", color = RosePink, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

private fun uiState_imageExists(allImages: List<CapturedImage>): Boolean = allImages.isNotEmpty()

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
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        images.forEachIndexed { index, image ->
            ImageThumbnail(
                image = image,
                isSelected = index == selectedIndex,
                onSelect = { onSelectImage(index) },
                onRemove = { onRemoveImage(index) }
            )
        }
        if (canAddMore && imagePicker != null) {
            AddMoreImageButton(onClick = onAddMoreFromGallery)
        }
    }
}

@Composable
private fun ImageThumbnail(
    image: CapturedImage,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(56.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) CrimsonRed else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
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

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(10.dp))
        }
    }
}

@Composable
private fun AddMoreImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(WarmMaroon.copy(alpha = 0.5f))
            .border(1.dp, CrimsonRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add more photos", tint = CrimsonRed, modifier = Modifier.size(24.dp))
    }
}
