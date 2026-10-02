package com.nuvio.app.features.shuffle

import com.nuvio.app.core.time.isEpisodeReleaseAired
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.watchprogress.WatchProgressEntry

/**
 * Filters a list of videos to watchable episodes:
 * - non-special (season > 0)
 * - excludes entire seasons where the first episode is not yet available
 * - excludes episodes that are not available or are future releases
 */
internal fun List<MetaVideo>.watchableEpisodes(): List<MetaVideo> {
    val candidates = filter {
        it.season != null && it.episode != null && (it.season ?: 0) > 0
    }
    val unavailableSeasons = candidates.groupBy { it.season }
        .filter { (_, eps) ->
            val first = eps.minByOrNull { it.episode ?: Int.MAX_VALUE }
                ?: return@filter false
            if (first.available == false) return@filter true
            isFutureRelease(first.released)
        }.keys
    return candidates
        .filter { it.season !in unavailableSeasons }
        .filter { it.available != false && !isFutureRelease(it.released) }
}

private fun isFutureRelease(raw: String?): Boolean = isEpisodeReleaseAired(raw) == false
