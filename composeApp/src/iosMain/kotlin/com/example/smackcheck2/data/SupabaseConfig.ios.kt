package com.example.smackcheck2.data

import platform.Foundation.NSBundle

/**
 * iOS implementation of SupabaseConfig
 * Reads values from Info.plist at runtime, injected at build-time via .xcconfig.
 *
 * To set up for local development:
 * 1. Create a file iosApp/Config.xcconfig (gitignored) with:
 *      SUPABASE_URL = https://your-project.supabase.co
 *      SUPABASE_ANON_KEY = your-anon-key
 * 2. Reference this xcconfig in your Xcode project build settings
 * 3. In Info.plist the values are referenced as $(SUPABASE_URL) and $(SUPABASE_ANON_KEY)
 *
 * Note: GEMINI_API_KEY and GOOGLE_PLACES_API_KEY are no longer needed client-side.
 * They are stored as Supabase secrets and used only by Edge Functions.
 */
actual object SupabaseConfig {
    actual val SUPABASE_URL: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_URL") as? String)
            ?: error("SUPABASE_URL not found in Info.plist. Add it via .xcconfig or directly.")

    actual val SUPABASE_ANON_KEY: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_ANON_KEY") as? String)
            ?: error("SUPABASE_ANON_KEY not found in Info.plist. Add it via .xcconfig or directly.")
}
