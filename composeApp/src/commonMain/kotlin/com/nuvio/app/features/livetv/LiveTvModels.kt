package com.nuvio.app.features.livetv

import androidx.compose.runtime.Immutable

@Immutable
data class LiveTvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val logoUrl: String? = null,
    val group: String = "",
    val headers: Map<String, String> = emptyMap(),
    val stalkerCommand: String? = null,
    val sourceId: String = "",
    val hideKey: Long = 0L,
    val guideKey: String = liveTvGuideKey(tvgId, name),
)

const val LIVE_TV_UNGROUPED = ""

fun liveTvHideKey(sourceId: String, group: String, name: String): Long {
    var hash = -0x340d631b7bdddcdbL
    fun mix(text: String) {
        text.forEach { hash = (hash xor it.code.toLong()) * 0x100000001b3L }
        hash = (hash xor 0x1fL) * 0x100000001b3L
    }
    mix(sourceId); mix(group); mix(name)
    return hash
}

@Immutable data class LiveTvRecentChannel(val streamUrl:String,val name:String,val logoUrl:String?=null,val group:String="",val tvgId:String?=null) {
    val guideKey: String get() = liveTvGuideKey(tvgId, name)
}
@Immutable data class LiveTvProgramme(val title:String,val startEpochMs:Long,val stopEpochMs:Long)
enum class LiveTvSourceType { M3u, Stalker, Xtream }
@Immutable data class LiveTvStalkerSettings(val portalUrl:String="",val macAddress:String="",val username:String="",val password:String="") {
    val isConfigured:Boolean get()=portalUrl.isNotBlank()&&macAddress.isNotBlank()
}
@Immutable data class LiveTvXtreamSettings(val serverUrl:String="",val username:String="",val password:String="") {
    val isConfigured:Boolean get()=serverUrl.isNotBlank()&&username.isNotBlank()&&password.isNotBlank()
}
@Immutable data class LiveTvSource(
    val id:String,val type:LiveTvSourceType,val url:String="",
    val stalker:LiveTvStalkerSettings=LiveTvStalkerSettings(),
    val xtream:LiveTvXtreamSettings=LiveTvXtreamSettings(),
) {
    val label:String get()=url.substringAfter("://",url).substringBefore('/').substringBefore('?').substringAfterLast('@').ifBlank{url}
    val identity:String get()=when(type){
        LiveTvSourceType.M3u->"m3u|"+url.lowercase()
        LiveTvSourceType.Xtream->"xtream|"+xtream.serverUrl.lowercase()+"|"+xtream.username
        LiveTvSourceType.Stalker->"stalker|"+stalker.portalUrl.lowercase()+"|"+stalker.macAddress
    }
}

internal val NAME_NOISE=hashSetOf("hd","fhd","uhd","sd","hq","4k","8k","hevc","h265","h264","1080p","1080i","720p","576p","50fps","60fps")
internal val NAME_TAG=Regex("""^\s*(?:[\[(|]\s*[A-Za-z]{2,3}\s*[\])|]|[A-Za-z]{2,3}\s*[:|])\s*""")
fun liveTvNameKey(name:String):String {
    val out=StringBuilder(); var word=StringBuilder()
    fun flush(){if(word.isNotEmpty()){val token=word.toString();if(token !in NAME_NOISE)out.append(token);word=StringBuilder()}}
    NAME_TAG.replaceFirst(name,"").forEach{c->when{c.isLetterOrDigit()->word.append(c.lowercaseChar());c=='+'->word.append(c);else->flush()}}
    flush();return out.toString()
}
fun liveTvGuideKey(tvgId:String?,name:String):String=tvgId?.trim()?.takeIf{it.isNotEmpty()}?.lowercase()?:"\u0001"+liveTvNameKey(name)
fun currentProgrammes(schedule:Map<String,List<LiveTvProgramme>>,keys:Collection<String>,nowEpochMs:Long):Map<String,LiveTvProgramme> =
    keys.mapNotNull { key -> schedule[key]?.firstOrNull{nowEpochMs>=it.startEpochMs&&nowEpochMs<it.stopEpochMs}?.let{key to it} }.toMap()
