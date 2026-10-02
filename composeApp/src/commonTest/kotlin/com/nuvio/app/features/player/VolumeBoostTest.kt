package com.nuvio.app.features.player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
class VolumeBoostTest {
 @Test fun `samples below knee pass unchanged`() { assertEquals(0.5f,softClipBoostedSample(0.5f));assertEquals(-0.8f,softClipBoostedSample(-0.8f)) }
 @Test fun `boosted peaks remain bounded and preserve sign`() { assertTrue(softClipBoostedSample(2f) in 0.8f..1f);assertTrue(softClipBoostedSample(-2f) in -1f..-0.8f) }
 @Test fun `mpv boost range clamps to reshaped range`() { assertEquals(0.0,volumeBoostPercentToMpv(-5));assertEquals(150.0,volumeBoostPercentToMpv(150));assertEquals(200.0,volumeBoostPercentToMpv(250)) }
}
