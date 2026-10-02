package com.nuvio.app.features.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionSpeedUtilsTest {

    @Test
    fun isValidThroughput_returnsTrueForValidRange() {
        assertTrue(0.5.isValidThroughput())
        assertTrue(1.0.isValidThroughput())
        assertTrue(500.0.isValidThroughput())
        assertTrue(999.9.isValidThroughput())
    }

    @Test
    fun isValidThroughput_returnsFalseForOutOfRange() {
        assertTrue(!0.1.isValidThroughput())
        assertTrue(!0.0.isValidThroughput())
        assertTrue(!(-1.0).isValidThroughput())
        assertTrue(!1001.0.isValidThroughput())
    }

    @Test
    fun isValidThroughput_returnsFalseForNonFinite() {
        assertTrue(!Double.NaN.isValidThroughput())
        assertTrue(!Double.POSITIVE_INFINITY.isValidThroughput())
        assertTrue(!Double.NEGATIVE_INFINITY.isValidThroughput())
    }

    @Test
    fun isInternetPlaybackSource_httpsUrl() {
        assertTrue("https://example.com/video.mp4".isInternetPlaybackSource())
        assertTrue("https://cdn.example.com/stream/hls.m3u8".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_httpUrl() {
        assertTrue("http://example.com/video.mp4".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_localhost() {
        assertTrue(!"http://localhost/video.mp4".isInternetPlaybackSource())
        assertTrue(!"http://myhost.local/video.mp4".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_localIpv4() {
        assertTrue(!"http://127.0.0.1/video.mp4".isInternetPlaybackSource())
        assertTrue(!"http://192.168.1.1/video.mp4".isInternetPlaybackSource())
        assertTrue(!"http://10.0.0.1/video.mp4".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_publicIpv4() {
        assertTrue("http://8.8.8.8/video.mp4".isInternetPlaybackSource())
        assertTrue("http://203.0.113.1/video.mp4".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_localIpv6() {
        assertTrue(!"http://[::1]/video.mp4".isInternetPlaybackSource())
        assertTrue(!"http://[fe80::1]/video.mp4".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_publicIpv6() {
        assertTrue("http://[2001:db8::1]/video.mp4".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_nonHttpScheme() {
        assertTrue(!"file:///sdcard/video.mp4".isInternetPlaybackSource())
        assertTrue(!"rtmp://example.com/live".isInternetPlaybackSource())
    }

    @Test
    fun isInternetPlaybackSource_emptyString() {
        assertTrue(!"".isInternetPlaybackSource())
    }
}
