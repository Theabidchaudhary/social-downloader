package app.siphon.data.remote

import app.siphon.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiException(val code: String, override val message: String) : IOException(message)

/**
 * Thin, hand-written HTTP client for the two Siphon endpoints the app uses.
 * OkHttp + kotlinx.serialization keeps the dependency surface minimal and the
 * request/response mapping fully explicit.
 *
 * The base URL is resolved fresh on every call via [baseUrlProvider] rather
 * than fixed at construction time, so a user can point the app at any
 * self-hosted or ad-hoc (Render/Railway free tier, etc.) backend from
 * Settings without reinstalling the app. [BuildConfig.SIPHON_API_BASE_URL] is
 * only the fallback when no override is configured.
 */
class SiphonApi(
    private val client: OkHttpClient,
    private val baseUrlProvider: suspend () -> String = { BuildConfig.SIPHON_API_BASE_URL },
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun resolve(url: String): ResolveResponseDto = withContext(Dispatchers.IO) {
        val base = baseUrlProvider()
        val body = """{"url":${Json.encodeToString(kotlinx.serialization.serializer<String>(), url)}}"""
        val request = Request.Builder()
            .url("${base.trimEnd('/')}/api/v1/resolve")
            .post(body.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).await().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw parseError(text, response.code)
            json.decodeFromString(ResolveResponseDto.serializer(), text)
        }
    }

    /** Current effective base URL — needed to absolutize relative downloadUrls. */
    suspend fun baseUrl(): String = baseUrlProvider()

    /** Used by the Settings "Test connection" action — checks an arbitrary candidate URL, not the configured one. */
    suspend fun ping(candidateBaseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${candidateBaseUrl.trimEnd('/')}/healthz").get().build()
            client.newCall(request).await().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun parseError(bodyText: String, httpCode: Int): ApiException = try {
        val parsed = json.decodeFromString(ApiErrorBodyDto.serializer(), bodyText)
        ApiException(parsed.error?.code ?: "HTTP_$httpCode", parsed.error?.message ?: "Request failed ($httpCode)")
    } catch (_: Exception) {
        ApiException("HTTP_$httpCode", "Request failed ($httpCode)")
    }

    companion object {
        /** Turns a relative downloadUrl from a resolve response into an absolute URL. */
        fun absoluteUrl(pathOrUrl: String, base: String): String =
            if (pathOrUrl.startsWith("http")) pathOrUrl else base.trimEnd('/') + pathOrUrl
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation { runCatching { cancel() } }
}
