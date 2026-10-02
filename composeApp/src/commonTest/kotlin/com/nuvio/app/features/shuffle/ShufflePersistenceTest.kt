package com.nuvio.app.features.shuffle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ShufflePersistenceTest {
    @Test
    fun restartAndSurfaceChangesNeverImmediatelyRepeatTheLastPlayedEpisode() {
        val storage = TestShuffleStorage()
        val store = EpisodeShuffleStore(storage, { 1 })
        val videos = (1..3).map { com.nuvio.app.features.details.MetaVideo(id = "show:1:$it", title = "Episode $it", season = 1, episode = it) }
        store.recordPlayed("show", "show:1:2")
        ShuffleSurface.entries.forEach { surface ->
            val restored = EpisodeShuffle(EpisodeShuffleStore(storage, { 1 }))
            val next = restored.select(1, "show", videos, true, surface = surface)
            kotlin.test.assertNotNull(next)
            kotlin.test.assertNotEquals("show:1:2", next.id)
        }
        store.recordPlayed("show", "show:1:3")
        val restored = EpisodeShuffle(store)
        val next = restored.select(1, "show", videos, false, watched = setOf(1 to 1), surface = ShuffleSurface.DETAIL)
        assertEquals("show:1:2", next?.id)
    }

    @Test
    fun settingsAndPlayedHistorySurviveRecreationAndStayInTheirProfile() {
        val disk = mutableMapOf<Int, String>()
        val storage = object : EpisodeShuffleStorage {
            override fun load(profileId: Int) = disk[profileId]
            override fun save(profileId: Int, payload: String) { disk[profileId] = payload }
        }
        val first = EpisodeShuffleStore(storage, { 1 })
        first.save("show", EpisodeShuffleSettings(true, true))
        first.recordPlayed("show", "show:1:2")
        val restored = EpisodeShuffleStore(storage, { 1 })
        assertEquals(EpisodeShuffleSettings(true, true), restored.settings("show", "series"))
        assertEquals(listOf("show:1:2"), restored.history("show"))
        val other = EpisodeShuffleStore(storage, { 2 })
        assertFalse(other.settings("show", "series").enabled)
        assertEquals(emptyList(), other.history("show"))
        other.clear()
        assertEquals(listOf("show:1:2"), restored.history("show"))
    }
}
