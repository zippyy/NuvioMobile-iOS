package com.nuvio.app.features.shuffle

internal class TestShuffleStorage : EpisodeShuffleStorage {
    private val payloads = mutableMapOf<Int, String>()
    override fun load(profileId: Int) = payloads[profileId]
    override fun save(profileId: Int, payload: String) { payloads[profileId] = payload }
}
