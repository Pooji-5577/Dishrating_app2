package com.example.smackcheck2.data

import com.example.smackcheck2.BuildConfig

actual object BackendConfig {
    actual val BACKEND_URL: String =
        BuildConfig.BACKEND_URL.takeUnless { it.isBlank() || it.startsWith("MISSING_") }
            ?: "https://api.withcouture.me"

    actual val CANDIDATE_URLS: List<String> = buildList {
        add(BACKEND_URL)
        addAll(expandDevPorts(BACKEND_URL))
    }.map { it.trimEnd('/') }.distinct()
}

private fun expandDevPorts(url: String, count: Int = 25): List<String> {
    val match = Regex("""^(https?://)([^/:]+)(?::(\d+))?.*""").matchEntire(url) ?: return listOf(url)
    val scheme = match.groupValues[1]
    val host = match.groupValues[2]
    val startPort = match.groupValues[3].toIntOrNull() ?: 3000
    return (startPort until startPort + count).map { port -> "$scheme$host:$port" }
}
