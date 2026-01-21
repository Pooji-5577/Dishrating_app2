package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.ui.components.StarRating
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.TextFieldShape
import com.example.smackcheck2.viewmodel.DishRatingViewModel

/**
 * Dish Rating Screen composable
 * Allows users to rate a dish with stars and add a comment
 * 
 * @param viewModel DishRatingViewModel instance
 * @param dishName Name of the dish being rated
 * @param imageUri URI of the dish image
 * @param onNavigateBack Callback to navigate back
 * @param onSubmitSuccess Callback when rating is submitted successfully
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishRatingScreen(
    viewModel: DishRatingViewModel,
    dishName: String,
    imageUri: String,
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(dishName, imageUri) {
        viewModel.initialize(dishName, imageUri)
    }
    
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate Dish") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dish image card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = CardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Dish name
            Text(
                text = dishName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Rating label
            Text(
                text = "How would you rate this dish?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Star rating
            StarRating(
                rating = uiState.rating,
                onRatingChange = viewModel::onRatingChange,
                starSize = 48.dp,
                isEditable = true,
                allowHalfRating = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rating value text
            Text(
                text = when {
                    uiState.rating == 0f -> "Tap to rate"
                    uiState.rating <= 1f -> "Poor"
                    uiState.rating <= 2f -> "Fair"
                    uiState.rating <= 3f -> "Good"
                    uiState.rating <= 4f -> "Very Good"
                    else -> "Excellent"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (uiState.rating > 0f) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Comment field
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = viewModel::onCommentChange,
                label = { Text("Add a comment (optional)") },
                placeholder = { Text("What did you think about this dish?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = TextFieldShape,
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Submit button
            Button(
                onClick = { viewModel.submitRating(onSubmitSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSubmitting && uiState.rating > 0f
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Rating",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
