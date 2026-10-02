package com.nuvio.app.features.connection

import com.nuvio.app.core.storage.ProfileScopedKey
import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Learns sustained download throughput from real playback, per [NetworkKind], and owns the
 * "match streams to connection" preference.
 *
 * Samples come passively from [PlaybackThroughputSampler]; nothing is measured when a stream list
 * loads, so reading the estimate is a memory lookup.
 */
internal object ConnectionSpeedEstimator {
    private const val PREFS_NAME = "nuvio_tv_connection_speed"
    private const val KEY_SAMPLES = "throughput_samples_v2"
    private const val KEY_ENABLED = "prefer_connection_fit"
    private const val MAX_SAMPLES_PER_NETWORK = 3
    private const val MIN_SAMPLES = 2
    private const val MAX_SAMPLE_AGE_MS = 14L * 24 * 60 * 60 * 1000

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = SynchronizedObject()

    private var cachedSamples: List<ConnectionSpeedSample>? = null

    @Volatile
    private var loaded = false

    @Volatile
    var enabled: Boolean = true
        private set

    /** Bumped on every recorded sample, so a settings row can refresh its status line. */
    @Volatile
    var revision: Int = 0
        private set

    fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            DefaultNetworkObserver.start()
            enabled = ConnectionSpeedStorage.getBoolean(ProfileScopedKey.of(KEY_ENABLED), true)
            loaded = true
        }
    }

    fun setEnabled(enabled: Boolean) {
        ensureLoaded()
        this.enabled = enabled
        ConnectionSpeedStorage.putBoolean(ProfileScopedKey.of(KEY_ENABLED), enabled)
    }

    /** The estimate for the current network, or null while still learning or offline. */
    fun estimateMbps(): Double? {
        ensureLoaded()
        val network = DefaultNetworkObserver.kind ?: return null
        return estimateMbps(loadedSamples(), network, currentTimeMillis())
    }

    /** Records a sample for [network], the network it was measured on (not necessarily the current one). */
    fun record(network: NetworkKind, mbps: Double) {
        if (!mbps.isValidThroughput()) return
        ensureLoaded()
        synchronized(lock) {
            val updated = appendSample(
                loadedSamples(),
                ConnectionSpeedSample(network, mbps, currentTimeMillis())
            )
            cachedSamples = updated
            ConnectionSpeedStorage.putString(ProfileScopedKey.of(KEY_SAMPLES), json.encodeToString(updated))
        }
        revision++
    }

    /**
     * The best of the last few samples. Each sample is capped by whichever server delivered it,
     * so the fastest one is the tightest lower bound on the connection itself; one slow host
     * must not make every other source look unplayable. Only the most recent samples count, so
     * a connection that really got slower takes over within a few playbacks. At least two
     * samples are required so a single session never drives ranking on its own.
     */
    internal fun estimateMbps(
        samples: List<ConnectionSpeedSample>,
        network: NetworkKind,
        nowMs: Long,
    ): Double? {
        var count = 0
        var best = 0.0
        for (index in samples.indices.reversed()) {
            val sample = samples[index]
            if (sample.network != network) continue
            if (nowMs - sample.recordedAtMs !in 0..MAX_SAMPLE_AGE_MS || !sample.mbps.isValidThroughput()) continue
            if (sample.mbps > best) best = sample.mbps
            if (++count == MAX_SAMPLES_PER_NETWORK) break
        }
        return best.takeIf { count >= MIN_SAMPLES }
    }

    internal fun appendSample(
        samples: List<ConnectionSpeedSample>,
        sample: ConnectionSpeedSample,
    ): List<ConnectionSpeedSample> {
        val (sameNetwork, otherNetworks) = samples.partition { it.network == sample.network }
        return otherNetworks + sameNetwork.takeLast(MAX_SAMPLES_PER_NETWORK - 1) + sample
    }

    private fun loadedSamples(): List<ConnectionSpeedSample> =
        cachedSamples ?: synchronized(lock) {
            cachedSamples ?: runCatching {
                json.decodeFromString<List<ConnectionSpeedSample>>(
                    ConnectionSpeedStorage.getString(ProfileScopedKey.of(KEY_SAMPLES)).orEmpty()
                )
            }.getOrDefault(emptyList()).also { cachedSamples = it }
        }
}
