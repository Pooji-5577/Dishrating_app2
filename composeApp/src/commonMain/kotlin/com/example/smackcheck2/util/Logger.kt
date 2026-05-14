package com.example.smackcheck2.util

/**
 * KMP-safe logger that can be disabled for production builds.
 *
 * Call [Logger.init] with `isDebug = true` from platform-specific
 * entry points (e.g., Android Application.onCreate using BuildConfig.DEBUG).
 * When disabled, all log calls are no-ops.
 */
object Logger {
    var isDebugEnabled: Boolean = false
        private set

    fun init(isDebug: Boolean) {
        isDebugEnabled = isDebug
    }

    fun d(tag: String, message: String) {
        if (isDebugEnabled) println("D/$tag: $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebugEnabled) {
            println("E/$tag: $message")
            throwable?.printStackTrace()
        }
    }

    fun w(tag: String, message: String) {
        if (isDebugEnabled) println("W/$tag: $message")
    }
}
