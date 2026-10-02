package com.nuvio.app.features.artwork

import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable enum class ArtworkScreen { HOME, DETAIL, CONTINUE_WATCHING, COLLECTIONS, LIBRARY, SEARCH }
enum class ArtworkKind { POSTER, BACKGROUND, LOGO }
@Serializable data class ArtworkOverride(val poster: String? = null, val background: String? = null, val logo: String? = null)
@Serializable data class ArtworkSettings(val posterTemplate: String = "", val screens: Set<ArtworkScreen> = ArtworkScreen.entries.toSet())
@Serializable private data class ArtworkPayload(val settings: ArtworkSettings = ArtworkSettings(), val overrides: Map<String, ArtworkOverride> = emptyMap())

interface ArtworkStorage {
    fun load(profileId: Int): String?
    fun save(profileId: Int, payload: String)
}
internal expect object PlatformArtworkStorage : ArtworkStorage {
    override fun load(profileId: Int): String?
    override fun save(profileId: Int, payload: String)
}

class ArtworkStore(private val storage: ArtworkStorage = PlatformArtworkStorage, private val profileId: () -> Int = { ProfileRepository.activeProfileId }) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()
    private fun read(): ArtworkPayload = storage.load(profileId())?.let { runCatching { json.decodeFromString<ArtworkPayload>(it) }.getOrNull() } ?: ArtworkPayload()
    private fun write(payload: ArtworkPayload) {
        storage.save(profileId(), json.encodeToString(payload))
        ArtworkFallbacks.clear()
        _revision.value += 1
    }
    private fun key(type: String, id: String): String = "${if (type.lowercase() in setOf("series", "tv", "show", "tvshow", "anime")) "series" else type}:$id"
    fun settings(): ArtworkSettings = read().settings
    fun saveSettings(settings: ArtworkSettings) {
        require(settings.posterTemplate.isBlank() || isArtworkUrl(settings.posterTemplate)) { "Use an HTTP(S) URL" }
        write(read().copy(settings = settings))
    }
    fun override(type: String, id: String): ArtworkOverride = read().overrides[key(type, id)] ?: ArtworkOverride()
    fun saveOverride(type: String, id: String, value: ArtworkOverride) {
        require(listOf(value.poster, value.background, value.logo).all { it.isNullOrBlank() || isArtworkUrl(it) }) { "Use HTTP(S) artwork URLs" }
        val payload = read()
        val overrides = if (value == ArtworkOverride()) payload.overrides - key(type, id) else payload.overrides + (key(type, id) to value)
        write(payload.copy(overrides = overrides))
    }
    fun resolve(type: String, id: String, screen: ArtworkScreen, kind: ArtworkKind, original: String?, shape: String = "poster"): String? {
        val payload = read()
        val override = payload.overrides[key(type, id)]
        val explicit = when (kind) { ArtworkKind.POSTER -> override?.poster; ArtworkKind.BACKGROUND -> override?.background; ArtworkKind.LOGO -> override?.logo }
        val custom = explicit?.takeIf { it.isNotBlank() } ?: if (kind == ArtworkKind.POSTER && screen in payload.settings.screens) {
            CustomPosterUrlResolver.resolve(payload.settings.posterTemplate, CustomPosterUrlResolver.extractIds(id), type, shape)
                ?.takeIf { isArtworkUrl(it) && '{' !in it && '}' !in it }
        } else null
        return custom?.also { ArtworkFallbacks.register(it, original) } ?: original
    }
}

fun isArtworkUrl(value: String): Boolean = value.startsWith("https://") || value.startsWith("http://")

/** Failed custom loads retry the unchanged provider art, not an empty placeholder. */
internal object ArtworkFallbacks {
    private val originals = mutableMapOf<String, String?>()
    fun register(custom: String, original: String?) { if (custom != original) originals[custom] = original }
    fun original(custom: String): String? = originals[custom]
    fun clear() = originals.clear()
}

object ArtworkRepository { val store = ArtworkStore() }
