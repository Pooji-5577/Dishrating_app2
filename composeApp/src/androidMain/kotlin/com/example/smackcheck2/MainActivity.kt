package com.example.smackcheck2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.smackcheck2.data.SupabaseClientProvider
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService

class MainActivity : ComponentActivity() {
    // ImagePicker must be created at Activity level for ActivityResult APIs
    private lateinit var imagePicker: ImagePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Supabase session early for session restoration
        SupabaseClientProvider.initializeSession()

        // ImagePicker must be created before setContent for ActivityResult registration
        imagePicker = ImagePicker(this)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val locationService = remember { LocationService(context) }
            val placesService = remember { PlacesService(context) }
            App(
                locationService = locationService,
                imagePicker = imagePicker,
                placesService = placesService
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}