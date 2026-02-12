package com.example.smackcheck2.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.model.PermissionState
import com.example.smackcheck2.ui.theme.CardShape

/**
 * Location Permission UI composable
 * 
 * @param permissionState Current permission state
 * @param onAllowClick Callback when user clicks Allow
 * @param onDenyClick Callback when user clicks Deny
 * @param onOpenSettingsClick Callback when user clicks Open Settings (for permanently denied state)
 * @param modifier Modifier for the container
 */
@Composable
fun LocationPermissionUI(
    permissionState: PermissionState,
    onAllowClick: () -> Unit,
    onDenyClick: () -> Unit,
    onOpenSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Location Access",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = getPermissionMessage(permissionState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            when (permissionState) {
                is PermissionState.NotRequested, is PermissionState.Denied -> {
                    Button(
                        onClick = onAllowClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Location Access")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onDenyClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Not Now")
                    }
                }
                
                is PermissionState.PermanentlyDenied -> {
                    Button(
                        onClick = onOpenSettingsClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onDenyClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue Without Location")
                    }
                }
                
                is PermissionState.Granted -> {
                    Text(
                        text = "✓ Location access granted",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Get appropriate message based on permission state
 */
private fun getPermissionMessage(permissionState: PermissionState): String {
    return when (permissionState) {
        is PermissionState.NotRequested -> 
            "We need your location to help you find nearby restaurants and tag your dish ratings with location information. This helps other users discover great food near them."
        
        is PermissionState.Denied -> 
            "Location access was denied. You can still use the app, but restaurant suggestions won't be based on your location. Tap 'Allow' to enable location-based features."
        
        is PermissionState.PermanentlyDenied -> 
            "Location permission was permanently denied. To enable location-based features, please go to Settings and allow location access for SmackCheck."
        
        is PermissionState.Granted -> 
            "Location access is enabled. You'll receive personalized restaurant recommendations based on your location."
    }
}
