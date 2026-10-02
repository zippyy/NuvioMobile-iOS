package com.nuvio.app.features.shuffle

import androidx.compose.runtime.*
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.WatchProgressRepository

/** All surfaces share the same profile-scoped store and actual-play history. */
object EpisodeShuffleRuntime {
    val store = EpisodeShuffleStore()
    val shuffle = EpisodeShuffle(store)
    val playback = EpisodeShufflePlayback(store, shuffle)

    fun state(meta: MetaDetails, profileId: Int = ProfileRepository.activeProfileId): PlaybackShuffleState {
        WatchedRepository.ensureLoaded()
        WatchProgressRepository.ensureLoaded()
        val watched = WatchedRepository.uiState.value.items.filter { profileId == ProfileRepository.activeProfileId && it.id == meta.id }
            .mapNotNull { item -> item.season?.let { s -> item.episode?.let { s to it } } }.toSet()
        val progress = WatchProgressRepository.uiState.value.entries.filter { profileId == ProfileRepository.activeProfileId && it.parentMetaId == meta.id }
            .mapNotNull { item -> item.seasonNumber?.let { s -> item.episodeNumber?.let { (s to it) to item } } }.toMap()
        return PlaybackShuffleState(store.settings(meta.id, meta.type, profileId), watched, progress)
    }

    fun select(meta: MetaDetails, surface: ShuffleSurface, profileId: Int = ProfileRepository.activeProfileId, visit: Long = 0): MetaVideo? {
        val state = state(meta, profileId)
        if (!state.settings.enabled) return null
        return shuffle.select(profileId, meta.id, meta.videos, state.settings.includeWatched, state.watched, state.progress, surface, visit = visit)
    }

    fun recordPlayed(contentId: String, videoId: String, profileId: Int = ProfileRepository.activeProfileId) {
        if (store.history(contentId, profileId).lastOrNull() == videoId) return
        store.recordPlayed(contentId, videoId, profileId)
        ShuffleSurface.entries.forEach { shuffle.clearSelection(profileId, contentId, it) }
    }

    /** Call from the existing next-episode resolver; all stream/progress/tracking paths remain unchanged. */
    fun nextEpisode(meta: MetaDetails, season: Int?, episode: Int, profileId: Int, preferredVideoId: String? = null): MetaVideo? =
        playback.nextEpisode(profileId, meta.id, meta.videos, season, episode, state(meta, profileId), preferredVideoId)
}

fun projectHomeShuffle(item: ContinueWatchingItem, video: MetaVideo): ContinueWatchingItem = item.copy(
    videoId = video.id,
    seasonNumber = video.season,
    episodeNumber = video.episode,
    episodeTitle = video.title,
    episodeThumbnail = video.thumbnail,
    pauseDescription = video.overview,
    released = video.released,
    subtitle = "Shuffle · S${video.season} E${video.episode} · ${video.title}",
    resumePositionMs = 0,
    resumeProgressFraction = null,
    durationMs = 0,
    progressFraction = 0f,
    isReleaseAlert = false,
    isNewSeasonRelease = false,
)

/** Only next-up cards change: an in-progress episode must always keep its resume identity. */
@Composable
fun rememberHomeShuffleItems(items: List<ContinueWatchingItem>): List<ContinueWatchingItem> {
    val profile by ProfileRepository.state.collectAsState()
    val profileId = profile.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId
    val revision by EpisodeShuffleRuntime.store.revision.collectAsState()
    val watched by WatchedRepository.uiState.collectAsState()
    val progress by WatchProgressRepository.uiState.collectAsState()
    var projected by remember(profileId) { mutableStateOf(items) }
    LaunchedEffect(items, profileId, revision, watched.items, progress.entries) {
        projected = items.mapNotNull { item ->
            if (!item.isNextUp || !EpisodeShuffleRuntime.store.settings(item.parentMetaId, item.parentMetaType, profileId).enabled) item
            else {
                val meta = MetaDetailsRepository.peek(item.parentMetaType, item.parentMetaId)
                    ?: MetaDetailsRepository.fetch(item.parentMetaType, item.parentMetaId)
                val selected = meta?.let { EpisodeShuffleRuntime.select(it, ShuffleSurface.HOME, profileId) }
                if (selected == null) { if (meta == null) item else null } else {
                    val saved = progress.entries.firstOrNull { it.parentMetaId == item.parentMetaId && it.seasonNumber == selected.season && it.episodeNumber == selected.episode && !it.isEffectivelyCompleted }
                    projectHomeShuffle(item, selected).let { projectedItem ->
                        if (saved == null) projectedItem else projectedItem.copy(resumePositionMs = saved.lastPositionMs, durationMs = saved.durationMs, progressFraction = saved.progressFraction)
                    }
                }
            }
        }
    }
    // Don't expose a previous profile's projection while its effect is pending.
    return projected.filter { projectedItem -> items.any { it.parentMetaId == projectedItem.parentMetaId } }
}
