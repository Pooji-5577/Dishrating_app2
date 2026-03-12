package com.example.smackcheck2.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class ShareService {
    actual fun shareText(text: String, title: String?) {
        val items = listOf(text)
        val activityController = UIActivityViewController(
            activityItems = items,
            applicationActivities = null
        )
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityController, animated = true, completion = null)
    }
}
