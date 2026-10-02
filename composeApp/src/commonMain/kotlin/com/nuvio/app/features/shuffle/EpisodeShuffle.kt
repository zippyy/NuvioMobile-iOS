package com.nuvio.app.features.shuffle

import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class EpisodeShuffle(private val store: EpisodeShuffleStore? = null) {
    private data class Key(
        val profileId: Int,
        val contentId: String,
        val surface: ShuffleSurface,
        val includeWatched: Boolean,
    )

    private class Session {
        var picker: RandomEpisodePicker? = null
        var selected: MetaVideo? = null
        var current: Pair<Int, Int>? = null
        var visit: Long = 0
    }

    private val sessions = LinkedHashMap<Key, Session>()
    private val lock = SynchronizedObject()

    fun select(
        profileId: Int,
        contentId: String,
        videos: List<MetaVideo>,
        includeWatched: Boolean,
        watched: Set<Pair<Int, Int>> = emptySet(),
        progress: Map<Pair<Int, Int>, WatchProgressEntry> = emptyMap(),
        surface: ShuffleSurface,
        current: Pair<Int, Int>? = null,
        visit: Long = 0,
        preferredVideoId: String? = null,
    ): MetaVideo? = synchronized(lock) {
        val key = Key(profileId, contentId, surface, includeWatched)
        val session = sessions.getOrPut(key, ::Session)
        if (sessions.size > 96) {
            val first = sessions.keys.firstOrNull()
            if (first != null) sessions.remove(first)
        }
        val picker = RandomEpisodePicker(contentId, videos, watched, progress)
        picker.inheritHistoryFrom(session.picker)
        val history = store?.history(contentId, profileId).orEmpty()
        if (session.picker == null) picker.restorePlayedHistory(history)
        val lastPlayedId = history.lastOrNull()
        val effectiveCurrent = current ?: videos.firstOrNull { it.id == lastPlayedId }?.let { video ->
            video.season?.let { season -> video.episode?.let { season to it } }
        }
        if (current != null && (session.picker == null || session.current != current)) picker.recordPlayed(current)
        session.picker = picker
        if (session.current != current || session.visit != visit) session.selected = null
        session.current = current
        session.visit = visit
        session.selected = session.selected?.let { picker.find(it.id, includeWatched, effectiveCurrent) }
            ?: preferredVideoId?.let { picker.find(it, includeWatched, effectiveCurrent) }
            ?: picker.pick(includeWatched, effectiveCurrent, consumeSelection = surface != ShuffleSurface.PLAYBACK || current == null)
        session.selected
    }

    fun clearSelection(profileId: Int, contentId: String, surface: ShuffleSurface) = synchronized(lock) {
        sessions.filterKeys {
            it.profileId == profileId && it.contentId == contentId && it.surface == surface
        }.values.forEach { it.selected = null }
    }
}
