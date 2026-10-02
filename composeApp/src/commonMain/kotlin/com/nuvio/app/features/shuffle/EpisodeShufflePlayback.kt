package com.nuvio.app.features.shuffle

import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.player.skip.PlayerNextEpisodeRules
import com.nuvio.app.features.watchprogress.WatchProgressEntry

data class PlaybackShuffleState(
    val settings: EpisodeShuffleSettings,
    val watched: Set<Pair<Int, Int>>,
    val progress: Map<Pair<Int, Int>, WatchProgressEntry>,
)

class EpisodeShufflePlayback(
    private val store: EpisodeShuffleStore,
    private val shuffle: EpisodeShuffle,
) {
    fun isEnabled(contentId: String, contentType: String): Boolean =
        store.settings(contentId, contentType).enabled

    fun nextEpisode(
        profileId: Int,
        contentId: String,
        videos: List<MetaVideo>,
        season: Int?,
        episode: Int,
        state: PlaybackShuffleState,
        preferredVideoId: String? = null,
    ): MetaVideo? {
        if (!state.settings.enabled) {
            shuffle.clearSelection(profileId, contentId, ShuffleSurface.PLAYBACK)
            return PlayerNextEpisodeRules.resolveNextEpisode(videos, season, episode)
        }
        return shuffle.select(
            profileId, contentId, videos, state.settings.includeWatched,
            state.watched, state.progress, ShuffleSurface.PLAYBACK,
            current = season?.let { it to episode }, preferredVideoId = preferredVideoId,
        )
    }
}
