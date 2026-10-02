package com.nuvio.app.features.shuffle

/**
 * In-memory persistence for episode shuffle settings.
 * Production code should wire this to the app's settings storage layer.
 */
class EpisodeShuffleStore {
    private val store = mutableMapOf<String, EpisodeShuffleSettings>()

    fun settings(contentId: String, contentType: String): EpisodeShuffleSettings {
        val saved = store[contentId] ?: EpisodeShuffleSettings()
        return saved.copy(
            enabled = saved.enabled &&
                (contentType.equals("series", ignoreCase = true) ||
                    contentType.equals("tv", ignoreCase = true)),
        )
    }

    fun save(contentId: String, settings: EpisodeShuffleSettings) {
        if (contentId.isBlank()) return
        store[contentId] = settings
    }

    fun clear() {
        store.clear()
    }
}
