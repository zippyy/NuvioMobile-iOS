package com.nuvio.app.features.shuffle

import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.player.skip.PlayerNextEpisodeRules
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EpisodeShufflePlaybackTest {
    private val store = EpisodeShuffleStore()
    private val shuffle = EpisodeShuffle()
    private val playback = EpisodeShufflePlayback(store, shuffle)
    private val episodes = (1..5).map { episode(it) }

    @Test
    fun `disabled shuffle falls back to linear next episode`() {
        store.save("show", EpisodeShuffleSettings(enabled = false))
        val state = PlaybackShuffleState(
            settings = store.settings("show", "series"),
            watched = emptySet(),
            progress = emptyMap(),
        )
        val next = playback.nextEpisode(1, "show", episodes, 1, 3, state)
        assertNotNull(next)
        assertEquals(4, next.episode)
    }

    @Test
    fun `enabled shuffle returns a random episode`() {
        store.save("show", EpisodeShuffleSettings(enabled = true, includeWatched = true))
        val state = PlaybackShuffleState(
            settings = store.settings("show", "series"),
            watched = emptySet(),
            progress = emptyMap(),
        )
        val next = playback.nextEpisode(1, "show", episodes, 1, 3, state)
        assertNotNull(next)
        // Should not return the current episode
        assertEquals(false, next.season == 1 && next.episode == 3)
    }

    @Test
    fun `isEnabled returns false for non-series content`() {
        store.save("movie", EpisodeShuffleSettings(enabled = true))
        assertEquals(false, playback.isEnabled("movie", "movie"))
    }

    @Test
    fun `isEnabled returns true for series content`() {
        store.save("show", EpisodeShuffleSettings(enabled = true))
        assertEquals(true, playback.isEnabled("show", "series"))
    }

    private fun episode(number: Int) = MetaVideo(
        id = "show:1:$number",
        title = "Episode $number",
        released = "2020-01-01",
        thumbnail = null,
        season = 1,
        episode = number,
        overview = null,
    )
}
