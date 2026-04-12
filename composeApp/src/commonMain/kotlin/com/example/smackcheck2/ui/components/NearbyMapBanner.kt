package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.PlusJakartaSans

@Composable
fun NearbyMapBanner(
    restaurantCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jakartaSans = PlusJakartaSans()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF0C0F0F))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Map pin icon in glass circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$restaurantCount restaurants near you",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = jakartaSans,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "READY TO EXPLORE?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = jakartaSans,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.2.sp
                )
            }

            // Gradient "Explore Map" button
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF642223),
                                Color(0xFFFF7669)
                            )
                        )
                    )
            ) {
                Text(
                    text = "Explore Map",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = jakartaSans,
                    color = Color.White
                )
            }
        }
    }
}
