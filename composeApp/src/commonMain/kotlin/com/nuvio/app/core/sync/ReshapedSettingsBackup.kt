package com.nuvio.app.core.sync

import kotlinx.serialization.json.*

/** Portable, credential-free profile settings backup. Not an account/session backup. */
internal object SettingsBackup {
    const val MAX_CHARS = 2_000_000
    val allowedFeatures = setOf("theme_settings", "poster_card_style_settings_payload", "card_depth_style_settings_payload",
        "player_settings", "stream_badge_settings", "debrid_settings", "tmdb_settings", "mdblist_settings",
        "meta_screen_settings_payload", "collection_mobile_settings_payload", "continue_watching_settings_payload",
        "trakt_settings_payload", "trakt_comments_settings", "notifications_settings")
    fun encode(profileId: Int, features: JsonObject): String = buildJsonObject {
        put("format", "nuvio-settings"); put("version", 1); put("profile", profileId)
        put("features", JsonObject(features.filterKeys { it in allowedFeatures }))
    }.toString()
    fun decode(text: String): JsonObject {
        require(text.length <= MAX_CHARS) { "Backup exceeds size limit" }
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: throw IllegalArgumentException("Invalid backup JSON")
        require((root["format"] as? JsonPrimitive)?.content == "nuvio-settings") { "Not a Nuvio settings backup" }
        require((root["version"] as? JsonPrimitive)?.intOrNull == 1) { "Unsupported backup version" }
        val features = root["features"] as? JsonObject ?: throw IllegalArgumentException("Missing backup features")
        require(features.isNotEmpty() && features.keys.all { it in allowedFeatures }) { "Unsupported backup features" }
        features.forEach { (key, value) ->
            if (key.endsWith("_payload")) {
                require(value is JsonPrimitive && value.isString) { "Invalid settings payload" }
                if (value.content.isNotBlank()) require(runCatching { Json.parseToJsonElement(value.content) is JsonObject }.getOrDefault(false)) { "Invalid settings payload" }
            } else require(value is JsonObject) { "Invalid settings object" }
        }
        return features
    }
    fun restore(current: JsonObject, imported: JsonObject, selected: Set<String>): JsonObject {
        require(selected.isNotEmpty() && selected.all { it in allowedFeatures && it in imported }) { "Select available settings sections" }
        return JsonObject(current + imported.filterKeys { it in selected })
    }
}
