package com.example.smackcheck2.data

/**
 * Supabase configuration
 * Values are loaded from local.properties or .env file via BuildConfig (Android)
 * or from expect/actual implementation (iOS)
 * 
 * Note: GEMINI_API_KEY is no longer needed client-side as we use Supabase Edge Functions
 * for AI dish detection. The API key is stored securely on the server.
 */
expect object SupabaseConfig {
    val SUPABASE_URL: String
    val SUPABASE_ANON_KEY: String
}
