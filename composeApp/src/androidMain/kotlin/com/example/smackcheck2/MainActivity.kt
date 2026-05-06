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
import androidx.lifecycle.lifecycleScope
import com.example.smackcheck2.crashlytics.CrashlyticsHelper
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.example.smackcheck2.analytics.Analytics
import com.example.smackcheck2.data.SupabaseClientProvider
import io.github.jan.supabase.auth.handleDeeplinks
import com.example.smackcheck2.location.AppLocationManager
import com.example.smackcheck2.notifications.NotificationDeepLink
import com.example.smackcheck2.notifications.NotificationRouteTarget
import com.example.smackcheck2.notifications.SmackCheckFirebaseMessagingService
import com.example.smackcheck2.platform.AutoLocationManager
import com.example.smackcheck2.platform.GeofencingService
import com.example.smackcheck2.platform.ImagePicker
import com.example.smackcheck2.platform.LocationService
import com.example.smackcheck2.platform.PlacesService
import com.example.smackcheck2.platform.PreferencesManager
import com.example.smackcheck2.platform.ShareService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // ImagePicker must be created at Activity level for ActivityResult APIs
    private lateinit var imagePicker: ImagePicker
    
    // AutoLocationManager for automatic location detection on app resume
    private lateinit var autoLocationManager: AutoLocationManager

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent)
        handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── Crashlytics ──────────────────────────────────────────────────────
        // Force-enable collection so crashes are reported in all build types.
        // Remove enableCollection() once you switch to a release build workflow
        // where Crashlytics is enabled by default.
        CrashlyticsHelper.enableCollection()
        CrashlyticsHelper.log("MainActivity.onCreate")

        // ── Uncomment the line below ONCE to verify your Firebase project is
        // ── wired up correctly, then remove it before shipping.
        // CrashlyticsHelper.simulateCrash()

        // Initialize Microsoft Clarity for session recording & heatmaps
        Clarity.initialize(applicationContext, ClarityConfig(projectId = "w0rgf1ugzh"))

        // Initialize Mixpanel analytics
        Analytics.setContext(applicationContext)
        Analytics.initialize(BuildConfig.MIXPANEL_TOKEN)

        // Initialize Supabase session early for session restoration
        SupabaseClientProvider.initializeSession()
        // Handle deep link if app was launched via OAuth redirect
        SupabaseClientProvider.client.handleDeeplinks(intent)
        // Handle deep link from a tapped push notification, if any
        handleNotificationIntent(intent)

        // Set app context for push notification channels
        com.example.smackcheck2.notifications.SmackCheckNotificationHelper.appContext = applicationContext

        // Initialize AppLocationManager for global location state management
        AppLocationManager.initialize(applicationContext)
        
        // Initialize and start AutoLocationManager for automatic location detection
        autoLocationManager = AutoLocationManager(applicationContext)
        autoLocationManager.startAutoDetection()

        // ImagePicker must be created before setContent for ActivityResult registration
        imagePicker = ImagePicker(this)

        // Track Mixpanel day-one retention after the first 24h window
        val appPreferencesManager = PreferencesManager(applicationContext)
        scheduleDayOneRetentionCheck(appPreferencesManager)

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

    private fun handleNotificationIntent(intent: Intent?) {
        intent ?: return
        if (!intent.getBooleanExtra(SmackCheckFirebaseMessagingService.EXTRA_FROM_NOTIFICATION, false)) {
            return
        }
        val screen = intent.getStringExtra(SmackCheckFirebaseMessagingService.EXTRA_SCREEN) ?: return
        NotificationDeepLink.push(
            NotificationRouteTarget(
                screen = screen,
                dishId = intent.getStringExtra(SmackCheckFirebaseMessagingService.EXTRA_DISH_ID),
                reviewId = intent.getStringExtra(SmackCheckFirebaseMessagingService.EXTRA_REVIEW_ID),
                ratingId = intent.getStringExtra(SmackCheckFirebaseMessagingService.EXTRA_RATING_ID),
                userId = intent.getStringExtra(SmackCheckFirebaseMessagingService.EXTRA_USER_ID)
            )
        )
        // Clear the extras so the same notification tap isn't replayed on config change / rotation.
        intent.removeExtra(SmackCheckFirebaseMessagingService.EXTRA_FROM_NOTIFICATION)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    App(preferencesManager = preferencesManager)
}

private fun MainActivity.scheduleDayOneRetentionCheck(preferencesManager: PreferencesManager) {
    lifecycleScope.launch {
        val firstOpenTimestamp = preferencesManager.getFirstOpenTimestamp()
        val now = System.currentTimeMillis()
        if (firstOpenTimestamp == 0L) {
            preferencesManager.saveFirstOpenTimestamp(now)
            return@launch
        }

        val alreadyTracked = preferencesManager.isDay1RetentionTracked()
        val hasReachedDayOne = now - firstOpenTimestamp >= DAY_ONE_RETENTION_WINDOW_MS
        if (!alreadyTracked && hasReachedDayOne) {
            Analytics.track(
                event = "day_1_retention",
                properties = mapOf(
                    "first_open_timestamp" to firstOpenTimestamp,
                    "retained_at" to now
                )
            )
            preferencesManager.setDay1RetentionTracked()
        }
    }
}

private const val DAY_ONE_RETENTION_WINDOW_MS = 24L * 60L * 60L * 1000L