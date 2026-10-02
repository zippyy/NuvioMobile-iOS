package com.nuvio.app.features.livetv

import kotlinx.serialization.json.*

interface LiveTvStore { fun read(key: String): String?; fun write(key: String, value: String) }
// Useful for deterministic tests and embedders; production uses platform protected storage.
class MemoryLiveTvStore : LiveTvStore {
    private val data=mutableMapOf<String,String>()
    override fun read(key: String)=data[key]
    override fun write(key: String,value: String) { data[key]=value }
}
data class LiveTvPreferences(
    val favorites: Set<Long> = emptySet(), val hiddenChannels: Set<Long> = emptySet(),
    val hiddenGroups: Set<String> = emptySet(), val groupNames: Map<String,String> = emptyMap(),
    val groupOrder: List<String> = emptyList(), val recentKey: Long? = null,
)
internal fun encodeLiveTvProfile(sources: List<LiveTvSource>, p: LiveTvPreferences): String = buildJsonObject {
    put("version",1)
    put("sources",buildJsonArray { sources.forEach { s -> add(buildJsonObject {
        put("id",s.id); put("type",s.type.name); put("url",s.url); put("epg",s.epgUrl); put("playlist",s.playlist)
        put("headers",buildJsonObject { s.headers.forEach { (k,v)->put(k,v) } })
        put("server",s.xtream.serverUrl); put("xtUser",s.xtream.username); put("xtPass",s.xtream.password)
        put("portal",s.stalker.portalUrl); put("mac",s.stalker.macAddress); put("stUser",s.stalker.username); put("stPass",s.stalker.password)
    }) } })
    put("favorites",buildJsonArray { p.favorites.forEach { add(it.toString()) } })
    put("hidden",buildJsonArray { p.hiddenChannels.forEach { add(it.toString()) } })
    put("groups",buildJsonArray { p.hiddenGroups.forEach { add(it) } })
    put("order",buildJsonArray { p.groupOrder.forEach { add(it) } })
    put("names",buildJsonObject { p.groupNames.forEach { (k,v)->put(k,v) } })
    p.recentKey?.let { put("recent",it.toString()) }
}.toString()
internal fun decodeLiveTvProfile(raw: String?): Pair<List<LiveTvSource>,LiveTvPreferences> = runCatching {
    val root=Json.parseToJsonElement(raw?:"{}") as JsonObject
    fun strings(key:String)=(root[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
    val sources=(root["sources"] as? JsonArray)?.mapNotNull { value ->
        val s=value as? JsonObject?:return@mapNotNull null
        val type=s.field("type")?.let { runCatching { LiveTvSourceType.valueOf(it) }.getOrNull() }?:return@mapNotNull null
        LiveTvSource(s.field("id")?:return@mapNotNull null,type,s.field("url").orEmpty(),
            headers=(s["headers"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }.orEmpty(),epgUrl=s.field("epg").orEmpty(),playlist=s.field("playlist").orEmpty(),
            xtream=LiveTvXtreamSettings(s.field("server").orEmpty(),s.field("xtUser").orEmpty(),s.field("xtPass").orEmpty()),
            stalker=LiveTvStalkerSettings(s.field("portal").orEmpty(),s.field("mac").orEmpty(),s.field("stUser").orEmpty(),s.field("stPass").orEmpty()))
    }.orEmpty()
    sources to LiveTvPreferences(strings("favorites").mapNotNull { it.toLongOrNull() }.toSet(),strings("hidden").mapNotNull { it.toLongOrNull() }.toSet(),strings("groups").toSet(),
        (root["names"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }.orEmpty(),strings("order"),root.field("recent")?.toLongOrNull())
}.getOrDefault(emptyList<LiveTvSource>() to LiveTvPreferences())

internal fun encodeLiveTvGuide(key: String, guide: LiveTvGuide): String = buildJsonObject {
    put("version",1); put("key",key); put("next",guide.nextReadAt)
    put("logos",buildJsonObject { guide.logos.forEach { (k,v)->put(k,v) } })
    put("schedule",buildJsonObject { guide.schedule.forEach { (k,list)->put(k,buildJsonArray {
        list.forEach { p->add(buildJsonArray { add(p.title); add(p.startEpochMs); add(p.stopEpochMs) }) }
    }) } })
}.toString()
internal fun decodeLiveTvGuide(raw: String?, key: String, now: Long): LiveTvGuide? = runCatching {
    val root=Json.parseToJsonElement(raw?:return null) as JsonObject
    if(root.field("version")!="1" || root.field("key")!=key || (root.field("next")?.toLongOrNull()?:0)<=now) return null
    val schedule=(root["schedule"] as JsonObject).mapValues { (_,value)->value.jsonArray.map { p->
        val fields=p.jsonArray; LiveTvProgramme(fields[0].jsonPrimitive.content,fields[1].jsonPrimitive.long,fields[2].jsonPrimitive.long)
    } }
    LiveTvGuide(schedule,(root["logos"] as JsonObject).mapValues { it.value.jsonPrimitive.content },root["next"]!!.jsonPrimitive.long)
}.getOrNull()
