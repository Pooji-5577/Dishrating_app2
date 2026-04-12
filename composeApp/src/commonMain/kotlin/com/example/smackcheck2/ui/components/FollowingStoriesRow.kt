package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.smackcheck2.model.UserSummary
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.ui.theme.appColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun FollowingStoriesRow(
    storyUsers: List<UserSummary>,
    currentUserAvatarUrl: String?,
    onAddStoryClick: () -> Unit,
    onStoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    val jakartaSans = PlusJakartaSans()

    Column(modifier = modifier) {
        // Section header
        Text(
            text = "Following",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = jakartaSans,
            color = Color(0xFF2D2F2F),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // "Your Story" item
            item(key = "your_story") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onAddStoryClick() }
                ) {
                    // Dashed circle border with plus icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color(0xFF642223),
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                                            0f
                                        )
                                    )
                                )
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(6.dp)
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFBB5B5C))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Story",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = "Your Story",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = jakartaSans,
                        color = Color(0xFF642223),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Followed users
            items(storyUsers, key = { it.id }) { user ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onStoryClick(user.id) }
                ) {
                    // Solid circle border with avatar
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .border(2.dp, Color(0xFF642223), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colors.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.profilePhotoUrl != null) {
                                KamelImage(
                                    resource = asyncPainterResource(user.profilePhotoUrl),
                                    contentDescription = user.name,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onFailure = {
                                        Text(
                                            text = user.name.firstOrNull()?.uppercase() ?: "?",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = colors.PrimaryDark
                                        )
                                    }
                                )
                            } else {
                                Text(
                                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.PrimaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = user.name.split(" ").firstOrNull() ?: user.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = jakartaSans,
                        color = Color(0xFF2D2F2F),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(64.dp)
                    )
                }
            }
        }
    }
}
