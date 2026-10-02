package com.nuvio.app.features.player

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.io.readByteArray
import io.ktor.utils.io.readRemaining

internal const val MAX_SUBTITLE_BYTES = 20 * 1024 * 1024
internal data class SubtitleHttpHop(val status: Int, val headers: Map<String, String>, val bytes: ByteArray)
internal class SubtitleTransportException(message: String) : IllegalStateException(message)
internal expect fun createSubtitleHttpClient(): HttpClient
private val subtitleHttpClient by lazy { createSubtitleHttpClient() }

/** Dedicated verified-TLS, no-cookie, no-auto-redirect client; never mpv global headers. */
internal suspend fun fetchSubtitleHttpHop(url: String, headers: Map<String, String>, maxBytes: Int): SubtitleHttpHop =
    subtitleHttpClient.prepareGet(url) {
        headers.forEach { (name, value) -> header(name, value) }
    }.execute { response ->
        if (response.headers["Content-Length"]?.toLongOrNull()?.let { it > maxBytes } == true)
            throw SubtitleTransportException("Subtitle exceeds size limit")
        val bytes = response.bodyAsChannel().readRemaining(maxBytes.toLong() + 1).readByteArray()
        if (bytes.size > maxBytes) throw SubtitleTransportException("Subtitle exceeds size limit")
        SubtitleHttpHop(response.status.value, response.headers.entries().associate { it.key.lowercase() to it.value.joinToString(",") }, bytes)
    }

/** Complete retry restarts the original URL/scopes. Cancellation is never swallowed. */
internal suspend fun downloadSubtitleBytes(
    url: String,
    streamUrl: String? = null,
    streamHeaders: Map<String, String> = emptyMap(),
    ownHeaders: Map<String, String> = emptyMap(),
    maxBytes: Int = MAX_SUBTITLE_BYTES,
    waitBeforeRetry: suspend (Long) -> Unit = { delay(it) },
    fetch: suspend (String, Map<String, String>, Int) -> SubtitleHttpHop = { target, headers, limit -> fetchSubtitleHttpHop(target, headers, limit) },
): ByteArray {
    require(maxBytes in 1..MAX_SUBTITLE_BYTES)
    subtitleOrigin(url) ?: throw SubtitleTransportException("Invalid subtitle URL")
    repeat(3) { attempt ->
        try {
            var current = url
            val visited = mutableSetOf<String>()
            repeat(9) {
                if (!visited.add(current)) throw SubtitleTransportException("Subtitle redirect loop")
                val headers = subtitleRequestHeaders(streamUrl, url, current, streamHeaders, ownHeaders)
                val response = fetch(current, headers, maxBytes)
                if (response.status in setOf(301, 302, 303, 307, 308)) {
                    val location = response.headers.entries.firstOrNull { it.key.equals("location", true) }?.value
                        ?: throw SubtitleTransportException("Subtitle redirect has no Location")
                    val next = URLBuilder(current).takeFrom(location).buildString()
                    val nextOrigin = subtitleOrigin(next) ?: throw SubtitleTransportException("Invalid subtitle redirect")
                    if (subtitleOrigin(current)?.scheme == "https" && nextOrigin.scheme == "http")
                        throw SubtitleTransportException("Refusing HTTPS subtitle downgrade")
                    current = next
                } else {
                    if (response.status !in 200..299) {
                        if (response.status !in setOf(408, 429) && response.status < 500)
                            throw SubtitleTransportException("Subtitle HTTP ${response.status}")
                        error("Subtitle HTTP ${response.status}")
                    }
                    if (response.bytes.isEmpty()) error("Empty subtitle response")
                    if (response.bytes.size > maxBytes) throw SubtitleTransportException("Subtitle exceeds size limit")
                    return response.bytes
                }
            }
            throw SubtitleTransportException("Too many subtitle redirects")
        } catch (error: Throwable) {
            if (error is CancellationException || error is SubtitleTransportException || attempt == 2) throw error
            waitBeforeRetry(350L * (attempt + 1))
        }
    }
    error("Unreachable subtitle retry state")
}
