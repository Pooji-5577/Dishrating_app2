package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.components.ByteArrayImage

private val ConfirmDeepMaroon  = Color(0xFF3B1011)
private val ConfirmWarmMaroon  = Color(0xFF642223)
private val ConfirmCrimsonRed  = Color(0xFF9B2335)
private val ConfirmCreamWhite  = Color(0xFFFFF8F0)
private val ConfirmPageBg      = Color(0xFFFFF8F0)

@Composable
fun DarkDishConfirmScreen(
    dishName: String,
    imageBytes: ByteArray?,
    cuisine: String?,
    confidence: Float,
    onNavigateBack: () -> Unit,
    onRateNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConfirmPageBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF8F0))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = ConfirmDeepMaroon,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onNavigateBack() }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "SmackCheck",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = ConfirmCrimsonRed
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Hero image ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ConfirmCreamWhite),
            contentAlignment = Alignment.Center
        ) {
            if (imageBytes != null) {
                ByteArrayImage(
                    imageBytes = imageBytes,
                    contentDescription = dishName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = ConfirmWarmMaroon.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Dish info card ────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ConfirmCreamWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status row: DISH RECOGNIZED + confidence badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DISH RECOGNIZED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = ConfirmWarmMaroon.copy(alpha = 0.7f)
                    )
                    if (confidence > 0f) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(ConfirmCrimsonRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${(confidence * 100).toInt()}% MATCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Dish name
                Text(
                    text = dishName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ConfirmDeepMaroon
                )

                // AI analysis label + cuisine
                if (!cuisine.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = ConfirmCrimsonRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "SMACKCHECK AI ANALYSIS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = ConfirmCrimsonRed
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = cuisine,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = ConfirmWarmMaroon.copy(alpha = 0.8f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Rate Now button
                Button(
                    onClick = onRateNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConfirmCrimsonRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Rate Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
