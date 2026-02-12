package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import com.example.smackcheck2.model.ProfileVisibility
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.PrivacySettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    viewModel: PrivacySettingsViewModel,
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
                title = { Text("Privacy & Security", color = colors.TextPrimary) },
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
                // Profile Visibility Section
                Text(
                    text = "Profile Visibility",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                ProfileVisibility.entries.forEach { visibility ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.settings.profileVisibility == visibility,
                                onClick = {
                                    viewModel.updateSetting { it.copy(profileVisibility = visibility) }
                                }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.settings.profileVisibility == visibility,
                            onClick = {
                                viewModel.updateSetting { it.copy(profileVisibility = visibility) }
                            }
                        )

                        Column(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = when (visibility) {
                                    ProfileVisibility.PUBLIC -> "Public"
                                    ProfileVisibility.FRIENDS_ONLY -> "Friends Only"
                                    ProfileVisibility.PRIVATE -> "Private"
                                },
                                fontSize = 16.sp,
                                color = colors.TextPrimary
                            )
                            Text(
                                text = when (visibility) {
                                    ProfileVisibility.PUBLIC -> "Anyone can see your profile"
                                    ProfileVisibility.FRIENDS_ONLY -> "Only your friends can see your profile"
                                    ProfileVisibility.PRIVATE -> "Only you can see your profile"
                                },
                                fontSize = 14.sp,
                                color = colors.TextSecondary
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // What Others Can See Section
                Text(
                    text = "What Others Can See",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                PrivacySwitch(
                    title = "Show Email",
                    description = "Display your email on your profile",
                    checked = uiState.settings.showEmail,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(showEmail = enabled) }
                    }
                )

                PrivacySwitch(
                    title = "Show Location",
                    description = "Display your city on reviews",
                    checked = uiState.settings.showLocation,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(showLocation = enabled) }
                    }
                )

                PrivacySwitch(
                    title = "Allow Tagging",
                    description = "Let others tag you in reviews",
                    checked = uiState.settings.allowTagging,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(allowTagging = enabled) }
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Data & Privacy Section
                Text(
                    text = "Data & Privacy",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                PrivacySwitch(
                    title = "Data Collection",
                    description = "Help improve the app by sharing usage data",
                    checked = uiState.settings.dataCollection,
                    onCheckedChange = { enabled ->
                        viewModel.updateSetting { it.copy(dataCollection = enabled) }
                    }
                )
            }
        }
    }
}

@Composable
private fun PrivacySwitch(
    title: String,
    description: String,
    checked: Boolean,
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
                color = colors.TextPrimary
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = colors.TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
