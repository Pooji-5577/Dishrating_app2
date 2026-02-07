package com.example.smackcheck2.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Animated XP gain notification that floats up and fades out
 */
@Composable
fun XpGainAnimation(
    xpAmount: Int,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }

    // Trigger animation end after duration
    LaunchedEffect(Unit) {
        delay(2000) // Show for 2 seconds
        visible = false
        delay(300) // Wait for fade out
        onAnimationComplete()
    }

    // Float up animation
    val offsetY by animateFloatAsState(
        targetValue = if (visible) -50f else -100f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "xp_float_animation"
    )

    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "xp_scale_animation"
    )

    // Fade animation
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.3f),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .scale(scale)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "XP",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+$xpAmount XP",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Level up celebration animation with confetti effect
 */
@Composable
fun LevelUpAnimation(
    newLevel: Int,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000) // Show for 3 seconds
        visible = false
        delay(500)
        onAnimationComplete()
    }

    // Pulsing scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "level_up_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.3f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.scale(pulseScale)
            ) {
                // Trophy icon with gradient background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA500)
                                )
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Level Up",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "LEVEL UP!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You reached level $newLevel",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Achievement unlock animation
 */
@Composable
fun AchievementUnlockAnimation(
    achievementTitle: String,
    achievementIcon: ImageVector,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        visible = false
        delay(500)
        onAnimationComplete()
    }

    // Slide in from top animation
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it }
        ) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF6A1B9A).copy(alpha = 0.95f),
            shadowElevation = 12.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = achievementIcon,
                        contentDescription = "Achievement",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Achievement Unlocked!",
                        fontSize = 14.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = achievementTitle,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Animated XP progress bar with smooth transitions
 */
@Composable
fun AnimatedXpProgressBar(
    currentXp: Int,
    maxXp: Int,
    level: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentXp.toFloat() / maxXp.toFloat()).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "xp_progress_animation"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Level $level",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$currentXp / $maxXp XP",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF8BC34A)
                            )
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}
