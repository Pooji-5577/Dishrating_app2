package com.example.smackcheck2.data

import platform.Foundation.NSBundle

actual object BackendConfig {
    actual val BACKEND_URL: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("BACKEND_URL") as? String)
            ?.takeIf { it.isUsableBackendUrl() }
            ?: "http://localhost:3000"

    actual val CANDIDATE_URLS: List<String> = buildList {
        add(BACKEND_URL)
        addAll(expandDevPorts(BACKEND_URL))
        addAll(expandDevPorts("http://localhost:3000"))
        addAll(expandDevPorts("http://127.0.0.1:3000"))
    }.map { it.trimEnd('/') }.distinct()
}

private fun String.isUsableBackendUrl(): Boolean {
    val value = trim()
    return value.isNotBlank() &&
        !value.startsWith("$(") &&
        value != "http:" &&
        value != "https:" &&
        (value.startsWith("http://") || value.startsWith("https://"))
}

private fun expandDevPorts(url: String, count: Int = 25): List<String> {
    val match = Regex("""^(https?://)([^/:]+)(?::(\d+))?.*""").matchEntire(url) ?: return listOf(url)
    val scheme = match.groupValues[1]
    val host = match.groupValues[2]
    val startPort = match.groupValues[3].toIntOrNull() ?: 3000
    return (startPort until startPort + count).map { port -> "$scheme$host:$port" }
}
