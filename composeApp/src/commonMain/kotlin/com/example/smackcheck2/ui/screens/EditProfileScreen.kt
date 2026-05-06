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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.platform.LocalImagePicker
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.EditProfileViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// ─── Screen-local colors ────────────────────────────────────────────────────
private val ScreenBackground = Color(0xFFFAFAFA)
private val FieldCardColor = Color.White
private val SectionLabelColor = Color(0xFF2D2F2F)
private val FieldTextColor = Color(0xFF333333)
private val FieldIconColor = Color(0xFF999999)
private val AccentMaroon = Color(0xFF642223)
private val CharCountColor = Color(0xFF642223)
private val CameraBadgeBg = Color(0xFF642223)
private val GradientStart = Color(0xFF9B2335)
private val GradientEnd = Color(0xFFBE3A50)

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavHome: () -> Unit = {},
    onNavMap: () -> Unit = {},
    onNavCamera: () -> Unit = {},
    onNavExplore: () -> Unit = {},
    onNavProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = appColors()
    val jakartaSans = PlusJakartaSans()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val imagePicker = LocalImagePicker.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar(
                message = "Profile updated successfully!",
                duration = SnackbarDuration.Short
            )
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // Extra bottom padding so the Update Profile button clears the
                // floating BottomNavBar (~88dp tall with its shadow/offset)
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SectionLabelColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Edit Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = jakartaSans,
                    color = SectionLabelColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Profile Photo ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickable {
                    scope.launch {
                        imagePicker?.pickFromGallery()?.let { result ->
                            val fileName = "profile_${Clock.System.now().toEpochMilliseconds()}.jpg"
                            viewModel.uploadProfilePhoto(result.bytes, fileName)
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(colors.SurfaceVariant),
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
                        Text(
                            text = uiState.name.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentMaroon
                        )
                    }

                    if (uiState.isUploadingPhoto) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // Camera badge (bottom-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(28.dp)
                        .background(CameraBadgeBg, CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "CHANGE PHOTO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = jakartaSans,
                color = AccentMaroon,
                letterSpacing = 0.8.sp,
                modifier = Modifier.clickable {
                    scope.launch {
                        imagePicker?.pickFromGallery()?.let { result ->
                            val fileName = "profile_${Clock.System.now().toEpochMilliseconds()}.jpg"
                            viewModel.uploadProfilePhoto(result.bytes, fileName)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Form Fields ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Full Name
                ProfileFieldSection(
                    label = "FULL NAME",
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    isError = uiState.nameError != null,
                    errorText = uiState.nameError,
                    jakartaSans = jakartaSans
                )

                // Username
                ProfileFieldSection(
                    label = "USERNAME",
                    value = uiState.username,
                    onValueChange = { viewModel.onUsernameChange(it) },
                    leadingIcon = Icons.Default.AlternateEmail,
                    jakartaSans = jakartaSans
                )

                // Bio
                ProfileFieldSection(
                    label = "BIO",
                    value = uiState.bio,
                    onValueChange = { viewModel.onBioChange(it) },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    charCount = uiState.bio.length,
                    maxCharCount = 160,
                    jakartaSans = jakartaSans
                )

                // Location
                ProfileFieldSection(
                    label = "LOCATION",
                    value = uiState.location,
                    onValueChange = { viewModel.onLocationChange(it) },
                    leadingIcon = Icons.Default.LocationOn,
                    jakartaSans = jakartaSans
                )

                // Email Address
                ProfileFieldSection(
                    label = "EMAIL ADDRESS",
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    leadingIcon = Icons.Default.Email,
                    jakartaSans = jakartaSans
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Change Password button
                OutlinedButton(
                    onClick = { /* TODO: Navigate to change password */ },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentMaroon)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentMaroon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHANGE PASSWORD",
                        color = AccentMaroon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = jakartaSans,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Update Profile button (gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(6.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        )
                        .clickable(enabled = !uiState.isSaving) {
                            viewModel.saveProfile(onSuccess = onNavigateBack)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Update Profile",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = jakartaSans
                        )
                    }
                }
            }
        }

        // Bottom nav bar
        com.example.smackcheck2.ui.components.BottomNavBar(
            selectedItem = com.example.smackcheck2.ui.components.NavItem.PROFILE,
            onHomeClick = onNavHome,
            onMapClick = onNavMap,
            onCameraClick = onNavCamera,
            onExploreClick = onNavExplore,
            onProfileClick = onNavProfile,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Snackbar host (above bottom nav)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        )
    }
}

// ─── Reusable field section ─────────────────────────────────────────────────

@Composable
private fun ProfileFieldSection(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    charCount: Int? = null,
    maxCharCount: Int? = null,
    isError: Boolean = false,
    errorText: String? = null,
    jakartaSans: androidx.compose.ui.text.font.FontFamily
) {
    Column {
        // Section label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = jakartaSans,
                color = SectionLabelColor,
                letterSpacing = 1.sp
            )
            if (charCount != null && maxCharCount != null) {
                Text(
                    text = "$charCount / $maxCharCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = jakartaSans,
                    color = CharCountColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Card-style input field
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = FieldCardColor,
            shadowElevation = 1.dp
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                isError = isError,
                leadingIcon = if (leadingIcon != null) {
                    {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = FieldIconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = FieldTextColor,
                    unfocusedTextColor = FieldTextColor,
                    cursorColor = AccentMaroon,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontFamily = jakartaSans,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Error text
        if (isError && errorText != null) {
            Text(
                text = errorText,
                fontSize = 11.sp,
                color = Color(0xFFE53935),
                fontFamily = jakartaSans,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
