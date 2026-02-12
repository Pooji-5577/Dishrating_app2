package com.example.smackcheck2.data

/**
 * Firebase configuration
 * Values are loaded from local.properties or .env file via BuildConfig (Android)
 * or from expect/actual implementation (iOS)
 */
expect object FirebaseConfig {
    val FIREBASE_WEB_CLIENT_ID: String
    val FACEBOOK_APP_ID: String
}
