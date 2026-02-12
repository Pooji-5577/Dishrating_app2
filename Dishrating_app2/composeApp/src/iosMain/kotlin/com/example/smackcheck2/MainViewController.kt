package com.example.smackcheck2

import androidx.compose.ui.window.ComposeUIViewController
import com.example.smackcheck2.data.FirebaseClientProvider
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.createFirebaseAuthProvider
import com.example.smackcheck2.platform.PreferencesManager

fun MainViewController() = ComposeUIViewController {
    // Initialize Firebase authentication early
    if (!FirebaseClientProvider.isInitialized()) {
        FirebaseClientProvider.initialize(createFirebaseAuthProvider())
    }

    // Initialize Supabase session for database operations
    SupabaseClientProvider.initializeSession()

    val preferencesManager = PreferencesManager()
    App(preferencesManager = preferencesManager)
}