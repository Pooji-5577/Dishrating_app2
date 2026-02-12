package com.example.smackcheck2.data

/**
 * iOS implementation of FirebaseConfig
 * For iOS, you can either:
 * 1. Replace these values directly with your Firebase configuration
 * 2. Use a plist file and read values at runtime
 * 3. Use environment variables in your CI/CD pipeline
 *
 * These values can be found in your Firebase project settings:
 * - FIREBASE_WEB_CLIENT_ID: Found in GoogleService-Info.plist as CLIENT_ID
 * - FACEBOOK_APP_ID: Your Facebook App ID from developers.facebook.com
 */
actual object FirebaseConfig {
    // Firebase Web Client ID from google-services.json
    actual val FIREBASE_WEB_CLIENT_ID: String = "422379094584-9fh1305pbo90ghinm5tnkmq3g132cl1f.apps.googleusercontent.com"

    // Replace with your actual Facebook App ID from Facebook Developers Console
    // TODO: Update this with your Facebook App ID
    actual val FACEBOOK_APP_ID: String = "YOUR_FACEBOOK_APP_ID_HERE"
}
