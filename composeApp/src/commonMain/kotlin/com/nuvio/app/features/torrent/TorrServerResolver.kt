package com.nuvio.app.features.torrent

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

/** Remote TorrServer is independent of the bundled P2P engine and never starts it. */
data class TorrServerSettings(val enabled: Boolean = false, val baseUrl: String = "", val saveToDatabase: Boolean = false)
data class TorrServerResponse(val status: Int, val body: String)
class TorrServerException(message: String) : Exception(message)

class TorrServerResolver(private val post: suspend (url: String, body: String) -> TorrServerResponse) {
    /** null means not our scheme; callers retain their existing HTTP, debrid and P2P routing. */
    suspend fun resolve(url: String, settings: TorrServerSettings): String? {
        if (!recognizes(url)) return null
        if (!settings.enabled) throw TorrServerException("Enable and configure TorrServer before playing this link")
        val base = normalizeServerUrl(settings.baseUrl)
        val raw = url.removePrefix("torrserver://")
        val hash = raw.substringBefore('?')
        if (!Regex("[a-fA-F0-9]{40}").matches(hash)) throw TorrServerException("Invalid TorrServer torrent hash")
        val parameters = raw.substringAfter('?', "").split('&').filter { it.isNotBlank() }
        if (parameters.any { !it.startsWith("index=") } || parameters.size > 1) throw TorrServerException("Invalid TorrServer parameters")
        val index = if (parameters.isEmpty()) 1 else parameters.single().removePrefix("index=").toIntOrNull()
            ?: throw TorrServerException("Invalid TorrServer file index")
        if (index < 1) throw TorrServerException("TorrServer file index must be positive (one-based)")
        val body = buildJsonObject {
            put("action", "add")
            put("link", "magnet:?xt=urn:btih:$hash")
            put("save_to_db", settings.saveToDatabase)
        }.toString()
        val response = try { post("$base/torrents", body) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { throw TorrServerException("Unable to connect to TorrServer") }
        if (response.status !in 200..299) throw TorrServerException(when (response.status) {
            401, 403 -> "TorrServer requires authentication"
            else -> "TorrServer request failed (${response.status})"
        })
        val returnedHash = try { Json.parseToJsonElement(response.body).jsonObject["hash"]?.jsonPrimitive?.contentOrNull }
        catch (_: Exception) { null }
        if (returnedHash == null || !returnedHash.equals(hash, ignoreCase = true)) throw TorrServerException("TorrServer returned an invalid torrent response")
        return "$base/stream?link=${hash.lowercase()}&index=$index&play"
    }

    companion object {
        fun recognizes(url: String): Boolean = url.startsWith("torrserver://")
        fun normalizeServerUrl(value: String): String {
            val normalized = value.trim().trimEnd('/')
            // Permit LAN HTTP and reverse-proxy prefixes; reject credentials, queries and fragments.
            if (!Regex("https?://(?:\\[[a-fA-F0-9:]+\\]|[a-zA-Z0-9.-]+)(?::[0-9]{1,5})?(?:/[a-zA-Z0-9_./~-]*)?").matches(normalized)) {
                throw TorrServerException("Enter a valid HTTP or HTTPS TorrServer address")
            }
            return normalized
        }
    }
}
