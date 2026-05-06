package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.data.repository.GeofenceEvent
import com.example.smackcheck2.platform.LocationResult
import com.example.smackcheck2.platform.MapMarker
import com.example.smackcheck2.platform.NearbyRestaurant
import com.example.smackcheck2.platform.PlatformMapView
import com.example.smackcheck2.ui.components.EmptyState
import com.example.smackcheck2.ui.components.LoadingState
import com.example.smackcheck2.ui.components.SmartRestaurantImage
import com.example.smackcheck2.viewmodel.NearbyRestaurantsUiState
import com.example.smackcheck2.viewmodel.NearbyRestaurantsViewModel
import com.example.smackcheck2.viewmodel.RestaurantPhotoViewModel
import kotlin.math.*

private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatDist(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else "${"%.1f".format(km)} km"

/**
 * Nearby Restaurants Screen
 * Displays restaurants near the user's current location with geofencing support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyRestaurantsScreen(
    viewModel: NearbyRestaurantsViewModel,
    photoViewModel: RestaurantPhotoViewModel,
    onNavigateBack: () -> Unit,
    onRestaurantClick: (NearbyRestaurant) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val geofenceEnabled by viewModel.geofenceEnabled.collectAsState()
    val geofenceEvent by viewModel.geofenceEvents.collectAsState()
    var showMapView by remember { mutableStateOf(false) }
    var selectedRadius by remember { mutableStateOf(2000) }
    var showRadiusDialog by remember { mutableStateOf(false) }
    
    // Show snackbar for geofence events
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(geofenceEvent) {
        geofenceEvent?.let { event ->
            val message = when (event) {
                is GeofenceEvent.Entered -> "You're near ${event.restaurantName}! Time to rate some dishes?"
                is GeofenceEvent.Exited -> "Left ${event.restaurantName} area"
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearGeofenceEvent()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nearby Restaurants") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Geofencing toggle
                    IconButton(onClick = { viewModel.toggleGeofencing() }) {
                        Icon(
                            imageVector = if (geofenceEnabled) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                            contentDescription = if (geofenceEnabled) "Disable Notifications" else "Enable Notifications",
                            tint = if (geofenceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Toggle between map and list view
                    IconButton(onClick = { showMapView = !showMapView }) {
                        Icon(
                            imageVector = if (showMapView) Icons.Filled.List else Icons.Filled.Map,
                            contentDescription = if (showMapView) "Show List" else "Show Map"
                        )
                    }
                    // Radius filter
                    IconButton(onClick = { showRadiusDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Change Radius"
                        )
                    }
                    // Refresh
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NearbyRestaurantsUiState.Initial,
                is NearbyRestaurantsUiState.Loading -> {
                    LoadingState(
                        message = "Finding nearby restaurants..."
                    )
                }

                is NearbyRestaurantsUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Geofencing status bar
                        if (state.geofencingEnabled && state.monitoredCount > 0) {
                            GeofencingStatusBar(monitoredCount = state.monitoredCount)
                        }
                        
                        if (state.restaurants.isEmpty()) {
                            EmptyState(
                                title = "No Restaurants Found",
                                message = "No restaurants found nearby",
                                icon = Icons.Filled.Restaurant
                            )
                        } else {
                            if (showMapView) {
                                val markers = state.restaurants.map { r ->
                                    MapMarker(
                                        id = r.id,
                                        latitude = r.latitude,
                                        longitude = r.longitude,
                                        title = r.name,
                                        snippet = r.address,
                                        rating = r.rating?.toFloat()
                                    )
                                }
                                val centerLat = state.currentLocation?.latitude ?: state.restaurants.firstOrNull()?.latitude ?: 0.0
                                val centerLng = state.currentLocation?.longitude ?: state.restaurants.firstOrNull()?.longitude ?: 0.0
                                PlatformMapView(
                                    latitude = centerLat,
                                    longitude = centerLng,
                                    zoom = 14f,
                                    markers = markers,
                                    onMarkerClick = { id ->
                                        state.restaurants.find { it.id == id }?.let { onRestaurantClick(it) }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // List view
                                RestaurantsList(
                                    restaurants = state.restaurants,
                                    photoViewModel = photoViewModel,
                                    currentLocation = state.currentLocation,
                                    onRestaurantClick = onRestaurantClick
                                )
                            }
                        }
                    }
                }

                is NearbyRestaurantsUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Radius selection dialog
    if (showRadiusDialog) {
        RadiusSelectionDialog(
            currentRadius = selectedRadius,
            onDismiss = { showRadiusDialog = false },
            onRadiusSelected = { radius ->
                selectedRadius = radius
                viewModel.changeRadius(radius)
                showRadiusDialog = false
            }
        )
    }
}

/**
 * Status bar showing geofencing is active
 */
@Composable
private fun GeofencingStatusBar(monitoredCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Monitoring $monitoredCount restaurants nearby",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * List of restaurants
 */
@Composable
private fun RestaurantsList(
    restaurants: List<NearbyRestaurant>,
    photoViewModel: RestaurantPhotoViewModel,
    currentLocation: LocationResult?,
    onRestaurantClick: (NearbyRestaurant) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(restaurants) { restaurant ->
            val distanceText = if (currentLocation != null &&
                restaurant.latitude != 0.0 && restaurant.longitude != 0.0)
                formatDist(distanceKm(currentLocation.latitude, currentLocation.longitude, restaurant.latitude, restaurant.longitude))
            else null
            RestaurantCard(
                restaurant = restaurant,
                photoViewModel = photoViewModel,
                distanceText = distanceText,
                onClick = { onRestaurantClick(restaurant) }
            )
        }
    }
}

/**
 * Restaurant card item
 */
@Composable
private fun RestaurantCard(
    restaurant: NearbyRestaurant,
    photoViewModel: RestaurantPhotoViewModel,
    distanceText: String?,
    onClick: () -> Unit
) {
    val photoStates by photoViewModel.photoStates.collectAsState()
    val photoState = photoStates[restaurant.id]

    LaunchedEffect(restaurant.id) {
        photoViewModel.loadThumbnail(
            restaurantId = restaurant.id,
            placeId = restaurant.id,
            name = restaurant.name
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartRestaurantImage(
                photoState = photoState,
                restaurantName = restaurant.name,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Restaurant details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (restaurant.address != null) {
                    Text(
                        text = restaurant.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (distanceText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rating and other info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (restaurant.rating != null) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(restaurant.rating * 10).toInt() / 10.0}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        restaurant.userRatingsTotal?.let { total ->
                            Text(
                                text = " ($total)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (restaurant.priceLevel != null && restaurant.priceLevel > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$".repeat(restaurant.priceLevel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    restaurant.isOpen?.let { isOpen ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isOpen) "Open" else "Closed",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOpen)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Radius selection dialog
 */
@Composable
private fun RadiusSelectionDialog(
    currentRadius: Int,
    onDismiss: () -> Unit,
    onRadiusSelected: (Int) -> Unit
) {
    val radiusOptions = listOf(
        500 to "500m",
        1000 to "1km",
        2000 to "2km",
        5000 to "5km",
        10000 to "10km"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Radius") },
        text = {
            Column {
                radiusOptions.forEach { (radius, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRadiusSelected(radius) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentRadius == radius,
                            onClick = { onRadiusSelected(radius) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
