package com.example.smackcheck2.data

import com.example.smackcheck2.BuildConfig

/**
 * Android implementation of FirebaseConfig
 * Reads values from BuildConfig (generated from local.properties or .env)
 */
actual object FirebaseConfig {
    actual val FIREBASE_WEB_CLIENT_ID: String = BuildConfig.FIREBASE_WEB_CLIENT_ID
    actual val FACEBOOK_APP_ID: String = BuildConfig.FACEBOOK_APP_ID
}
