package com.nuvio.app.features.torrent

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object TorrServerSettingsStorage {
    private val defaults get() = NSUserDefaults.standardUserDefaults
    actual fun load(): TorrServerSettings = TorrServerSettings(
        defaults.boolForKey(ProfileScopedKey.of("torrserver_enabled")),
        defaults.stringForKey(ProfileScopedKey.of("torrserver_url")).orEmpty(),
        defaults.boolForKey(ProfileScopedKey.of("torrserver_save_to_db")),
    )
    actual fun save(settings: TorrServerSettings) {
        defaults.setBool(settings.enabled, forKey = ProfileScopedKey.of("torrserver_enabled"))
        defaults.setObject(settings.baseUrl, forKey = ProfileScopedKey.of("torrserver_url"))
        defaults.setBool(settings.saveToDatabase, forKey = ProfileScopedKey.of("torrserver_save_to_db"))
    }
}
