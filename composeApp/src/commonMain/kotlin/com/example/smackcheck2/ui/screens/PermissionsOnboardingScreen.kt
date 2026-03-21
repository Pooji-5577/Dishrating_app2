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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.smackcheck2.platform.RequestCameraPermission
import com.example.smackcheck2.platform.RequestLocationPermission
import com.example.smackcheck2.platform.RequestNotificationPermission
import com.example.smackcheck2.ui.theme.ThemeColors
import com.example.smackcheck2.ui.theme.appColors

/**
 * Full-screen permissions onboarding overlay shown on first app launch.
 * Requests Location → Camera → Notifications in sequence.
 *
 * @param onComplete Called when all permissions are resolved.
 *                   [locationGranted] is true if location was granted.
 */
@Composable
fun PermissionsOnboardingScreen(
    onComplete: (locationGranted: Boolean) -> Unit
) {
    // step 0 = show UI, step 1 = request location, step 2 = request camera, step 3 = request notif
    var step by remember { mutableIntStateOf(0) }
    var locationGranted by remember { mutableIntStateOf(-1) } // -1 = pending

    val colors = appColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(colors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SmackCheck",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A few permissions to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            PermissionRow(
                icon = Icons.Filled.LocationOn,
                title = "Location",
                description = "Detect nearby restaurants automatically",
                colors = colors
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionRow(
                icon = Icons.Filled.CameraAlt,
                title = "Camera",
                description = "Take photos of your dishes",
                colors = colors
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                description = "Get notified about likes, comments & more",
                colors = colors
            )

            Spacer(modifier = Modifier.height(56.dp))

            Button(
                onClick = { step = 1 },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.Primary)
            ) {
                Text(
                    text = "Allow Permissions",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.Background
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { onComplete(false) }) {
                Text(
                    text = "Maybe Later",
                    color = colors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Step 1: Request location
        if (step == 1) {
            RequestLocationPermission(
                onPermissionResult = { granted ->
                    locationGranted = if (granted) 1 else 0
                    step = 2
                }
            ) { requestPermission ->
                LaunchedEffect(Unit) { requestPermission() }
            }
        }

        // Step 2: Request camera
        if (step == 2) {
            RequestCameraPermission(
                onPermissionResult = { step = 3 }
            ) { requestPermission ->
                LaunchedEffect(Unit) { requestPermission() }
            }
        }

        // Step 3: Request notifications
        if (step == 3) {
            RequestNotificationPermission(
                onPermissionResult = { onComplete(locationGranted == 1) }
            ) { requestPermission ->
                LaunchedEffect(Unit) { requestPermission() }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    colors: ThemeColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = colors.Primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.Primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.TextSecondary
            )
        }
    }
}
