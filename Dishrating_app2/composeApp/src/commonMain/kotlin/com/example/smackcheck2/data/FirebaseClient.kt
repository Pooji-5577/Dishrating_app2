package com.example.smackcheck2.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton Firebase client instance
 * Note: This is a lightweight wrapper for the Firebase auth provider
 * We keep Supabase for database, storage, and realtime features
 */
object FirebaseClientProvider {

    // Lazy initialization of the auth provider
    // This will be initialized on first access
    lateinit var authProvider: FirebaseAuthProvider
        private set

    private var isInitialized = false

    /**
     * Initialize Firebase auth provider
     * Call this early in the app lifecycle (e.g., in Application.onCreate or MainActivity.onCreate)
     * @param provider Platform-specific FirebaseAuthProvider instance
     */
    fun initialize(provider: FirebaseAuthProvider) {
        if (!isInitialized) {
            authProvider = provider
            isInitialized = true
            println("FirebaseClient: Auth provider initialized")
        }
    }

    /**
     * Initialize and check session
     * Call this early in the app lifecycle to ensure auth state is restored
     */
    fun initializeSession() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (!isInitialized) {
                    println("FirebaseClient: Auth provider not initialized yet")
                    return@launch
                }

                // Firebase automatically restores session on initialization
                val currentUser = authProvider.getCurrentUser()
                println("FirebaseClient: Session initialized, user: ${currentUser?.uid ?: "none"}")
            } catch (e: Exception) {
                println("FirebaseClient: Failed to initialize session: ${e.message}")
            }
        }
    }

    /**
     * Check if the client is initialized
     */
    fun isInitialized(): Boolean = isInitialized
}
