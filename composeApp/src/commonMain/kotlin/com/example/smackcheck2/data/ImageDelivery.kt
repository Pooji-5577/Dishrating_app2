package com.example.smackcheck2.data

/**
 * Single module for turning stored image URLs into display-specific delivery URLs.
 *
 * Supabase Storage originals remain the source of truth. Scroll-heavy surfaces should
 * use transformed render URLs so the device downloads and decodes feed-sized assets.
 */
object ImageDelivery {
    private const val PUBLIC_STORAGE_MARKER = "/storage/v1/object/public/"
    private const val PUBLIC_RENDER_MARKER = "/storage/v1/render/image/public/"

    fun feed(url: String?): String? =
        transform(url = url, width = 900, quality = 75)

    fun thumbnail(url: String?): String? =
        transform(url = url, width = 360, quality = 72)

    fun avatar(url: String?): String? =
        transform(url = url, width = 160, height = 160, quality = 76)

    fun story(url: String?): String? =
        transform(url = url, width = 900, quality = 76)

    fun original(url: String?): String? =
        url?.takeIf { it.isNotBlank() }

    private fun transform(
        url: String?,
        width: Int,
        height: Int? = null,
        quality: Int
    ): String? {
        val source = url?.takeIf { it.isNotBlank() } ?: return null
        if (source.contains(PUBLIC_RENDER_MARKER)) return source

        val markerIndex = source.indexOf(PUBLIC_STORAGE_MARKER)
        if (markerIndex < 0) return source

        val baseUrl = source.substring(0, markerIndex)
        val objectPath = source
            .substring(markerIndex + PUBLIC_STORAGE_MARKER.length)
            .substringBefore("?")

        val params = buildList {
            add("width=$width")
            if (height != null) add("height=$height")
            add("resize=cover")
            add("quality=$quality")
        }.joinToString("&")

        return "$baseUrl$PUBLIC_RENDER_MARKER$objectPath?$params"
    }
}
