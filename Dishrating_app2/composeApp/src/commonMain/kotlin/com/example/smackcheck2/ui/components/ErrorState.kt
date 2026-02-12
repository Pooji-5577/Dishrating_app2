package com.example.smackcheck2.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Error state dialog composable
 * 
 * @param message Error message to display
 * @param onRetry Callback for retry action
 * @param onDismiss Callback for dismiss/cancel action
 * @param title Optional dialog title
 * @param retryText Text for retry button
 * @param dismissText Text for dismiss button
 */
@Composable
fun ErrorStateDialog(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Error",
    retryText: String = "Retry",
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text(retryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
        modifier = modifier
    )
}

/**
 * Not a dish error dialog - shown when image is not recognized as food
 */
@Composable
fun NotADishErrorDialog(
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorStateDialog(
        title = "Not a Dish",
        message = "We couldn't recognize this image as a dish. Please try taking another photo or selecting a different image.",
        onRetry = onRetry,
        onDismiss = onCancel,
        retryText = "Try Again",
        dismissText = "Cancel",
        modifier = modifier
    )
}

/**
 * Network error dialog
 */
@Composable
fun NetworkErrorDialog(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorStateDialog(
        title = "Connection Error",
        message = "Unable to connect to the server. Please check your internet connection and try again.",
        onRetry = onRetry,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

/**
 * Generic confirmation dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
        modifier = modifier
    )
}
