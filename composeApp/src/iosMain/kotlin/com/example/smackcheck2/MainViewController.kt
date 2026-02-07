package com.example.smackcheck2

import androidx.compose.ui.window.ComposeUIViewController
import com.example.smackcheck2.platform.PreferencesManager

fun MainViewController() = ComposeUIViewController {
    val preferencesManager = PreferencesManager()
    App(preferencesManager = preferencesManager)
}