package com.nuvio.app.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonPrimitive

class ReshapedSyncDocTest {
    @Test fun slowClockWinsAfterObservedEditAndOtherProfilesRemain() {
        val base = mapOf("settings/shared" to mapOf("x" to SyncEntry(JsonPrimitive(1), 20)), "live_tv/2/favorites" to mapOf("a" to SyncEntry(JsonPrimitive(true), 20)))
        val time = SyncDoc.stampTime(1, base)
        assertEquals(21L, time)
        val stamped = SyncDoc.stamp(base, mapOf("settings/shared" to mapOf("x" to JsonPrimitive(2))), time)
        assertEquals(base["live_tv/2/favorites"], stamped["live_tv/2/favorites"])
        assertEquals(JsonPrimitive(2), SyncDoc.values(SyncDoc.merge(base, stamped), "settings/shared")["x"])
    }
    @Test fun expiredTombstonesArePrunedAndFutureVersionsAreRefused() {
        val doc = mapOf("s" to mapOf("a" to SyncEntry(null, 0), "b" to SyncEntry(JsonPrimitive(true), 0)))
        assertEquals(setOf("b"), SyncDoc.prune(doc, SyncDoc.TOMBSTONE_MS).getValue("s").keys)
        assertFailsWith<SyncDoc.NewerFormatException> { SyncDoc.decode("{\"v\":99,\"s\":{}}") }
        assertEquals(emptyMap<String, Map<String, SyncEntry>>(), SyncDoc.decode("bad json"))
    }
    @Test fun newDeviceDefaultsLoseToRemoteAndDeletionsSurvive() {
        val section = "settings/shared"
        val remote = mapOf(section to mapOf("live_tv" to SyncEntry(JsonPrimitive(false), 20)))
        val local = SyncDoc.stamp(emptyMap(), mapOf(section to mapOf("live_tv" to JsonPrimitive(true))), 100)
        assertEquals(remote, SyncDoc.merge(local, remote))
        val deleted = SyncDoc.stamp(remote, mapOf(section to emptyMap()), 101)
        assertEquals(null, SyncDoc.merge(remote, deleted)[section]?.get("live_tv")?.value)
        assertEquals(deleted, SyncDoc.decode(SyncDoc.encode(deleted)))
    }
}
