package com.example.smackcheck2.data

/**
 * Backend URL configuration.
 * Android reads from BuildConfig (local.properties / .env).
 * iOS reads from Info.plist, falls back to localhost for dev.
 */
expect object BackendConfig {
    val BACKEND_URL: String
    val CANDIDATE_URLS: List<String>
}
