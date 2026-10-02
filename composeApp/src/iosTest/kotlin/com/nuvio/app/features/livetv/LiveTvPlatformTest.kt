package com.nuvio.app.features.livetv

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LiveTvPlatformTest {
    @Test fun inflatesProviderGzipEvenWithoutContentEncoding() {
        val compressed = byteArrayOf(31, -117, 8, 0, 0, 0, 0, 0, 2, 3, -77, 41, 41, -77, -77, 73, -50, 72, -52, -53, 75, -51, 81, -56, 76, -79, 85, -54, 75, 45, 47, 86, -46, -73, -77, -47, 7, 74, 0, 0, 14, -39, 111, -87, 29, 0, 0, 0)
        assertContentEquals("<tv><channel id=\"news\"/></tv>".encodeToByteArray(), gunzipLiveTv(compressed))
    }
    @Test fun leavesPlainXmlUnchanged() {
        val plain = "<tv/>".encodeToByteArray()
        assertContentEquals(plain, gunzipLiveTv(plain))
        assertContentEquals(byteArrayOf(), gunzipLiveTv(byteArrayOf()))
    }
    @Test fun rejectsTruncatedCompressedGuide() {
        assertFailsWith<IllegalStateException> { gunzipLiveTv(byteArrayOf(31, -117, 8, 0)) }
    }
    @Test fun returnsMillisecondsForCurrentWallClock() {
        assertTrue(liveTvNow() > 1_700_000_000_000L)
    }
}
