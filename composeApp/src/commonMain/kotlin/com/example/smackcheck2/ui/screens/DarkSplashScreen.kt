package com.example.smackcheck2.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.theme.NewsreaderFontFamily
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import smackcheck.composeapp.generated.resources.Res
import smackcheck.composeapp.generated.resources.smackcheck_logo_image_transparent

/**
 * Splash Screen – shown only after first-time login / registration.
 * Clean white background, centred fork+knife icon in a soft-pink circle,
 * elegant serif wordmark and spaced-out tagline.
 */
@Composable
fun DarkSplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    isAuthenticated: Boolean?,
    isDataReady: Boolean = false
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    var animationDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        animationDone = true
    }

    // Navigate once auth is known AND splash animation is done AND home data is ready.
    // For unauthenticated users we don't wait for data (go straight to login).
    LaunchedEffect(isAuthenticated, animationDone, isDataReady) {
        if (isAuthenticated == null) return@LaunchedEffect
        if (!animationDone) return@LaunchedEffect

        if (isAuthenticated) {
            // Wait until home data is loaded, then navigate
            if (isDataReady) {
                delay(300) // Small pause so the transition feels intentional
                onNavigateToHome()
            }
            // If not ready, keep showing splash until isDataReady triggers this again
        } else {
            delay(300)
            onNavigateToLogin()
        }
    }

    // Safety timeout: if auth or data never resolves, fall through after 6 seconds
    LaunchedEffect(Unit) {
        delay(6000)
        if (isAuthenticated == true && !isDataReady) {
            // Even without data, don't trap the user on splash forever
            onNavigateToHome()
        } else if (isAuthenticated == null) {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F0)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.smackcheck_logo_image_transparent),
                contentDescription = "SmackCheck Logo",
                modifier = Modifier
                    .size(94.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Elegant serif wordmark
            SmackCheckWordmark(
                modifier = Modifier.alpha(alpha.value),
                fontFamily = NewsreaderFontFamily(),
                fontSize = 38.sp,
                smackColor = Color(0xFF5A1A1A),
                checkColor = Color(0xFF5A1A1A),
                letterSpacing = 0.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tagline
            Text(
                text = "Rate \u2022 Discover \u2022 Share",
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans(),
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8A8A8A),
                modifier = Modifier.alpha(alpha.value),
                letterSpacing = 3.sp
            )
        }
    }
}
