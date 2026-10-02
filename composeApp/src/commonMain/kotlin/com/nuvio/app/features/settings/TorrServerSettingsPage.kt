package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.torrent.TorrServerSettingsRepository
import com.nuvio.app.features.torrent.TorrServerException

/** Append to Playback settings without coupling remote resolution to native P2P controls. */
internal fun LazyListScope.torrServerSettingsContent(isTablet: Boolean) {
    item {
        val saved = remember { TorrServerSettingsRepository.snapshot() }
        var address by remember(saved.baseUrl) { mutableStateOf(saved.baseUrl) }
        var enabled by remember(saved.enabled) { mutableStateOf(saved.enabled) }
        var saveToDatabase by remember(saved.saveToDatabase) { mutableStateOf(saved.saveToDatabase) }
        var validation by remember { mutableStateOf<String?>(null) }
        val resolutionError by TorrServerSettingsRepository.error.collectAsState()
        SettingsSection(title = "TorrServer", isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(title = "Use remote TorrServer", description = "Only explicit torrserver:// links are resolved; local P2P and HTTP streams are unchanged.",
                    checked = enabled, isTablet = isTablet, onCheckedChange = { enabled = it })
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Server URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("Example: http://192.168.1.10:8090. File indexes are one-based.", style = MaterialTheme.typography.bodySmall)
                    SettingsSwitchRow(title = "Save torrents on the server", checked = saveToDatabase,
                        isTablet = isTablet, onCheckedChange = { saveToDatabase = it })
                    Button(onClick = {
                        try {
                            TorrServerSettingsRepository.save(saved.copy(enabled = enabled, baseUrl = address, saveToDatabase = saveToDatabase))
                            validation = null
                        } catch (failure: TorrServerException) { validation = failure.message }
                    }) { Text("Save") }
                    (validation ?: resolutionError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
