package com.example.smackcheck2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import com.example.smackcheck2.ui.theme.appColors

/**
 * Food image URLs for dishes - using Unsplash for free high-quality food images
 */
object FoodImages {
    // Dish images
    val dishImages = listOf(
        "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=400&fit=crop", // Salad
        "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=400&fit=crop", // Pizza
        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=400&fit=crop", // Healthy bowl
        "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=400&h=400&fit=crop", // Pancakes
        "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=400&h=400&fit=crop", // Colorful food
        "https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=400&h=400&fit=crop", // French toast
        "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=400&h=400&fit=crop", // Pasta
        "https://images.unsplash.com/photo-1499028344343-cd173ffc68a9?w=400&h=400&fit=crop", // Burger
        "https://images.unsplash.com/photo-1432139555190-58524dae6a55?w=400&h=400&fit=crop", // Sushi
        "https://images.unsplash.com/photo-1563379926898-05f4575a45d8?w=400&h=400&fit=crop", // Pasta dish
        "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=400&h=400&fit=crop", // Avocado toast
        "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=400&fit=crop", // Meat dish
    )

    // Restaurant images (wider aspect ratio)
    val restaurantImages = listOf(
        "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600&h=400&fit=crop", // Restaurant interior
        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&h=400&fit=crop", // Restaurant tables
        "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=600&h=400&fit=crop", // Restaurant outdoor
        "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=600&h=400&fit=crop", // Cafe
        "https://images.unsplash.com/photo-1537047902294-62a40c20a6ae?w=600&h=400&fit=crop", // Fine dining
        "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=600&h=400&fit=crop", // Asian restaurant
        "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=600&h=400&fit=crop", // Food on table
        "https://images.unsplash.com/photo-1578474846511-04ba529f0b88?w=600&h=400&fit=crop", // Italian restaurant
    )

    // Get a dish image based on index (cycles through available images)
    fun getDishImage(index: Int): String = dishImages[index % dishImages.size]

    // Get a restaurant image based on index
    fun getRestaurantImage(index: Int): String = restaurantImages[index % restaurantImages.size]

    // Get image based on dish name (creates consistent mapping)
    fun getDishImageByName(name: String): String {
        val hash = name.hashCode().let { if (it < 0) -it else it }
        return dishImages[hash % dishImages.size]
    }

    // Get image based on restaurant name
    fun getRestaurantImageByName(name: String): String {
        val hash = name.hashCode().let { if (it < 0) -it else it }
        return restaurantImages[hash % restaurantImages.size]
    }
}

/**
 * Network image component using Kamel for Kotlin Multiplatform.
 *
 * If [fallbackUrl] is provided and the primary [imageUrl] fails to load,
 * automatically retries with the fallback URL (e.g. an Unsplash stock image)
 * instead of showing the orange error gradient.
 */
@Composable
fun NetworkImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIcon: ImageVector = Icons.Filled.Restaurant,
    fallbackUrl: String? = null,
    showGradientOnFailure: Boolean = true
) {
    val colors = appColors()

    // Validate URL before attempting to load
    if (imageUrl.isBlank()) {
        println("[DEBUG][NetworkImage] Empty URL for '$contentDescription'")
        Box(
            modifier = modifier
                .background(
                    if (showGradientOnFailure)
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B4513),
                                Color(0xFFD2691E),
                                Color(0xFFA0522D)
                            )
                        )
                    else
                        Brush.linearGradient(
                            colors = listOf(colors.Surface, colors.Surface)
                        )
                ),
            contentAlignment = Alignment.Center
        ){}
        return
    }

    // Track whether the primary URL failed so we can retry with fallbackUrl
    var useFallback by remember(imageUrl) { mutableStateOf(false) }
    val urlToLoad = if (useFallback && fallbackUrl != null) fallbackUrl else imageUrl

    KamelImage(
        resource = { asyncPainterResource(data = urlToLoad) },
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onLoading = {
            ShimmerBox(modifier = Modifier.fillMaxSize())
        },
        onFailure = { exception ->
            println("[DEBUG][NetworkImage] FAILED to load '$urlToLoad' for '$contentDescription'")
            println("[DEBUG][NetworkImage] Error: ${exception::class.simpleName} - ${exception.message}")
            println("[DEBUG][NetworkImage] Cause: ${exception.cause?.message}")
            if (!useFallback && fallbackUrl != null) {
                // Primary URL failed — retry once with the fallback
                useFallback = true
            } else {
                // No fallback available (or fallback also failed)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (showGradientOnFailure)
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF8B4513),
                                        Color(0xFFD2691E),
                                        Color(0xFFA0522D)
                                    )
                                )
                            else
                                Brush.linearGradient(
                                    colors = listOf(colors.Surface, colors.Surface)
                                )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = colors.TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    )
}

/**
 * Dish image — loads the real DB URL first, falls back to a generic placeholder icon if none.
 */
@Composable
fun DishImage(
    dishName: String,
    imageUrl: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (!imageUrl.isNullOrBlank()) {
        NetworkImage(
            imageUrl = imageUrl,
            contentDescription = dishName,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        val colors = appColors()
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(colors.Surface, colors.Surface))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = dishName,
                modifier = Modifier.size(48.dp),
                tint = colors.TextSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Restaurant image — loads the real DB / Places URL first, shows icon placeholder if none.
 */
@Composable
fun RestaurantImage(
    restaurantName: String,
    imageUrl: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (!imageUrl.isNullOrBlank()) {
        NetworkImage(
            imageUrl = imageUrl,
            contentDescription = restaurantName,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        val colors = appColors()
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(colors.Surface, colors.Surface))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = restaurantName,
                modifier = Modifier.size(48.dp),
                tint = colors.TextSecondary.copy(alpha = 0.4f)
            )
        }
    }
}
