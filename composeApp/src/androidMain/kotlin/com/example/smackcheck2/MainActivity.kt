package com.example.smackcheck2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.SupabaseClientProvider
import io.github.jan.supabase.auth.handleDeeplinks
import com.example.smackcheck2.location.AppLocationManager
import com.example.smackcheck2.platform.AutoLocationManager
import com.example.smackcheck2.platform.GeofencingService
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.PreferencesManager
import com.example.smackcheck2.platform.ShareService

class MainActivity : ComponentActivity() {
    // ImagePicker must be created at Activity level for ActivityResult APIs
    private lateinit var imagePicker: ImagePicker
    
    // AutoLocationManager for automatic location detection on app resume
    private lateinit var autoLocationManager: AutoLocationManager

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Microsoft Clarity for session recording & heatmaps
        Clarity.initialize(applicationContext, ClarityConfig(projectId = "w0rgf1ugzh"))

        // Initialize Mixpanel analytics
        Analytics.setContext(applicationContext)
        Analytics.initialize(BuildConfig.MIXPANEL_TOKEN)

        // Initialize Supabase session early for session restoration
        SupabaseClientProvider.initializeSession()
        // Handle deep link if app was launched via OAuth redirect
        SupabaseClientProvider.client.handleDeeplinks(intent)

        // Set app context for push notification channels
        com.example.smackcheck2.notifications.SmackCheckNotificationHelper.appContext = applicationContext

        // Initialize AppLocationManager for global location state management
        AppLocationManager.initialize(applicationContext)
        
        // Initialize and start AutoLocationManager for automatic location detection
        autoLocationManager = AutoLocationManager(applicationContext)
        autoLocationManager.startAutoDetection()

        // ImagePicker must be created before setContent for ActivityResult registration
        imagePicker = ImagePicker(this)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val preferencesManager = remember { PreferencesManager(context) }
            val locationService = remember { LocationService(context) }
            val placesService = remember { PlacesService() }
            val shareService = remember { ShareService(context) }
            val geofencingService = remember { GeofencingService(context) }
            App(
                preferencesManager = preferencesManager,
                locationService = locationService,
                imagePicker = imagePicker,
                placesService = placesService,
                shareService = shareService,
                geofencingService = geofencingService
            )
        }
    }
    
    override fun onDestroy() {
        // Stop automatic location detection when activity is destroyed
        if (::autoLocationManager.isInitialized) {
            autoLocationManager.stopAutoDetection()
        }
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    App(preferencesManager = preferencesManager)
}