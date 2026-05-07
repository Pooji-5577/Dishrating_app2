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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.theme.PlusJakartaSans
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
    val bg = Color(0xFFF6F6F6)
    val accent = Color(0xFF642223)
    val iconBg = accent.copy(alpha = 0.12f)
    val textPrimary = Color(0xFF1E1E1E)
    val textSecondary = Color(0xFF6F6F6F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SmackCheckWordmark(
                fontFamily = PlusJakartaSans(),
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                letterSpacing = 0.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A few permissions to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            PermissionRow(
                icon = Icons.Filled.LocationOn,
                title = "Location",
                description = "Detect nearby restaurants automatically",
                colors = colors,
                iconBg = iconBg,
                iconTint = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionRow(
                icon = Icons.Filled.CameraAlt,
                title = "Camera",
                description = "Take photos of your dishes",
                colors = colors,
                iconBg = iconBg,
                iconTint = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                description = "Get notified about likes, comments & more",
                colors = colors,
                iconBg = iconBg,
                iconTint = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            Spacer(modifier = Modifier.height(56.dp))

            Button(
                onClick = { step = 1 },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(
                    text = "Allow Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { onComplete(false) }) {
                Text(
                    text = "Maybe Later",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyLarge
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
    colors: ThemeColors,
    iconBg: Color,
    iconTint: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = iconBg,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                fontSize = 22.sp / 1.3f
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = textSecondary
            )
        }
    }
}
