package com.example.smackcheck2.platform

import androidx.compose.runtime.staticCompositionLocalOf

expect class ShareService {
    fun shareText(text: String, title: String? = null)
}

val LocalShareService = staticCompositionLocalOf<ShareService?> { null }
