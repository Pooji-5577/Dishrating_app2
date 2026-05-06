package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.ui.components.BadgeGrid
import com.example.smackcheck2.ui.components.LoadingState
import com.example.smackcheck2.ui.components.UserProgressDashboard
import com.example.smackcheck2.viewmodel.UserProgressViewModel

private val AchievementBg = androidx.compose.ui.graphics.Color(0xFFF6F6F6)

/**
 * Badges Screen composable
 * Displays all earned and available badges
 * 
 * @param viewModel UserProgressViewModel instance
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    viewModel: UserProgressViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        containerColor = AchievementBg,
        topBar = {
            TopAppBar(
                title = { Text("Badges") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues),
                    message = "Loading badges..."
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AchievementBg)
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Earned badges section
                    val earnedBadges = uiState.badges.filter { it.isEarned }
                    val unearnedBadges = uiState.badges.filter { !it.isEarned }

                    if (earnedBadges.isNotEmpty()) {
                        Text(
                            text = "Earned Badges (${earnedBadges.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        BadgeGrid(
                            badges = earnedBadges,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (unearnedBadges.isNotEmpty()) {
                        Text(
                            text = "Badges to Earn (${unearnedBadges.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        BadgeGrid(
                            badges = unearnedBadges,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Bottom spacing for smooth scrolling
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * User Progress Dashboard Screen composable
 * Shows XP progress, level, streak and badges
 * 
 * @param viewModel UserProgressViewModel instance
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToBadges Callback to navigate to badges screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProgressScreen(
    viewModel: UserProgressViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBadges: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        containerColor = AchievementBg,
        topBar = {
            TopAppBar(
                title = { Text("Your Progress") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues),
                    message = "Loading progress..."
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AchievementBg)
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress Dashboard
                    UserProgressDashboard(
                        currentXp = uiState.currentXp,
                        maxXp = uiState.maxXp,
                        level = uiState.level,
                        streakCount = uiState.streakCount
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Recent Badges
                    Text(
                        text = "Recent Badges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    val earnedBadges = uiState.badges.filter { it.isEarned }.take(6)
                    
                    BadgeGrid(
                        badges = earnedBadges,
                        modifier = Modifier.height(300.dp)
                    )
                    
                    // View all badges button
                    androidx.compose.material3.TextButton(
                        onClick = onNavigateToBadges,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text("View All Badges")
                    }
                }
            }
        }
    }
}
