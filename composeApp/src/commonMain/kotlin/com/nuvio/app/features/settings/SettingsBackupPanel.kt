package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.sync.ProfileSettingsSync
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Clipboard JSON works on both platforms without a LAN server or extra permissions. */
@Composable
internal fun SettingsBackupPanel(isTablet: Boolean) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf(emptySet<String>()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var profileId by remember { mutableStateOf(0) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    SettingsSection(title = "Settings backup and restore", isTablet = isTablet) {
        SettingsGroup(isTablet = isTablet) {
            Text("Export this profile's settings as JSON. API keys, PINs, sessions, library and watch history are not included.", modifier = Modifier.padding(16.dp))
            SettingsNavigationRow(title = "Copy settings backup", description = "Save the copied JSON in a private file", isTablet = isTablet, onClick = {
                runCatching { ProfileSettingsSync.exportBackup() }.onSuccess {
                    clipboard.setText(AnnotatedString(it)); message = "Settings backup copied"
                }.onFailure { message = "Could not export settings" }
            })
            SettingsNavigationRow(title = "Restore copied backup", description = "Preview sections and confirm before replacing settings", isTablet = isTablet, onClick = {
                val text = clipboard.getText()?.text.orEmpty()
                runCatching { ProfileSettingsSync.inspectBackup(text) }.onSuccess {
                    available = it; selected = emptySet(); profileId = ProfileRepository.activeProfileId
                    accountId = (com.nuvio.app.core.auth.AuthRepository.state.value as? com.nuvio.app.core.auth.AuthState.Authenticated)?.userId
                    pending = text
                }.onFailure { message = "Invalid or unsupported settings backup" }
            })
            if (message.isNotBlank()) Text(message, modifier = Modifier.padding(16.dp))
        }
    }
    if (pending != null) AlertDialog(
        onDismissRequest = { if (!busy) pending = null },
        title = { Text("Restore settings to profile $profileId?") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text("Only selected sections will be replaced. Existing API keys are kept. This cannot be undone; export first.")
                available.sorted().forEach { key ->
                    Row {
                        Checkbox(checked = key in selected, enabled = !busy, onCheckedChange = { checked -> selected = if (checked) selected + key else selected - key })
                        Text(key.removeSuffix("_payload").replace('_', ' '), modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = !busy && selected.isNotEmpty(), onClick = {
                val text = pending ?: return@TextButton
                busy = true
                scope.launch {
                    try {
                        ProfileSettingsSync.restoreBackup(text, selected, profileId, accountId)
                        message = "Selected settings restored"
                        pending = null
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) { message = "Restore failed. Check the backup and active profile." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Restoring…" else "Restore selected") }
        },
    )
}
