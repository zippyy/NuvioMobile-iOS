package com.nuvio.app.features.player

private val crossOriginSubtitleHeaders = setOf("referer", "origin", "user-agent", "accept-language", "accept")
private val forbiddenSubtitleHeaders = setOf("host", "range", "connection", "transfer-encoding", "content-length", "accept-encoding", "proxy-authorization", "proxy-connection", "te", "trailer", "upgrade")

internal data class SubtitleOrigin(val scheme: String, val host: String, val port: Int)

/** Reject ambiguous authorities/user-info rather than comparing raw host substrings. */
internal fun subtitleOrigin(url: String?): SubtitleOrigin? {
    if (url == null || url.any { it <= ' ' || it == '\\' }) return null
    val match = Regex("^(https?)://(\\[[0-9a-fA-F:]+]|[a-zA-Z0-9.-]+)(?::([0-9]+))?(?:[/?#]|$)", RegexOption.IGNORE_CASE).find(url) ?: return null
    val scheme = match.groupValues[1].lowercase()
    val portText = match.groupValues[3]
    val port = if (portText.isEmpty()) (if (scheme == "https") 443 else 80) else portText.toIntOrNull() ?: return null
    if (port !in 1..65535) return null
    return SubtitleOrigin(scheme, match.groupValues[2].lowercase(), port)
}

/** Rebuild from immutable source scopes for EVERY hop, never from the last request. */
internal fun subtitleRequestHeaders(
    streamUrl: String?, subtitleUrl: String, destinationUrl: String,
    streamHeaders: Map<String, String>, ownHeaders: Map<String, String>,
    allowCredentials: Boolean = true,
): Map<String, String> {
    val destination = subtitleOrigin(destinationUrl) ?: error("Invalid subtitle HTTP URL")
    val streamOrigin = subtitleOrigin(streamUrl)
    val ownOrigin = subtitleOrigin(subtitleUrl) ?: error("Invalid subtitle HTTP URL")
    return buildMap {
        fun appendScoped(headers: Map<String, String>, origin: SubtitleOrigin?) {
            val downgrade = origin?.scheme == "https" && destination.scheme == "http"
            for ((rawName, value) in headers) {
                val name = rawName.trim().lowercase()
                if (!Regex("^[!#$%&'*+.^_`|~0-9a-z-]+$").matches(name) || value.isBlank() || value.any { it == '\r' || it == '\n' || it == '\u0000' }) continue
                if (name in forbiddenSubtitleHeaders) continue
                if ((!allowCredentials || origin != destination) && name !in crossOriginSubtitleHeaders) continue
                if (downgrade && name in setOf("referer", "origin")) continue
                put(name, value)
            }
        }
        appendScoped(streamHeaders, streamOrigin)
        appendScoped(ownHeaders, ownOrigin)
        if (!containsKey("accept")) put("accept", "text/vtt, application/x-subrip, text/plain, */*")
        if (!containsKey("user-agent")) put("user-agent", "Nuvio/1.0")
    }
}
