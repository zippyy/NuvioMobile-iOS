package com.nuvio.app.features.mdblist

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import kotlinx.coroutines.CancellationException

object MdbListMetadataService {
    const val PROVIDER_IMDB = "imdb"
    const val PROVIDER_TMDB = "tmdb"
    const val PROVIDER_TOMATOES = "tomatoes"
    const val PROVIDER_METACRITIC = "metacritic"
    const val PROVIDER_TRAKT = "trakt"
    const val PROVIDER_LETTERBOXD = "letterboxd"
    const val PROVIDER_AUDIENCE = "audience"
    const val PROVIDER_MAL = "mal"

    val PROVIDER_PRIORITY_ORDER = listOf(
        PROVIDER_IMDB,
        PROVIDER_TMDB,
        PROVIDER_TOMATOES,
        PROVIDER_METACRITIC,
        PROVIDER_TRAKT,
        PROVIDER_LETTERBOXD,
        PROVIDER_AUDIENCE,
        PROVIDER_MAL,
    )

    private val client = MdbListClient(
        clock = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
        transport = { method, url, body ->
            val response = com.nuvio.app.features.addons.httpRequestRaw(
                method = method, url = url, headers = mapOf("Content-Type" to "application/json"),
                body = body, followRedirects = false,
            )
            MdbListResponse(response.status, response.body, response.headers)
        },
    )
    private val _error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val error: kotlinx.coroutines.flow.StateFlow<String?> = _error
    private val imdbRegex = Regex("^(tt\\d+)(?::\\d+:\\d+)?$")

    fun shouldFetchForMeta(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: MdbListSettings,
    ): Boolean {
        if (!settings.enabled) return false
        if (settings.apiKey.trim().isBlank()) return false
        if (settings.enabledProvidersInPriorityOrder().isEmpty()) return false
        return extractImdbId(meta.id) != null || extractImdbId(fallbackItemId) != null
    }

    suspend fun enrichMeta(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: MdbListSettings,
    ): MetaDetails {
        if (!shouldFetchForMeta(meta, fallbackItemId, settings)) {
            return meta.copy(externalRatings = emptyList())
        }
        val apiKey = settings.apiKey.trim()

        val imdbId = extractImdbId(meta.id)
            ?: extractImdbId(fallbackItemId)
            ?: return meta.copy(externalRatings = emptyList())
        val mediaType = toMdbListMediaType(meta.type)
        val enabledProviders = settings.enabledProvidersInPriorityOrder()

        val ratings = fetchRatings(
            imdbId = imdbId,
            mediaType = mediaType,
            apiKey = apiKey,
            providers = enabledProviders,
        )

        return meta.copy(externalRatings = ratings)
    }

    fun clearCache() {
        client.clearCache()
        _error.value = null
    }

    suspend fun loadList(listId: Long): List<MdbListItem> {
        val settings = MdbListSettingsRepository.snapshot()
        require(settings.hasApiKey) { "Add an MDBList API key first" }
        return try {
            client.listItems(listId, settings.apiKey.trim())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val message = if (failure is MdbListApiException) failure.message else "Unable to load MDBList list"
            _error.value = message
            throw IllegalStateException(message)
        }
    }

    /** Home/catalog callers can prefetch up to 200 IDs per provider request. */
    suspend fun prefetchRatings(ids: List<String>, mediaType: String, settings: MdbListSettings) {
        if (!settings.enabled || !settings.hasApiKey) return
        for (provider in settings.enabledProvidersInPriorityOrder()) {
            fetchBatch(ids, mediaType, provider, settings.apiKey.trim())
        }
    }

    private suspend fun fetchBatch(ids: List<String>, mediaType: String, provider: String, apiKey: String): Map<String, Double> {
        return try {
            client.ratings(ids, mediaType, provider, apiKey).also { _error.value = null }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _error.value = if (error is MdbListApiException) error.message else "Unable to load MDBList ratings"
            emptyMap()
        }
    }

    private suspend fun fetchRatings(
        imdbId: String, mediaType: String, apiKey: String, providers: List<String>,
    ): List<MetaExternalRating> = providers.mapNotNull { provider ->
        fetchBatch(listOf(imdbId), mediaType, provider, apiKey)[imdbId]?.let {
            MetaExternalRating(source = provider, value = it)
        }
    }

    private fun extractImdbId(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return imdbRegex.matchEntire(value)?.groupValues?.get(1)
    }

    private fun toMdbListMediaType(metaType: String): String {
        val normalized = metaType.trim().lowercase()
        return if (normalized == "movie") "movie" else "show"
    }
}
