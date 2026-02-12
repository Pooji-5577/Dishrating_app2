package com.example.smackcheck2.ui.theme

import android.content.res.Configuration
import android.content.res.Resources

actual fun isSystemInDarkMode(): Boolean {
    return try {
        val config = Resources.getSystem().configuration
        (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    } catch (e: Exception) {
        false // Default to light mode if detection fails
    }
}
