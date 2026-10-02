package com.nuvio.app.features.streams

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class StreamConnectionFitTest {
    @Test
    fun `moves streams too heavy for connection to end without reordering peers`() {
        val heavyA = stream("heavy-a", 30_000_000_000L)
        val light = stream("light", 4_000_000_000L)
        val heavyB = stream("heavy-b", 24_000_000_000L)

        val result = StreamConnectionFit(runtimeMinutes = 120, connectionMbps = 20.0)
            .apply(listOf(heavyA, light, heavyB))

        assertEquals(listOf(light, heavyA, heavyB), result)
    }

    @Test
    fun `unknown stream metadata stays in original ordering tier`() {
        val heavy = stream("heavy", 30_000_000_000L)
        val unknown = stream("unknown", null)

        val result = StreamConnectionFit(runtimeMinutes = 120, connectionMbps = 20.0)
            .apply(listOf(heavy, unknown))

        assertEquals(listOf(unknown, heavy), result)
    }

    @Test
    fun `returns same list when no stream needs moving`() {
        val streams = listOf(stream("light-a", 2_000_000_000L), stream("light-b", 3_000_000_000L))
        val result = StreamConnectionFit(runtimeMinutes = 120, connectionMbps = 100.0).apply(streams)
        assertSame(streams, result)
    }

    @Test
    fun `bitrate estimate rejects implausible runtime and missing size`() {
        assertNull(stream("missing", null).averageBitrateMbps(120))
        assertNull(stream("short", 1_000_000_000L).averageBitrateMbps(2))
    }

    private fun stream(name: String, sizeBytes: Long?): StreamItem = StreamItem(
        name = name,
        addonName = "Test",
        addonId = "addon:test",
        behaviorHints = StreamBehaviorHints(videoSize = sizeBytes),
    )
}
