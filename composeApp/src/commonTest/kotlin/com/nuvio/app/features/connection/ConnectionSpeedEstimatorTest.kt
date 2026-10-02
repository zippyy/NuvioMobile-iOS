package com.nuvio.app.features.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConnectionSpeedEstimatorTest {

    @Test
    fun estimateMbps_returnsNullForEmptySamples() {
        val result = ConnectionSpeedEstimator.estimateMbps(
            samples = emptyList(),
            network = NetworkKind.WIFI,
            nowMs = 1000000L,
        )
        assertNull(result)
    }

    @Test
    fun estimateMbps_returnsNullForInsufficientSamples() {
        val samples = listOf(
            ConnectionSpeedSample(NetworkKind.WIFI, 50.0, 1000L),
        )
        val result = ConnectionSpeedEstimator.estimateMbps(
            samples = samples,
            network = NetworkKind.WIFI,
            nowMs = 2000L,
        )
        assertNull(result)
    }

    @Test
    fun estimateMbps_returnsBestOfRecentSamples() {
        val samples = listOf(
            ConnectionSpeedSample(NetworkKind.WIFI, 30.0, 1000L),
            ConnectionSpeedSample(NetworkKind.WIFI, 50.0, 2000L),
            ConnectionSpeedSample(NetworkKind.WIFI, 40.0, 3000L),
        )
        val result = ConnectionSpeedEstimator.estimateMbps(
            samples = samples,
            network = NetworkKind.WIFI,
            nowMs = 4000L,
        )
        assertEquals(50.0, result)
    }

    @Test
    fun estimateMbps_ignoresOtherNetworks() {
        val samples = listOf(
            ConnectionSpeedSample(NetworkKind.WIFI, 50.0, 1000L),
            ConnectionSpeedSample(NetworkKind.CELLULAR, 100.0, 2000L),
            ConnectionSpeedSample(NetworkKind.WIFI, 60.0, 3000L),
        )
        val result = ConnectionSpeedEstimator.estimateMbps(
            samples = samples,
            network = NetworkKind.WIFI,
            nowMs = 4000L,
        )
        assertEquals(60.0, result)
    }

    @Test
    fun estimateMbps_respectsMaxSampleAge() {
        val samples = listOf(
            ConnectionSpeedSample(NetworkKind.WIFI, 50.0, 1000L),
            ConnectionSpeedSample(NetworkKind.WIFI, 60.0, 2000L),
        )
        // 14 days + 1 ms → sample is expired
        val farFuture = 1000L + 14L * 24 * 60 * 60 * 1000 + 1
        val result = ConnectionSpeedEstimator.estimateMbps(
            samples = samples,
            network = NetworkKind.WIFI,
            nowMs = farFuture,
        )
        assertNull(result)
    }

    @Test
    fun estimateMbps_capsAtMaxSamplesPerNetwork() {
        val samples = (1..5).map { i ->
            ConnectionSpeedSample(NetworkKind.WIFI, i * 10.0, i * 1000L)
        }
        val result = ConnectionSpeedEstimator.estimateMbps(
            samples = samples,
            network = NetworkKind.WIFI,
            nowMs = 6000L,
        )
        // Only the 3 most recent (3, 4, 5) count; best is 50.0
        assertEquals(50.0, result)
    }

    @Test
    fun appendSample_addsToSameNetworkBucket() {
        val existing = listOf(
            ConnectionSpeedSample(NetworkKind.CELLULAR, 20.0, 1000L),
        )
        val newSample = ConnectionSpeedSample(NetworkKind.WIFI, 50.0, 2000L)
        val result = ConnectionSpeedEstimator.appendSample(existing, newSample)
        assertEquals(2, result.size)
        assertEquals(NetworkKind.WIFI, result.last().network)
    }

    @Test
    fun appendSample_evictsOldestSameNetworkSample() {
        val existing = listOf(
            ConnectionSpeedSample(NetworkKind.WIFI, 10.0, 1000L),
            ConnectionSpeedSample(NetworkKind.WIFI, 20.0, 2000L),
            ConnectionSpeedSample(NetworkKind.WIFI, 30.0, 3000L),
        )
        val newSample = ConnectionSpeedSample(NetworkKind.WIFI, 40.0, 4000L)
        val result = ConnectionSpeedEstimator.appendSample(existing, newSample)
        assertEquals(3, result.size)
        // Oldest WIFI sample (10.0) should be evicted
        assertEquals(20.0, result[result.size - 3].mbps)
        assertEquals(40.0, result.last().mbps)
    }

    @Test
    fun appendSample_preservesOtherNetworks() {
        val existing = listOf(
            ConnectionSpeedSample(NetworkKind.CELLULAR, 20.0, 1000L),
            ConnectionSpeedSample(NetworkKind.WIFI, 50.0, 2000L),
        )
        val newSample = ConnectionSpeedSample(NetworkKind.WIFI, 60.0, 3000L)
        val result = ConnectionSpeedEstimator.appendSample(existing, newSample)
        assertEquals(3, result.size)
        // CELLULAR sample should still be there
        assertNotNull(result.find { it.network == NetworkKind.CELLULAR })
    }

    @Test
    fun estimateMbps_filtersInvalidThroughput() {
        val samples = listOf(
            ConnectionSpeedSample(NetworkKind.WIFI, 0.1, 1000L),  // below MIN_VALID_MBPS
            ConnectionSpeedSample(NetworkKind.WIFI, 50.0, 2000L),
        )
        val result = ConnectionSpeedEstimator.estimateMbps(
            samples = samples,
            network = NetworkKind.WIFI,
            nowMs = 3000L,
        )
        // Only 1 valid sample remaining → null
        assertNull(result)
    }
}
