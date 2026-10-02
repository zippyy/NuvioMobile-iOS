package com.nuvio.app.features.torrent

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object TorrServerSettingsStorage {
    private var preferences: SharedPreferences? = null
    fun initialize(context: Context) {
        preferences = context.getSharedPreferences("nuvio_torrserver", Context.MODE_PRIVATE)
    }
    actual fun load(): TorrServerSettings = TorrServerSettings(
        preferences?.getBoolean(ProfileScopedKey.of("enabled"), false) ?: false,
        preferences?.getString(ProfileScopedKey.of("url"), "").orEmpty(),
        preferences?.getBoolean(ProfileScopedKey.of("save_to_db"), false) ?: false,
    )
    actual fun save(settings: TorrServerSettings) {
        checkNotNull(preferences) { "TorrServerSettingsStorage must be initialized" }.edit()
            .putBoolean(ProfileScopedKey.of("enabled"), settings.enabled)
            .putString(ProfileScopedKey.of("url"), settings.baseUrl)
            .putBoolean(ProfileScopedKey.of("save_to_db"), settings.saveToDatabase)
            .apply()
    }
}
