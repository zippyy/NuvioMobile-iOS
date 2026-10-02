package com.nuvio.app.features.shuffle

@kotlinx.serialization.Serializable
data class EpisodeShuffleSettings(
    val enabled: Boolean = false,
    val includeWatched: Boolean = false,
)
