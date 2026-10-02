package com.nuvio.app.features.shuffle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository

@Composable
fun DetailShuffleControls(meta: MetaDetails, onPlay: (MetaVideo) -> Unit) {
    val profile by ProfileRepository.state.collectAsState()
    val profileId = profile.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId
    val revision by EpisodeShuffleRuntime.store.revision.collectAsState()
    val watched by WatchedRepository.uiState.collectAsState()
    val progress by WatchProgressRepository.uiState.collectAsState()
    val settings = remember(profileId, meta.id, revision) { EpisodeShuffleRuntime.store.settings(meta.id, meta.type, profileId) }
    var visit by remember(profileId, meta.id) { mutableStateOf(0L) }
    val preview = remember(profileId, meta, settings, watched.items, progress.entries, visit, revision) {
        EpisodeShuffleRuntime.select(meta, ShuffleSurface.DETAIL, profileId, visit)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Episode shuffle")
            Switch(settings.enabled, { EpisodeShuffleRuntime.store.save(meta.id, settings.copy(enabled = it), profileId) })
        }
        if (settings.enabled) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Include watched episodes")
                Switch(settings.includeWatched, { EpisodeShuffleRuntime.store.save(meta.id, settings.copy(includeWatched = it), profileId) })
            }
            Text(preview?.let { "S${it.season} E${it.episode} · ${it.title}" } ?: "No eligible episodes")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { visit += 1 }) { Text("Shuffle again") }
                Button(enabled = preview != null, onClick = { preview?.let(onPlay) }) { Text("Play shuffled episode") }
            }
        }
    }
}
