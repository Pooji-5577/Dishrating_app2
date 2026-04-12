package com.example.smackcheck2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.model.MapUserMarker
import com.example.smackcheck2.model.SocialMapUiState
import com.example.smackcheck2.platform.MapMarker
import com.example.smackcheck2.platform.PlatformMapView
import com.example.smackcheck2.ui.components.NetworkImage
import com.example.smackcheck2.ui.components.StarRatingDisplay
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.BottomSheetShape
import com.example.smackcheck2.ui.theme.ThemeColors
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.SocialMapViewModel
import kotlinx.coroutines.launch

/**
 * Social Map Screen - Snapchat-style map showing nearby users with dish posts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMapScreen(
    viewModel: SocialMapViewModel,
    onNavigateBack: () -> Unit,
    onUserProfileClick: (String) -> Unit,
    onDishDetailClick: (String) -> Unit,
    onRateDishClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeColors = appColors()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showSettings by remember { mutableStateOf(false) }
    var showRadiusSlider by remember { mutableStateOf(false) }
    
    // Sheet state for selected user preview
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Request location on first load
    LaunchedEffect(Unit) {
        if (uiState.currentLatitude == null) {
            viewModel.requestCurrentLocation()
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = themeColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            com.example.smackcheck2.ui.components.BottomNavBar(
                selectedItem = com.example.smackcheck2.ui.components.NavItem.MAP
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Discover Foodies",
                        color = themeColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = themeColors.TextPrimary
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = themeColors.Primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = themeColors.TextPrimary
                            )
                        }
                    }
                    
                    // Settings button
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = themeColors.TextPrimary
                        )
                    }
                    
                    // Settings dropdown
                    DropdownMenu(
                        expanded = showSettings,
                        onDismissRequest = { showSettings = false }
                    ) {
                        // Location sharing toggle
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.currentUserProfile?.locationSharingEnabled == true)
                                            Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        if (uiState.currentUserProfile?.locationSharingEnabled == true)
                                            "Location sharing ON" else "Location sharing OFF"
                                    )
                                }
                            },
                            onClick = {
                                val currentEnabled = uiState.currentUserProfile?.locationSharingEnabled ?: true
                                viewModel.toggleLocationSharing(!currentEnabled)
                            }
                        )
                        
                        // Radius adjustment
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("Search radius: ${uiState.radiusMeters / 1000}km")
                                }
                            },
                            onClick = {
                                showSettings = false
                                showRadiusSlider = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.Surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.nearbyUsers.isEmpty() -> {
                    // Loading state — show spinner only while the first batch loads
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = themeColors.Primary)
                            Text(
                                "Loading dish posts...",
                                color = themeColors.TextSecondary
                            )
                        }
                    }
                }
                
                !uiState.locationPermissionGranted -> {
                    // Location permission not granted
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = themeColors.Primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "Location Required",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.TextPrimary
                            )
                            Text(
                                "Enable location to discover foodies nearby",
                                color = themeColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(onClick = { viewModel.requestCurrentLocation() }) {
                                Text("Enable Location")
                            }
                        }
                    }
                }
                
                else -> {
                    // Map view — centers on user if GPS available, otherwise falls back to NYC
                    val currentLat = uiState.currentLatitude ?: 40.7128
                    val currentLng = uiState.currentLongitude ?: -74.0060

                    // Derive marker list from current mode
                    val activeMarkerSource = if (uiState.mapMode == com.example.smackcheck2.model.MapMode.MY_RATINGS)
                        uiState.myRatingMarkers else uiState.nearbyUsers
                    val markers = activeMarkerSource.map { user ->
                        MapMarker(
                            id = user.latestRatingId ?: user.userId,
                            latitude = user.latitude,
                            longitude = user.longitude,
                            title = user.latestDishName ?: user.username,
                            snippet = user.latestRestaurantName ?: user.username,
                            rating = user.latestRating,
                            imageUrl = user.latestDishImage ?: user.avatarUrl
                        )
                    }

                    // Use recenterTrigger as key to force map re-center when Locate Me is tapped
                    key(uiState.recenterTrigger) {
                        PlatformMapView(
                            latitude = currentLat,
                            longitude = currentLng,
                            zoom = 14f,
                            markers = markers,
                            onMarkerClick = { markerId ->
                                val user = activeMarkerSource.find {
                                    it.latestRatingId == markerId || it.userId == markerId
                                }
                                viewModel.selectUser(user)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Empty state overlay — shown when there are no nearby posts
                    if (markers.isEmpty() && !uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = themeColors.Surface.copy(alpha = 0.95f)
                                ),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Restaurant,
                                        contentDescription = null,
                                        tint = themeColors.Primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        "No dish posts nearby yet — be the first!",
                                        color = themeColors.TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // User count indicator
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.Surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = themeColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${uiState.nearbyUsers.size} dish posts",
                                color = themeColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // List View state
                    var showListView by remember { mutableStateOf(false) }

                    // Floating controls — top-right: List View (white pill) + Locate Me (red)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // List View white pill button
                        Surface(
                            onClick = { showListView = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Star, null, tint = Color(0xFF3B1011), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("List View", color = Color(0xFF3B1011), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Locate Me red button — requests GPS and re-centers map
                        Surface(
                            onClick = { viewModel.recenter() },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF9B2335),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.MyLocation, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Locate Me", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // MY RATINGS / NEARBY toggle pill — wired to ViewModel
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 72.dp)
                            .shadow(4.dp, RoundedCornerShape(30.dp))
                            .background(Color.White, RoundedCornerShape(30.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        listOf(
                            "MY RATINGS" to com.example.smackcheck2.model.MapMode.MY_RATINGS,
                            "NEARBY" to com.example.smackcheck2.model.MapMode.NEARBY
                        ).forEach { (label, mode) ->
                            val isActive = uiState.mapMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(if (isActive) Color(0xFF9B2335) else Color.Transparent)
                                    .clickable { viewModel.setMapMode(mode) }
                                    .padding(horizontal = 22.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isActive) Color.White else Color(0xFF767777),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // List View bottom sheet
                    if (showListView) {
                        ModalBottomSheet(
                            onDismissRequest = { showListView = false },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                            containerColor = Color.White
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (uiState.mapMode == com.example.smackcheck2.model.MapMode.MY_RATINGS)
                                            "My Ratings (${markers.size})"
                                        else "Nearby Dishes (${markers.size})",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2D2F2F)
                                    )
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close",
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clickable { showListView = false },
                                        tint = Color(0xFF767777)
                                    )
                                }
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFFE7E8E8))
                                if (markers.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No dish posts found.",
                                            color = Color(0xFF767777), fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(bottom = 32.dp)
                                    ) {
                                        items(activeMarkerSource, key = { it.latestRatingId ?: it.userId }) { post ->
                                            MapListItem(
                                                post = post,
                                                onClick = {
                                                    viewModel.selectUser(post)
                                                    showListView = false
                                                }
                                            )
                                            androidx.compose.material3.HorizontalDivider(
                                                color = Color(0xFFF0F0F0),
                                                modifier = Modifier.padding(horizontal = 20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Empty state overlay when no dish posts
                    AnimatedVisibility(
                        visible = uiState.nearbyUsers.isEmpty() && !uiState.isLoading,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = themeColors.Surface.copy(alpha = 0.95f)
                            ),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = themeColors.Primary.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "No dish posts yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Be the first to share a dish rating in your area!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = themeColors.TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = onRateDishClick,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Rate a Dish")
                                }
                            }
                        }
                    }
                }
            }
            
            // Radius slider overlay
            AnimatedVisibility(
                visible = showRadiusSlider,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                RadiusSliderCard(
                    currentRadius = uiState.radiusMeters,
                    onRadiusChange = { viewModel.setRadius(it) },
                    onDismiss = { showRadiusSlider = false },
                    themeColors = themeColors
                )
            }
        }
        
        // Bottom sheet for selected user preview
        if (uiState.selectedUser != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissUserPreview() },
                sheetState = sheetState,
                shape = BottomSheetShape,
                containerColor = themeColors.Surface
            ) {
                UserDishPreviewContent(
                    user = uiState.selectedUser!!,
                    onViewProfile = {
                        viewModel.dismissUserPreview()
                        onUserProfileClick(uiState.selectedUser!!.userId)
                    },
                    onViewDish = {
                        uiState.selectedUser?.latestRatingId?.let { ratingId ->
                            viewModel.dismissUserPreview()
                            onDishDetailClick(ratingId)
                        }
                    },
                    themeColors = themeColors
                )
            }
        }
    }
}

/**
 * User's dish preview content shown in the bottom sheet
 */
@Composable
private fun UserDishPreviewContent(
    user: MapUserMarker,
    onViewProfile: () -> Unit,
    onViewDish: () -> Unit,
    themeColors: ThemeColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // User info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(themeColors.Primary.copy(alpha = 0.1f))
                    .then(
                        if (user.isCurrentUser) {
                            Modifier.border(3.dp, themeColors.Primary, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl != null) {
                    NetworkImage(
                        imageUrl = user.avatarUrl,
                        contentDescription = user.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = themeColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.TextPrimary
                    )
                    if (user.isCurrentUser) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = themeColors.Primary
                        ) {
                            Text(
                                "You",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                
                if (user.distanceMeters > 0) {
                    Text(
                        text = formatDistance(user.distanceMeters),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.TextSecondary
                    )
                }
            }
            
            Button(
                onClick = onViewProfile,
                modifier = Modifier.height(36.dp)
            ) {
                Text("View Profile", fontSize = 12.sp)
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        // Latest dish card
        if (user.latestDishName != null) {
            Text(
                "Latest Dish",
                style = MaterialTheme.typography.labelLarge,
                color = themeColors.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewDish() },
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.SurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dish image
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.latestDishImage != null) {
                            NetworkImage(
                                imageUrl = user.latestDishImage,
                                contentDescription = user.latestDishName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = themeColors.TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.latestDishName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (user.latestRestaurantName != null) {
                            Text(
                                text = user.latestRestaurantName,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // Rating
                        if (user.latestRating != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = formatRating(user.latestRating),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.TextPrimary
                                )
                            }
                        }
                        
                        // Time ago
                        user.latestPostTime?.let { timestamp ->
                            Text(
                                text = formatTimeAgo(timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.TextTertiary
                            )
                        }
                    }
                }
            }
        } else {
            // No recent dishes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = themeColors.TextTertiary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No recent dishes",
                        color = themeColors.TextSecondary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Radius slider card
 */
@Composable
private fun RadiusSliderCard(
    currentRadius: Int,
    onRadiusChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.Surface
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Search Radius",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = themeColors.TextSecondary
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "${currentRadius / 1000} km",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = themeColors.Primary
            )
            
            Slider(
                value = currentRadius.toFloat(),
                onValueChange = { onRadiusChange(it.toInt()) },
                valueRange = 1000f..10000f,
                steps = 8,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 km", color = themeColors.TextTertiary, fontSize = 12.sp)
                Text("10 km", color = themeColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Format distance for display
 */
private fun formatDistance(meters: Double): String {
    return when {
        meters < 100 -> "Just here"
        meters < 1000 -> "${meters.toInt()} m away"
        else -> {
            val km = meters / 1000
            val formatted = formatOneDecimal(km)
            "$formatted km away"
        }
    }
}

/**
 * Format a Double to one decimal place (Kotlin/Native compatible)
 */
private fun formatOneDecimal(value: Double): String {
    val intPart = value.toInt()
    val decimalPart = ((value - intPart) * 10).toInt()
    return "$intPart.$decimalPart"
}

/**
 * Format rating to one decimal place
 */
private fun formatRating(rating: Float?): String {
    if (rating == null) return "0.0"
    return formatOneDecimal(rating.toDouble())
}

/**
 * Format timestamp to "X time ago" format
 */
private fun formatTimeAgo(timestamp: Long): String {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val diff = now - timestamp
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

@Composable
private fun MapListItem(
    post: MapUserMarker,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dish thumbnail
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5EDE3)),
            contentAlignment = Alignment.Center
        ) {
            if (!post.latestDishImage.isNullOrBlank()) {
                NetworkImage(
                    imageUrl = post.latestDishImage,
                    contentDescription = post.latestDishName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = Color(0xFF9B2335).copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.latestDishName ?: "Unknown Dish",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF2D2F2F),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = post.latestRestaurantName ?: post.username,
                fontSize = 13.sp,
                color = Color(0xFF767777),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (post.latestPostTime != null && post.latestPostTime > 0L) {
                Text(
                    text = formatTimeAgo(post.latestPostTime),
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
        // Star rating badge
        if (post.latestRating != null && post.latestRating > 0) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF9B2335), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "%.1f".format(post.latestRating),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
