package com.example.smackcheck2.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIColor
import platform.darwin.NSObject
import kotlin.math.pow

/**
 * Custom annotation class to hold marker data
 */
@OptIn(ExperimentalForeignApi::class)
class RestaurantAnnotation(
    val markerId: String,
    latitude: Double,
    longitude: Double,
    title: String,
    subtitle: String?
) : MKPointAnnotation() {
    init {
        setCoordinate(CLLocationCoordinate2DMake(latitude, longitude))
        setTitle(title)
        subtitle?.let { setSubtitle(it) }
    }
}

/**
 * Map delegate to handle marker clicks
 */
@OptIn(ExperimentalForeignApi::class)
class MapViewDelegate(
    private val onMarkerClick: (String) -> Unit
) : NSObject(), MKMapViewDelegateProtocol {
    
    override fun mapView(
        mapView: MKMapView,
        viewForAnnotation: MKAnnotationProtocol
    ): MKAnnotationView? {
        // Return null for user location annotation
        if (viewForAnnotation === mapView.userLocation) {
            return null
        }
        
        val identifier = "RestaurantMarker"
        var annotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier) as? MKMarkerAnnotationView
        
        if (annotationView == null) {
            annotationView = MKMarkerAnnotationView(viewForAnnotation, identifier)
            annotationView.canShowCallout = true
        } else {
            annotationView.annotation = viewForAnnotation
        }
        
        // Customize marker appearance
        annotationView.markerTintColor = UIColor.orangeColor
        
        return annotationView
    }
    
    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val annotation = didSelectAnnotationView.annotation
        if (annotation is RestaurantAnnotation) {
            onMarkerClick(annotation.markerId)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    latitude: Double,
    longitude: Double,
    zoom: Float,
    markers: List<MapMarker>,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier
) {
    // Create delegate that persists across recompositions
    val delegate = remember(onMarkerClick) { MapViewDelegate(onMarkerClick) }
    
    // Convert zoom level to span distance (approximate)
    // Zoom level 14 ~ 1000m, each level doubles/halves the distance
    val spanDistance = 1000.0 * 2.0.pow((14.0 - zoom).toDouble())
    
    UIKitView(
        factory = {
            MKMapView().apply {
                // Set initial region
                val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
                val region = MKCoordinateRegionMakeWithDistance(coordinate, spanDistance, spanDistance)
                setRegion(region, animated = false)
                
                // Configure map
                this.delegate = delegate
                this.showsUserLocation = true
                this.showsCompass = true
                this.showsScale = true
                
                // Add markers
                markers.forEach { marker ->
                    val annotation = RestaurantAnnotation(
                        markerId = marker.id,
                        latitude = marker.latitude,
                        longitude = marker.longitude,
                        title = marker.title,
                        subtitle = marker.snippet ?: marker.rating?.let { "Rating: ${formatRating(it)}" }
                    )
                    addAnnotation(annotation)
                }
            }
        },
        modifier = modifier,
        update = { mapView ->
            // Update region when coordinates change
            val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
            val region = MKCoordinateRegionMakeWithDistance(coordinate, spanDistance, spanDistance)
            mapView.setRegion(region, animated = true)
            
            // Update markers
            // Remove existing custom annotations
            val existingAnnotations = mapView.annotations.filterIsInstance<RestaurantAnnotation>()
            existingAnnotations.forEach { mapView.removeAnnotation(it) }
            
            // Add new markers
            markers.forEach { marker ->
                val annotation = RestaurantAnnotation(
                    markerId = marker.id,
                    latitude = marker.latitude,
                    longitude = marker.longitude,
                    title = marker.title,
                    subtitle = marker.snippet ?: marker.rating?.let { "Rating: ${formatRating(it)}" }
                )
                mapView.addAnnotation(annotation)
            }
        }
    )
}

/**
 * Format a rating value to one decimal place
 */
private fun formatRating(value: Float): String {
    val intPart = value.toInt()
    val decimalPart = ((value - intPart) * 10).toInt()
    return "$intPart.$decimalPart"
}
