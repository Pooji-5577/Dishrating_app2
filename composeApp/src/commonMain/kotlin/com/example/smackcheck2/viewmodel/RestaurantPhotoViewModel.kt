package com.example.smackcheck2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smackcheck2.data.RestaurantPhotoService
import com.example.smackcheck2.data.repository.DatabaseRepository
import com.example.smackcheck2.model.Restaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.smackcheck2.util.Logger
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
    private val databaseRepository = DatabaseRepository()

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
            Logger.d("RestaurantPhotoViewModel", "Cache hit for '$name' (id=$restaurantId)")
            updateState(restaurantId, PhotoState.ThumbnailLoaded(url))
            return
        }

        Logger.d("RestaurantPhotoViewModel", "Loading thumbnail for '$name' (id=$restaurantId, placeId=$placeId, city=$city)")

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
                    Logger.d("RestaurantPhotoViewModel", "Got thumbnail for '$name': $url")
                    thumbnailUrlCache[restaurantId] = url
                    updateState(restaurantId, PhotoState.ThumbnailLoaded(url))
                } else {
                    val fallbackUrl = getPlatformDishFallback(restaurantId)
                    if (fallbackUrl != null) {
                        Logger.d("RestaurantPhotoViewModel", "Using platform dish fallback for '$name': $fallbackUrl")
                        thumbnailUrlCache[restaurantId] = fallbackUrl
                        updateState(restaurantId, PhotoState.ThumbnailLoaded(fallbackUrl))
                    } else {
                        Logger.d("RestaurantPhotoViewModel", "No photos found for '$name' (id=$restaurantId, placeId=$placeId)")
                        updateState(restaurantId, PhotoState.NoPhotos)
                    }
                }
            } catch (e: Exception) {
                Logger.e("RestaurantPhotoViewModel", "Error loading thumbnail for '$name': ${e::class.simpleName} - ${e.message}", e)
                updateState(restaurantId, PhotoState.Error(e.message ?: "Failed"))
            }
        }
    }

    /**
     * Directly set a known thumbnail URL (e.g. from Google Places nearby search result).
     * Avoids calling the edge function when a photo URL is already available.
     */
    fun setThumbnailUrl(restaurantId: String, url: String) {
        // Skip if a better (full) state is already loaded
        val current = _photoStates.value[restaurantId]
        if (current is PhotoState.ThumbnailLoaded || current is PhotoState.FullPhotosLoaded) return

        thumbnailUrlCache[restaurantId] = url
        updateState(restaurantId, PhotoState.ThumbnailLoaded(url))
    }

    /**
     * Warm the first visible restaurant card images from already-loaded list data.
     */
    fun preloadThumbnails(restaurants: List<Restaurant>, limit: Int = 12) {
        restaurants
            .asSequence()
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .take(limit)
            .forEach { restaurant ->
                val knownUrl = restaurant.photoUrl ?: restaurant.imageUrls.firstOrNull()
                if (!knownUrl.isNullOrBlank()) {
                    setThumbnailUrl(restaurant.id, knownUrl)
                } else {
                    loadThumbnail(
                        restaurantId = restaurant.id,
                        placeId = restaurant.googlePlaceId,
                        name = restaurant.name,
                        city = restaurant.city
                    )
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
                    Logger.d("RestaurantPhotoViewModel", "Got ${urls.size} full photos for '$name'")
                    fullUrlCache[restaurantId] = urls
                    updateState(restaurantId, PhotoState.FullPhotosLoaded(urls))
                } else {
                    val fallbackUrl = getPlatformDishFallback(restaurantId)
                    if (fallbackUrl != null) {
                        fullUrlCache[restaurantId] = listOf(fallbackUrl)
                        updateState(restaurantId, PhotoState.FullPhotosLoaded(listOf(fallbackUrl)))
                    } else {
                        Logger.d("RestaurantPhotoViewModel", "No full photos found for '$name' (id=$restaurantId)")
                        updateState(restaurantId, PhotoState.NoPhotos)
                    }
                }
            } catch (e: Exception) {
                Logger.e("RestaurantPhotoViewModel", "Error loading full photos for '$name': ${e::class.simpleName} - ${e.message}", e)
                updateState(restaurantId, PhotoState.Error(e.message ?: "Failed"))
            }
        }
    }

    private fun updateState(restaurantId: String, state: PhotoState) {
        _photoStates.value = _photoStates.value.toMutableMap().apply {
            put(restaurantId, state)
        }
    }

    private suspend fun getPlatformDishFallback(restaurantId: String): String? {
        val dishes = databaseRepository.getDishesForRestaurant(restaurantId).getOrDefault(emptyList())
        if (dishes.isEmpty()) return null

        val ratedImage = databaseRepository
            .getRatingsByDishIds(dishes.map { it.id })
            .filter { !it.imageUrl.isNullOrBlank() }
            .maxByOrNull { it.rating }
            ?.imageUrl

        return ratedImage ?: dishes
            .sortedByDescending { it.rating }
            .firstOrNull { !it.imageUrl.isNullOrBlank() }
            ?.imageUrl
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
