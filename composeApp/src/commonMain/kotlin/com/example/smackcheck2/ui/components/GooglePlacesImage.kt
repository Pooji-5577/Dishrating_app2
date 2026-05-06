package com.example.smackcheck2.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.example.smackcheck2.viewmodel.PhotoState

@Composable
fun PlaceholderRestaurantImage(
    restaurantName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    RestaurantImage(
        restaurantName = restaurantName,
        modifier = modifier,
        contentScale = contentScale
    )
}

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
            NetworkImage(
                imageUrl = photoState.url,
                contentDescription = restaurantName,
                modifier = modifier,
                contentScale = contentScale
            )
        }

        is PhotoState.FullPhotosLoaded -> {
            val firstUrl = photoState.urls.firstOrNull()
            if (firstUrl != null) {
                NetworkImage(
                    imageUrl = firstUrl,
                    contentDescription = restaurantName,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
            }
        }

        is PhotoState.NoPhotos -> {
            PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
        }

        is PhotoState.Error -> {
            PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
        }

        null -> {
            PlaceholderRestaurantImage(restaurantName, modifier, contentScale)
        }
    }
}
