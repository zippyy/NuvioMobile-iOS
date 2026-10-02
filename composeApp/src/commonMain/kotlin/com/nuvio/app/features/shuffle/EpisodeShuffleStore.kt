package com.nuvio.app.features.shuffle

import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface EpisodeShuffleStorage {
    fun load(profileId: Int): String?
    fun save(profileId: Int, payload: String)
}

internal expect object PlatformEpisodeShuffleStorage : EpisodeShuffleStorage {
    override fun load(profileId: Int): String?
    override fun save(profileId: Int, payload: String)
}

@Serializable
private data class ShufflePayload(
    val settings: Map<String, EpisodeShuffleSettings> = emptyMap(),
    val history: Map<String, List<String>> = emptyMap(),
)

/** Disk is authoritative; explicit profile arguments also protect delayed playback callbacks. */
class EpisodeShuffleStore(
    private val storage: EpisodeShuffleStorage = PlatformEpisodeShuffleStorage,
    private val profileId: () -> Int = { ProfileRepository.activeProfileId },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()

    private fun read(profile: Int): ShufflePayload = storage.load(profile)?.let {
        runCatching { json.decodeFromString<ShufflePayload>(it) }.getOrNull()
    } ?: ShufflePayload()

    private fun write(profile: Int, payload: ShufflePayload) {
        storage.save(profile, json.encodeToString(payload))
        _revision.value += 1
    }

    fun settings(contentId: String, contentType: String, profile: Int = profileId()): EpisodeShuffleSettings {
        val saved = read(profile).settings[contentId] ?: EpisodeShuffleSettings()
        return saved.copy(enabled = saved.enabled && contentType.lowercase() in setOf("series", "tv", "show", "tvshow", "anime"))
    }

    fun save(contentId: String, settings: EpisodeShuffleSettings, profile: Int = profileId()) {
        if (contentId.isBlank()) return
        val payload = read(profile)
        write(profile, payload.copy(settings = payload.settings + (contentId to settings)))
    }

    fun history(contentId: String, profile: Int = profileId()): List<String> = read(profile).history[contentId].orEmpty()

    fun recordPlayed(contentId: String, videoId: String, profile: Int = profileId()) {
        if (contentId.isBlank() || videoId.isBlank()) return
        val payload = read(profile)
        val old = payload.history[contentId].orEmpty()
        if (old.lastOrNull() == videoId) return
        write(profile, payload.copy(history = payload.history + (contentId to ((old - videoId) + videoId).takeLast(256))))
    }

    fun clear(profile: Int = profileId()) = write(profile, ShufflePayload())
}
