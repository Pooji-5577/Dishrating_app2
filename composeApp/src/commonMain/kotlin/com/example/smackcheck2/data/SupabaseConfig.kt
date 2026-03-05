package com.example.smackcheck2.data

/**
 * Supabase configuration
 * Values are loaded from local.properties or .env file via BuildConfig (Android)
 * or from Info.plist via NSBundle (iOS)
 * 
 * Note: All third-party API keys (GEMINI_API_KEY, GOOGLE_PLACES_API_KEY) are stored
 * as Supabase secrets and accessed only by Edge Functions. They are NOT needed client-side.
 */
expect object SupabaseConfig {
    val SUPABASE_URL: String
    val SUPABASE_ANON_KEY: String
}
