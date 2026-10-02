package com.nuvio.app.features.livetv

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import kotlinx.coroutines.*

@Composable
fun LiveTvScreen(profileId: Int,onPlay: (LiveTvChannel)->Unit,onBack: ()->Unit) {
    val store=rememberLiveTvStore()
    val repository=remember(store) { LiveTvRepository(LiveTvTransport(::liveTvHttpText),store,::liveTvNow) }
    val state by repository.state.collectAsState()
    val scope=rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf("Channels") }
    var search by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var favoriteOnly by rememberSaveable { mutableStateOf(false) }
    var sourceFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var playJob by remember { mutableStateOf<Job?>(null) }
    val play: (LiveTvChannel)->Unit = { channel ->
        playJob?.cancel()
        playJob=scope.launch {
            resolving=true; playbackError=null
            try { onPlay(repository.resolve(channel)) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { playbackError=redactedLiveTvError(e) }
            finally { resolving=false }
        }
    }
    LaunchedEffect(profileId) { repository.switchProfile(profileId); repository.refresh() }
    LaunchedEffect(repository,profileId) { repository.whileVisible() }
    DisposableEffect(repository) { onDispose { repository.stop() } }
    val visible=state.shownChannels.filter { channel ->
        (search.isBlank() || channel.name.contains(search,true)) && (group==null || channel.group==group) &&
        (!favoriteOnly || channel.hideKey in state.preferences.favorites) && (sourceFilter==null || channel.sourceId==sourceFilter)
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=onBack) { Text("Back") }
            Text("Live TV",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            TextButton(onClick={ scope.launch { repository.refresh(forceGuide=true) } },enabled=state.loadingSources.isEmpty()) { Text("Refresh") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf("Channels","Guide","Sources","Categories").forEach { title ->
                FilterChip(selected=tab==title,onClick={tab=title},label={Text(title)},modifier=Modifier.padding(end=6.dp))
            }
        }
        if(state.loadingSources.isNotEmpty() || state.epgLoading || resolving) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(if(resolving) "Connecting…" else if(state.epgLoading) "Loading programme guide…" else "Loading channels…",style=MaterialTheme.typography.labelSmall)
        }
        (playbackError?:state.epgError)?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        when(tab) {
            "Sources" -> LiveTvSources(state,repository) { scope.launch { repository.refresh(forceGuide=true) } }
            "Categories" -> LiveTvCategories(state,repository)
            else -> {
                OutlinedTextField(search,{search=it},label={Text("Search channels")},singleLine=true,modifier=Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    FilterChip(selected=group==null && !favoriteOnly && sourceFilter==null,onClick={ group=null; favoriteOnly=false; sourceFilter=null },label={Text("All")})
                    FilterChip(selected=favoriteOnly,onClick={favoriteOnly=!favoriteOnly},label={Text("Favorites")},modifier=Modifier.padding(start=6.dp))
                    state.sources.forEach { source -> FilterChip(selected=sourceFilter==source.id,onClick={sourceFilter=if(sourceFilter==source.id)null else source.id},label={Text(source.displayLabel)},modifier=Modifier.padding(start=6.dp)) }
                    state.groups.filterNot { it in state.preferences.hiddenGroups }.forEach { key ->
                        FilterChip(selected=group==key,onClick={group=if(group==key)null else key},label={Text(state.preferences.groupNames[key]?:key.ifBlank { "Ungrouped" })},modifier=Modifier.padding(start=6.dp))
                    }
                }
                if(state.sources.isEmpty()) Text("Add an M3U playlist, direct stream, Xtream account or Stalker portal in Sources.",Modifier.padding(16.dp))
                else if(visible.isEmpty() && state.loadingSources.isEmpty()) Text("No visible channels. Check filters, Categories or source errors.",Modifier.padding(16.dp))
                if(tab=="Guide") LiveTvGuideGrid(visible,state.guide,play,Modifier.weight(1f))
                else LazyColumn(Modifier.weight(1f)) {
                    if(search.isBlank() && group==null && !favoriteOnly && sourceFilter==null) state.recent?.let { recent->item("recent") {
                        Text("Recently watched",style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(top=10.dp))
                        LiveTvChannelRow(recent,state,play,{repository.toggleFavorite(recent)})
                    } }
                    items(visible,key={it.id}) { channel -> LiveTvChannelRow(channel,state,play,{repository.toggleFavorite(channel)}) }
                    state.sourceErrors.forEach { (id,error)->item("error-$id") { Text("${state.sources.firstOrNull { it.id==id }?.displayLabel}: $error",color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(12.dp)) } }
                }
            }
        }
    }
}
private val LiveTvSource.displayLabel: String get() {
    val address=when(type) { LiveTvSourceType.M3u->url; LiveTvSourceType.Xtream->xtream.serverUrl; LiveTvSourceType.Stalker->stalker.portalUrl }
    return if(playlist.isNotBlank()) "Imported playlist" else address.substringAfter("://").substringBefore('/').substringBefore('?').substringAfterLast('@').ifBlank { type.name }
}
@Composable
private fun LiveTvChannelRow(channel: LiveTvChannel,state: LiveTvUiState,onPlay: (LiveTvChannel)->Unit,onFavorite: ()->Unit) {
    val programme=state.current[channel.guideKey]
    Row(Modifier.fillMaxWidth().clickable { onPlay(channel) }.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        AsyncImage(model=channel.logoUrl?:state.guide.logos[channel.guideKey],contentDescription=null,modifier=Modifier.size(44.dp))
        Column(Modifier.weight(1f).padding(horizontal=10.dp)) {
            Text(channel.name,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(programme?.let { "${liveTvTimeLabel(it.startEpochMs)}–${liveTvTimeLabel(it.stopEpochMs)}  ${it.title}" }?:"No programme guide",style=MaterialTheme.typography.bodySmall,maxLines=2)
            programme?.let { val progress=((liveTvNow()-it.startEpochMs).toFloat()/(it.stopEpochMs-it.startEpochMs)).coerceIn(0f,1f)
                LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().padding(top=4.dp)) }
        }
        TextButton(onClick=onFavorite) { Text(if(channel.hideKey in state.preferences.favorites) "★" else "☆") }
    }
    HorizontalDivider()
}

@Composable
private fun LiveTvSources(state: LiveTvUiState,repository: LiveTvRepository,onSaved: ()->Unit) {
    var editing by remember { mutableStateOf<LiveTvSource?>(null) }
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<LiveTvSource?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Button(onClick={adding=true; editing=null},modifier=Modifier.padding(vertical=12.dp)) { Text("Add source") } }
        items(state.sources,key={it.id}) { source ->
            Column(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
                Text(source.displayLabel,style=MaterialTheme.typography.titleMedium)
                Text("${source.type} · ${state.channels.count { it.sourceId==source.id }} channels")
                state.sourceErrors[source.id]?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                Row {
                    TextButton(onClick={editing=source; adding=true}) { Text("Edit") }
                    TextButton(onClick={removing=source}) { Text("Remove") }
                }
                HorizontalDivider()
            }
        }
    }
    if(adding) LiveTvSourceForm(editing,{adding=false}) { source -> repository.addSource(source); adding=false; onSaved() }
    removing?.let { source -> AlertDialog(onDismissRequest={removing=null},title={Text("Remove ${source.displayLabel}?")},text={Text("Its channels will be removed from this profile.")},
        confirmButton={TextButton(onClick={repository.removeSource(source.id); removing=null; onSaved()}) { Text("Remove") }},dismissButton={TextButton(onClick={removing=null}) { Text("Cancel") }}) }
}
@Composable
private fun LiveTvSourceForm(source: LiveTvSource?,onDismiss: ()->Unit,onSave: (LiveTvSource)->Unit) {
    var type by remember(source) { mutableStateOf(source?.type?:LiveTvSourceType.M3u) }
    var address by remember(source) { mutableStateOf(source?.let { when(it.type) { LiveTvSourceType.M3u->it.url; LiveTvSourceType.Xtream->it.xtream.serverUrl; LiveTvSourceType.Stalker->it.stalker.portalUrl } }.orEmpty()) }
    var username by remember(source) { mutableStateOf(source?.let { if(it.type==LiveTvSourceType.Stalker)it.stalker.username else it.xtream.username }.orEmpty()) }
    var password by remember(source) { mutableStateOf(source?.let { if(it.type==LiveTvSourceType.Stalker)it.stalker.password else it.xtream.password }.orEmpty()) }
    var mac by remember(source) { mutableStateOf(source?.stalker?.macAddress.orEmpty()) }
    var epg by remember(source) { mutableStateOf(source?.epgUrl.orEmpty()) }
    var playlist by remember(source) { mutableStateOf(source?.playlist.orEmpty()) }
    var headers by remember(source) { mutableStateOf(source?.headers?.entries?.joinToString("\n") { "${it.key}: ${it.value}" }.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(source==null) "Add Live TV source" else "Edit source")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState())) { LiveTvSourceType.entries.forEach { item -> FilterChip(selected=type==item,onClick={type=item},label={Text(item.name)},modifier=Modifier.padding(end=4.dp)) } }
            OutlinedTextField(address,{address=it},label={Text(if(type==LiveTvSourceType.M3u)"Playlist / stream URL" else "Server / portal URL")},singleLine=true)
            if(type==LiveTvSourceType.M3u) OutlinedTextField(playlist,{playlist=it},label={Text("Or paste M3U playlist (up to 2 MB)")},maxLines=4)
            if(type!=LiveTvSourceType.M3u) {
                OutlinedTextField(username,{username=it},label={Text("Username")},singleLine=true)
                OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation())
            }
            if(type==LiveTvSourceType.Stalker) OutlinedTextField(mac,{mac=it},label={Text("MAC address (00:11:22:33:44:55)")},singleLine=true)
            OutlinedTextField(epg,{epg=it},label={Text("XMLTV guide URL (optional)")},singleLine=true)
            OutlinedTextField(headers,{headers=it},label={Text("HTTP headers (one Name: Value per line)")},maxLines=4,visualTransformation=PasswordVisualTransformation())
            if(address.startsWith("http://",true)) Text("HTTP does not encrypt provider credentials. Use HTTPS if your provider supports it.",style=MaterialTheme.typography.bodySmall)
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }
    },confirmButton={TextButton(onClick={
        try {
            val parsedHeaders=headers.lineSequence().filter { it.isNotBlank() }.associate { line -> require(':' in line) { "Use Name: Value for each header" }; line.substringBefore(':').trim() to line.substringAfter(':').trim() }
            require(safeLiveTvHeaders(parsedHeaders).size==parsedHeaders.size) { "Invalid or unsafe HTTP header" }
            onSave(LiveTvSource(source?.id?:"source-${liveTvNow()}",type,address.trim(),parsedHeaders,epg.trim(),playlist,
                LiveTvStalkerSettings(address.trim(),mac.trim(),username.trim(),password),LiveTvXtreamSettings(address.trim(),username.trim(),password)))
        } catch(_: Exception) { error="Check the URL, required credentials, MAC address and header syntax. Sources must be unique." }
    }) { Text("Save") }},dismissButton={TextButton(onClick=onDismiss) { Text("Cancel") }})
}

@Composable
private fun LiveTvCategories(state: LiveTvUiState,repository: LiveTvRepository) {
    var rename by remember { mutableStateOf<String?>(null) }; var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick={repository.showAll()}) { Text("Show all") }
            TextButton(onClick={state.groups.forEach { repository.hideGroup(it,true) }}) { Text("Hide all") }
            TextButton(onClick={repository.orderGroups(state.groups.sorted())}) { Text("Sort A–Z") }
        } }
        items(state.groups,key={it}) { key ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=key !in state.preferences.hiddenGroups,onCheckedChange={repository.hideGroup(key,!it)})
                TextButton(onClick={selected=if(selected==key)null else key},modifier=Modifier.weight(1f)) { Text(state.preferences.groupNames[key]?:key.ifBlank { "Ungrouped" }) }
                TextButton(onClick={rename=key; name=state.preferences.groupNames[key]?:key}) { Text("Rename") }
                TextButton(onClick={val list=state.groups.toMutableList(); val index=list.indexOf(key); if(index>0) { list.removeAt(index); list.add(index-1,key); repository.orderGroups(list) }}) { Text("↑") }
                TextButton(onClick={val list=state.groups.toMutableList(); val index=list.indexOf(key); if(index<list.lastIndex) { list.removeAt(index); list.add(index+1,key); repository.orderGroups(list) }}) { Text("↓") }
            }
            if(selected==key) state.channels.filter { it.group==key }.forEach { channel ->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=channel.hideKey !in state.preferences.hiddenChannels,onCheckedChange={repository.hideChannel(channel,!it)})
                    Text(channel.name,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider()
        }
    }
    rename?.let { key -> AlertDialog(onDismissRequest={rename=null},title={Text("Rename category")},text={OutlinedTextField(name,{name=it},singleLine=true)},
        confirmButton={TextButton(onClick={repository.renameGroup(key,name); rename=null}) { Text("Save") }},dismissButton={TextButton(onClick={rename=null}) { Text("Cancel") }}) }
}

@Composable
private fun LiveTvGuideGrid(channels: List<LiveTvChannel>,guide: LiveTvGuide,onPlay: (LiveTvChannel)->Unit,modifier: Modifier) {
    val scroll=rememberScrollState()
    val now=liveTvNow(); val start=remember { now/1_800_000L*1_800_000L-1_800_000L }; val end=start+6*1_800_000L
    var selected by remember { mutableStateOf<Pair<LiveTvChannel,LiveTvProgramme>?>(null) }
    Column(modifier.horizontalScroll(scroll)) {
        Row { Spacer(Modifier.width(140.dp)); repeat(6) { Text(liveTvTimeLabel(start+it*1_800_000L),Modifier.width(150.dp).padding(4.dp),style=MaterialTheme.typography.labelMedium) } }
        LazyColumn(Modifier.width(1040.dp)) {
            items(channels,key={it.id}) { channel ->
                Row(Modifier.height(70.dp)) {
                    TextButton(onClick={onPlay(channel)},modifier=Modifier.width(140.dp).fillMaxHeight()) { Text(channel.name,maxLines=2,overflow=TextOverflow.Ellipsis) }
                    Box(Modifier.width(900.dp).fillMaxHeight()) {
                        val programmes=guide.schedule[channel.guideKey].orEmpty().filter { it.stopEpochMs>start && it.startEpochMs<end }
                        if(programmes.isEmpty()) Text("No guide",Modifier.padding(12.dp))
                        programmes.forEach { programme ->
                            val left=((maxOf(start,programme.startEpochMs)-start).toFloat()/1_800_000L*150).dp
                            val width=((minOf(end,programme.stopEpochMs)-maxOf(start,programme.startEpochMs)).toFloat()/1_800_000L*150).dp
                            val playing=programme.startEpochMs<=now && now<programme.stopEpochMs
                            Surface(Modifier.offset(x=left).width(width).fillMaxHeight().padding(2.dp).clickable { selected=channel to programme },
                                color=if(playing)MaterialTheme.colorScheme.primaryContainer else if(programme.stopEpochMs<=now)MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer) {
                                Text(programme.title,Modifier.padding(8.dp),maxLines=2,overflow=TextOverflow.Ellipsis)
                            }
                        }
                        if(now in start..end) Box(Modifier.offset(x=((now-start).toFloat()/1_800_000L*150).dp).width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.error))
                    }
                }
                HorizontalDivider()
            }
        }
    }
    selected?.let { (channel,programme)->AlertDialog(onDismissRequest={selected=null},title={Text(programme.title)},text={Text("${channel.name}\n${liveTvTimeLabel(programme.startEpochMs)}–${liveTvTimeLabel(programme.stopEpochMs)}")},
        confirmButton={TextButton(onClick={selected=null; onPlay(channel)}) { Text("Watch live") }},dismissButton={TextButton(onClick={selected=null}) { Text("Close") }}) }
}
