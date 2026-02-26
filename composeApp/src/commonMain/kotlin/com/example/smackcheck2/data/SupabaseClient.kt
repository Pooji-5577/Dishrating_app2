package com.example.smackcheck2.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.FlowType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton Supabase client instance with session persistence
 */
object SupabaseClientProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                // Enable automatic session persistence
                autoLoadFromStorage = true
                autoSaveToStorage = true
                // Use implicit flow type for better session handling
                flowType = FlowType.IMPLICIT
                // Deep link scheme for OAuth redirects
                scheme = "smackcheck"
                host = "login"
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }

    /**
     * Initialize and load session from storage
     * Call this early in the app lifecycle (e.g., in Application.onCreate or MainActivity.onCreate)
     */
    fun initializeSession() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // The session will be automatically loaded due to autoLoadFromStorage = true
                // This call ensures the client is initialized
                val currentUser = client.auth.currentUserOrNull()
                println("SupabaseClient: Session initialized, user: ${currentUser?.id ?: "none"}")
            } catch (e: Exception) {
                println("SupabaseClient: Failed to initialize session: ${e.message}")
            }
        }
    }
}
