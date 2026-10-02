package com.nuvio.app.features.connection

internal const val MIN_VALID_MBPS = 0.2
internal const val MAX_VALID_MBPS = 1_000.0

internal fun Double.isValidThroughput(): Boolean = isFinite() && this in MIN_VALID_MBPS..MAX_VALID_MBPS

/**
 * True for http(s) sources reached over the internet. Local files, the on-device torrent
 * proxy and LAN servers measure something other than the internet connection.
 */
internal fun String.isInternetPlaybackSource(): Boolean {
    val value = trim()
    val scheme = value.substringBefore("://", missingDelimiterValue = "").lowercase()
    if (scheme != "http" && scheme != "https") return false
    val authority = value.substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')
    val host = if (authority.startsWith("[")) {
        authority.removePrefix("[").substringBefore(']')
    } else {
        authority.substringBefore(':')
    }.lowercase()
    if (host.isEmpty() || host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) {
        return false
    }
    if (':' in host) {
        return host != "::1" && !host.startsWith("fe80:") && !host.startsWith("fc") && !host.startsWith("fd")
    }
    val octets = host.split('.').map { it.toIntOrNull() }
    if (octets.size != 4 || octets.any { it == null || it !in 0..255 }) return true
    val first = octets[0]!!
    val second = octets[1]!!
    return !(
        first == 0 ||
            first == 10 ||
            first == 127 ||
            (first == 169 && second == 254) ||
            (first == 172 && second in 16..31) ||
            (first == 192 && second == 168)
        )
}
