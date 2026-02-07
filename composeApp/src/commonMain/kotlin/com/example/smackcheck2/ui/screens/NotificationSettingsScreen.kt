package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.NotificationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = appColors()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.TextPrimary
                        )
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Push Notifications Section
                Text(
                    text = "Push Notifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingSwitch(
                    title = "Enable Push Notifications",
                    description = "Receive notifications on your device",
                    checked = uiState.settings.pushEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(pushEnabled = enabled) }
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Email Notifications Section
                Text(
                    text = "Email Notifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingSwitch(
                    title = "Enable Email Notifications",
                    description = "Receive notifications via email",
                    checked = uiState.settings.emailEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(emailEnabled = enabled) }
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Notification Types Section
                Text(
                    text = "Notification Types",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val notificationsEnabled = uiState.settings.pushEnabled

                SettingSwitch(
                    title = "New Followers",
                    description = "When someone follows you",
                    checked = uiState.settings.newFollowerNotif,
                    enabled = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(newFollowerNotif = enabled) }
                    }
                )

                SettingSwitch(
                    title = "New Likes",
                    description = "When someone likes your review",
                    checked = uiState.settings.newLikeNotif,
                    enabled = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(newLikeNotif = enabled) }
                    }
                )

                SettingSwitch(
                    title = "New Comments",
                    description = "When someone comments on your review",
                    checked = uiState.settings.newCommentNotif,
                    enabled = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(newCommentNotif = enabled) }
                    }
                )

                SettingSwitch(
                    title = "Achievements",
                    description = "When you earn a badge or level up",
                    checked = uiState.settings.achievementNotif,
                    enabled = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(achievementNotif = enabled) }
                    }
                )

                SettingSwitch(
                    title = "Weekly Digest",
                    description = "Summary of your week's activity",
                    checked = uiState.settings.weeklyDigest,
                    enabled = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(weeklyDigest = enabled) }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = appColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (enabled) colors.TextPrimary else colors.TextSecondary
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = colors.TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
