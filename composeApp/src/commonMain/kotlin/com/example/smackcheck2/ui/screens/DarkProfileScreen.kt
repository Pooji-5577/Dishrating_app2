package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.LocalThemeState
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToGames: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState = LocalThemeState.current
    val isDark = themeState.isDarkMode
    val colors = appColors()
    
    Scaffold(
        containerColor = colors.Background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profile",
                        color = colors.TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.Background
                )
            )
        }
    ) { paddingValues ->
        val user = uiState.user
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.Background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile photo placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(colors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.name?.firstOrNull()?.toString() ?: "J",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.Primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Name
            Text(
                text = user?.name ?: "John Doe",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Email
            Text(
                text = user?.email ?: "john.doe@example.com",
                fontSize = 14.sp,
                color = colors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItemThemed(value = "${user?.level ?: 5}", label = "Level")
                ProfileStatItemThemed(value = "${user?.xp ?: 450}", label = "XP")
                ProfileStatItemThemed(value = "${user?.streakCount ?: 7}", label = "Streak")
                ProfileStatItemThemed(value = "${user?.badges?.size ?: 2}", label = "Badges")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Games & Challenges Section - moved to top for visibility
            Text(
                text = "Games & Challenges",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToGames() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.Surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = colors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Play Games",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.TextPrimary
                        )
                        Text(
                            text = "Complete challenges to earn XP and badges",
                            fontSize = 12.sp,
                            color = colors.TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Settings Section
            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.Surface
                )
            ) {
                Column {
                    // Dark Mode Toggle
                    SettingsToggleItemThemed(
                        icon = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        title = "Dark Mode",
                        subtitle = if (isDark) "Currently using dark theme" else "Currently using light theme",
                        isChecked = isDark,
                        onCheckedChange = { themeState.toggleTheme() }
                    )
                    
                    HorizontalDivider(color = colors.Divider)
                    
                    // Notifications
                    SettingsItemThemed(
                        icon = Icons.Filled.Notifications,
                        title = "Notifications",
                        subtitle = "Manage push notifications",
                        onClick = { /* TODO */ }
                    )
                    
                    HorizontalDivider(color = colors.Divider)
                    
                    // Account Settings
                    SettingsItemThemed(
                        icon = Icons.Filled.Person,
                        title = "Account",
                        subtitle = "Manage account settings",
                        onClick = { /* TODO */ }
                    )
                    
                    HorizontalDivider(color = colors.Divider)
                    
                    // Privacy
                    SettingsItemThemed(
                        icon = Icons.Filled.Security,
                        title = "Privacy & Security",
                        subtitle = "Manage your data and privacy",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Edit Profile button
            Button(
                onClick = onEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.Primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sign Out button
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.TextPrimary
                )
            ) {
                Text(
                    text = "Sign Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SettingsItemThemed(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = appColors()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.TextSecondary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.TextSecondary
        )
    }
}

@Composable
private fun SettingsToggleItemThemed(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = appColors()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.TextSecondary
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.TextSecondary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun ProfileStatItemThemed(
    value: String,
    label: String
) {
    val colors = appColors()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.Primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.TextSecondary
        )
    }
}
