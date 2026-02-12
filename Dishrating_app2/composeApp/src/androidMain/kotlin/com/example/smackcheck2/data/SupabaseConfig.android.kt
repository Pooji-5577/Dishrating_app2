package com.example.smackcheck2.data

import com.example.smackcheck2.BuildConfig

/**
 * Android implementation of SupabaseConfig
 * Reads values from BuildConfig (generated from local.properties or .env)
 */
actual object SupabaseConfig {
    actual val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    actual val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    actual val GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY
}
