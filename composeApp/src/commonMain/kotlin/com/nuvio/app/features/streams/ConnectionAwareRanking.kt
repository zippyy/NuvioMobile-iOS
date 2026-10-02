package com.nuvio.app.features.streams

import com.nuvio.app.features.connection.ConnectionSpeedEstimator
import com.nuvio.app.features.details.MetaDetailsRepository

internal fun connectionFitPolicy(runtimeMinutes: Int?, connectionMbps: Double?): StreamConnectionFit? {
    if (runtimeMinutes == null || runtimeMinutes !in 10..600 ||
        connectionMbps == null || !connectionMbps.isFinite() || connectionMbps <= 0) return null
    return StreamConnectionFit(runtimeMinutes, connectionMbps)
}

/** Snapshot once per request; never launch network probes or reshuffle during a user's click. */
internal fun connectionFitSnapshot(type: String, videoId: String, parentMetaId: String? = null): Pair<Int?, Double?> {
    ConnectionSpeedEstimator.ensureLoaded()
    val speed = if (ConnectionSpeedEstimator.enabled) ConnectionSpeedEstimator.estimateMbps() else null
    val meta = parentMetaId?.let { MetaDetailsRepository.peek(type, it) }
        ?: MetaDetailsRepository.peek(type, videoId)
        ?: MetaDetailsRepository.uiState.value.meta?.takeIf { meta ->
            meta.id == videoId || meta.id == parentMetaId || meta.videos.any { it.id == videoId }
        }
    val runtime = meta?.videos?.firstOrNull { it.id == videoId }?.runtime
        ?: meta?.runtime?.let(::parseConnectionFitRuntime)
    return runtime to speed
}

internal fun parseConnectionFitRuntime(raw: String): Int? {
    val value = raw.trim()
    val colon = Regex("^(\\d{1,3}):(\\d{1,2})$").matchEntire(value)
    val runtime = if (colon != null) {
        val minutes = colon.groupValues[2].toInt()
        if (minutes >= 60) return null
        colon.groupValues[1].toInt() * 60 + minutes
    } else {
        val hours = Regex("(?i)(\\d{1,3})\\s*h(?:ours?)?").find(value)?.groupValues?.get(1)?.toInt()
        val minutes = Regex("(?i)(\\d{1,3})\\s*m(?:in(?:ute)?s?)?").find(value)?.groupValues?.get(1)?.toInt()
        if (hours != null || minutes != null) (hours ?: 0) * 60 + (minutes ?: 0)
        else value.toIntOrNull()
    }
    return runtime?.takeIf { it in 10..600 }
}
