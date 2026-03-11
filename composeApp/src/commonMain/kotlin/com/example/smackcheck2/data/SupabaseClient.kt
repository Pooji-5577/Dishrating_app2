package com.example.smackcheck2.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase Client Configuration
 * 
 * This singleton provides access to the Supabase backend for:
 * - Authentication (Auth)
 * - Database operations (Postgrest)
 */
object SupabaseClient {
    
    // Your Supabase project credentials
    private const val SUPABASE_URL = "https://ayopmvhtfuwbsjxhpfgd.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF5b3Btdmh0ZnV3YnNqeGhwZmdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyNjAyMTksImV4cCI6MjA4NDgzNjIxOX0.2siGUJfE3iLoaEKae5gycw_6mo748KKyi5C7YEHuUlQ"
    
    /**
     * The Supabase client instance.
     * Use this to make API calls to your Supabase backend.
     */
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        // Install authentication plugin
        install(Auth)
        
        // Install database plugin (for CRUD operations)
        install(Postgrest)
    }
}
