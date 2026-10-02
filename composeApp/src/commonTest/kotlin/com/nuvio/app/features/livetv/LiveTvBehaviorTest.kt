package com.nuvio.app.features.livetv

import kotlinx.coroutines.runBlocking
private fun assertEquals(expected: Any?, actual: Any?) = check(expected == actual) { "Expected <$expected>, got <$actual>" }
private fun assertFalse(value: Boolean) = check(!value)

fun main() = runBlocking {
    val channel = parseM3uPlaylist(sequenceOf("#EXTM3U", "#EXTINF:-1,News", "#EXTHTTP:{\"Authorization\":\"Bearer abc\"}", "https://tv.test/live|Referer=https%3A%2F%2Ftv.test%2F&X-Bad=bad%0D%0AInjected%3Ayes")).channels.single()
    assertEquals("Bearer abc", channel.headers["Authorization"])
    assertEquals("https://tv.test/", channel.headers["Referer"])
    assertFalse(channel.headers.keys.any { it == "X-Bad" })
    println("PASS playlist JSON headers, URL decoding, injection rejection")
    val requests = mutableListOf<Pair<String, Map<String,String>>>()
    val transport = LiveTvTransport { url, headers ->
        requests += url to headers
        when {
            "get_live_categories" in url -> """[{"category_id":"1","category_name":"News"}]"""
            "get_live_streams" in url -> """[{"stream_id":42,"name":"News HD","category_id":"1","epg_channel_id":"news"}]"""
            else -> """{"user_info":{"auth":1,"allowed_output_formats":["m3u8"]}}"""
        }
    }
    val providers = LiveTvProviders(transport)
    val source = LiveTvSource("one", LiveTvSourceType.Xtream, xtream=LiveTvXtreamSettings("https://panel.test/player_api.php", "u ser", "p&ss"))
    val loaded = providers.load(source)
    assertEquals("https://panel.test/live/u%20ser/p%26ss/42.m3u8", loaded.channels.single().streamUrl)
    assertEquals("News", loaded.channels.single().group)
    check(loaded.epgUrls.single().contains("xmltv.php?username=u%20ser&password=p%26ss"))
    println("PASS Xtream login negotiation, categories, credential encoding, guide endpoint")
    var links = 0
    val portal = LiveTvProviders(LiveTvTransport { url, headers ->
        val action=url.substringAfter("action=").substringBefore('&')
        if(action != "handshake") assertEquals("Bearer session",headers["Authorization"])
        when(action) {
            "handshake" -> """{"js":{"token":"session"}}"""
            "get_profile" -> """{"js":{}}"""
            "get_genres" -> """{"js":[{"id":"1","title":"News"}]}"""
            "get_ordered_list" -> if("p=1&" in url || url.endsWith("p=1")) """{"js":{"total_items":2,"data":[{"id":"1","name":"One","cmd":"ffmpeg http://localhost/ch/1","tv_genre_id":"1"}]}}""" else """{"js":{"total_items":2,"data":[{"id":"2","name":"Two","cmd":"ffmpeg http://localhost/ch/2"}]}}"""
            "create_link" -> { links++; """{"js":{"cmd":"ffmpeg https://play.test/expiring-$links"}}""" }
            else -> error("Unexpected request")
        }
    })
    val stalker=LiveTvSource("portal",LiveTvSourceType.Stalker,stalker=LiveTvStalkerSettings("https://portal.test/c", "00:11:22:33:44:55"))
    val portalChannels=portal.load(stalker).channels
    assertEquals(2,portalChannels.size)
    assertEquals("News",portalChannels.first().group)
    assertEquals("https://play.test/expiring-1",portal.resolve(stalker,portalChannels.first()).streamUrl)
    assertEquals("https://play.test/expiring-2",portal.resolve(stalker,portalChannels.first()).streamUrl)
    println("PASS Stalker handshake, authenticated profile, pagination and fresh per-play links")
    val now=xmlTvTimestamp("20261002120000 +0000")!!
    assertEquals(now,xmlTvTimestamp("20261002140000 +0200"))
    assertEquals(null,xmlTvTimestamp("20260230120000 +0000"))
    val epg=readXmlTvGuide("""<tv><channel id='news'><display-name>UK: News HD</display-name><icon src='https://logo.test/news'/></channel><programme channel='news' start='20261002110000 +0000' stop='20261002130000 +0000'><title>News &amp; Weather</title></programme><programme channel='news' start='20261002130000 +0000' stop='20261002140000 +0000'><title>Next</title></programme><programme channel='news' start='broken'""",listOf(channel.copy(tvgId="news",guideKey="news"),channel.copy(name="News",guideKey=liveTvGuideKey(null,"News"))),now)
    assertEquals("News & Weather",currentProgrammes(epg.schedule,epg.schedule.keys,now)["news"]?.title)
    assertEquals(2,epg.schedule[liveTvGuideKey(null,"News")]?.size)
    assertEquals("https://logo.test/news",epg.logos["news"])
    println("PASS XMLTV offsets, invalid dates, aliases, entities, current/next and malformed tail")
    val memory=MemoryLiveTvStore()
    var fail=false
    val repository=LiveTvRepository(LiveTvTransport { url, _ ->
        if(fail) throw LiveTvHttpException(403)
        """#EXTM3U
#EXTINF:-1 tvg-id="news" group-title="News",News
https://play.test/news
"""
    },memory,{now})
    repository.switchProfile(1)
    repository.addSource(LiveTvSource("m3u",LiveTvSourceType.M3u,"https://list.test/channels"))
    repository.refresh()
    val savedChannel=repository.state.value.channels.single()
    repository.toggleFavorite(savedChannel)
    repository.hideChannel(savedChannel,true)
    repository.recordPlayback(savedChannel)
    val restored=LiveTvRepository(transport,memory,{now})
    restored.switchProfile(1)
    assertEquals(setOf(savedChannel.hideKey),restored.state.value.preferences.favorites)
    assertEquals(savedChannel.hideKey,restored.state.value.preferences.recentKey)
    restored.switchProfile(2)
    assertEquals(0,restored.state.value.sources.size)
    fail=true; repository.refresh()
    assertEquals(1,repository.state.value.channels.size)
    assertEquals("Server returned HTTP 403",repository.state.value.sourceErrors["m3u"])
    repository.hideChannel(savedChannel,false)
    assertEquals(savedChannel,repository.neighbour(savedChannel,1))
    println("PASS persisted profile sources/favorites/hidden/recent, refresh failure isolation, wrapping")
    val cachedGuide=LiveTvGuide(mapOf("news" to listOf(LiveTvProgramme("Now",now-1000,now+1000))),nextReadAt=now+2000)
    val encodedGuide=encodeLiveTvGuide("source signature",cachedGuide)
    assertEquals(cachedGuide,decodeLiveTvGuide(encodedGuide,"source signature",now))
    assertEquals(null,decodeLiveTvGuide(encodedGuide,"changed source",now))
    assertEquals(null,decodeLiveTvGuide(encodedGuide,"source signature",now+2000))
    assertEquals(emptyMap<String,String>(),liveTvRedirectHeaders("https://a.test/path","https://b.test/path",mapOf("Authorization" to "secret","Cookie" to "secret","X-Token" to "secret")))
    var blocked=false
    try { liveTvRedirectHeaders("https://a.test/path","http://a.test/path",emptyMap()) } catch(_: IllegalArgumentException) { blocked=true }
    check(blocked)
    println("PASS guide cache roundtrip/invalidation/expiry and redirect credential/downgrade guards")
    repository.removeSource("m3u")
    repository.addSource(LiveTvSource("import",LiveTvSourceType.M3u,playlist="#EXTM3U\n#EXTINF:-1,Imported\nhttps://play.test/imported"))
    repository.refresh()
    assertEquals("Imported",repository.state.value.channels.single().name)
    println("PASS imported playlist source survives provider-independent refresh")
}
