package com.nuvio.app.features.player
import kotlin.test.*

class SubtitleRequestPolicyTest {
    @Test fun ownHeadersOverrideStreamCaseInsensitively() {
        val headers = subtitleRequestHeaders("https://media.test/a", "https://media.test/sub", "https://media.test/sub", mapOf("Authorization" to "stream"), mapOf("authorization" to "subtitle"))
        assertEquals("subtitle", headers["authorization"])
        assertEquals(1, headers.keys.count { it.equals("authorization", true) })
    }
    @Test fun crossOriginDropsUnknownAndCredentialHeaders() {
        val headers = subtitleRequestHeaders("https://media.test/a", "https://sub.test/a", "https://other.test/a", mapOf("Cookie" to "secret", "User-Agent" to "UA"), mapOf("X-Token" to "secret", "Accept-Language" to "en"))
        assertFalse(headers.containsKey("cookie"))
        assertFalse(headers.containsKey("x-token"))
        assertEquals("UA", headers["user-agent"])
    }
    @Test fun portChangeIsCrossOrigin() {
        assertFalse(subtitleRequestHeaders("https://a.test/a", "https://a.test/sub", "https://a.test:444/sub", emptyMap(), mapOf("Authorization" to "secret")).containsKey("authorization"))
    }
    @Test fun downgradeRemovesRefererOriginAndCredentials() {
        val headers = subtitleRequestHeaders("https://a.test/a", "https://a.test/sub", "http://a.test/sub", mapOf("Referer" to "secret", "Origin" to "secret"), mapOf("Authorization" to "secret"))
        assertFalse(headers.containsKey("referer")); assertFalse(headers.containsKey("origin")); assertFalse(headers.containsKey("authorization"))
    }
    @Test fun rejectsHeaderInjectionAndHopByHopFields() {
        val headers = subtitleRequestHeaders(null, "https://a.test/a", "https://a.test/a", emptyMap(), mapOf("X-Test" to "a\r\nb", "Host" to "evil", "Range" to "bytes=0-1"))
        assertFalse(headers.containsKey("x-test")); assertFalse(headers.containsKey("host")); assertFalse(headers.containsKey("range"))
    }
}
