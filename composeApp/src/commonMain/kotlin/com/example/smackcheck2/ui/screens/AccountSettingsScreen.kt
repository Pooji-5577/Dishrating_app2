package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.AccountSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = appColors()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }

    // Show messages
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(
                message = success,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account", color = colors.TextPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Current Email
            Text(
                text = "Email",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = uiState.email,
                fontSize = 14.sp,
                color = colors.TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Change Password
            SettingItem(
                title = "Change Password",
                description = "Update your account password",
                onClick = { showPasswordDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Change Email
            SettingItem(
                title = "Change Email",
                description = "Update your email address",
                onClick = { showEmailDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Delete Account
            SettingItem(
                title = "Delete Account",
                description = "Permanently delete your account and data",
                onClick = { viewModel.showDeleteConfirmation() },
                isDanger = true
            )
        }
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        PasswordChangeDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { newPassword ->
                viewModel.changePassword(newPassword) {
                    showPasswordDialog = false
                }
            },
            isLoading = uiState.isChangingPassword
        )
    }

    // Email Change Dialog
    if (showEmailDialog) {
        EmailChangeDialog(
            onDismiss = { showEmailDialog = false },
            onConfirm = { newEmail ->
                viewModel.changeEmail(newEmail) {
                    showEmailDialog = false
                }
            },
            isLoading = uiState.isChangingEmail
        )
    }

    // Delete Account Confirmation Dialog
    if (uiState.showDeleteConfirmation) {
        DeleteAccountDialog(
            onDismiss = { viewModel.hideDeleteConfirmation() },
            onConfirm = {
                viewModel.deleteAccount(onSuccess = onAccountDeleted)
            },
            isLoading = uiState.isDeletingAccount
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val colors = appColors()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDanger) colors.Error else colors.TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = colors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.TextSecondary
            )
        }
    }
}

@Composable
private fun PasswordChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        error = null
                    },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = null
                    },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let { errorText ->
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        newPassword.length < 8 -> error = "Password must be at least 8 characters"
                        newPassword != confirmPassword -> error = "Passwords do not match"
                        else -> onConfirm(newPassword)
                    }
                },
                enabled = !isLoading && newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Change")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmailChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean
) {
    var newEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Change Email") },
        text = {
            Column {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("New Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "You will need to verify your new email address",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newEmail) },
                enabled = !isLoading && newEmail.isNotBlank() && newEmail.contains("@")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Change")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    var confirmationText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Delete Account") },
        text = {
            Column {
                Text("This action cannot be undone. All your data will be permanently deleted.")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Type DELETE to confirm:", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading && confirmationText == "DELETE",
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
