package com.example.smackcheck2.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String? = null,
    val rating: Float? = null,
    val imageUrl: String? = null
)

@Composable
expect fun PlatformMapView(
    latitude: Double,
    longitude: Double,
    zoom: Float = 14f,
    markers: List<MapMarker> = emptyList(),
    onMarkerClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
)
