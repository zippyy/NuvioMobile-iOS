package com.nuvio.app.features.livetv

data class ParsedM3uPlaylist(val channels:List<LiveTvChannel>,val epgUrls:List<String>,val isHlsStream:Boolean=false)
fun parseM3uPlaylist(lines:Sequence<String>):ParsedM3uPlaylist {
    val channels=ArrayList<LiveTvChannel>();val seen=HashSet<String>();val epg=LinkedHashSet<String>()
    var meta:M3uMetadata?=null;var pending=emptyMap<String,String>()
    // Interning for groups and header sets shared across many channels
    val groupPool=HashMap<String,String>();val headerPool=HashMap<Map<String,String>,Map<String,String>>()
    fun internGroup(g:String)=groupPool.getOrPut(g){g}
    fun internHeaders(h:Map<String,String>)=headerPool.getOrPut(h){h}
    for(raw in lines){val line=raw.trim().removePrefix("\ufeff");when{
        line.isEmpty()->Unit
        line.startsWith("#EXTM3U",true)->parseM3uAttributes(line).let{a->listOfNotNull(a["url-tvg"],a["x-tvg-url"],a["tvg-url"]).flatMap{it.split(',',';')}.map(String::trim).filter{it.isHttpUrl()}.forEach(epg::add)}
        line.startsWith("#EXT-X-",true)->return ParsedM3uPlaylist(emptyList(),emptyList(),true)
        line.startsWith("#EXTINF",true)->meta=parseExtInf(line)
        line.startsWith("#EXTVLCOPT:http-user-agent=",true)->pending=pending+("User-Agent" to line.substringAfter('=').trim())
        line.startsWith("#EXTVLCOPT:http-referrer=",true)->pending=pending+("Referer" to line.substringAfter('=').trim())
        line.startsWith("#EXTHTTP:",true)->{ // EXTHTTP:{'key':'val','key2':'val2'} - TVHeadend style JSON-like headers
            val json=line.substringAfter("#EXTHTTP:").trim()
            val parsed=parseExthttpHeaders(json)
            if(parsed.isNotEmpty())pending=pending+parsed
        }
        line.startsWith("#")->Unit
        else->{val url=line.substringBefore('|').trim();val m=meta;meta=null;val headers=pending+parseUrlHeaders(line);pending=emptyMap()
            if(url.isEmpty()||!seen.add(url))continue
            val name=m?.name?.takeIf(String::isNotBlank)?:"Channel "+(channels.size+1);if(isLikelyCategoryHeading(name))continue
            channels+=LiveTvChannel(id="m"+channels.size,name=name,streamUrl=url,tvgId=m?.tvgId,logoUrl=m?.logoUrl,group=internGroup(m?.group.orEmpty()),headers=internHeaders(safeLiveTvHeaders(defaultStreamHeaders(url), headers)))
        }
    }}
    return ParsedM3uPlaylist(channels,epg.toList())
}

/**
 * Parses EXTHTTP JSON-like header format: {'key':'val','key2':'val2'}
 * TVHeadend and some IPTV providers use this format for per-channel HTTP headers.
 */
private fun parseExthttpHeaders(json: String): Map<String, String> = runCatching {
    kotlinx.serialization.json.Json.parseToJsonElement(json.replace('\'', '"')).let { element ->
        (element as? kotlinx.serialization.json.JsonObject)?.mapNotNull { (key, value) ->
            (value as? kotlinx.serialization.json.JsonPrimitive)?.content?.let { key to it }
        }?.toMap().orEmpty()
    }
}.getOrDefault(emptyMap())

private data class M3uMetadata(val name:String,val tvgId:String?,val logoUrl:String?,val group:String)
private val attr=Regex("""([\w-]+)="([^"]*)"""")
private fun parseM3uAttributes(line:String)=attr.findAll(line).associate{it.groupValues[1].lowercase() to it.groupValues[2].trim()}
private fun parseExtInf(line:String):M3uMetadata{val comma=firstUnquotedComma(line);val a=parseM3uAttributes(if(comma>=0)line.substring(0,comma) else line);val n=(if(comma>=0)line.substring(comma+1)else"").trim().ifBlank{a["tvg-name"].orEmpty()};return M3uMetadata(n,a["tvg-id"]?.takeIf(String::isNotBlank),a["tvg-logo"]?.takeIf(String::isNotBlank),a["group-title"].orEmpty())}
private fun parseUrlHeaders(line:String):Map<String,String>{val o=line.substringAfter('|',"");if(o.isEmpty())return emptyMap();return o.split('&').mapNotNull{val k=it.substringBefore('=').trim();val v=it.substringAfter('=',"").trim();if(k.isBlank()||v.isBlank())null else decodeLiveTvComponent(k) to decodeLiveTvComponent(v)}.toMap()}
fun String.isHttpUrl()=startsWith("http://",true)||startsWith("https://",true)
private val heading=Regex("""^\s*#+\s*.+\s*#+\s*$""")
fun isLikelyCategoryHeading(name:String)=name.trim().let{it.length>=3&&it.startsWith('#')&&it.endsWith('#')&&heading.matches(it)}
fun String.looksLikeDirectVideoUrl():Boolean{val p=substringBefore('#').substringBefore('?').lowercase();if(p.endsWith(".m3u")||p.endsWith(".m3u8"))return false;return listOf(".mp4",".mkv",".webm",".mov",".avi",".ts",".mpeg",".mpg").any(p::endsWith)}
fun directStreamChannel(url:String)=LiveTvChannel("direct-"+url.hashCode(),url.substringBefore('?').substringAfterLast('/').ifBlank{"Live stream"},url,headers=defaultStreamHeaders(url))
val LIVE_TV_PLAYLIST_HEADERS=mapOf("User-Agent" to "VLC/3.0.0 LibVLC/3.0.0","Accept" to "application/x-mpegURL, application/vnd.apple.mpegurl, audio/mpegurl, text/plain, */*")
val LIVE_TV_STREAM_HEADERS=mapOf("User-Agent" to "VLC/3.0.0 LibVLC/3.0.0")
fun defaultStreamHeaders(url:String)=if(url.isHttpUrl()) LIVE_TV_STREAM_HEADERS else emptyMap()
private fun firstUnquotedComma(s:String):Int{var q=false;s.forEachIndexed{i,c->if(c=='"')q=!q else if(c==','&&!q)return i};return -1}
