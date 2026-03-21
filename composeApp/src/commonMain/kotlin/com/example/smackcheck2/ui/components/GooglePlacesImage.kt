package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.viewmodel.PhotoState

/**
 * Placeholder shown when no Google Places photo is available.
 * Delegates to the existing NetworkImage with Unsplash fallback.
 */
@Composable
fun PlaceholderRestaurantImage(
    restaurantName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    NetworkImage(
        imageUrl = FoodImages.getRestaurantImageByName(restaurantName),
        contentDescription = restaurantName,
        modifier = modifier,
        contentScale = contentScale
    )
}

/**
 * Smart restaurant image that tries Google Places URL first, falls back to Unsplash.
 *
 * Uses URL strings from the Edge Function — loaded by existing NetworkImage (Kamel).
 * No platform-specific SDK needed.
 *
 * @param photoState  The current photo loading state from RestaurantPhotoViewModel
 * @param restaurantName  Restaurant name for placeholder fallback
 */
@Composable
fun SmartRestaurantImage(
    photoState: PhotoState?,
    restaurantName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val themeColors = appColors()

    when (photoState) {
        is PhotoState.Loading -> {
            // Show loading spinner while fetching from Edge Function
            Box(
                modifier = modifier.background(themeColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = themeColors.Primary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        is PhotoState.ThumbnailLoaded -> {
            // Show the Google Places photo via URL.
            // Pass a fallbackUrl so if the Google URL fails (e.g. expired CDN link),
            // Kamel automatically retries with an Unsplash stock image.
            NetworkImage(
                imageUrl = photoState.url,
                contentDescription = restaurantName,
                modifier = modifier,
                contentScale = contentScale,
                fallbackUrl = FoodImages.getRestaurantImageByName(restaurantName)
            )
        }

        is PhotoState.FullPhotosLoaded -> {
            // Show the first photo URL from the full set
            val firstUrl = photoState.urls.firstOrNull()
            if (firstUrl != null) {
                NetworkImage(
                    imageUrl = firstUrl,
                    contentDescription = restaurantName,
                    modifier = modifier,
                    contentScale = contentScale,
                    fallbackUrl = FoodImages.getRestaurantImageByName(restaurantName)
                )
            } else {
                PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
            }
        }

        is PhotoState.NoPhotos -> {
            println("[DEBUG][SmartRestaurantImage] No Google Places photos for '$restaurantName' — using Unsplash fallback")
            PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
        }

        is PhotoState.Error -> {
            println("[DEBUG][SmartRestaurantImage] Error loading photo for '$restaurantName': ${photoState.message} — using Unsplash fallback")
            PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
        }

        null -> {
            println("[DEBUG][SmartRestaurantImage] PhotoState is null for '$restaurantName' (loadThumbnail not called?) — using Unsplash fallback")
            PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
        }
    }
}
