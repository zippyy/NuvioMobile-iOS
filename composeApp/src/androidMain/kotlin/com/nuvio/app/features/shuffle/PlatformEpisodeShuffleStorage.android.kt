package com.nuvio.app.features.shuffle

import android.content.Context
import android.content.SharedPreferences

internal actual object PlatformEpisodeShuffleStorage : EpisodeShuffleStorage {
    private var preferences: SharedPreferences? = null
    fun initialize(context: Context) { preferences = context.getSharedPreferences("episode_shuffle", Context.MODE_PRIVATE) }
    actual override fun load(profileId: Int): String? = checkNotNull(preferences) { "Episode shuffle storage not initialized" }.getString("profile_$profileId", null)
    actual override fun save(profileId: Int, payload: String) { checkNotNull(preferences) { "Episode shuffle storage not initialized" }.edit().putString("profile_$profileId", payload).apply() }
}
