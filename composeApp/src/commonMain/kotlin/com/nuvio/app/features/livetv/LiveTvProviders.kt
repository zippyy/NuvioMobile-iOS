package com.nuvio.app.features.livetv

import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun JsonElement.field(key: String): String? = (this as? JsonObject)?.get(key)?.let { (it as? JsonPrimitive)?.contentOrNull }
internal fun jsonObjects(text: String): List<JsonObject> = (Json.parseToJsonElement(text) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

class LiveTvProviders(private val http: LiveTvTransport) {
    suspend fun load(source: LiveTvSource): ParsedM3uPlaylist {
        val result = when (source.type) {
            LiveTvSourceType.M3u -> {
                if(source.playlist.isNotBlank()) parseM3uPlaylist(source.playlist.lineSequence())
                else if (source.url.looksLikeDirectVideoUrl()) ParsedM3uPlaylist(listOf(directStreamChannel(requireLiveTvUrl(source.url))), emptyList())
                else {
                    val parsed = parseM3uPlaylist(http.text(requireLiveTvUrl(source.url), safeLiveTvHeaders(LIVE_TV_PLAYLIST_HEADERS, source.headers)).lineSequence())
                    if (parsed.isHlsStream) parsed.copy(channels=listOf(directStreamChannel(source.url))) else parsed
                }
            }
            LiveTvSourceType.Xtream -> xtream(source.xtream)
            LiveTvSourceType.Stalker -> stalker(source.stalker)
        }
        return result.copy(channels=result.channels.map { channel -> channel.copy(
            id=source.id+":"+channel.id, sourceId=source.id,
            hideKey=liveTvHideKey(source.id, channel.group, channel.name),
            headers=safeLiveTvHeaders(channel.headers, source.headers),
        ) }, epgUrls=(result.epgUrls+source.epgUrl.takeIf { it.isNotBlank() }.orEmpty()).filter { it.isHttpUrl() }.distinct())
    }
    private suspend fun xtream(raw: LiveTvXtreamSettings): ParsedM3uPlaylist {
        val s=raw.copy(serverUrl=requireLiveTvUrl(raw.serverUrl.trim().substringBefore("/player_api.php").trimEnd('/')), username=raw.username.trim())
        require(s.isConfigured) { "Missing Xtream login" }
        val query="username=${encodeLiveTvComponent(s.username)}&password=${encodeLiveTvComponent(s.password)}"
        suspend fun api(action: String="")=http.text("${s.serverUrl}/player_api.php?$query"+if(action.isEmpty()) "" else "&action=$action", LIVE_TV_PLAYLIST_HEADERS)
        val user=(Json.parseToJsonElement(api()) as? JsonObject)?.get("user_info") as? JsonObject
        if(user?.field("auth")=="0") throw LiveTvHttpException(401)
        val formats=(user?.get("allowed_output_formats") as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
        val extension=if(formats.isNotEmpty() && "ts" !in formats && "m3u8" in formats) "m3u8" else "ts"
        val categories=jsonObjects(api("get_live_categories")).associate { it.field("category_id").orEmpty() to it.field("category_name").orEmpty() }
        val channels=jsonObjects(api("get_live_streams")).mapNotNull { c ->
            val id=c.field("stream_id")?:return@mapNotNull null; val name=c.field("name")?:return@mapNotNull null
            LiveTvChannel("xtream-$id",name,"${s.serverUrl}/live/${encodeLiveTvComponent(s.username)}/${encodeLiveTvComponent(s.password)}/${encodeLiveTvComponent(id)}.$extension",
                tvgId=c.field("epg_channel_id"),logoUrl=c.field("stream_icon"),group=categories[c.field("category_id")].orEmpty(),headers=LIVE_TV_STREAM_HEADERS)
        }.distinctBy { it.streamUrl }
        return ParsedM3uPlaylist(channels,listOf("${s.serverUrl}/xmltv.php?$query"))
    }
    private val sessions=mutableMapOf<LiveTvStalkerSettings,String>()
    private val sessionMutex=Mutex()
    private fun base(s: LiveTvStalkerSettings)=safeLiveTvHeaders(mapOf(
        "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 MAG254 stbapp ver: 2 rev: 250 Safari/533.3",
        "X-User-Agent" to "Model: MAG254; Link: Ethernet", "Referer" to s.portalUrl.trimEnd('/')+"/c/",
        "Cookie" to "mac=${encodeLiveTvComponent(s.macAddress.uppercase())}; stb_lang=en; timezone=UTC"))
    private suspend fun request(s: LiveTvStalkerSettings, token: String?, type: String, action: String, extra: Map<String,String> = emptyMap()): JsonElement {
        val root=requireLiveTvUrl(s.portalUrl.trim().trimEnd('/'))
        val endpoint=when { root.endsWith(".php") -> root; root.endsWith("/c") -> root.removeSuffix("/c")+"/server/load.php"; else -> "$root/server/load.php" }
        val args=mapOf("type" to type,"action" to action,"JsHttpRequest" to "1-xml")+extra
        val text=http.text(endpoint+"?"+args.entries.joinToString("&") { encodeLiveTvComponent(it.key)+"="+encodeLiveTvComponent(it.value) },safeLiveTvHeaders(base(s),token?.let { mapOf("Authorization" to "Bearer $it") }.orEmpty()))
        val json=Json.parseToJsonElement(text)
        return (json as? JsonObject)?.get("js")?:json
    }
    private suspend fun token(s: LiveTvStalkerSettings, renew: Boolean=false): String = sessionMutex.withLock {
        if(renew) sessions.remove(s)
        sessions[s]?:run {
            val token=request(s,null,"stb","handshake").field("token")?.takeIf { it.isNotBlank() }?:error("Portal did not return a token")
            request(s,token,"stb","get_profile",mapOf("mac" to s.macAddress,"stb_type" to "MAG254","hd" to "1","auth_second_step" to "1","login" to s.username,"password" to s.password))
            sessions[s]=token; token
        }
    }
    private suspend fun <T> session(s: LiveTvStalkerSettings, block: suspend (String)->T): T {
        require(s.isConfigured); val token=token(s)
        return try { block(token) } catch (e: CancellationException) { throw e } catch (_: Exception) { block(token(s,true)) }
    }
    private fun entries(js: JsonElement): List<JsonObject> = when(js) { is JsonArray->js.mapNotNull { it as? JsonObject }; is JsonObject->(js["data"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty(); else->emptyList() }
    private suspend fun stalker(s: LiveTvStalkerSettings): ParsedM3uPlaylist = session(s) { token ->
        val genres=entries(request(s,token,"itv","get_genres")).associate { it.field("id").orEmpty() to (it.field("title")?:it.field("name")).orEmpty() }
        val all=mutableListOf<JsonObject>()
        var page=1; var expected: Int?=null
        do {
            val js=request(s,token,"itv","get_ordered_list",mapOf("p" to page.toString()))
            val data=entries(js); if(page==1) expected=js.field("total_items")?.toIntOrNull()
            if(data.isEmpty()) { check(expected==null || all.size>=expected) { "Incomplete portal channel list" }; break }
            all+=data; page++
        } while(page<=500 && (expected==null || all.size<expected))
        check(expected==null || all.size>=expected) { "Incomplete portal channel list" }
        val channels=all.mapNotNull { c ->
            val command=c.field("cmd")?:return@mapNotNull null; val name=c.field("name")?:return@mapNotNull null
            val id=c.field("id")?:command
            val logo=c.field("logo")?.let { if(it.isHttpUrl()) it else liveTvOrigin(s.portalUrl)+if(it.startsWith('/')) it else "/misc/logos/320/$it" }
            LiveTvChannel("stalker-$id",name,command.substringAfter("ffmpeg ").trim(),tvgId=c.field("xmltv_id"),logoUrl=logo,group=genres[c.field("tv_genre_id")?:c.field("genre_id")].orEmpty(),headers=base(s),stalkerCommand=command)
        }.distinctBy { it.id }
        ParsedM3uPlaylist(channels,emptyList())
    }
    suspend fun resolve(source: LiveTvSource, channel: LiveTvChannel): LiveTvChannel {
        if(channel.stalkerCommand==null) return channel
        return session(source.stalker) { token ->
            val js=request(source.stalker,token,"itv","create_link",mapOf("cmd" to channel.stalkerCommand))
            val url=(js.field("cmd")?:js.field("url")?:error("Portal did not return a link")).substringAfter("ffmpeg ").trim()
            channel.copy(streamUrl=requireLiveTvUrl(url),headers=safeLiveTvHeaders(channel.headers,base(source.stalker),mapOf("Authorization" to "Bearer $token")))
        }
    }
}
