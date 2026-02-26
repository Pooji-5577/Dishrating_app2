package com.example.smackcheck2.data

/**
 * iOS implementation of SupabaseConfig
 * For iOS, you can either:
 * 1. Replace these values directly
 * 2. Use a plist file and read values at runtime
 * 3. Use environment variables in your CI/CD pipeline
 * 
 * Note: GEMINI_API_KEY is no longer needed client-side as we use Supabase Edge Functions
 */
actual object SupabaseConfig {
    actual val SUPABASE_URL: String = "https://ayopmvhtfuwbsjxhpfgd.supabase.co"
    actual val SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF5b3Btdmh0ZnV3YnNqeGhwZmdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyNjAyMTksImV4cCI6MjA4NDgzNjIxOX0.2siGUJfE3iLoaEKae5gycw_6mo748KKyi5C7YEHuUlQ"
}
