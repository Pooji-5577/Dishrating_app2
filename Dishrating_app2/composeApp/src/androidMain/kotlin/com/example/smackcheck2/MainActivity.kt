package com.example.smackcheck2

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.smackcheck2.data.FirebaseClientProvider
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.data.createFirebaseAuthProvider
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.PreferencesManager

class MainActivity : ComponentActivity() {
    // ImagePicker must be created at Activity level for ActivityResult APIs
    private lateinit var imagePicker: ImagePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        // IMPORTANT: Allow screenshots - clear FLAG_SECURE before calling super
        try {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (e: Exception) {
            println("MainActivity: Failed to clear FLAG_SECURE in onCreate: ${e.message}")
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize Firebase authentication early
        FirebaseClientProvider.initialize(createFirebaseAuthProvider(this))

        // Initialize Supabase session for database operations
        SupabaseClientProvider.initializeSession()

        // ImagePicker must be created before setContent for ActivityResult registration
        imagePicker = ImagePicker(this)

        // CRITICAL: Ensure screenshots are allowed after super.onCreate()
        allowScreenshots()

        setContent {
            val context = LocalContext.current
            val preferencesManager = remember { PreferencesManager(context) }
            val locationService = remember { LocationService(context) }
            val placesService = remember { PlacesService(context) }
            App(
                preferencesManager = preferencesManager,
                locationService = locationService,
                imagePicker = imagePicker,
                placesService = placesService
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-enable screenshots every time the activity resumes
        allowScreenshots()
    }

    override fun onStart() {
        super.onStart()
        // Re-enable screenshots every time the activity starts
        allowScreenshots()
    }

    /**
     * Ensures screenshots are allowed by clearing FLAG_SECURE
     */
    private fun allowScreenshots() {
        try {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            println("MainActivity: Screenshots enabled successfully")
        } catch (e: Exception) {
            println("MainActivity: Failed to enable screenshots: ${e.message}")
            e.printStackTrace()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    App(preferencesManager = preferencesManager)
}