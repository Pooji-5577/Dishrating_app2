package com.example.smackcheck2.util

import kotlinx.datetime.Clock

class InMemoryCache<K, V>(
    private val maxSize: Int = 100,
    private val ttlMillis: Long = 5 * 60 * 1000L // 5 minutes default
) {
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Long
    )

    // Use a standard LinkedHashMap (no accessOrder constructor in Kotlin/Native).
    // We manually manage eviction by removing and re-inserting on access
    // to approximate LRU behavior.
    private val cache = LinkedHashMap<K, CacheEntry<V>>()
    private val accessOrder = mutableListOf<K>()

    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        val now = Clock.System.now().toEpochMilliseconds()
        return if (now - entry.timestamp < ttlMillis) {
            // Update access order for LRU
            accessOrder.remove(key)
            accessOrder.add(key)
            entry.value
        } else {
            cache.remove(key)
            accessOrder.remove(key)
            null
        }
    }

    fun put(key: K, value: V) {
        // Evict oldest entry if at capacity
        if (cache.size >= maxSize && !cache.containsKey(key)) {
            val oldest = accessOrder.firstOrNull()
            if (oldest != null) {
                cache.remove(oldest)
                accessOrder.remove(oldest)
            }
        }
        cache[key] = CacheEntry(
            value = value,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        accessOrder.remove(key)
        accessOrder.add(key)
    }

    fun remove(key: K) {
        cache.remove(key)
        accessOrder.remove(key)
    }

    fun clear() {
        cache.clear()
        accessOrder.clear()
    }

    fun evictExpired() {
        val now = Clock.System.now().toEpochMilliseconds()
        val expiredKeys = cache.entries
            .filter { now - it.value.timestamp >= ttlMillis }
            .map { it.key }
        expiredKeys.forEach {
            cache.remove(it)
            accessOrder.remove(it)
        }
    }
}
