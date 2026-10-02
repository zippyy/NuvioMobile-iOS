package com.nuvio.app.features.livetv

/** Bounded programme windows avoid retaining an entire multi-day provider schedule. */
data class LiveTvGuideWindow(val pastMs: Long=3*3_600_000L,val maxPast: Int=6,val aheadMs: Long=12*3_600_000L,val maxAhead: Int=18)
data class LiveTvGuide(val schedule: Map<String,List<LiveTvProgramme>> = emptyMap(), val logos: Map<String,String> = emptyMap(), val nextReadAt: Long=0)

fun xmlTvTimestamp(raw: String, localOffsetMinutes: Int=0): Long? = runCatching {
    val parts=raw.trim().split(Regex("\\s+")); val stamp=parts[0]
    require(stamp.length==14 && stamp.all(Char::isDigit))
    val year=stamp.substring(0,4).toInt(); val month=stamp.substring(4,6).toInt(); val day=stamp.substring(6,8).toInt()
    val hour=stamp.substring(8,10).toInt(); val minute=stamp.substring(10,12).toInt(); val second=stamp.substring(12,14).toInt()
    val leap=year%4==0 && (year%100!=0 || year%400==0)
    val daysInMonth=listOf(31,if(leap)29 else 28,31,30,31,30,31,31,30,31,30,31)
    require(year in 1600..9999 && month in 1..12 && day in 1..daysInMonth[month-1] && hour in 0..23 && minute in 0..59 && second in 0..59)
    val zone=parts.getOrNull(1)
    val offset=if(zone==null) localOffsetMinutes else {
        require(zone.matches(Regex("[+-]\\d{4}")))
        val h=zone.substring(1,3).toInt(); val m=zone.substring(3,5).toInt(); require(h<=23 && m<60)
        (h*60+m)*if(zone[0]=='-') -1 else 1
    }
    // Gregorian civil date to days since Unix epoch (no platform date formatter state).
    val y=year-if(month<=2)1 else 0; val era=y/400; val yoe=y-era*400
    val mp=month+if(month>2)-3 else 9; val doy=(153*mp+2)/5+day-1
    val doe=yoe*365+yoe/4-yoe/100+doy
    val days=era*146097L+doe-719468
    (days*86400+hour*3600+minute*60+second-offset*60)*1000
}.getOrNull()

private fun xmlText(raw: String): String {
    val cdata=Regex("<!\\[CDATA\\[(.*?)]]>",RegexOption.DOT_MATCHES_ALL).replace(raw) { it.groupValues[1] }
    val plain=Regex("<[^>]+>").replace(cdata,"")
    return Regex("&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);").replace(plain) {
        when(val entity=it.groupValues[1]) {
            "amp"->"&"; "lt"->"<"; "gt"->">"; "quot"->"\""; "apos"->"'"
            else->runCatching { val code=if(entity.startsWith("#x"))entity.drop(2).toInt(16) else entity.drop(1).toInt(); if(code in 0..65535) code.toChar().toString() else "" }.getOrDefault("")
        }
    }.trim()
}
private fun xmlAttributes(tag: String)=Regex("([\\w:-]+)\\s*=\\s*(['\"])(.*?)\\2",RegexOption.DOT_MATCHES_ALL).findAll(tag).associate { it.groupValues[1] to xmlText(it.groupValues[3]) }
private fun xmlElements(xml: String, tag: String): Sequence<Pair<Map<String,String>,String>> = sequence {
    var offset=0
    while(true) {
        val start=xml.indexOf("<$tag",offset); if(start<0) break
        val openEnd=xml.indexOf('>',start); if(openEnd<0) break
        val end=xml.indexOf("</$tag>",openEnd); if(end<0) break
        yield(xmlAttributes(xml.substring(start,openEnd)) to xml.substring(openEnd+1,end))
        offset=end+tag.length+3
    }
}
fun readXmlTvGuide(xml: String, channels: List<LiveTvChannel>, now: Long, window: LiveTvGuideWindow=LiveTvGuideWindow(), localOffsetMinutes: Int=0): LiveTvGuide {
    // Never process DTD/entity declarations or perform network entity resolution.
    require(!xml.contains("<!ENTITY",true)) { "XMLTV entity declarations are not supported" }
    val keysById=channels.filter { !it.tvgId.isNullOrBlank() }.groupBy { it.tvgId!!.trim().lowercase() }
    val keysByName=channels.groupBy { liveTvNameKey(it.name) }
    val aliases=mutableMapOf<String,Set<String>>(); val logos=mutableMapOf<String,String>()
    xmlElements(xml,"channel").forEach { (attrs,body) ->
        val id=attrs["id"]?.lowercase()?:return@forEach
        val keys=(keysById[id].orEmpty()+xmlElements(body,"display-name").flatMap { keysByName[liveTvNameKey(xmlText(it.second))].orEmpty().asSequence() }).map { it.guideKey }.toSet()
        aliases[id]=keys
        val icon=Regex("<icon\\s+[^>]*>").find(body)?.value?.let(::xmlAttributes)?.get("src")
        if(icon?.isHttpUrl()==true) keys.forEach { logos[it]=icon }
    }
    val schedule=mutableMapOf<String,MutableList<LiveTvProgramme>>(); val titlePool=mutableMapOf<String,String>(); val truncated=mutableSetOf<String>()
    xmlElements(xml,"programme").forEach { (attrs,body) ->
        val id=attrs["channel"]?.lowercase()?:return@forEach
        val keys=aliases[id]?:keysById[id]?.map { it.guideKey }?.toSet().orEmpty()
        if(keys.isEmpty()) return@forEach
        val start=xmlTvTimestamp(attrs["start"].orEmpty(),localOffsetMinutes)?:return@forEach
        val stop=xmlTvTimestamp(attrs["stop"].orEmpty(),localOffsetMinutes)?:return@forEach
        if(stop<=start || stop<=now-window.pastMs || start>=now+window.aheadMs) return@forEach
        val title=xmlElements(body,"title").firstOrNull()?.second?.let(::xmlText)?.take(1000)?.takeIf { it.isNotBlank() }?:return@forEach
        val programme=LiveTvProgramme(titlePool.getOrPut(title){title},start,stop)
        keys.forEach { key ->
            val list=schedule.getOrPut(key){mutableListOf()}
            if(list.none { it.startEpochMs==start }) {
                list.add(programme); list.sortBy { it.startEpochMs }
                while(list.count { it.stopEpochMs<=now }>window.maxPast) list.removeAt(0)
                while(list.count { it.stopEpochMs>now }>window.maxAhead) { list.removeAt(list.lastIndex); truncated+=key }
            }
        }
    }
    val next=truncated.mapNotNull { schedule[it]?.lastOrNull()?.stopEpochMs }.minOrNull()?.coerceIn(now+3_600_000L,now+10*3_600_000L)?:now+10*3_600_000L
    return LiveTvGuide(schedule,logos,next)
}
