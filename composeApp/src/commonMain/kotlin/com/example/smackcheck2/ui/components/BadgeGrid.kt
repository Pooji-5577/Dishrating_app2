package com.example.smackcheck2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.model.Badge
import com.example.smackcheck2.ui.theme.BadgeShape

/**
 * Badge Grid composable for displaying earned badges
 * 
 * @param badges List of badges to display
 * @param modifier Modifier for the grid
 * @param columns Number of columns in the grid
 */
@Composable
fun BadgeGrid(
    badges: List<Badge>,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(badges) { badge ->
            BadgeItem(
                badge = badge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Individual badge item
 */
@Composable
fun BadgeItem(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    val icon = getBadgeIcon(badge.id)
    
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = BadgeShape,
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isEarned) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (badge.isEarned) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = badge.name,
                modifier = Modifier.size(36.dp),
                tint = if (badge.isEarned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
            
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (badge.isEarned) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Get icon for badge based on id
 */
private fun getBadgeIcon(badgeId: String): ImageVector {
    return when (badgeId) {
        "1" -> Icons.Filled.Restaurant // First Bite
        "2" -> Icons.Filled.Star // Foodie
        "3" -> Icons.Filled.Explore // Explorer
        "4" -> Icons.Filled.EmojiEvents // Critic
        "5" -> Icons.Filled.LocalFireDepartment // Streak Master
        "6" -> Icons.Filled.ThumbUp // Social Butterfly
        else -> Icons.Filled.EmojiEvents
    }
}
