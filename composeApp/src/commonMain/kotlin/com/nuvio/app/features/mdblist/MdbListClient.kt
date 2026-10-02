package com.nuvio.app.features.mdblist

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/** Transport failures never include request URLs (which contain credentials). */
class MdbListApiException(val status: Int) : Exception(when (status) {
    401, 403 -> "MDBList API key is invalid or unauthorized"
    429 -> "MDBList rate limit reached; try again later"
    else -> "MDBList request failed ($status)"
})

data class MdbListResponse(val status: Int, val body: String, val headers: Map<String, String> = emptyMap())
data class MdbListItem(val imdbId: String?, val tmdbId: Long?, val type: String, val title: String, val poster: String?)

/** Cache the raw provider value, not UI toggles. Serialized access coalesces duplicate requests. */
class MdbListClient(
    private val clock: () -> Long,
    private val transport: suspend (method: String, url: String, body: String) -> MdbListResponse,
) {
    private data class Entry(val value: Double?, val fetchedAt: Long)
    private val cache = mutableMapOf<String, Entry>()
    private val mutex = Mutex()
    private val generation = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private var cachedGeneration = 0L
    fun clearCache() { generation.value = generation.value + 1 }


    /** Public/personal API-key lists; OAuth tracking is a separate provider capability. */
    suspend fun listItems(listId: Long, apiKey: String): List<MdbListItem> {
        require(listId > 0 && apiKey.isNotBlank())
        val items = linkedMapOf<String, MdbListItem>()
        val visited = mutableSetOf<String>()
        var next = ""
        repeat(1000) {
            check(visited.add(next)) { "MDBList pagination repeated a page" }
            val response = transport("GET", "https://api.mdblist.com/lists/$listId/items?apikey=${encodeMdbComponent(apiKey)}&unified=true&limit=1000$next", "")
            if (response.status !in 200..299) throw MdbListApiException(response.status)
            val root = Json.parseToJsonElement(response.body)
            val rows = when (root) {
                is JsonArray -> root
                is JsonObject -> root["items"] as? JsonArray ?: error("Invalid MDBList items response")
                else -> error("Invalid MDBList items response")
            }
            for (element in rows) {
                val row = element.jsonObject
                val type = row["mediatype"]?.jsonPrimitive?.contentOrNull ?: row["type"]?.jsonPrimitive?.contentOrNull
                if (type !in listOf("movie", "show", "series")) continue
                val imdb = row["imdb_id"]?.jsonPrimitive?.contentOrNull
                    ?: (row["ids"] as? JsonObject)?.get("imdb")?.jsonPrimitive?.contentOrNull
                val tmdb = row["id"]?.jsonPrimitive?.longOrNull
                    ?: (row["ids"] as? JsonObject)?.get("tmdb")?.jsonPrimitive?.longOrNull
                if (imdb == null && tmdb == null) continue
                val item = MdbListItem(imdb, tmdb, if (type == "movie") "movie" else "series",
                    row["title"]?.jsonPrimitive?.contentOrNull ?: row["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    row["poster"]?.jsonPrimitive?.contentOrNull)
                items["${item.type}:${imdb ?: tmdb}"] = item
            }
            val objectRoot = root as? JsonObject
            fun header(name: String) = response.headers.entries.firstOrNull { it.key.equals(name, true) }?.value
            val cursor = objectRoot?.get("next_cursor")?.jsonPrimitive?.contentOrNull ?: header("X-Next-Cursor")
            val offset = objectRoot?.get("next_offset")?.jsonPrimitive?.intOrNull
            next = when {
                !cursor.isNullOrBlank() -> "&cursor=${encodeMdbComponent(cursor)}"
                offset != null -> "&offset=$offset"
                header("X-Has-More").equals("true", true) -> error("MDBList pagination is missing a continuation")
                else -> return items.values.toList()
            }
        }
        error("MDBList pagination exceeded its safety limit")
    }

    suspend fun ratings(ids: List<String>, mediaType: String, provider: String, apiKey: String): Map<String, Double> = mutex.withLock {
        require(mediaType in listOf("movie", "show"))
        require(provider in listOf("imdb", "tmdb", "tomatoes", "metacritic", "trakt", "letterboxd", "audience", "mal"))
        require(apiKey.isNotBlank())
        val requested = ids.distinct().filter { Regex("tt\\d+").matches(it) }
        val scope = generation.value
        if (cachedGeneration != scope) { cache.clear(); cachedGeneration = scope }
        fun key(id: String) = "$apiKey|$mediaType|$provider|$id"
        val missing = requested.filter { id -> cache[key(id)]?.let { clock() - it.fetchedAt in 0 until 1_800_000L } != true }
        for (batch in missing.chunked(200)) {
            val body = buildJsonObject {
                put("ids", JsonArray(batch.map(::JsonPrimitive)))
                put("provider", "imdb")
            }.toString()
            val response = transport("POST", "https://api.mdblist.com/rating/$mediaType/$provider?apikey=${encodeMdbComponent(apiKey)}", body)
            if (response.status !in 200..299) throw MdbListApiException(response.status)
            val root = Json.parseToJsonElement(response.body).jsonObject
            require(root["error"] == null) { "MDBList returned an API error" }
            val values = (root["ratings"] as? JsonArray) ?: error("Invalid MDBList ratings response")
            val decoded = mutableMapOf<String, Double>()
            values.forEach { element ->
                val item = element.jsonObject
                val id = item["id"]?.jsonPrimitive?.contentOrNull
                    ?: item["imdbid"]?.jsonPrimitive?.contentOrNull
                    ?: batch.singleOrNull()
                val value = item["rating"]?.jsonPrimitive?.doubleOrNull
                val maximum = when (provider) { "imdb", "mal" -> 10.0; "letterboxd" -> 5.0; else -> 100.0 }
                if (id in batch && value != null && value.isFinite() && value in 0.0..maximum) decoded[id!!] = value
            }
            if (scope != generation.value) throw kotlinx.coroutines.CancellationException("MDBList settings changed")
            if (scope == generation.value) {
                if (cache.size > 4096) cache.clear()
                batch.forEach { cache[key(it)] = Entry(decoded[it], clock()) }
            }
        }
        requested.mapNotNull { id -> cache[key(id)]?.value?.let { id to it } }.toMap()
    }
}

internal fun encodeMdbComponent(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val n = byte.toInt() and 255
        if (n in 65..90 || n in 97..122 || n in 48..57 || n in listOf(45, 46, 95, 126)) append(n.toChar())
        else { append('%'); append("0123456789ABCDEF"[n shr 4]); append("0123456789ABCDEF"[n and 15]) }
    }
}
