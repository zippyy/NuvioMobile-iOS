package com.nuvio.app.features.streams

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectionAwareSelectionTest {
    private fun stream(name: String, size: Long, binge: String? = null) = StreamItem(
        name = name, addonName = "Test", addonId = "addon:test", url = "https://example.com/$name",
        behaviorHints = StreamBehaviorHints(videoSize = size, bingeGroup = binge),
    )
    private fun select(streams: List<StreamItem>, mode: StreamAutoPlayMode = StreamAutoPlayMode.FIRST_STREAM,
                       regex: String = "", binge: String? = null, speed: Double? = 20.0) =
        StreamAutoPlaySelector.selectAutoPlayStream(streams, mode, regex, StreamAutoPlaySource.ALL_SOURCES,
            setOf("Test"), emptySet(), emptySet(), preferredBingeGroup = binge,
            preferBingeGroupInSelection = binge != null, runtimeMinutes = 120, connectionMbps = speed)

    @Test fun connectionFitChangesActualAutoplayWinner() {
        val heavy = stream("heavy", 30_000_000_000)
        val light = stream("light", 4_000_000_000)
        assertEquals(light, select(listOf(heavy, light)))
        assertEquals(heavy, select(listOf(heavy, light), speed = null))
    }
    @Test fun regexManualAndBingeStillWinOverConnectionFit() {
        val heavy = stream("preferred", 30_000_000_000, "group")
        val light = stream("light", 4_000_000_000)
        assertEquals(heavy, select(listOf(heavy, light), StreamAutoPlayMode.REGEX_MATCH, "preferred"))
        assertNull(select(listOf(heavy, light), StreamAutoPlayMode.MANUAL))
        assertEquals(heavy, select(listOf(light, heavy), binge = "group"))
    }
    @Test fun unknownOrInvalidSpeedDoesNotInventRanking() {
        val streams = listOf(stream("heavy", 30_000_000_000), stream("light", 4_000_000_000))
        assertEquals(streams.first(), select(streams, speed = Double.NaN))
        assertEquals(streams.first(), select(streams, speed = -1.0))
    }
    @Test fun metadataRuntimeIsParsedWithoutArbitraryMovieDefault() {
        assertEquals(125, parseConnectionFitRuntime("2h 5min"))
        assertEquals(90, parseConnectionFitRuntime("1:30"))
        assertEquals(42, parseConnectionFitRuntime("42 min"))
        assertNull(parseConnectionFitRuntime("unknown"))
        assertNull(parseConnectionFitRuntime("3"))
    }
}
