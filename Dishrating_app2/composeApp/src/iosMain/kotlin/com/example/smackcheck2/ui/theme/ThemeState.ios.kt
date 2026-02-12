package com.example.smackcheck2.ui.theme

import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

actual fun isSystemInDarkMode(): Boolean {
    return try {
        UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    } catch (e: Exception) {
        false // Default to light mode if detection fails
    }
}
