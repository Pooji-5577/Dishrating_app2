package com.example.smackcheck2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HelpBackground = Color(0xFFF6F6F6)
private val HelpCard = Color.White
private val HelpPrimary = Color(0xFF642223)

private data class FaqItem(
    val question: String,
    val answer: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HelpFaqScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContactSupport: () -> Unit
) {
    val faqs = remember {
        listOf(
            FaqItem(
                question = "How do I edit my profile details?",
                answer = "Go to Profile, tap Edit Profile, update your details, and tap Update Profile to save changes."
            ),
            FaqItem(
                question = "Why is my profile photo not updating?",
                answer = "A stable network connection is required while uploading. Wait for upload to finish before leaving the screen."
            ),
            FaqItem(
                question = "Can I change my username from this screen?",
                answer = "Username is shown on Edit Profile but can be restricted for edits based on your current account setup."
            ),
            FaqItem(
                question = "How can I reset my password?",
                answer = "From Edit Profile, use Change Password. You can also use password reset from login if needed."
            ),
            FaqItem(
                question = "How long does support take to reply?",
                answer = "Most support messages receive a response within 24 to 48 hours."
            )
        )
    }

    var expandedIndex by remember { mutableStateOf<Int?>(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & FAQ",
                        color = Color(0xFF2D2F2F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF2D2F2F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = HelpBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HelpCard, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Need a quick answer?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2F2F)
                )
                Text(
                    text = "Find common answers below or contact our support team directly.",
                    fontSize = 13.sp,
                    color = Color(0xFF5A5C5C)
                )
                Button(
                    onClick = onNavigateToContactSupport,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HelpPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Contact Support",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            faqs.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HelpCard, RoundedCornerShape(20.dp))
                        .clickable {
                            expandedIndex = if (expandedIndex == index) null else index
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.question,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2F2F)
                        )
                        Icon(
                            imageVector = if (expandedIndex == index) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = HelpPrimary
                        )
                    }

                    AnimatedVisibility(visible = expandedIndex == index) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = Color(0xFFE7E8E8)
                            )
                            Text(
                                text = item.answer,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF4A4C4C)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
