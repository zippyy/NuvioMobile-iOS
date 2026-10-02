package com.nuvio.app.features.livetv

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** UI state publication happens on the caller's dispatcher (Main in the screen). */
data class LiveTvUiState(
    val profileId: Int=0, val sources: List<LiveTvSource> = emptyList(), val channels: List<LiveTvChannel> = emptyList(),
    val preferences: LiveTvPreferences=LiveTvPreferences(), val loadingSources: Set<String> = emptySet(),
    val sourceErrors: Map<String,String> = emptyMap(), val guide: LiveTvGuide=LiveTvGuide(),
    val current: Map<String,LiveTvProgramme> = emptyMap(), val epgLoading: Boolean=false, val epgError: String?=null,
) {
    val shownChannels get()=channels.filter { it.hideKey !in preferences.hiddenChannels && it.group !in preferences.hiddenGroups }
    val groups get()=channels.map { it.group }.distinct().sortedWith(compareBy<String> { preferences.groupOrder.indexOf(it).takeIf { index->index>=0 }?:Int.MAX_VALUE }.thenBy { preferences.groupNames[it]?:it })
    val recent get()=shownChannels.firstOrNull { it.hideKey==preferences.recentKey }
}
class LiveTvRepository(private val http: LiveTvTransport, private val store: LiveTvStore, private val now: ()->Long) {
    private val providers=LiveTvProviders(http)
    private val mutable=MutableStateFlow(LiveTvUiState())
    val state: StateFlow<LiveTvUiState> = mutable.asStateFlow()
    private var generation=0
    private val channelsBySource=mutableMapOf<String,List<LiveTvChannel>>()
    private val guideUrls=mutableMapOf<String,List<String>>()
    private var refreshJob: Job?=null
    fun switchProfile(profileId: Int) {
        if(state.value.profileId==profileId) return
        generation++; refreshJob?.cancel(); channelsBySource.clear(); guideUrls.clear()
        val (sources,preferences)=decodeLiveTvProfile(store.read("profile-$profileId"))
        mutable.value=LiveTvUiState(profileId=profileId,sources=sources,preferences=preferences)
    }
    private fun save(sources: List<LiveTvSource> = state.value.sources,p: LiveTvPreferences=state.value.preferences) {
        // Persist first; a failed Keychain write must not claim a successful edit.
        store.write("profile-${state.value.profileId}",encodeLiveTvProfile(sources,p))
        mutable.update { it.copy(sources=sources,preferences=p) }
    }
    fun addSource(source: LiveTvSource) {
        require(source.id.isNotBlank())
        if(source.epgUrl.isNotBlank()) requireLiveTvUrl(source.epgUrl)
        when(source.type) {
            LiveTvSourceType.M3u -> if(source.playlist.isBlank()) requireLiveTvUrl(source.url) else require(source.playlist.length<=2*1024*1024 && source.playlist.trimStart().startsWith("#EXTM3U")) { "Import a valid M3U playlist (up to 2 MB)" }
            LiveTvSourceType.Xtream -> { requireLiveTvUrl(source.xtream.serverUrl); require(source.xtream.isConfigured) }
            LiveTvSourceType.Stalker -> { requireLiveTvUrl(source.stalker.portalUrl); require(source.stalker.macAddress.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) }
        }
        val sources=state.value.sources
        require(sources.none { it.identity==source.identity && it.id!=source.id }) { "This source is already saved" }
        save(if(sources.any { it.id==source.id }) sources.map { if(it.id==source.id) source else it } else sources+source)
    }
    fun removeSource(id: String) {
        save(state.value.sources.filterNot { it.id==id }); generation++; refreshJob?.cancel()
        channelsBySource.remove(id); guideUrls.remove(id)
        mutable.update { it.copy(channels=it.sources.flatMap { source->channelsBySource[source.id].orEmpty() },loadingSources=emptySet(),sourceErrors=it.sourceErrors-id) }
    }
    fun toggleFavorite(channel: LiveTvChannel) {
        val p=state.value.preferences; save(p=p.copy(favorites=if(channel.hideKey in p.favorites)p.favorites-channel.hideKey else p.favorites+channel.hideKey))
    }
    fun hideChannel(channel: LiveTvChannel,hidden: Boolean) {
        val p=state.value.preferences; save(p=p.copy(hiddenChannels=if(hidden)p.hiddenChannels+channel.hideKey else p.hiddenChannels-channel.hideKey))
    }
    fun hideGroup(group: String,hidden: Boolean) {
        val p=state.value.preferences; save(p=p.copy(hiddenGroups=if(hidden)p.hiddenGroups+group else p.hiddenGroups-group))
    }
    fun renameGroup(group: String,name: String) { val p=state.value.preferences; save(p=p.copy(groupNames=p.groupNames+(group to name.trim().take(100)))) }
    fun orderGroups(groups: List<String>) { save(p=state.value.preferences.copy(groupOrder=groups.distinct())) }
    fun showAll() { save(p=state.value.preferences.copy(hiddenGroups=emptySet(),hiddenChannels=emptySet())) }
    fun recordPlayback(channel: LiveTvChannel) { save(p=state.value.preferences.copy(recentKey=channel.hideKey)) }
    fun neighbour(channel: LiveTvChannel,direction: Int): LiveTvChannel? {
        val list=state.value.shownChannels; if(list.isEmpty())return null
        val index=list.indexOfFirst { it.hideKey==channel.hideKey }.coerceAtLeast(0)
        return list[((index+direction)%list.size+list.size)%list.size]
    }
    suspend fun resolve(channel: LiveTvChannel): LiveTvChannel {
        val source=state.value.sources.firstOrNull { it.id==channel.sourceId }?:error("Source was removed")
        val resolved=providers.resolve(source,channel)
        recordPlayback(channel); LiveTvPlaybackRegistry.register(resolved.streamUrl,channel.streamUrl)
        return resolved
    }
    suspend fun refresh(forceGuide: Boolean=false) = coroutineScope {
        refreshJob?.cancel(); refreshJob=currentCoroutineContext()[Job]
        val gen=++generation; val profile=state.value.profileId; val sources=state.value.sources
        mutable.update { it.copy(loadingSources=sources.map { s->s.id }.toSet()) }
        val permits=Semaphore(2)
        try {
            sources.map { source->async {
                permits.withPermit {
                    try {
                        val loaded=providers.load(source)
                        if(gen==generation) {
                            channelsBySource[source.id]=loaded.channels; guideUrls[source.id]=loaded.epgUrls
                            mutable.update { it.copy(channels=sources.flatMap { s->channelsBySource[s.id].orEmpty() },sourceErrors=it.sourceErrors-source.id) }
                        }
                    } catch(e: CancellationException) { throw e } catch(e: Exception) {
                        if(gen==generation) mutable.update { it.copy(sourceErrors=it.sourceErrors+(source.id to redactedLiveTvError(e))) }
                    } finally { if(gen==generation) mutable.update { it.copy(loadingSources=it.loadingSources-source.id) } }
                }
            } }.awaitAll()
            if(gen==generation) loadGuide(gen,profile,forceGuide)
        } finally { if(gen==generation) mutable.update { it.copy(loadingSources=emptySet(),epgLoading=false) } }
    }
    private suspend fun loadGuide(gen: Int,profile: Int,force: Boolean) {
        val channels=state.value.channels
        val urls=state.value.sources.flatMap { s->guideUrls[s.id].orEmpty().map { s to it } }.distinctBy { it.second }
        if(urls.isEmpty()) { tick(); return }
        val key=urls.joinToString("|"){it.second}+"|"+channels.map { it.guideKey+":"+liveTvNameKey(it.name) }.sorted().joinToString("|")
        val cacheKey=key.fold(-0x340d631b7bdddcdbL) { hash,c -> (hash xor c.code.toLong())*0x100000001b3L }.toString()
        val cached=if(force)null else decodeLiveTvGuide(store.read("guide-$profile"),cacheKey,now())
        if(cached!=null) { mutable.update { it.copy(guide=cached,epgError=null) }; tick(); return }
        mutable.update { it.copy(epgLoading=true,epgError=null) }
        var guide=LiveTvGuide(nextReadAt=now()+10*3_600_000L); var failed=false
        for((source,url) in urls) {
            try {
                val sourceUrl=when(source.type) { LiveTvSourceType.M3u->source.url; LiveTvSourceType.Xtream->source.xtream.serverUrl; LiveTvSourceType.Stalker->source.stalker.portalUrl }
                val headers=safeLiveTvHeaders(LIVE_TV_PLAYLIST_HEADERS,if(liveTvOrigin(sourceUrl)==liveTvOrigin(url))source.headers else emptyMap())
                val read=withContext(Dispatchers.Default) { readXmlTvGuide(http.text(url,headers),channels,now()) }
                if(gen!=generation) return
                guide=LiveTvGuide((guide.schedule.keys+read.schedule.keys).associateWith { k->(guide.schedule[k].orEmpty()+read.schedule[k].orEmpty()).distinctBy { it.startEpochMs }.sortedBy { it.startEpochMs } },guide.logos+read.logos,minOf(guide.nextReadAt,read.nextReadAt))
                mutable.update { it.copy(guide=guide) }; tick()
            } catch(e: CancellationException) { throw e } catch(_: Exception) { failed=true }
        }
        if(gen!=generation) return
        if(failed) {
            mutable.update { it.copy(epgError="Some programme guides could not be loaded. Refresh to retry.") }
            if(guide.schedule.isNotEmpty()) store.write("guide-$profile",encodeLiveTvGuide(cacheKey,guide.copy(nextReadAt=now()+30*60_000L)))
        } else store.write("guide-$profile",encodeLiveTvGuide(cacheKey,guide))
    }
    fun tick() { mutable.update { it.copy(current=currentProgrammes(it.guide.schedule,it.channels.map { c->c.guideKey },now())) } }
    suspend fun whileVisible() {
        while(currentCoroutineContext().isActive) {
            tick(); if(state.value.guide.nextReadAt>0 && now()>=state.value.guide.nextReadAt) refresh()
            delay(60_000)
        }
    }
    fun stop() { generation++; refreshJob?.cancel(); refreshJob=null; mutable.update { it.copy(loadingSources=emptySet(),epgLoading=false) } }
}

object LiveTvPlaybackRegistry {
    private val urls=linkedMapOf<String,String>()
    fun register(playbackUrl: String,listUrl: String) { urls.remove(playbackUrl); urls[playbackUrl]=listUrl; while(urls.size>16) urls.remove(urls.keys.first()) }
    fun isLiveTv(url: String)=url in urls
    fun listUrlFor(url: String)=urls[url]
}
