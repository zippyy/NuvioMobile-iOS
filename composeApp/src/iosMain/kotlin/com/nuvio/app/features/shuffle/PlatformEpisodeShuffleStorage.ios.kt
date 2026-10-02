package com.nuvio.app.features.shuffle

import platform.Foundation.NSUserDefaults

internal actual object PlatformEpisodeShuffleStorage : EpisodeShuffleStorage {
    actual override fun load(profileId: Int): String? = NSUserDefaults.standardUserDefaults.stringForKey("episode_shuffle_$profileId")
    actual override fun save(profileId: Int, payload: String) { NSUserDefaults.standardUserDefaults.setObject(payload, forKey = "episode_shuffle_$profileId") }
}
