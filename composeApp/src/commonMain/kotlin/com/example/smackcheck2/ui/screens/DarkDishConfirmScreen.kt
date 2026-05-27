package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.CapturedDishDraft
import com.example.smackcheck2.ui.components.ByteArrayImage
import com.example.smackcheck2.ui.components.SmackCheckWordmark
import com.example.smackcheck2.ui.theme.BrandRed
import com.example.smackcheck2.ui.theme.BrandRedDark
import com.example.smackcheck2.ui.theme.PlusJakartaSans

private val ConfirmDeepMaroon  = Color(0xFF3B1011)
private val ConfirmWarmMaroon  = BrandRedDark
private val ConfirmCrimsonRed  = BrandRed
private val ConfirmCreamWhite  = Color(0xFFFFF8F0)
private val ConfirmPageBg      = Color(0xFFFFF8F0)

@Composable
fun DarkDishConfirmScreen(
    dishName: String,
    imageBytes: ByteArray?,
    dishDrafts: List<CapturedDishDraft> = emptyList(),
    cuisine: String?,
    confidence: Float,
    onNavigateBack: () -> Unit,
    onRateNow: () -> Unit
) {
    val confirmItems = remember(dishDrafts, imageBytes, dishName, confidence) {
        if (dishDrafts.isNotEmpty()) {
            dishDrafts.map { ConfirmDishItem(it.dishName, it.image.bytes, it.confidence) }
        } else {
            listOf(ConfirmDishItem(dishName, imageBytes, confidence))
        }
    }
    val pagerState = rememberPagerState(pageCount = { confirmItems.size })
    val currentItem = confirmItems.getOrNull(pagerState.currentPage) ?: confirmItems.first()

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
            SmackCheckWordmark(
                fontFamily = PlusJakartaSans(),
                fontSize = 20.sp,
                letterSpacing = 0.sp
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
            HorizontalPager(state = pagerState) { page ->
                val item = confirmItems[page]
                if (item.imageBytes != null) {
                    ByteArrayImage(
                        imageBytes = item.imageBytes,
                        contentDescription = item.dishName,
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

            if (confirmItems.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${confirmItems.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(confirmItems.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                                .background(
                                    Color.White.copy(alpha = if (index == pagerState.currentPage) 0.95f else 0.55f),
                                    CircleShape
                                )
                        )
                    }
                }
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
                    if (currentItem.confidence > 0f) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(ConfirmCrimsonRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${(currentItem.confidence * 100).toInt()}% MATCH",
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
                    text = currentItem.dishName.ifBlank { dishName },
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

private data class ConfirmDishItem(
    val dishName: String,
    val imageBytes: ByteArray?,
    val confidence: Float
)
