package com.nuvio.app.features.torrent

import com.nuvio.app.features.addons.httpRequestRaw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Loaded per call so profile changes cannot reuse another profile's server/configuration. */
object TorrServerSettingsRepository {
    fun snapshot(): TorrServerSettings = TorrServerSettingsStorage.load()
    fun save(settings: TorrServerSettings) {
        val url = if (settings.baseUrl.isBlank() && !settings.enabled) "" else TorrServerResolver.normalizeServerUrl(settings.baseUrl)
        TorrServerSettingsStorage.save(settings.copy(baseUrl = url))
        _error.value = null
    }
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val resolver = TorrServerResolver { url, body ->
        val response = httpRequestRaw("POST", url, mapOf("Content-Type" to "application/json"), body, followRedirects = false)
        TorrServerResponse(response.status, response.body)
    }
    suspend fun resolve(url: String): String? {
        try {
            return resolver.resolve(url, snapshot()).also { _error.value = null }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: TorrServerException) {
            _error.value = failure.message
            throw failure
        }
    }
}
