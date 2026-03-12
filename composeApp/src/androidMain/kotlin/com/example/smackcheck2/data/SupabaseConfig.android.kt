package com.example.smackcheck2.data

import com.example.smackcheck2.BuildConfig

/**
 * Android implementation of SupabaseConfig
 * Reads values from BuildConfig (generated from local.properties or .env)
 * 
 * Note: GEMINI_API_KEY is no longer needed client-side as we use Supabase Edge Functions
 */
actual object SupabaseConfig {
    actual val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    actual val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
}
