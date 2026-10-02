package com.nuvio.app.features.artwork

import platform.Foundation.NSUserDefaults
internal actual object PlatformArtworkStorage : ArtworkStorage {
    actual override fun load(profileId: Int): String? = NSUserDefaults.standardUserDefaults.stringForKey("custom_artwork_$profileId")
    actual override fun save(profileId: Int, payload: String) { NSUserDefaults.standardUserDefaults.setObject(payload, forKey = "custom_artwork_$profileId") }
}
