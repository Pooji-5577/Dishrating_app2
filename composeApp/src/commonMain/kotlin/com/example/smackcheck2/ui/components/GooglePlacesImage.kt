package com.example.smackcheck2.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
    when (photoState) {
        is PhotoState.Loading -> {
            ShimmerBox(modifier = modifier)
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
