package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.PlusJakartaSans

enum class NavItem { HOME, MAP, CAMERA, EXPLORE, PROFILE }

@Composable
fun BottomNavBar(
    selectedItem: NavItem = NavItem.HOME,
    onHomeClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val jakartaSans = PlusJakartaSans()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The floating pill bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 30.dp,
                    shape = RoundedCornerShape(9999.dp),
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                ),
            shape = RoundedCornerShape(9999.dp),
            color = Color.White.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = Icons.Filled.Home,
                        label = "HOME",
                        isSelected = selectedItem == NavItem.HOME,
                        selectedColor = Color(0xFF9B2335),
                        onClick = onHomeClick,
                        fontFamily = jakartaSans
                    )
                }

                // Map
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = Icons.Filled.Map,
                        label = "MAP",
                        isSelected = selectedItem == NavItem.MAP,
                        selectedColor = Color(0xFF9B2335),
                        onClick = onMapClick,
                        fontFamily = jakartaSans
                    )
                }

                // Camera (center) - equal-weight placeholder so camera overlay lands at 50%
                Box(modifier = Modifier.weight(1f))

                // Explore
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = Icons.Filled.Explore,
                        label = "EXPLORE",
                        isSelected = selectedItem == NavItem.EXPLORE,
                        selectedColor = Color(0xFF9B2335),
                        onClick = onExploreClick,
                        fontFamily = jakartaSans
                    )
                }

                // Profile
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavBarItem(
                        icon = Icons.Filled.Person,
                        label = "PROFILE",
                        isSelected = selectedItem == NavItem.PROFILE,
                        selectedColor = Color(0xFF9B2335),
                        onClick = onProfileClick,
                        fontFamily = jakartaSans
                    )
                }
            }
        }

        // Center camera button - elevated above the bar
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-16).dp)
                .size(52.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF9B2335).copy(alpha = 0.3f),
                    spotColor = Color(0xFF9B2335).copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF9B2335),
                            Color(0xFFBE3A50)
                        )
                    )
                )
                .clickable { onCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Rate a dish",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    fontFamily: androidx.compose.ui.text.font.FontFamily
) {
    val color = if (isSelected) selectedColor else Color(0xFFA1A1AA)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = fontFamily,
            color = color,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}
