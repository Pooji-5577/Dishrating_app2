package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.model.UserProfileUiState
import com.example.smackcheck2.ui.theme.appColors

/**
 * Screen that displays another user's public profile.
 *
 * @param uiState           Current state of the user profile being viewed.
 * @param onNavigateBack    Navigate back to the previous screen.
 * @param onFollowClick     Toggle follow / unfollow for this user.
 * @param onFollowersClick  Navigate to the followers list.
 * @param onFollowingClick  Navigate to the following list.
 * @param onLikeClick       Like / unlike a rating in the feed.
 * @param onCommentClick    Open comments for a rating.
 * @param onShareClick      Share a rating.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    uiState: UserProfileUiState,
    onNavigateBack: () -> Unit,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onLikeClick: (FeedItem) -> Unit,
    onCommentClick: (FeedItem) -> Unit,
    onShareClick: (FeedItem) -> Unit
) {
    val colors = appColors()

    Scaffold(
        containerColor = colors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.user?.name ?: "",
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.Primary)
            }
            return@Scaffold
        }

        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage,
                    color = colors.Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Scaffold
        }

        val user = uiState.user ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ---- Profile header ----
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(colors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.firstOrNull()?.uppercase() ?: "",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.Primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    Text(
                        text = user.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.TextPrimary
                    )

                    // Bio
                    if (!user.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = user.bio,
                            fontSize = 14.sp,
                            color = colors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }

            // ---- Stats row ----
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    UserProfileStatItem(
                        value = "${user.followersCount}",
                        label = "Followers",
                        onClick = onFollowersClick
                    )
                    UserProfileStatItem(
                        value = "${user.followingCount}",
                        label = "Following",
                        onClick = onFollowingClick
                    )
                    UserProfileStatItem(
                        value = "${uiState.ratings.size}",
                        label = "Ratings",
                        onClick = null
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ---- Follow / Unfollow button ----
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    if (uiState.isFollowing) {
                        OutlinedButton(
                            onClick = onFollowClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isFollowLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.TextPrimary
                            )
                        ) {
                            if (uiState.isFollowLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.TextPrimary
                                )
                            } else {
                                Text(
                                    text = "Following",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onFollowClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isFollowLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.Primary,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isFollowLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Follow",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- Divider ----
            item {
                HorizontalDivider(color = colors.Divider)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ---- Section header ----
            item {
                Text(
                    text = "Recent Ratings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ---- Ratings list ----
            if (uiState.ratings.isEmpty()) {
                item {
                    Text(
                        text = "No ratings yet.",
                        fontSize = 14.sp,
                        color = colors.TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(uiState.ratings, key = { it.id }) { feedItem ->
                    com.example.smackcheck2.ui.components.ReviewPostCard(
                        feedItem = feedItem,
                        onLikeClick = { onLikeClick(feedItem) },
                        onCommentClick = { onCommentClick(feedItem) },
                        onShareClick = { onShareClick(feedItem) },
                        onBookmarkClick = { },
                        onUserClick = { },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileStatItem(
    value: String,
    label: String,
    onClick: (() -> Unit)?
) {
    val colors = appColors()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.Primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.TextSecondary
        )
    }
}
