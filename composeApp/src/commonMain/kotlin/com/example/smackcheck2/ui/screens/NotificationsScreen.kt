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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.notifications.NotificationEventType
import com.example.smackcheck2.notifications.NotificationRecord
import com.example.smackcheck2.notifications.NotificationViewModel

/**
 * Notifications Screen
 *
 * Displays the user's notifications with unread indicators,
 * mark-as-read functionality, and navigation to related content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateTo: (screen: String, params: Map<String, String>) -> Unit = { _, _ -> }
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Refresh on screen open
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications")
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Mark all read",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Read All", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading && notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No notifications yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "When you get likes, comments, or earn points,\nthey'll show up here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    items(notifications, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                val target = viewModel.handleNotificationTap(notification)
                                if (target != null) {
                                    onNavigateTo(target.screen, target.params)
                                }
                            }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ─── Notification Card ───────────────────────────────────────────

@Composable
private fun NotificationCard(
    notification: NotificationRecord,
    onClick: () -> Unit
) {
    val eventType = NotificationEventType.fromValue(notification.eventType)
    val iconData = getNotificationIcon(eventType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconData.backgroundColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconData.icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconData.backgroundColor
                )
            }

            Spacer(Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTimeAgo(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // Unread indicator
            if (!notification.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

// ─── Icon Mapping ────────────────────────────────────────────────

private data class NotificationIconData(
    val icon: ImageVector,
    val backgroundColor: Color
)

private fun getNotificationIcon(eventType: NotificationEventType?): NotificationIconData {
    return when (eventType) {
        NotificationEventType.REVIEW_LIKED -> NotificationIconData(
            icon = Icons.Filled.Favorite,
            backgroundColor = Color(0xFFE91E63) // Pink
        )
        NotificationEventType.DISH_COMMENT -> NotificationIconData(
            icon = Icons.Filled.ModeComment,
            backgroundColor = Color(0xFF2196F3) // Blue
        )
        NotificationEventType.POINTS_EARNED -> NotificationIconData(
            icon = Icons.Filled.Stars,
            backgroundColor = Color(0xFFFF9800) // Orange
        )
        NotificationEventType.CHALLENGE_COMPLETED -> NotificationIconData(
            icon = Icons.Filled.EmojiEvents,
            backgroundColor = Color(0xFF4CAF50) // Green
        )
        NotificationEventType.TRENDING_DISH -> NotificationIconData(
            icon = Icons.Filled.LocalFireDepartment,
            backgroundColor = Color(0xFFFF6B35) // SmackCheck brand
        )
        null -> NotificationIconData(
            icon = Icons.Filled.Notifications,
            backgroundColor = Color(0xFF9E9E9E) // Grey
        )
    }
}

// ─── Time Formatting ─────────────────────────────────────────────

/**
 * Simple relative time formatter.
 * Converts an ISO timestamp to a human-readable "time ago" string.
 */
private fun formatTimeAgo(isoTimestamp: String): String {
    return try {
        val instant = kotlinx.datetime.Instant.parse(isoTimestamp)
        val now = kotlinx.datetime.Clock.System.now()
        val duration = now - instant
        val minutes = duration.inWholeMinutes
        val hours = duration.inWholeHours
        val days = duration.inWholeDays

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            else -> "${days / 30}mo ago"
        }
    } catch (_: Exception) {
        ""
    }
}
