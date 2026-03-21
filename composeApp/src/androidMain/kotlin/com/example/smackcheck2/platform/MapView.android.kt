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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
actual fun PlatformMapView(
    latitude: Double,
    longitude: Double,
    zoom: Float,
    markers: List<MapMarker>,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), zoom)
    }

    val markerIcons = remember { mutableStateMapOf<String, BitmapDescriptor>() }

    // Load circular marker images
    LaunchedEffect(markers) {
        markers.forEach { marker ->
            val url = marker.imageUrl
            if (url != null && !markerIcons.containsKey(marker.id)) {
                withContext(Dispatchers.IO) {
                    try {
                        val stream = URL(url).openStream()
                        val original = BitmapFactory.decodeStream(stream)
                        stream.close()
                        if (original != null) {
                            val icon = createCircularMarkerBitmap(original, 120)
                            original.recycle()
                            val descriptor = BitmapDescriptorFactory.fromBitmap(icon)
                            withContext(Dispatchers.Main) {
                                markerIcons[marker.id] = descriptor
                            }
                        }
                    } catch (_: Exception) {
                        // Keep default pin on failure
                    }
                }
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    ) {
        markers.forEach { marker ->
            Marker(
                state = MarkerState(position = LatLng(marker.latitude, marker.longitude)),
                title = marker.title,
                snippet = marker.snippet ?: marker.rating?.let { "Rating: ${"%.1f".format(it)}" },
                icon = markerIcons[marker.id],
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
