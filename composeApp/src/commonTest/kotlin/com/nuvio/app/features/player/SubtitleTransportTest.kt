package com.nuvio.app.features.player

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SubtitleTransportTest {
    @Test fun redirectsAreManuallyScoped() = runBlocking {
        val requests = mutableListOf<Pair<String, Map<String, String>>>()
        val bytes = downloadSubtitleBytes("https://sub.test/a", ownHeaders = mapOf("Authorization" to "secret"), waitBeforeRetry = {}, fetch = { url, headers, _ ->
            requests += url to headers
            if (requests.size == 1) SubtitleHttpHop(302, mapOf("location" to "https://other.test/b"), byteArrayOf())
            else SubtitleHttpHop(200, emptyMap(), "hello".encodeToByteArray())
        })
        assertEquals("hello", bytes.decodeToString())
        assertEquals("secret", requests[0].second["authorization"])
        assertNull(requests[1].second["authorization"])
    }
    @Test fun retriesThreeTimesWithLinearBackoff() = runBlocking {
        var calls = 0; val waits = mutableListOf<Long>()
        downloadSubtitleBytes("https://sub.test/a", waitBeforeRetry = { waits += it }, fetch = { _, _, _ ->
            calls++; if (calls < 3) error("network")
            SubtitleHttpHop(200, emptyMap(), "hello".encodeToByteArray())
        })
        assertEquals(3, calls); assertEquals(listOf(350L, 700L), waits)
    }
    @Test fun httpsDowngradeIsRefused() = runBlocking {
        var calls = 0
        assertFailsWith<SubtitleTransportException> {
            downloadSubtitleBytes("https://sub.test/a", waitBeforeRetry = {}, fetch = { _, _, _ ->
                calls++; SubtitleHttpHop(302, mapOf("location" to "http://sub.test/b"), byteArrayOf())
            })
        }
        assertEquals(1, calls)
    }
    @Test fun cancellationNeverRetries() = runBlocking {
        var calls = 0
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            downloadSubtitleBytes("https://sub.test/a", waitBeforeRetry = {}, fetch = { _, _, _ -> calls++; throw kotlinx.coroutines.CancellationException() })
        }
        assertEquals(1, calls)
    }
}
