package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.platform.LocalImagePicker
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.EditProfileViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = appColors()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker
    val imagePicker = LocalImagePicker.current

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.TextPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveProfile(onSuccess = onNavigateBack)
                        },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", color = colors.Primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.Surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(colors.SurfaceVariant)
                    .clickable {
                        scope.launch {
                            imagePicker?.pickFromGallery()?.let { result ->
                                val fileName = "profile_${Clock.System.now().toEpochMilliseconds()}.jpg"
                                viewModel.uploadProfilePhoto(result.bytes, fileName)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.profilePhotoUrl != null) {
                    NetworkImage(
                        imageUrl = uiState.profilePhotoUrl!!,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add Photo",
                        modifier = Modifier.size(48.dp),
                        tint = colors.TextSecondary
                    )
                }

                // Upload indicator
                if (uiState.isUploadingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                // Camera overlay
                if (uiState.profilePhotoUrl != null && !uiState.isUploadingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap to change photo",
                fontSize = 14.sp,
                color = colors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name Field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = colors.Error
                        )
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.TextPrimary,
                    unfocusedTextColor = colors.TextPrimary,
                    focusedBorderColor = colors.Primary,
                    unfocusedBorderColor = colors.TextSecondary,
                    focusedLabelColor = colors.Primary,
                    unfocusedLabelColor = colors.TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bio Field
            OutlinedTextField(
                value = uiState.bio,
                onValueChange = { viewModel.onBioChange(it) },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text(
                        text = "${uiState.bio.length}/150",
                        color = colors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 12.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.TextPrimary,
                    unfocusedTextColor = colors.TextPrimary,
                    focusedBorderColor = colors.Primary,
                    unfocusedBorderColor = colors.TextSecondary,
                    focusedLabelColor = colors.Primary,
                    unfocusedLabelColor = colors.TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
