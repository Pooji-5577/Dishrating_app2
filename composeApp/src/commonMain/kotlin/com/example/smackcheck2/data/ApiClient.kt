package com.example.smackcheck2.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Centralised HTTP client for all communication with the SmackCheck custom backend.
 *
 * Auth: pulls the Supabase JWT from the current Auth session and attaches it as
 *       "Authorization: Bearer <token>" on every request.
 *
 * Usage:
 *   val data: MyDto = ApiClient.get("restaurants/abc123")
 *   val result: ResponseDto = ApiClient.post("ratings", body = myRequestDto)
 */
object ApiClient {

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        coerceInputValues = true
    }

    @PublishedApi
    internal val http = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
    }

    @PublishedApi
    internal suspend fun token(): String =
        SupabaseClientProvider.client.auth.currentSessionOrNull()?.accessToken ?: ""

    @PublishedApi
    internal var resolvedBackendUrl: String? = null

    private val backendResolveMutex = Mutex()

    @PublishedApi
    internal suspend fun apiBase(): String = "${resolveBackendUrl()}/api"

    private suspend fun resolveBackendUrl(): String {
        resolvedBackendUrl?.let { return it }

        return backendResolveMutex.withLock {
            resolvedBackendUrl?.let { return@withLock it }

            val fallback = BackendConfig.BACKEND_URL.trimEnd('/')
            val resolved = BackendConfig.CANDIDATE_URLS
                .asSequence()
                .map { it.trimEnd('/') }
                .filter { it.isNotBlank() && !it.startsWith("MISSING_") }
                .distinct()
                .firstOrNull { candidate -> isHealthyBackend(candidate) }
                ?: fallback

            resolvedBackendUrl = resolved
            resolved
        }
    }

    private suspend fun isHealthyBackend(url: String): Boolean {
        return try {
            withTimeout(700) {
                http.get("$url/health").status.isSuccess()
            }
        } catch (_: Exception) {
            false
        }
    }

    // ── GET ──────────────────────────────────────────────────────────────────

    suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String?> = emptyMap()
    ): T {
        val tok = token()
        val base = apiBase()
        return http.get("$base/$path") {
            bearerAuth(tok)
            params.forEach { (k, v) -> if (v != null) parameter(k, v) }
        }.body()
    }

    suspend fun getJsonElement(
        path: String,
        params: Map<String, String?> = emptyMap()
    ): JsonElement {
        val tok = token()
        val base = apiBase()
        val text = http.get("$base/$path") {
            bearerAuth(tok)
            params.forEach { (k, v) -> if (v != null) parameter(k, v) }
        }.bodyAsText()
        return json.parseToJsonElement(text)
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    suspend inline fun <reified B, reified R> post(path: String, body: B): R {
        val tok = token()
        val base = apiBase()
        return http.post("$base/$path") {
            bearerAuth(tok)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    /** POST with no body — returns parsed response */
    suspend inline fun <reified R> postEmpty(path: String): R {
        val tok = token()
        val base = apiBase()
        return http.post("$base/$path") { bearerAuth(tok) }.body()
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    suspend inline fun <reified B, reified R> put(path: String, body: B): R {
        val tok = token()
        val base = apiBase()
        return http.put("$base/$path") {
            bearerAuth(tok)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    // ── PATCH ────────────────────────────────────────────────────────────────

    suspend inline fun <reified B, reified R> patch(path: String, body: B): R {
        val tok = token()
        val base = apiBase()
        return http.patch("$base/$path") {
            bearerAuth(tok)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    suspend fun delete(path: String): Boolean {
        val tok = token()
        val base = apiBase()
        return http.delete("$base/$path") { bearerAuth(tok) }.status.isSuccess()
    }

    suspend inline fun <reified B> deleteWithBody(path: String, body: B): Boolean {
        val tok = token()
        val base = apiBase()
        return http.delete("$base/$path") {
            bearerAuth(tok)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.status.isSuccess()
    }

    // ── Multipart upload ─────────────────────────────────────────────────────

    /**
     * Upload image bytes to /api/storage/upload.
     * [bucket] must be one of: dish, profile, restaurant, story
     * Returns { url: String, path: String }
     */
    suspend fun uploadImage(
        bucket: String,
        imageBytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg"
    ): StorageUploadResponse {
        val tok = token()
        val base = apiBase()
        return http.submitFormWithBinaryData(
            url = "$base/storage/upload",
            formData = formData {
                append("bucket", bucket)
                append("file", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        ) {
            bearerAuth(tok)
        }.body()
    }
}

@kotlinx.serialization.Serializable
data class StorageUploadResponse(val url: String, val path: String)
