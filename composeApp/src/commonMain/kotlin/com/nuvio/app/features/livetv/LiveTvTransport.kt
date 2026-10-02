package com.nuvio.app.features.livetv

/** Last map wins case-insensitively. Never pass hop-by-hop or injected headers to a player. */
fun safeLiveTvHeaders(vararg maps: Map<String, String>): Map<String, String> {
    val result = linkedMapOf<String, String>()
    maps.forEach { headers -> headers.forEach { (rawKey, rawValue) ->
        val key = rawKey.trim(); val value = rawValue.trim()
        if (Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+").matches(key) && value.isNotEmpty() &&
            rawValue.none { it == '\r' || it == '\n' || it == '\u0000' } &&
            key.lowercase() !in setOf("range", "host", "connection", "content-length", "transfer-encoding")) {
            result.keys.firstOrNull { it.equals(key, true) }?.let(result::remove)
            result[key] = value
        }
    } }
    return result
}
fun encodeLiveTvComponent(value: String): String = buildString {
    value.encodeToByteArray().forEach { b ->
        val n = b.toInt() and 255
        if (n in 65..90 || n in 97..122 || n in 48..57 || n.toChar() in "-._~") append(n.toChar())
        else { append('%'); append("0123456789ABCDEF"[n / 16]); append("0123456789ABCDEF"[n % 16]) }
    }
}
fun decodeLiveTvComponent(value: String): String {
    val bytes = ArrayList<Byte>(); var i = 0
    while (i < value.length) {
        if (value[i] == '%' && i + 2 < value.length && value.substring(i + 1, i + 3).toIntOrNull(16) != null) {
            bytes += value.substring(i + 1, i + 3).toInt(16).toByte(); i += 3
        } else { bytes.addAll(value[i].toString().encodeToByteArray().toList()); i++ }
    }
    return bytes.toByteArray().decodeToString()
}
fun requireLiveTvUrl(url: String): String {
    require(url.isHttpUrl() && url.none { it.isWhitespace() || it == '\u0000' }) { "Enter a valid HTTP or HTTPS URL" }
    require(url.substringAfter("://").substringBefore('/').substringBefore('?').isNotBlank()) { "Missing server host" }
    return url
}
fun liveTvOrigin(url: String) = url.substringBefore("://") + "://" + url.substringAfter("://").substringBefore('/').substringBefore('?')
fun liveTvRedirectHeaders(from: String, to: String, headers: Map<String,String>): Map<String,String> {
    requireLiveTvUrl(to)
    require(!from.startsWith("https://",true) || to.startsWith("https://",true)) { "Refusing insecure redirect" }
    return if(liveTvOrigin(from).equals(liveTvOrigin(to),true)) safeLiveTvHeaders(headers)
        else safeLiveTvHeaders(headers.filterKeys { it.equals("Accept",true) || it.equals("User-Agent",true) })
}
fun redactedLiveTvError(error: Throwable): String = when (error) {
    is LiveTvHttpException -> "Server returned HTTP ${error.status}"
    else -> "Unable to load source. Check its address, credentials and connection."
}
class LiveTvHttpException(val status: Int) : Exception("HTTP $status")
fun interface LiveTvTransport { suspend fun text(url: String, headers: Map<String, String>): String }
