package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.TextFieldShape

/**
 * Dish Preview Screen composable
 * Shows captured dish image and lets the user confirm or edit the dish name
 * before proceeding to the rating screen.
 *
 * @param imageUri URI of the captured/selected image
 * @param imageBytes Raw bytes of the captured image (for display)
 * @param detectedName AI-detected dish name, or null if not available
 * @param onNavigateBack Callback to navigate back
 * @param onConfirm Callback when user confirms the dish name
 * @param onRetake Callback when user wants to retake the photo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishPreviewScreen(
    imageUri: String,
    imageBytes: ByteArray? = null,
    detectedName: String? = null,
    onNavigateBack: () -> Unit,
    onConfirm: (String) -> Unit,
    onRetake: () -> Unit
) {
    // Editable dish name — start with AI-detected name or empty for user to type
    var dishName by remember { mutableStateOf(detectedName ?: "") }
    var isEditing by remember { mutableStateOf(detectedName == null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview Dish") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image preview card — show real image if bytes available
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                shape = CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBytes != null) {
                        ByteArrayImage(
                            imageBytes = imageBytes,
                            contentDescription = "Captured dish photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Fallback placeholder when bytes are not available
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Dish name section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (detectedName != null && !isEditing) {
                        // Show AI-detected label
                        Text(
                            text = "Detected Dish",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dishName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Edit button to correct the name
                        OutlinedButton(
                            onClick = { isEditing = true }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Name")
                        }
                    } else {
                        // Editable text field for dish name
                        Text(
                            text = "Enter Dish Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dishName,
                            onValueChange = { dishName = it },
                            placeholder = { Text("e.g. Margherita Pizza") },
                            singleLine = true,
                            shape = TextFieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Retake button
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retake")
                }

                // Confirm button — disabled if dish name is blank
                Button(
                    onClick = { onConfirm(dishName.trim()) },
                    enabled = dishName.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
