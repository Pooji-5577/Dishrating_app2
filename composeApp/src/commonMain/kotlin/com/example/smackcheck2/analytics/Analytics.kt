package com.example.smackcheck2.analytics

/**
 * Cross-platform analytics abstraction.
 * Android: Mixpanel SDK. iOS: no-op stub (wire up later).
 */
expect object Analytics {
    fun initialize(token: String)
    fun identify(userId: String)
    fun track(event: String, properties: Map<String, Any?> = emptyMap())
    fun reset()
}
