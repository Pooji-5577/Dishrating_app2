package com.example.smackcheck2.data

import platform.Foundation.NSBundle

/**
 * iOS implementation of SupabaseConfig.
 * Values are hardcoded here because xcconfig variable substitution is unreliable for JWT keys
 * (the dots in the JWT segments can cause xcconfig to truncate the value).
 * The Supabase anon key is a public client-side credential by design — it is safe to embed.
 *
 * Note: GEMINI_API_KEY and GOOGLE_PLACES_API_KEY are not needed client-side.
 * They are stored as Supabase secrets and used only by Edge Functions.
 */
actual object SupabaseConfig {
    actual val SUPABASE_URL: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_URL") as? String)
            ?.takeIf { it.isNotBlank() && !it.startsWith("$(") }
            ?: "https://ayopmvhtfuwbsjxhpfgd.supabase.co"

    actual val SUPABASE_ANON_KEY: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_ANON_KEY") as? String)
            ?.takeIf { it.isNotBlank() && !it.startsWith("$(") && it.length > 20 }
            ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF5b3Btdmh0ZnV3YnNqeGhwZmdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyNjAyMTksImV4cCI6MjA4NDgzNjIxOX0.2siGUJfE3iLoaEKae5gycw_6mo748KKyi5C7YEHuUlQ"
}
