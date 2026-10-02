package com.nuvio.app.features.artwork

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.details.MetaDetails

@Composable
fun ArtworkTemplateSettings() {
    val revision = rememberArtworkRevision()
    val settings = remember(revision) { ArtworkRepository.store.settings() }
    var template by remember(revision) { mutableStateOf(settings.posterTemplate) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Custom poster service", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(template, { template = it }, label = { Text("HTTP(S) template URL") }, modifier = Modifier.fillMaxWidth())
        Text("Placeholders: {imdb_id}, {tmdb_id}, {tvdb_id}, {type}, {shape}. Leave empty to use original posters.")
        Button(onClick = {
            error = runCatching { ArtworkRepository.store.saveSettings(settings.copy(posterTemplate = template.trim())) }.exceptionOrNull()?.message
        }) { Text("Save poster template") }
        ArtworkScreen.entries.forEach { screen ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(screen.name.lowercase().replace('_', ' '))
                Switch(screen in settings.screens, { enabled ->
                    ArtworkRepository.store.saveSettings(settings.copy(screens = if (enabled) settings.screens + screen else settings.screens - screen))
                })
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun DetailArtworkControls(meta: MetaDetails) {
    val revision = rememberArtworkRevision()
    var expanded by remember(meta.id) { mutableStateOf(false) }
    val value = remember(revision, meta.id) { ArtworkRepository.store.override(meta.type, meta.id) }
    var poster by remember(revision, meta.id) { mutableStateOf(value.poster.orEmpty()) }
    var background by remember(revision, meta.id) { mutableStateOf(value.background.orEmpty()) }
    var logo by remember(revision, meta.id) { mutableStateOf(value.logo.orEmpty()) }
    var error by remember(meta.id) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        TextButton(onClick = { expanded = !expanded }) { Text("Customize artwork") }
        if (expanded) {
            OutlinedTextField(poster, { poster = it }, label = { Text("Poster URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(background, { background = it }, label = { Text("Background URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(logo, { logo = it }, label = { Text("Logo URL") }, modifier = Modifier.fillMaxWidth())
            Row {
                TextButton(onClick = {
                    error = runCatching {
                        ArtworkRepository.store.saveOverride(meta.type, meta.id, ArtworkOverride(poster.trim().ifBlank { null }, background.trim().ifBlank { null }, logo.trim().ifBlank { null }))
                    }.exceptionOrNull()?.message
                }) { Text("Save artwork") }
                TextButton(onClick = { ArtworkRepository.store.saveOverride(meta.type, meta.id, ArtworkOverride()) }) { Text("Reset artwork") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
