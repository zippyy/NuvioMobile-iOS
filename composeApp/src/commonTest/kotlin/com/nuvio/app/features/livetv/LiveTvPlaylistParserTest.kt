package com.nuvio.app.features.livetv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
class LiveTvPlaylistParserTest {
 private fun parse(s:String)=parseM3uPlaylist(s.trimIndent().lineSequence())
 @Test fun `reads channels groups logos guide and headers`() {
  val p=parse("""#EXTM3U url-tvg="https://guide.example/epg.xml.gz"
#EXTINF:-1 tvg-id="one.uk" tvg-logo="https://logo/1.png" group-title="News",Channel One
https://stream.example/1.m3u8
#EXTINF:-1 group-title="Sports",Sport, HD
#EXTVLCOPT:http-user-agent=Custom
https://stream.example/2.ts|Referer=https://ref.example""")
  assertEquals(listOf("https://guide.example/epg.xml.gz"),p.epgUrls);assertEquals(2,p.channels.size)
  assertEquals("one.uk",p.channels[0].tvgId);assertEquals("Sport, HD",p.channels[1].name)
  assertEquals("Custom",p.channels[1].headers["User-Agent"]);assertEquals("https://ref.example",p.channels[1].headers["Referer"])
 }
 @Test fun `drops duplicates and separators`() {
  val p=parse("""#EXTM3U
#EXTINF:-1,##### SPORTS #####
https://stream.example/separator
#EXTINF:-1,A
https://stream.example/a
#EXTINF:-1,A again
https://stream.example/a""")
  assertEquals(listOf("A"),p.channels.map{it.name})
 }
 @Test fun `recognises hls manifest`() {val p=parse("""#EXTM3U
#EXT-X-VERSION:3
#EXTINF:6.0,
segment1.ts""");assertTrue(p.isHlsStream);assertTrue(p.channels.isEmpty())}
 @Test fun `guide keys normalize channel names`() {
  assertEquals("bbcone",liveTvNameKey("UK: BBC One HD"));assertEquals("bbcone",liveTvNameKey("|UK| BBC-One FHD"));assertEquals("channel4+1",liveTvNameKey("Channel 4 +1"))
 }
}
