package com.nuvio.app.features.trailer

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class YouTubeStreamResolverTest {
    @Test fun preservesSeparateAudioWhenResolvingYoutubeId() = runBlocking {
        val source = TrailerPlaybackSource("https://video.example/v", "https://audio.example/a")
        val resolver = YouTubeStreamResolver { url ->
            assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", url)
            source
        }
        assertEquals(source, resolver.resolve("dQw4w9WgXcQ"))
        assertEquals(source, resolver.resolve("https://youtu.be/dQw4w9WgXcQ?t=10"))
        assertEquals(source, resolver.resolve("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(resolver.resolve("https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ"))
        assertNull(resolver.resolve("https://example.com/video.mp4"))
    }
}
