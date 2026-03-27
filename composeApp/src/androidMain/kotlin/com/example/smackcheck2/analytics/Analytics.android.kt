package com.example.smackcheck2.analytics

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

actual object Analytics {
    private var mixpanel: MixpanelAPI? = null
    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    actual fun initialize(token: String) {
        val ctx = appContext ?: return
        mixpanel = MixpanelAPI.getInstance(ctx, token, true)
    }

    actual fun identify(userId: String) {
        mixpanel?.identify(userId)
    }

    actual fun track(event: String, properties: Map<String, Any?>) {
        val json = JSONObject()
        properties.forEach { (key, value) ->
            json.put(key, value)
        }
        mixpanel?.track(event, json)
    }

    actual fun reset() {
        mixpanel?.reset()
    }
}
