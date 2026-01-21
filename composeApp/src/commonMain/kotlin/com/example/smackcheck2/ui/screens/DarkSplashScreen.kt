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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors
import kotlinx.coroutines.delay

/**
 * Dark themed Splash Screen
 */
@Composable
fun DarkSplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    isAuthenticated: Boolean?
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
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
            .background(appColors().Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo with glow effect
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Glow background
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(scale.value)
                        .background(
                            color = appColors().Primary.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = "SmackCheck Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale.value),
                    tint = appColors().Primary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "SmackCheck",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = appColors().TextPrimary,
                modifier = Modifier.alpha(alpha.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Rate • Discover • Share",
                fontSize = 16.sp,
                color = appColors().TextSecondary,
                modifier = Modifier.alpha(alpha.value),
                letterSpacing = 2.sp
            )
        }
    }
}
