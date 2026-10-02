package com.nuvio.app.features.artwork

import android.content.Context
import android.content.SharedPreferences
internal actual object PlatformArtworkStorage : ArtworkStorage {
    private var preferences: SharedPreferences? = null
    fun initialize(context: Context) { preferences = context.getSharedPreferences("custom_artwork", Context.MODE_PRIVATE) }
    actual override fun load(profileId: Int): String? = checkNotNull(preferences).getString("profile_$profileId", null)
    actual override fun save(profileId: Int, payload: String) { checkNotNull(preferences).edit().putString("profile_$profileId", payload).apply() }
}
