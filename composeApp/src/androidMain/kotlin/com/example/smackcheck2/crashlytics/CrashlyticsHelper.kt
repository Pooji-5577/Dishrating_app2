package com.example.smackcheck2.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralised Crashlytics wrapper.
 *
 * Usage:
 *   CrashlyticsHelper.setUser(userId)
 *   CrashlyticsHelper.log("Opened RestaurantDetailScreen")
 *   CrashlyticsHelper.recordNonFatal(exception)
 *
 * To simulate a crash in a debug build and verify the alert fires:
 *   CrashlyticsHelper.simulateCrash()
 *
 * NOTE: Requires a real google-services.json (not the placeholder).
 * Download it from Firebase Console → Project Settings → Your apps → Android app
 * and replace composeApp/google-services.json.
 */
object CrashlyticsHelper {

    private fun instanceOrNull(): FirebaseCrashlytics? =
        runCatching { FirebaseCrashlytics.getInstance() }
            .onFailure { println("CrashlyticsHelper: Firebase Crashlytics unavailable: ${it.message}") }
            .getOrNull()

    /** Attach the signed-in user's ID so crashes are grouped per user in the console. */
    fun setUser(userId: String) {
        instanceOrNull()?.setUserId(userId)
    }

    /** Clear user identity on sign-out. */
    fun clearUser() {
        instanceOrNull()?.setUserId("")
    }

    /**
     * Add a breadcrumb log visible in the crash report timeline.
     * Call this at key navigation or lifecycle events.
     */
    fun log(message: String) {
        instanceOrNull()?.log(message)
    }

    /** Record a caught exception as a non-fatal issue (appears in Crashlytics dashboard). */
    fun recordNonFatal(throwable: Throwable) {
        instanceOrNull()?.recordException(throwable)
    }

    /** Attach arbitrary key-value metadata to the next crash report. */
    fun setKey(key: String, value: String) {
        instanceOrNull()?.setCustomKey(key, value)
    }

    fun setKey(key: String, value: Int) {
        instanceOrNull()?.setCustomKey(key, value)
    }

    fun setKey(key: String, value: Boolean) {
        instanceOrNull()?.setCustomKey(key, value)
    }

    /**
     * Force an unhandled exception to verify that Crashlytics captures and uploads it.
     *
     * Steps to test:
     *  1. Replace google-services.json with your real Firebase project file.
     *  2. Build a RELEASE variant (Crashlytics is disabled in debug by default;
     *     or call enableCollection() below to force-enable in debug).
     *  3. Call this method once (e.g. from a hidden debug button or a test in onCreate).
     *  4. Relaunch the app — Crashlytics uploads the crash on next launch.
     *  5. Within ~5 minutes the crash appears in Firebase Console → Crashlytics.
     *  6. Real-time alerts fire automatically if configured (see README below).
     *
     * ── Setting up real-time crash alerts ──────────────────────────────────
     *  Firebase Console → your project → Crashlytics → (kebab menu) → Alerts
     *  Enable "New issues" and "Velocity alerts". Add your email or a Slack
     *  webhook. Alerts fire within minutes of the first occurrence of a new
     *  crash signature.
     */
    fun simulateCrash() {
        instanceOrNull()?.log("simulateCrash() called — intentional test crash")
        throw RuntimeException(
            "SmackCheck · Crashlytics test crash — safe to ignore in Firebase Console"
        )
    }

    /**
     * Force-enable collection in debug builds.
     * Call once from Application/Activity if you want to test Crashlytics
     * without building a release APK.
     */
    fun enableCollection() {
        instanceOrNull()?.setCrashlyticsCollectionEnabled(true)
    }
}
