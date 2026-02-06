package com.example.smackcheck2.data

/**
 * Supabase configuration
 * Values are loaded from local.properties or .env file via BuildConfig (Android)
 * or from expect/actual implementation (iOS)
 */
expect object SupabaseConfig {
    val SUPABASE_URL: String
    val SUPABASE_ANON_KEY: String
    val GEMINI_API_KEY: String
}
