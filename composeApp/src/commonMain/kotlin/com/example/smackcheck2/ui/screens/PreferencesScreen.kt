package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.data.repository.PreferencesRepository
import com.example.smackcheck2.model.NotificationSettings
import kotlinx.coroutines.launch

private val SettingsBg = Color(0xFFF6F6F6)
private val CardWhite = Color(0xFFFFFFFF)
private val DeepMaroon = Color(0xFF3B1011)
private val WarmMaroon = Color(0xFF642223)
private val CrimsonRed = Color(0xFF9B2335)
private val MutedGrey = Color(0xFF767777)
private val DividerGrey = Color(0xFFEAE0D8)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    preferencesRepository: PreferencesRepository?,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var pushEnabled by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(true) }
    var cuisineExpanded by remember { mutableStateOf(false) }

    val allCuisines = listOf("Japanese", "Italian", "Indian", "Asian", "Mexican", "French",
        "Chinese", "Thai", "American", "Mediterranean", "Korean")
    var selectedCuisines by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(Unit) {
        preferencesRepository?.let { repo ->
            val settings = repo.getAppPreferences()
            pushEnabled = settings.notificationSettings.pushEnabled
            locationEnabled = settings.privacySettings.showLocation
        }
    }

    Scaffold(
        containerColor = SettingsBg,
        topBar = {
            TopAppBar(
                title = { Text("Preferences", color = DeepMaroon, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepMaroon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsBg
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = DeepMaroon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Push Notifications", color = DeepMaroon, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = pushEnabled,
                                onCheckedChange = { newVal ->
                                    pushEnabled = newVal
                                    coroutineScope.launch {
                                        preferencesRepository?.saveNotificationSettings(
                                            NotificationSettings(pushEnabled = newVal)
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CrimsonRed,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MutedGrey.copy(alpha = 0.4f)
                                )
                            )
                        }
                        HorizontalDivider(color = DividerGrey)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = DeepMaroon, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Location Access", color = DeepMaroon, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = locationEnabled,
                                onCheckedChange = { newVal ->
                                    locationEnabled = newVal
                                    coroutineScope.launch {
                                        preferencesRepository?.let { repo ->
                                            val current = repo.getAppPreferences()
                                            repo.savePrivacySettings(
                                                current.privacySettings.copy(showLocation = newVal)
                                            )
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CrimsonRed,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MutedGrey.copy(alpha = 0.4f)
                                )
                            )
                        }
                        HorizontalDivider(color = DividerGrey)
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { cuisineExpanded = !cuisineExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = DeepMaroon, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Cuisine Preferences", color = DeepMaroon, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MutedGrey)
                            }
                            if (cuisineExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    allCuisines.forEach { cuisine ->
                                        val selected = selectedCuisines.contains(cuisine)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .border(
                                                    width = if (selected) 2.dp else 0.dp,
                                                    color = if (selected) CrimsonRed else Color.Transparent,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .background(Color(0x33642223))
                                                .clickable {
                                                    selectedCuisines = if (selected) selectedCuisines - cuisine else selectedCuisines + cuisine
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(cuisine, color = WarmMaroon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                if (selected) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(15.dp)
                                                            .background(WarmMaroon, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        cuisineExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmMaroon, contentColor = Color.White)
                                ) {
                                    Text("Save Preferences", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
