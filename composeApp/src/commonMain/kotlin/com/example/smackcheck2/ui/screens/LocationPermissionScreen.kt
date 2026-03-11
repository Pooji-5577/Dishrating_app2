package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.smackcheck2.model.PermissionState
import com.example.smackcheck2.location.requestCurrentLocationDetection
import com.example.smackcheck2.ui.components.LocationPermissionUI

/**
 * Location Permission Screen composable
 * Requests and handles location permission
 * 
 * @param onNavigateBack Callback to navigate back
 * @param onPermissionGranted Callback when permission is granted
 * @param onPermissionDenied Callback when permission is denied
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPermissionScreen(
    onNavigateBack: () -> Unit,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.NotRequested) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Access") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Better Experience",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Enable location to discover nearby restaurants",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            LocationPermissionUI(
                permissionState = permissionState,
                onAllowClick = {
                    // Trigger real platform location permission + detection
                    requestCurrentLocationDetection()
                    permissionState = PermissionState.Granted
                    onPermissionGranted()
                },
                onDenyClick = {
                    permissionState = PermissionState.Denied
                    onPermissionDenied()
                },
                onOpenSettingsClick = {
                    // Platform-specific: open app settings
                    // On Android this is handled by AppLocationManager.openAppSettings()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
