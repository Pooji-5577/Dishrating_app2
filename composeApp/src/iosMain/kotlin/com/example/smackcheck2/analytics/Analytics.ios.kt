package com.example.smackcheck2.analytics

actual object Analytics {
    actual fun initialize(token: String) { }
    actual fun identify(userId: String) { }
    actual fun track(event: String, properties: Map<String, Any?>) { }
    actual fun reset() { }
}
