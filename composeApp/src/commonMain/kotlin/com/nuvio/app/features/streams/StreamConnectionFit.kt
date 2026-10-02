package com.nuvio.app.features.streams

/**
 * Reshaped connection-fit policy, ported to common Kotlin so iOS and Android can share it.
 *
 * Streams whose estimated average bitrate exceeds the measured connection with peak headroom
 * are moved to the end of the list. Unknown sizes/runtimes remain in their original position.
 */
internal class StreamConnectionFit(
    private val runtimeMinutes: Int,
    connectionMbps: Double,
) {
    private val maxBitrateMbps = connectionMbps / BITRATE_HEADROOM

    fun applyToGroups(groups: List<AddonStreamGroup>): List<AddonStreamGroup> {
        var result: ArrayList<AddonStreamGroup>? = null
        for (index in groups.indices) {
            val group = groups[index]
            val streams = apply(group.streams)
            if (streams === group.streams && result == null) continue
            if (result == null) {
                result = ArrayList<AddonStreamGroup>(groups.size).apply {
                    addAll(groups.subList(0, index))
                }
            }
            result += if (streams === group.streams) group else group.copy(streams = streams)
        }
        return result ?: groups
    }

    fun apply(streams: List<StreamItem>): List<StreamItem> {
        if (streams.size < 2) return streams

        var firstHeavy = -1
        var mustMove = false
        for (index in streams.indices) {
            if (isHeavy(streams[index])) {
                if (firstHeavy < 0) firstHeavy = index
            } else if (firstHeavy >= 0) {
                mustMove = true
                break
            }
        }
        if (!mustMove) return streams

        val ordered = ArrayList<StreamItem>(streams.size)
        val heavy = ArrayList<StreamItem>(streams.size - firstHeavy)
        for (index in 0 until firstHeavy) ordered += streams[index]
        heavy += streams[firstHeavy]
        for (index in firstHeavy + 1 until streams.size) {
            val stream = streams[index]
            if (isHeavy(stream)) heavy += stream else ordered += stream
        }
        ordered.addAll(heavy)
        return ordered
    }

    private fun isHeavy(stream: StreamItem): Boolean =
        (stream.averageBitrateMbps(runtimeMinutes) ?: return false) > maxBitrateMbps

    companion object {
        private const val BITRATE_HEADROOM = 1.5
    }
}

private const val MIN_RUNTIME_MINUTES = 10
private const val MAX_RUNTIME_MINUTES = 600
private const val MIN_SIZE_BYTES = 50L * 1024 * 1024
private const val MIN_PLAUSIBLE_MBPS = 0.2
private const val MAX_PLAUSIBLE_MBPS = 200.0

internal fun StreamItem.averageBitrateMbps(runtimeMinutes: Int): Double? {
    if (runtimeMinutes !in MIN_RUNTIME_MINUTES..MAX_RUNTIME_MINUTES) return null
    val sizeBytes = clientResolve?.stream?.raw?.size ?: behaviorHints.videoSize ?: return null
    if (sizeBytes < MIN_SIZE_BYTES) return null
    val mbps = sizeBytes * 8.0 / (runtimeMinutes * 60.0) / 1_000_000.0
    return mbps.takeIf { it in MIN_PLAUSIBLE_MBPS..MAX_PLAUSIBLE_MBPS }
}
