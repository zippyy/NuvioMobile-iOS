package com.nuvio.app.features.torrent

internal expect object TorrServerSettingsStorage {
    fun load(): TorrServerSettings
    fun save(settings: TorrServerSettings)
}
