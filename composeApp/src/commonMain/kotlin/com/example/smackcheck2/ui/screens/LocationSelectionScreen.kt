package com.example.smackcheck2.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.platform.LocationResult
import com.example.smackcheck2.ui.theme.CardShape
import com.example.smackcheck2.ui.theme.TextFieldShape
import kotlinx.coroutines.delay

/**
 * Location Selection Screen
 * Allows users to select their location/city with GPS detection and search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    currentLocation: String?,
    isDetectingLocation: Boolean = false,
    locationError: String? = null,
    searchResults: List<LocationResult> = emptyList(),
    onNavigateBack: () -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSearchLocation: (String) -> Unit = {},
    onClearError: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    // Debounce search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(300)
            onSearchLocation(searchQuery)
        }
    }

    val allCities = mapOf(
        "New York" to (40.7128 to -74.0060),
        "Los Angeles" to (34.0522 to -118.2437),
        "Chicago" to (41.8781 to -87.6298),
        "Houston" to (29.7604 to -95.3698),
        "Phoenix" to (33.4484 to -112.0740),
        "Philadelphia" to (39.9526 to -75.1652),
        "San Antonio" to (29.4241 to -98.4936),
        "San Diego" to (32.7157 to -117.1611),
        "Dallas" to (32.7767 to -96.7970),
        "San Jose" to (37.3382 to -121.8863),
        "Austin" to (30.2672 to -97.7431),
        "Jacksonville" to (30.3322 to -81.6557),
        "Fort Worth" to (32.7555 to -97.3308),
        "Columbus" to (39.9612 to -82.9988),
        "Charlotte" to (35.2271 to -80.8431),
        "San Francisco" to (37.7749 to -122.4194),
        "Indianapolis" to (39.7684 to -86.1581),
        "Seattle" to (47.6062 to -122.3321),
        "Denver" to (39.7392 to -104.9903),
        "Boston" to (42.3601 to -71.0589),
        "El Paso" to (31.7619 to -106.4850),
        "Nashville" to (36.1627 to -86.7816),
        "Detroit" to (42.3314 to -83.0458),
        "Portland" to (45.5152 to -122.6784),
        "Las Vegas" to (36.1699 to -115.1398),
        "Memphis" to (35.1495 to -90.0490),
        "Louisville" to (38.2527 to -85.7585),
        "Baltimore" to (39.2904 to -76.6122),
        "Milwaukee" to (43.0389 to -87.9065),
        "Miami" to (25.7617 to -80.1918)
    )

    val allCityNames = allCities.keys.toList()

    // Show search results if available, otherwise filter popular cities
    val displayedLocations = if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
        searchResults.mapNotNull { it.cityName }.distinct()
    } else if (searchQuery.isEmpty()) {
        allCityNames
    } else {
        allCityNames.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Use current location button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = !isDetectingLocation) { onUseCurrentLocation() },
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDetectingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDetectingLocation) "Detecting Location..." else "Use Current Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isDetectingLocation) "Please wait" else "Automatically detect your location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Error message
            if (locationError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onClearError() },
                    shape = CardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = locationError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = TextFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (searchResults.isNotEmpty() && searchQuery.isNotEmpty()) "Search Results" else "Popular Cities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cities list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(displayedLocations) { city ->
                    CityItem(
                        city = city,
                        isSelected = city == currentLocation,
                        onClick = {
                            val coords = allCities[city]
                            val lat = coords?.first ?: 0.0
                            val lng = coords?.second ?: 0.0
                            onLocationSelected(city, lat, lng)
                        }
                    )
                }

                if (displayedLocations.isEmpty() && searchQuery.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cities found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CityItem(
    city: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = city,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
