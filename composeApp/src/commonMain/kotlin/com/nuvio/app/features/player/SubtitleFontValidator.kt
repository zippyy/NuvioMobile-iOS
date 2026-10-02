package com.nuvio.app.features.player

internal const val MAX_SUBTITLE_FONT_BYTES = 20 * 1024 * 1024
internal data class SubtitleFontMetadata(val familyName: String, val extension: String)

/** Bounds-check sfnt directory AND name table; file names are never trusted. */
internal fun validateSubtitleFont(bytes: ByteArray): SubtitleFontMetadata? {
    if (bytes.size !in 12..MAX_SUBTITLE_FONT_BYTES) return null
    fun u16(at: Int) = ((bytes[at].toInt() and 255) shl 8) or (bytes[at + 1].toInt() and 255)
    fun u32(at: Int) = (u16(at).toLong() shl 16) or u16(at + 2).toLong()
    val extension = when (u32(0)) { 0x00010000L, 0x74727565L -> "ttf"; 0x4f54544fL -> "otf"; else -> return null }
    val count = u16(4)
    if (count !in 1..512 || 12 + count * 16 > bytes.size) return null
    var nameOffset = -1; var nameLength = 0
    repeat(count) { index ->
        val at = 12 + index * 16
        val offset = u32(at + 8); val size = u32(at + 12)
        if (offset > bytes.size || size > bytes.size.toLong() - offset) return null
        if (u32(at) == 0x6e616d65L) { nameOffset = offset.toInt(); nameLength = size.toInt() }
    }
    if (nameOffset < 0 || nameLength < 6) return null
    val names = u16(nameOffset + 2); val strings = u16(nameOffset + 4)
    if (names > 4096 || 6 + names * 12 > nameLength || strings < 6 + names * 12 || strings > nameLength) return null
    var fallback: String? = null
    repeat(names) { index ->
        val at = nameOffset + 6 + index * 12
        if (u16(at + 6) != 1) return@repeat
        val size = u16(at + 8); val offset = u16(at + 10)
        if (size == 0 || offset > nameLength - strings || size > nameLength - strings - offset) return null
        val start = nameOffset + strings + offset
        val platform = u16(at); val encoding = u16(at + 2)
        val name = when {
            platform == 0 || platform == 3 -> {
                if (size % 2 != 0) return null
                buildString { for (i in start until start + size step 2) append(u16(i).toChar()) }
            }
            platform == 1 && encoding == 0 -> decodeSubtitleCharset(bytes, start, size, "macintosh") ?: return@repeat
            else -> return@repeat
        }.trim()
        if (name.isEmpty() || name.length > 256 || name.any { it.isISOControl() }) return@repeat
        if (platform == 0 || (platform == 3 && u16(at + 4) == 0x0409)) return SubtitleFontMetadata(name, extension)
        if (fallback == null) fallback = name
    }
    return fallback?.let { SubtitleFontMetadata(it, extension) }
}
