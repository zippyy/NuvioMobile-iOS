package com.nuvio.app.core.sync

import kotlinx.serialization.json.*

/** Wire-compatible Reshaped v1 per-key last-write-wins document. */
internal data class SyncEntry(val value: JsonElement?, val time: Long)
internal typealias SyncSections = Map<String, Map<String, SyncEntry>>

internal object SyncDoc {
    const val VERSION = 1
    const val TOMBSTONE_MS = 60L * 24 * 60 * 60 * 1000
    class NewerFormatException : Exception("Sync file was written by a newer app version")
    fun encode(sections: SyncSections): String = buildJsonObject {
        put("v", VERSION)
        put("s", buildJsonObject {
            sections.toSortedMap().forEach { (section, entries) ->
                if (entries.isNotEmpty()) put(section, buildJsonObject {
                    entries.toSortedMap().forEach { (key, entry) ->
                        put(key, buildJsonObject { entry.value?.let { put("v", it) }; put("t", entry.time) })
                    }
                })
            }
        })
    }.toString()
    fun decode(text: String?): SyncSections {
        if (text.isNullOrBlank()) return emptyMap()
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return emptyMap()
        val version = (root["v"] as? JsonPrimitive)?.longOrNull ?: return emptyMap()
        if (version > VERSION) throw NewerFormatException()
        return (root["s"] as? JsonObject)?.mapNotNull { (name, raw) ->
            val entries = (raw as? JsonObject)?.mapNotNull { (key, rawEntry) ->
                val entry = rawEntry as? JsonObject ?: return@mapNotNull null
                val time = (entry["t"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
                key to SyncEntry(entry["v"], time)
            }?.toMap() ?: return@mapNotNull null
            name to entries
        }?.toMap().orEmpty()
    }
    fun stamp(base: SyncSections, current: Map<String, Map<String, JsonElement>>, now: Long): SyncSections {
        val result = base.toMutableMap()
        current.forEach { (name, values) ->
            val before = base[name].orEmpty()
            val entries = before.toMutableMap()
            values.forEach { (key, value) ->
                val old = before[key]
                entries[key] = when {
                    old == null -> SyncEntry(value, if (name in base) now else 0)
                    old.value == value -> old
                    else -> SyncEntry(value, now)
                }
            }
            before.forEach { (key, old) -> if (key !in values && old.value != null) entries[key] = SyncEntry(null, now) }
            if (entries.isEmpty()) result.remove(name) else result[name] = entries
        }
        return result
    }
    fun merge(a: SyncSections, b: SyncSections): SyncSections = (a.keys + b.keys).associateWith { name ->
        val left = a[name].orEmpty(); val right = b[name].orEmpty()
        (left.keys + right.keys).associateWith { key ->
            val l = left[key]; val r = right[key]
            when { l == null -> r!!; r == null -> l; l.time > r.time -> l; else -> r }
        }
    }
    fun prune(sections: SyncSections, now: Long): SyncSections = sections.mapValues { (_, entries) ->
        entries.filterValues { it.value != null || now - it.time < TOMBSTONE_MS }
    }.filterValues { it.isNotEmpty() }
    fun values(sections: SyncSections, section: String): Map<String, JsonElement> =
        sections[section].orEmpty().mapNotNull { (key, entry) -> entry.value?.let { key to it } }.toMap()
    fun latestTime(sections: SyncSections): Long = sections.values.flatMap { it.values }.maxOfOrNull { it.time } ?: 0
    fun stampTime(clock: Long, vararg seen: SyncSections): Long = maxOf(clock, (seen.maxOfOrNull(::latestTime) ?: 0) + 1)
}
