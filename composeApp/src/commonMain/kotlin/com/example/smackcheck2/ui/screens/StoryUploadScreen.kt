package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.repository.SocialRepository
import com.example.smackcheck2.data.repository.StorageRepository
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.ui.theme.PlusJakartaSans
import com.example.smackcheck2.ui.theme.appColors
import io.github.jan.supabase.auth.auth
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryUploadScreen(
    imagePicker: ImagePicker,
    onNavigateBack: () -> Unit,
    onStoryUploaded: () -> Unit
) {
    val colors = appColors()
    val jakartaSans = PlusJakartaSans()
    val scope = rememberCoroutineScope()
    val storageRepository = remember { StorageRepository() }
    val socialRepository = remember { SocialRepository() }

    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = imagePicker.pickFromGallery()
        if (result != null) {
            selectedImageUri = result.uri
            selectedImageBytes = result.bytes
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = colors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Story",
                        color = colors.TextPrimary,
                        fontFamily = jakartaSans,
                        fontWeight = FontWeight.Bold
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selectedImageUri != null && selectedImageBytes != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    KamelImage(
                        resource = asyncPainterResource(selectedImageUri!!),
                        contentDescription = "Story preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = colors.Error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            isUploading = true
                            errorMessage = null
                            try {
                                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                                    ?: throw Exception("Not signed in")

                                val uploadResult = storageRepository.uploadStoryImage(
                                    userId = user.id,
                                    imageBytes = selectedImageBytes!!,
                                    fileName = "story_${System.currentTimeMillis()}.jpg"
                                )

                                val imageUrl = uploadResult.getOrThrow()

                                socialRepository.uploadStory(
                                    userId = user.id,
                                    imageUrl = imageUrl
                                ).getOrThrow()

                                onStoryUploaded()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to upload story"
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Share Story",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = jakartaSans
                        )
                    }
                }
            } else {
                CircularProgressIndicator(color = colors.Primary)
            }
        }
    }
}
