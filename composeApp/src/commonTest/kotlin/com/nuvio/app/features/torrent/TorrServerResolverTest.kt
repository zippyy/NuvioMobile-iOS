package com.nuvio.app.features.torrent

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class TorrServerResolverTest {
    @Test fun rejectsInvalidIndexBeforeCallingServer() = runBlocking {
        val resolver = TorrServerResolver { _, _ -> error("Must not contact server") }
        val settings = TorrServerSettings(true, "http://localhost:8090")
        assertFailsWith<TorrServerException> { resolver.resolve("torrserver://0123456789abcdef0123456789abcdef01234567?index=0", settings) }
        assertFailsWith<TorrServerException> { resolver.resolve("torrserver://not-a-hash", settings) }
        assertFailsWith<TorrServerException> { resolver.resolve("torrserver://0123456789abcdef0123456789abcdef01234567?index=-1", settings) }
        Unit
    }
    @Test fun exposesAuthFailuresWithoutLeakingServerBody() = runBlocking {
        val resolver = TorrServerResolver { _, _ -> TorrServerResponse(401, "secret server details") }
        val error = assertFailsWith<TorrServerException> { resolver.resolve("torrserver://0123456789abcdef0123456789abcdef01234567", TorrServerSettings(true, "http://localhost:8090")) }
        assertEquals("TorrServer requires authentication", error.message)
    }
    @Test fun cancellationIsNotConvertedToNetworkError() = runBlocking {
        val resolver = TorrServerResolver { _, _ -> throw kotlinx.coroutines.CancellationException("cancelled") }
        assertFailsWith<kotlinx.coroutines.CancellationException> { resolver.resolve("torrserver://0123456789abcdef0123456789abcdef01234567", TorrServerSettings(true, "http://localhost:8090")) }
        Unit
    }
    @Test fun serverUrlRejectsEmbeddedCredentialsAndQueries() {
        assertEquals("https://example.test/proxy", TorrServerResolver.normalizeServerUrl(" https://example.test/proxy/ "))
        assertFailsWith<TorrServerException> { TorrServerResolver.normalizeServerUrl("http://user:password@example.test") }
        assertFailsWith<TorrServerException> { TorrServerResolver.normalizeServerUrl("http://example.test?secret=value") }
    }

    @Test fun resolvesExplicitTorrentWithoutHijackingHttpStreams() = runBlocking {
        var requests = 0
        val resolver = TorrServerResolver { url, body ->
            requests++
            assertEquals("http://192.168.1.10:8090/torrents", url)
            assertTrue(body.contains("save_to_db"))
            TorrServerResponse(200, """{"hash":"0123456789abcdef0123456789abcdef01234567"}""")
        }
        assertNull(resolver.resolve("https://example.com/video.mp4", TorrServerSettings()))
        assertEquals(0, requests)
        val result = resolver.resolve("torrserver://0123456789abcdef0123456789abcdef01234567?index=2", TorrServerSettings(true, "http://192.168.1.10:8090/"))
        assertEquals("http://192.168.1.10:8090/stream?link=0123456789abcdef0123456789abcdef01234567&index=2&play", result)
        assertEquals(1, requests)
        assertFailsWith<TorrServerException> { resolver.resolve("torrserver://0123456789abcdef0123456789abcdef01234567", TorrServerSettings()) }
        Unit
    }
}
