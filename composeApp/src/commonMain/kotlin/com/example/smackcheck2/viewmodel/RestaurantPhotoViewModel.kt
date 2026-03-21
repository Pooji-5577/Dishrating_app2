package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.RestaurantPhotoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing restaurant photo URLs.
 *
 * Fetches photo URLs from the Supabase Edge Function (which calls Google Places
 * API server-side with the stored API key). Returns direct image URLs that can
 * be loaded by any image composable (KamelImage, NetworkImage, etc).
 *
 * Responsibilities:
 *   1. Fetch thumbnail URL for search cards (single URL per restaurant)
 *   2. Fetch multiple photo URLs for detail screen carousel
 *   3. Cache URLs in memory
 *   4. Handle loading/error states per restaurant
 */
class RestaurantPhotoViewModel : ViewModel() {

    private val photoService = RestaurantPhotoService()

    // ── Photo state per restaurant: restaurantId → PhotoState ──
    private val _photoStates = MutableStateFlow<Map<String, PhotoState>>(emptyMap())
    val photoStates: StateFlow<Map<String, PhotoState>> = _photoStates.asStateFlow()

    // ── In-memory URL cache ──
    private val thumbnailUrlCache = mutableMapOf<String, String>()
    private val fullUrlCache = mutableMapOf<String, List<String>>()

    /**
     * Load a single thumbnail URL for a restaurant search card.
     */
    fun loadThumbnail(
        restaurantId: String,
        placeId: String? = null,
        name: String = "",
        city: String = ""
    ) {
        // Skip if already loaded or loading
        val current = _photoStates.value[restaurantId]
        if (current is PhotoState.ThumbnailLoaded || current is PhotoState.Loading) return

        // Check cache first
        thumbnailUrlCache[restaurantId]?.let { url ->
            println("[DEBUG][PhotoVM] Cache hit for '$name' (id=$restaurantId)")
            updateState(restaurantId, PhotoState.ThumbnailLoaded(url))
            return
        }

        println("[DEBUG][PhotoVM] Loading thumbnail for '$name' (id=$restaurantId, placeId=$placeId, city=$city)")

        viewModelScope.launch {
            updateState(restaurantId, PhotoState.Loading)

            try {
                val url = photoService.getThumbnailUrl(
                    restaurantId = restaurantId,
                    restaurantName = name,
                    city = city,
                    placeId = placeId
                )

                if (url != null) {
                    println("[DEBUG][PhotoVM] Got thumbnail for '$name': $url")
                    thumbnailUrlCache[restaurantId] = url
                    updateState(restaurantId, PhotoState.ThumbnailLoaded(url))
                } else {
                    println("[DEBUG][PhotoVM] No photos found for '$name' (id=$restaurantId, placeId=$placeId)")
                    updateState(restaurantId, PhotoState.NoPhotos)
                }
            } catch (e: Exception) {
                println("[DEBUG][PhotoVM] ERROR loading thumbnail for '$name': ${e::class.simpleName} - ${e.message}")
                updateState(restaurantId, PhotoState.Error(e.message ?: "Failed"))
            }
        }
    }

    /**
     * Load all photo URLs for the restaurant detail screen carousel.
     */
    fun loadFullPhotos(
        restaurantId: String,
        placeId: String? = null,
        name: String = "",
        city: String = "",
    ) {
        // Skip if already loaded
        val current = _photoStates.value[restaurantId]
        if (current is PhotoState.FullPhotosLoaded) return

        // Check cache
        fullUrlCache[restaurantId]?.let { cached ->
            updateState(restaurantId, PhotoState.FullPhotosLoaded(cached))
            return
        }

        viewModelScope.launch {
            updateState(restaurantId, PhotoState.Loading)

            try {
                val urls = photoService.getRestaurantPhotos(
                    restaurantId = restaurantId,
                    restaurantName = name,
                    city = city,
                    placeId = placeId
                )

                if (urls.isNotEmpty()) {
                    println("[DEBUG][PhotoVM] Got ${urls.size} full photos for '$name'")
                    fullUrlCache[restaurantId] = urls
                    updateState(restaurantId, PhotoState.FullPhotosLoaded(urls))
                } else {
                    println("[DEBUG][PhotoVM] No full photos found for '$name' (id=$restaurantId)")
                    updateState(restaurantId, PhotoState.NoPhotos)
                }
            } catch (e: Exception) {
                println("[DEBUG][PhotoVM] ERROR loading full photos for '$name': ${e::class.simpleName} - ${e.message}")
                updateState(restaurantId, PhotoState.Error(e.message ?: "Failed"))
            }
        }
    }

    private fun updateState(restaurantId: String, state: PhotoState) {
        _photoStates.value = _photoStates.value.toMutableMap().apply {
            put(restaurantId, state)
        }
    }
}

/**
 * State for a single restaurant's photos.
 * Now uses URL strings instead of byte arrays — URLs are loaded by image composables.
 */
sealed class PhotoState {
    /** Photos are being fetched from Edge Function. */
    data object Loading : PhotoState()

    /** Single thumbnail URL loaded (for search cards). */
    data class ThumbnailLoaded(val url: String) : PhotoState()

    /** Multiple photo URLs loaded (for detail carousel). */
    data class FullPhotosLoaded(val urls: List<String>) : PhotoState()

    /** Restaurant has no photos on Google Places. */
    data object NoPhotos : PhotoState()

    /** API call failed. */
    data class Error(val message: String) : PhotoState()
}
