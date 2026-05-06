package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.User
import com.example.smackcheck2.platform.LocalImagePicker
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.theme.TextFieldShape
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.ProfileSetupViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel,
    currentUser: User?,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = appColors()
    val bg = Color(0xFFF6F6F6)
    val accent = Color(0xFF642223)
    val textPrimary = Color(0xFF1E1E1E)
    val textSecondary = Color(0xFF6F6F6F)
    val fieldBorder = Color(0xFFA79A94)
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val imagePicker = LocalImagePicker.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Set Up Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                fontSize = 48.sp / 1.5f
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose a username so others can find you",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profile photo picker
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F1F1))
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
                        tint = textSecondary
                    )
                }

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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add Photo (optional)",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Username field
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.AlternateEmail,
                        contentDescription = null,
                        tint = Color(0xFF5A4A43)
                    )
                },
                trailingIcon = when {
                    uiState.isCheckingUsername -> ({
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    })
                    uiState.usernameAvailable == true -> ({
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Username available",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    })
                    uiState.usernameAvailable == false -> ({
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Username taken",
                            tint = MaterialTheme.colorScheme.error
                        )
                    })
                    else -> null
                },
                isError = uiState.usernameError != null,
                supportingText = when {
                    uiState.usernameError != null -> { { Text(uiState.usernameError!!) } }
                    uiState.usernameAvailable == true -> { { Text("Available!", color = MaterialTheme.colorScheme.primary) } }
                    else -> null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.saveProfile { onComplete() }
                    }
                ),
                singleLine = true,
                shape = TextFieldShape,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = fieldBorder,
                    unfocusedBorderColor = fieldBorder,
                    focusedContainerColor = bg,
                    unfocusedContainerColor = bg,
                    focusedLabelColor = textSecondary,
                    unfocusedLabelColor = textSecondary,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = { viewModel.saveProfile { onComplete() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving && !uiState.isCheckingUsername && !uiState.isUploadingPhoto
                ,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip for now
            TextButton(onClick = onComplete) {
                Text(
                    text = "Skip for now",
                    color = textSecondary,
                    fontSize = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
