package com.nuvio.app.features.shuffle

import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpisodeShuffleStoreTest {
    private val store = EpisodeShuffleStore(TestShuffleStorage(), { 1 })

    @Test
    fun `settings for series content type respects enabled flag`() {
        store.save("show1", EpisodeShuffleSettings(enabled = true, includeWatched = false))
        val settings = store.settings("show1", "series")
        assertTrue(settings.enabled)
        assertEquals(false, settings.includeWatched)
    }

    @Test
    fun `settings for non-series content type disables shuffle`() {
        store.save("movie1", EpisodeShuffleSettings(enabled = true, includeWatched = true))
        val settings = store.settings("movie1", "movie")
        assertEquals(false, settings.enabled)
        assertEquals(true, settings.includeWatched)
    }

    @Test
    fun `settings defaults to disabled when not saved`() {
        val settings = store.settings("unknown", "series")
        assertEquals(false, settings.enabled)
        assertEquals(false, settings.includeWatched)
    }

    @Test
    fun `blank content id is not saved`() {
        store.save("", EpisodeShuffleSettings(enabled = true))
        val settings = store.settings("", "series")
        assertEquals(false, settings.enabled)
    }

    @Test
    fun `tv content type is also treated as series`() {
        store.save("show_tv", EpisodeShuffleSettings(enabled = true, includeWatched = true))
        val settings = store.settings("show_tv", "tv")
        assertTrue(settings.enabled)
    }
}
