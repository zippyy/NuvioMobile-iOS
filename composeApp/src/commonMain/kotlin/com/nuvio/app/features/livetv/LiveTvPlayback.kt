package com.nuvio.app.features.livetv

import com.nuvio.app.features.player.PlayerLaunch

/** Channel content type is explicit: 'tv' means a series elsewhere in the app. */
fun liveTvPlayerLaunch(profileId: Int, channel: LiveTvChannel): PlayerLaunch = PlayerLaunch(
    profileId=profileId,title=channel.name,sourceUrl=channel.streamUrl,
    sourceHeaders=safeLiveTvHeaders(channel.headers),logo=channel.logoUrl,
    streamTitle=channel.name,providerName="Live TV",contentType="channel",streamType="live",
    videoId="live:${channel.sourceId}:${channel.hideKey}",parentMetaId="live:${channel.sourceId}:${channel.hideKey}",parentMetaType="channel",
    initialPositionMs=0,initialProgressFraction=null,
)
