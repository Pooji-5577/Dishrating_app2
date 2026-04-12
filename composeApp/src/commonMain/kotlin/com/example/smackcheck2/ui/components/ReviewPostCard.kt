package com.example.smackcheck2.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.FeedItem
import com.example.smackcheck2.ui.theme.NewsreaderFontFamily
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.util.formatRelativeTime
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun ReviewPostCard(
    feedItem: FeedItem,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    val jakartaSans = PlusJakartaSans()
    val newsreader = NewsreaderFontFamily()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: avatar + name + badge + timestamp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // User avatar - 48dp
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.PrimaryDark.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (feedItem.userProfileImageUrl != null) {
                            KamelImage(
                                resource = asyncPainterResource(feedItem.userProfileImageUrl),
                                contentDescription = feedItem.userName,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                onFailure = {
                                    Text(
                                        text = feedItem.userName.firstOrNull()?.uppercase() ?: "?",
                                        color = colors.PrimaryDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            )
                        } else {
                            Text(
                                text = feedItem.userName.firstOrNull()?.uppercase() ?: "?",
                                color = colors.PrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    // Name + badge
                    Column {
                        Text(
                            text = feedItem.userName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = jakartaSans,
                            color = Color.Black
                        )
                        if (feedItem.roleBadge != null) {
                            Text(
                                text = feedItem.roleBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = jakartaSans,
                                color = Color(0xFF642223)
                            )
                        }
                    }
                }

                // Timestamp
                Text(
                    text = formatRelativeTime(feedItem.timestamp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = jakartaSans,
                    color = Color(0xFF642223)
                )
            }

            // Content section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Dish image - 256dp, full-bleed with rating chip overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(256.dp)
                ) {
                    if (feedItem.dishImageUrl != null) {
                        KamelImage(
                            resource = asyncPainterResource(feedItem.dishImageUrl),
                            contentDescription = feedItem.dishName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onFailure = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colors.SurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = colors.TextTertiary
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = colors.TextTertiary
                            )
                        }
                    }

                    // Star rating chip - top-right corner of photo
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Text(
                                text = String.format("%.1f", feedItem.rating),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = jakartaSans,
                                color = Color.White
                            )
                        }
                    }
                }

                // Dish name + restaurant + review
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    // Dish name - Newsreader Italic
                    Text(
                        text = feedItem.dishName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Italic,
                        fontFamily = newsreader,
                        color = Color(0xFF642223),
                        lineHeight = 30.sp
                    )

                    // "at Restaurant Name"
                    if (feedItem.restaurantName.isNotBlank()) {
                        Text(
                            text = feedItem.restaurantName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = jakartaSans,
                            color = Color(0xFF9B2335),
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Review text - Newsreader Regular 16sp, quoted
                    if (feedItem.comment.isNotBlank()) {
                        Text(
                            text = "\u201C${feedItem.comment}\u201D",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = newsreader,
                            color = Color(0xFF444444),
                            lineHeight = 26.sp
                        )
                    }
                }
            }

            // Action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeClick() }
                    ) {
                        Icon(
                            imageVector = if (feedItem.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (feedItem.isLiked) colors.PrimaryRed else Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                        if (feedItem.likesCount > 0) {
                            Text(
                                text = "${feedItem.likesCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = jakartaSans,
                                color = Color.Black
                            )
                        }
                    }

                    // Comment
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCommentClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                        if (feedItem.commentsCount > 0) {
                            Text(
                                text = "${feedItem.commentsCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = jakartaSans,
                                color = Color.Black
                            )
                        }
                    }

                    // Share
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp).clickable { onShareClick() }
                    )
                }

                // Bookmark
                Icon(
                    imageVector = if (feedItem.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (feedItem.isBookmarked) colors.PrimaryDark else Color.Black,
                    modifier = Modifier.size(18.dp).clickable { onBookmarkClick() }
                )
            }
        }
    }
}
