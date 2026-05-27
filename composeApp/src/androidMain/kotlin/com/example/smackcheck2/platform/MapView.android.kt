package com.example.smackcheck2.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private const val MAX_CUSTOM_MARKER_ICONS = 36

// Static cache across recompositions and screen navigations
private val globalBitmapCache = ConcurrentHashMap<String, BitmapDescriptor>()

@Composable
actual fun PlatformMapView(
    latitude: Double,
    longitude: Double,
    zoom: Float,
    markers: List<MapMarker>,
    onMarkerClick: (String) -> Unit,
    showMyLocation: Boolean,
    recenterTrigger: Int,
    fitBoundsTrigger: Int,
    cameraFocusLatitude: Double?,
    cameraFocusLongitude: Double?,
    cameraFocusTrigger: Int,
    modifier: Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), zoom)
    }

    // Animate to user location when GPS coordinates arrive or recenter is triggered
    LaunchedEffect(latitude, longitude, recenterTrigger) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom),
            durationMs = 800
        )
    }

    LaunchedEffect(cameraFocusTrigger) {
        val focusLat = cameraFocusLatitude
        val focusLng = cameraFocusLongitude
        if (focusLat != null && focusLng != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(LatLng(focusLat, focusLng), 15f),
                durationMs = 800
            )
        }
    }

    // Fit camera to all markers when fitBoundsTrigger changes
    LaunchedEffect(fitBoundsTrigger, markers.size) {
        val validMarkers = markers.filter { it.latitude != 0.0 || it.longitude != 0.0 }
        if (validMarkers.size >= 2) {
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            validMarkers.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            val bounds = builder.build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 120),
                durationMs = 800
            )
        } else if (validMarkers.size == 1) {
            val m = validMarkers.first()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(LatLng(m.latitude, m.longitude), 15f),
                durationMs = 800
            )
        }
    }

    val markerIcons = remember { mutableStateMapOf<String, BitmapDescriptor>() }

    val iconCandidates = remember(markers) {
        markers
            .asSequence()
            .filter { !it.imageUrl.isNullOrBlank() }
            .take(MAX_CUSTOM_MARKER_ICONS)
            .toList()
    }

    // Load marker images in parallel with global caching
    LaunchedEffect(iconCandidates) {
        val missing = iconCandidates.filter {
            !globalBitmapCache.containsKey(it.id) && !markerIcons.containsKey(it.id)
        }

        // Populate from global cache first
        iconCandidates.forEach { marker ->
            globalBitmapCache[marker.id]?.let { markerIcons[marker.id] = it }
        }

        if (missing.isEmpty()) return@LaunchedEffect

        coroutineScope {
            missing.map { marker ->
                async(Dispatchers.IO) {
                    val url = marker.imageUrl ?: return@async null
                    try {
                        globalBitmapCache[marker.id]?.let { descriptor ->
                            marker.id to descriptor
                        } ?: run {
                            val stream = URL(url).openStream()
                            val original = BitmapFactory.decodeStream(stream)
                            stream.close()
                            if (original != null) {
                                val icon = createCircularMarkerBitmap(original, 120)
                                original.recycle()
                                val descriptor = BitmapDescriptorFactory.fromBitmap(icon)
                                globalBitmapCache[marker.id] = descriptor
                                marker.id to descriptor
                            } else null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().forEach { (id, descriptor) ->
                markerIcons[id] = descriptor
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = showMyLocation),
        uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
    ) {
        markers.forEach { marker ->
            Marker(
                state = MarkerState(position = LatLng(marker.latitude, marker.longitude)),
                title = marker.title,
                snippet = marker.snippet ?: marker.rating?.let { "Rating: ${"%.1f".format(it)}" },
                icon = markerIcons[marker.id] ?: globalBitmapCache[marker.id],
                onClick = {
                    onMarkerClick(marker.id)
                    true
                }
            )
        }
    }
}

/**
 * Create a circular bitmap with a white border for use as a map marker.
 */
private fun createCircularMarkerBitmap(source: Bitmap, sizePx: Int): Bitmap {
    val borderWidth = (sizePx * 0.08f).toInt()
    val totalSize = sizePx + borderWidth * 2

    val output = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // Draw white circle border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val center = totalSize / 2f
    canvas.drawCircle(center, center, totalSize / 2f, borderPaint)

    // Draw circular image
    val scaled = Bitmap.createScaledBitmap(source, sizePx, sizePx, true)
    val circularImage = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val imgCanvas = Canvas(circularImage)
    val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    imgCanvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, imgPaint)
    imgPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    imgCanvas.drawBitmap(scaled, 0f, 0f, imgPaint)
    scaled.recycle()

    canvas.drawBitmap(circularImage, borderWidth.toFloat(), borderWidth.toFloat(), null)
    circularImage.recycle()

    return output
}
