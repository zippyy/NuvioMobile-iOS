package com.nuvio.app.features.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class PlaybackThroughputSamplerTest {

    @Test
    fun ineligibleSourceDoesNotSample() {
        var sampled = false
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "file:///local/video.mp4",
            onSample = { _, _ -> sampled = true },
        )
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        sampler.finish()
        assertTrue(!sampled)
    }

    @Test
    fun notFetchingDoesNotSample() {
        var sampled = false
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            onSample = { _, _ -> sampled = true },
        )
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = false)
        sampler.finish()
        assertTrue(!sampled)
    }

    @Test
    fun warmupPeriodIsSkipped() {
        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            timeSource = ControlledTimeSource(),
            onSample = { network, mbps -> samples.add(network to mbps) },
        )
        // One tick at 500ms → warmupMs = 500 < 1000, still warmup
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        // Second tick at 500ms → warmupMs = 1000 >= 1000, warmup ends
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        // After warmup, no active ticks yet
        assertEquals(0, samples.size)
    }

    @Test
    fun networkChangeDuringMeasurementDropsSample() {
        val gen = mutableListOf(0)
        val kind = mutableListOf<NetworkKind?>(NetworkKind.WIFI)

        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            timeSource = ControlledTimeSource(),
            networkKind = { kind.last() },
            networkGeneration = { gen.last() },
            onSample = { network, mbps -> samples.add(network to mbps) },
        )
        // Warmup
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        // Network changes
        gen[0] = 1
        // Active tick
        sampler.onBytesTick(bytes = 4L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 4L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 4L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 4L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 4L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 4L * 1024 * 1024, isFetching = true)
        sampler.finish()
        assertEquals(0, samples.size)
    }

    @Test
    fun finishReportsCorrectMbps() {
        val kind = mutableListOf<NetworkKind?>(NetworkKind.WIFI)
        val gen = mutableListOf(0)

        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            timeSource = ControlledTimeSource(),
            networkKind = { kind.last() },
            networkGeneration = { gen.last() },
            onSample = { network, mbps -> samples.add(network to mbps) },
        )
        // First tick establishes a mark; two more warm up 1000 ms.
        repeat(3) { sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true) }
        // Active: 6 ticks × 500ms = 3000ms (MIN_WINDOW_MS)
        // 6 × 2MB = 12MB over 3000ms → 12×1024×1024 × 8 / 3000 / 1000 ≈ 33.55 Mbps
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.finish()
        assertEquals(1, samples.size)
        assertEquals(NetworkKind.WIFI, samples[0].first)
        val expectedMbps = 12.0 * 1024.0 * 1024.0 * 8.0 / 3000.0 / 1000.0
        assertEquals(expectedMbps, samples[0].second, 0.01)
    }

    @Test
    fun finishCalledMultipleTimesReportsOnce() {
        val kind = mutableListOf<NetworkKind?>(NetworkKind.WIFI)
        val gen = mutableListOf(0)

        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            timeSource = ControlledTimeSource(),
            networkKind = { kind.last() },
            networkGeneration = { gen.last() },
            onSample = { network, mbps -> samples.add(network to mbps) },
        )
        repeat(3) { sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true) }
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 2L * 1024 * 1024, isFetching = true)
        sampler.finish()
        sampler.finish()
        assertEquals(1, samples.size)
    }

    @Test
    fun rateTickConvertsCorrectly() {
        val kind = mutableListOf<NetworkKind?>(NetworkKind.WIFI)
        val gen = mutableListOf(0)

        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            timeSource = ControlledTimeSource(),
            networkKind = { kind.last() },
            networkGeneration = { gen.last() },
            onSample = { network, mbps -> samples.add(network to mbps) },
        )
        // First tick establishes a mark; two more warm up 1000 ms.
        repeat(3) { sampler.onRateTick(bytesPerSecond = 4_000_000, isFetching = true) }
        // Active: 6 ticks × 500ms = 3000ms
        // Six 500ms intervals at 4,000,000 bytes/s yield 12,000,000 bytes (32 Mbps).
        sampler.onRateTick(bytesPerSecond = 4_000_000, isFetching = true)
        sampler.onRateTick(bytesPerSecond = 4_000_000, isFetching = true)
        sampler.onRateTick(bytesPerSecond = 4_000_000, isFetching = true)
        sampler.onRateTick(bytesPerSecond = 4_000_000, isFetching = true)
        sampler.onRateTick(bytesPerSecond = 4_000_000, isFetching = true)
        sampler.onRateTick(bytesPerSecond = 4_000_000, isFetching = true)
        sampler.finish()
        assertEquals(1, samples.size)
        val expectedMbps = 4_000_000.0 * 8 / 1_000_000.0
        assertEquals(expectedMbps, samples[0].second, 0.01)
    }

    @Test
    fun tickWithLargeGapIsSkipped() {
        val kind = mutableListOf<NetworkKind?>(NetworkKind.WIFI)
        val gen = mutableListOf(0)

        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            timeSource = ControlledTimeSource(tickMs = 2_500L),
            networkKind = { kind.last() },
            networkGeneration = { gen.last() },
            onSample = { network, mbps -> samples.add(network to mbps) },
        )
        // 2500ms gap (> MAX_TICK_GAP_MS = 2000ms)
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        sampler.finish()
        assertEquals(0, samples.size)
    }

    @Test
    fun finishWithInsufficientActiveTimeReturnsEarly() {
        val kind = mutableListOf<NetworkKind?>(NetworkKind.WIFI)
        val gen = mutableListOf(0)

        val samples = mutableListOf<Pair<NetworkKind, Double>>()
        val sampler = PlaybackThroughputSampler(
            sourceUrl = "https://example.com/video.mp4",
            timeSource = ControlledTimeSource(),
            networkKind = { kind.last() },
            networkGeneration = { gen.last() },
            onSample = { network, mbps -> samples.add(network to mbps) },
        )
        // Only warmup, no active ticks
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        sampler.onBytesTick(bytes = 1024 * 1024, isFetching = true)
        sampler.finish()
        assertEquals(0, samples.size)
    }
}

/**
 * A controlled time source where each [markNow] call advances the clock by [tickMs].
 * The elapsed time between consecutive marks equals [tickMs].
 */
private class ControlledTimeSource(
    private val tickMs: Long = 500L,
) : TimeSource {
    private var nowMs = 0L

    override fun markNow(): TimeMark {
        nowMs += tickMs
        val markMs = nowMs
        return object : TimeMark {
            override fun elapsedNow(): Duration = (nowMs - markMs).milliseconds
        }
    }
}
