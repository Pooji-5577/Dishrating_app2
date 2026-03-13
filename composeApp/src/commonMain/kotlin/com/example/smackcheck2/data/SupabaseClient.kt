package com.example.smackcheck2.data

import com.example.smackcheck2.data.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.FlowType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton Supabase client instance with session persistence
 */
// Type alias for backward compatibility with code using "SupabaseClient"
typealias SupabaseClient = SupabaseClientProvider

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
            install(Realtime)
        }
    }

    /**
     * Initialize and load session from storage.
     * Also ensures the user's profile exists in the database (self-healing for existing users
     * who may have signed up before profile creation was properly implemented).
     * Call this early in the app lifecycle (e.g., in Application.onCreate or MainActivity.onCreate)
     */
    fun initializeSession() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // The session will be automatically loaded due to autoLoadFromStorage = true
                // This call ensures the client is initialized
                val currentAuthUser = client.auth.currentUserOrNull()
                println("SupabaseClient: Session initialized, user: ${currentAuthUser?.id ?: "none"}")
                
                // If user is logged in, ensure their profile exists in the database
                // This handles users who signed up but profile creation failed or was skipped
                if (currentAuthUser != null) {
                    println("SupabaseClient: Ensuring profile exists for user ${currentAuthUser.id}...")
                    try {
                        val authRepository = AuthRepository()
                        val user = authRepository.getCurrentUser()
                        if (user != null) {
                            println("SupabaseClient: ✓ Profile verified/created for ${user.name} (${user.id})")
                        } else {
                            println("SupabaseClient: ⚠ Could not verify/create profile")
                        }
                    } catch (e: Exception) {
                        println("SupabaseClient: ⚠ Profile verification failed: ${e.message}")
                        // Don't throw - user can still use the app, profile will be created on next action
                    }
                }
            } catch (e: Exception) {
                println("SupabaseClient: Failed to initialize session: ${e.message}")
            }
        }
    }
}
