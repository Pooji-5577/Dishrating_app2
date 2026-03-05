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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.Comment
import com.example.smackcheck2.model.CommentsUiState
import com.example.smackcheck2.ui.theme.appColors

/**
 * Screen that displays nested comments for a rating.
 *
 * @param uiState            Current state of the comments.
 * @param currentUserId      ID of the logged-in user (for showing delete option).
 * @param onNavigateBack     Navigate back.
 * @param onSubmitComment    Submit a new comment (content text, optional parent comment id).
 * @param onDeleteComment    Delete a comment by its id.
 * @param onReplyClick       Set the comment being replied to.
 * @param onCancelReply      Clear the reply target.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    uiState: CommentsUiState,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onSubmitComment: (content: String, parentCommentId: String?) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onReplyClick: (Comment) -> Unit,
    onCancelReply: () -> Unit
) {
    val colors = appColors()
    var commentText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Comments",
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
        },
        bottomBar = {
            CommentInputBar(
                text = commentText,
                onTextChange = { commentText = it },
                isSubmitting = uiState.isSubmitting,
                replyingTo = uiState.replyingTo,
                onCancelReply = onCancelReply,
                onSend = {
                    if (commentText.isNotBlank()) {
                        onSubmitComment(commentText.trim(), uiState.replyingTo?.id)
                        commentText = ""
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.Primary)
                }
            }

            uiState.errorMessage != null -> {
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
            }

            uiState.comments.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet. Be the first!",
                        fontSize = 16.sp,
                        color = colors.TextSecondary
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            currentUserId = currentUserId,
                            isReply = false,
                            onReplyClick = onReplyClick,
                            onDeleteClick = onDeleteComment
                        )

                        // Render nested replies
                        if (comment.replies.isNotEmpty()) {
                            comment.replies.forEach { reply ->
                                CommentItem(
                                    comment = reply,
                                    currentUserId = currentUserId,
                                    isReply = true,
                                    onReplyClick = onReplyClick,
                                    onDeleteClick = onDeleteComment
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    currentUserId: String,
    isReply: Boolean,
    onReplyClick: (Comment) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val colors = appColors()
    val startPadding = if (isReply) 48.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.userName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.Primary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Username and timestamp
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatCommentTimestamp(comment.createdAt),
                        fontSize = 12.sp,
                        color = colors.TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Content
                Text(
                    text = comment.content,
                    fontSize = 14.sp,
                    color = colors.TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action row: reply and delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onReplyClick(comment) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Reply",
                            tint = colors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reply",
                            fontSize = 12.sp,
                            color = colors.TextTertiary
                        )
                    }

                    if (comment.userId == currentUserId) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onDeleteClick(comment.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = colors.Error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                fontSize = 12.sp,
                                color = colors.Error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isSubmitting: Boolean,
    replyingTo: Comment?,
    onCancelReply: () -> Unit,
    onSend: () -> Unit
) {
    val colors = appColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.CardBackground)
            .imePadding()
    ) {
        // Replying-to chip
        if (replyingTo != null) {
            HorizontalDivider(color = colors.Divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Replying to @${replyingTo.userName}",
                    fontSize = 13.sp,
                    color = colors.Primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCancelReply,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel reply",
                        tint = colors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = colors.Divider)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Add a comment...",
                        color = colors.TextTertiary
                    )
                },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.TextPrimary,
                    unfocusedTextColor = colors.TextPrimary,
                    focusedBorderColor = colors.Primary,
                    unfocusedBorderColor = colors.Divider,
                    focusedContainerColor = colors.Surface,
                    unfocusedContainerColor = colors.Surface,
                    cursorColor = colors.Primary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = colors.Primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) colors.Primary else colors.TextTertiary
                    )
                }
            }
        }
    }
}

/**
 * Formats a timestamp (epoch millis) into a human-readable relative string.
 */
private fun formatCommentTimestamp(epochMillis: Long): String {
    if (epochMillis == 0L) return ""
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val diff = now - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        days < 30 -> "${days / 7}w"
        else -> "${days / 30}mo"
    }
}
