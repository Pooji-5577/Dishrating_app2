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

    private val cache = LinkedHashMap<K, CacheEntry<V>>()

    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        val now = Clock.System.now().toEpochMilliseconds()
        return if (now - entry.timestamp < ttlMillis) {
            entry.value
        } else {
            cache.remove(key)
            null
        }
    }

    fun put(key: K, value: V) {
        if (cache.size >= maxSize) {
            // Remove oldest entry
            val oldestKey = cache.keys.firstOrNull()
            if (oldestKey != null) {
                cache.remove(oldestKey)
            }
        }
        cache[key] = CacheEntry(
            value = value,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun remove(key: K) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

    fun evictExpired() {
        val now = Clock.System.now().toEpochMilliseconds()
        val expiredKeys = cache.entries
            .filter { now - it.value.timestamp >= ttlMillis }
            .map { it.key }
        expiredKeys.forEach { cache.remove(it) }
    }
}
