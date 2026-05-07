package com.example.smackcheck2.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay

/**
 * Splash Screen composable
 * Shows app logo with animation and navigates based on auth state after 2 seconds
 * 
 * @param onNavigateToLogin Callback to navigate to login screen
 * @param onNavigateToHome Callback to navigate to home screen
 * @param isAuthenticated Whether the user is authenticated
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    isAuthenticated: Boolean?
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Animate logo appearance
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
    }
    
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated != null) {
            delay(2000)
            if (isAuthenticated) {
                onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = "SmackCheck Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SmackCheckWordmark(
                modifier = Modifier.alpha(alpha.value),
                fontFamily = PlusJakartaSans(),
                fontSize = 32.sp,
                letterSpacing = 0.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Rate. Discover. Share.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
