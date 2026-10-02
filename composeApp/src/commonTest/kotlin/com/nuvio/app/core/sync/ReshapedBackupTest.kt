package com.nuvio.app.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.*

class ReshapedBackupTest {
    @Test fun selectiveRestoreKeepsUnselectedFeaturesAndRejectsUnknownSelections() {
        val old = buildJsonObject { put("theme_settings", buildJsonObject { put("theme", "old") }); put("player_settings", buildJsonObject { put("volume", 10) }) }
        val imported = buildJsonObject { put("theme_settings", buildJsonObject { put("theme", "new") }) }
        val decoded = SettingsBackup.decode(SettingsBackup.encode(2, imported))
        val restored = SettingsBackup.restore(old, decoded, setOf("theme_settings"))
        assertEquals(imported["theme_settings"], restored["theme_settings"])
        assertEquals(old["player_settings"], restored["player_settings"])
        assertFailsWith<IllegalArgumentException> { SettingsBackup.restore(old, decoded, setOf("auth_session")) }
    }
    @Test fun importRejectsMalformedAndFutureDocuments() {
        assertFailsWith<IllegalArgumentException> { SettingsBackup.decode("{}") }
        assertFailsWith<IllegalArgumentException> { SettingsBackup.decode("{\"format\":\"nuvio-settings\",\"version\":99,\"features\":{}}") }
    }
}
