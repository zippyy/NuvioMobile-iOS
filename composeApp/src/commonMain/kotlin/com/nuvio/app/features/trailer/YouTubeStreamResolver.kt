package com.nuvio.app.features.trailer

/** Stream-level adapter reuses the full-distribution extractor without flattening DASH audio. */
class YouTubeStreamResolver(private val extract: suspend (String) -> TrailerPlaybackSource?) {
    suspend fun resolve(urlOrId: String): TrailerPlaybackSource? {
        val id = videoId(urlOrId) ?: return null
        return extract(watchUrl(id))
    }

    companion object {
        fun watchUrl(id: String): String {
            require(Regex("[A-Za-z0-9_-]{11}").matches(id)) { "Invalid YouTube video ID" }
            return "https://www.youtube.com/watch?v=$id"
        }

        fun videoId(value: String): String? {
            val idPattern = Regex("[A-Za-z0-9_-]{11}")
            if (idPattern.matches(value)) return value
            val match = Regex("^https?://([^/?#]+)([^?#]*)(?:\\?([^#]*))?(?:#.*)?$").matchEntire(value) ?: return null
            val host = match.groupValues[1].lowercase()
            val path = match.groupValues[2]
            val id = when (host) {
                "youtu.be" -> path.removePrefix("/")
                "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "www.youtube-nocookie.com" -> when {
                    path == "/watch" -> match.groupValues[3].split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
                    path.startsWith("/shorts/") -> path.removePrefix("/shorts/")
                    path.startsWith("/embed/") -> path.removePrefix("/embed/")
                    path.startsWith("/live/") -> path.removePrefix("/live/")
                    else -> null
                }
                else -> null
            }
            return id?.takeIf { idPattern.matches(it) }
        }
    }
}
