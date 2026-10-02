package com.nuvio.app.features.connection

import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Measures sustained download throughput over one playback session and reports it once.
 *
 * Only ticks where the player is fetching and data actually arrives count. Players stop
 * downloading once their buffer is full, and they also wait on connection setup, redirects and
 * seeks (resume position, file index) without receiving anything; counting either would make a
 * fast connection look slow. A genuinely slow link still delivers data on every tick.
 *
 * The first second of transfer is skipped: it is mostly TCP/TLS slow start and reads low on
 * fast lines. The sample is tied to the network it was measured on and dropped if the
 * default network changed during the measurement. Adds no requests and no polling of its own:
 * it runs on the player's existing progress tick.
 */
internal class PlaybackThroughputSampler(
    sourceUrl: String,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val networkKind: () -> NetworkKind? = { DefaultNetworkObserver.kind },
    private val networkGeneration: () -> Int = { DefaultNetworkObserver.generation },
    private val onSample: (NetworkKind, Double) -> Unit,
) {
    private val isEligible = sourceUrl.isInternetPlaybackSource()
    private var lastTick: TimeMark? = null
    private var warmupMs = 0L
    private var activeBytes = 0L
    private var activeMs = 0L
    private var measuredNetwork: NetworkKind? = null
    private var measuredGeneration = 0
    private var isFinished = false

    /** [bytes] is the number of bytes received since the previous tick. */
    fun onBytesTick(bytes: Long, isFetching: Boolean) {
        tick(isFetching) { bytes }
    }

    /** For players that report a transfer rate rather than a byte count. */
    fun onRateTick(bytesPerSecond: Long, isFetching: Boolean) {
        tick(isFetching) { elapsedMs -> bytesPerSecond * elapsedMs / 1000 }
    }

    fun finish() {
        if (isFinished) return
        isFinished = true
        val network = measuredNetwork ?: return
        if (activeMs < MIN_WINDOW_MS) return
        if (activeBytes < MIN_WINDOW_BYTES && activeMs < SLOW_WINDOW_MS) return
        if (networkGeneration() != measuredGeneration) return
        onSample(network, activeBytes * 8.0 / activeMs / 1000.0)
    }

    private inline fun tick(isFetching: Boolean, bytesFor: (elapsedMs: Long) -> Long) {
        if (!isEligible || isFinished) return
        val previous = lastTick
        lastTick = timeSource.markNow()
        val elapsedMs = previous?.elapsedNow()?.inWholeMilliseconds ?: return
        // A long gap means the app was suspended; the interval says nothing about the network.
        if (!isFetching || elapsedMs <= 0 || elapsedMs > MAX_TICK_GAP_MS) return
        val bytes = bytesFor(elapsedMs)
        if (bytes <= 0L) return
        if (warmupMs < WARMUP_MS) {
            if (warmupMs == 0L) {
                measuredNetwork = networkKind() ?: run { isFinished = true; return }
                measuredGeneration = networkGeneration()
            }
            warmupMs += elapsedMs
            return
        }
        activeBytes += bytes
        activeMs += elapsedMs
        if (activeMs >= MAX_WINDOW_MS) finish()
    }

    private companion object {
        const val WARMUP_MS = 1_000L
        const val MIN_WINDOW_MS = 3_000L
        const val MIN_WINDOW_BYTES = 8L * 1024 * 1024
        const val SLOW_WINDOW_MS = 10_000L
        const val MAX_WINDOW_MS = 10_000L
        const val MAX_TICK_GAP_MS = 2_000L
    }
}
