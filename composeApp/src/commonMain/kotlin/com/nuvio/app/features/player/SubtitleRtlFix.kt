package com.nuvio.app.features.player

/**
 * RTL cue text normalization for LTR display containers.
 * KMP-compatible version — no Android/Java dependencies.
 * Safe to run once at parse/load time; [fixText] is a no-op for non-RTL text.
 */
internal object SubtitleRtlFix {

    fun fixText(text: String): String {
        if (!hasAnyRtlCharacter(text)) return text

        if (containsArabic(text)) {
            val fixed = wrapArabicLines(text)
            return fixed
        }

        if (containsRtlChars(text)) {
            val fixed = fixHebrewLines(text)
            return fixed
        }

        return text
    }

    private fun wrapArabicLines(text: String): String {
        val lines = text.split('\n')
        val sb = StringBuilder(text.length + 8)
        for (i in lines.indices) {
            if (i > 0) sb.append('\n')
            val line = lines[i].stripDirectionalMarkers()
            if (line.isEmpty()) {
                sb.append(line)
                continue
            }
            val hasCr = line.lastOrNull() == '\r'
            val core = if (hasCr) line.substring(0, line.length - 1) else line
            if (core.isEmpty()) {
                sb.append(line)
                continue
            }
            sb.append('\u202B').append(core).append('\u202C')
            if (hasCr) sb.append('\r')
        }
        return sb.toString()
    }

    private fun fixHebrewLines(text: String): String {
        val lines = text.split('\n')
        val sb = StringBuilder(text.length)
        for (i in lines.indices) {
            if (i > 0) sb.append('\n')
            val line = lines[i]
            sb.append(fixRtlPunctuationForLtr(line))
        }
        return sb.toString()
    }

    private fun fixRtlPunctuationForLtr(line: String): String {
        if (line.isEmpty()) return line
        val hasCr = line.lastOrNull() == '\r'
        val end0 = if (hasCr) line.length - 1 else line.length
        if (end0 == 0) return line

        var start = 0
        while (start < end0 && isRtlPunctuation(line[start], isEnd = false)) start++

        var end = end0
        while (end > start && isRtlPunctuation(line[end - 1], isEnd = true)) end--

        if (start == 0 && end == end0) return line

        val out = StringBuilder(end0)
        appendMirroredReversed(out, line, end, end0) // trailing punct/numbers -> front
        out.append(line.substring(start, end)) // middle, untouched
        appendMirroredReversed(out, line, 0, start) // leading punct/numbers -> end
        if (hasCr) out.append('\r')
        return out.toString()
    }

    private fun appendMirroredReversed(out: StringBuilder, line: String, from: Int, toExclusive: Int) {
        if (from >= toExclusive) return

        fun isNumberSeparator(c: Char) = c == ',' || c == ':' || c == '.' || c == '-' || c == '/'

        // Split into chunks: number-runs stay together
        val chunks = ArrayList<IntRange>()
        var i = from
        while (i < toExclusive) {
            if (line[i].isDigit()) {
                val start = i; i++
                while (i < toExclusive) {
                    if (line[i].isDigit()) { i++ }
                    else if (isNumberSeparator(line[i]) && i + 1 < toExclusive && line[i + 1].isDigit()) { i++ }
                    else break
                }
                chunks.add(start until i)
            } else {
                chunks.add(i until i + 1); i++
            }
        }

        for (idx in chunks.indices.reversed()) {
            val range = chunks[idx]
            if (range.last - range.first + 1 > 1) {
                out.append(line.substring(range.first, range.last + 1))
            } else {
                val c = line[range.first]
                out.append(mirrorPunctuation(c))
            }
        }
    }

    private fun containsArabic(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val cp = text[i].code
            if (cp in 0x0600..0x06FF || cp in 0x0750..0x077F || cp in 0x0870..0x08FF ||
                cp in 0xFB50..0xFDFF || cp in 0xFE70..0xFEFF) return true
            i++
        }
        return false
    }

    private fun containsRtlChars(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val cp = text[i].code
            if (cp in 0x0590..0x05FF || cp in 0xFB1D..0xFB4F || // Hebrew
                cp in 0x0600..0x06FF || cp in 0x0750..0x077F || cp in 0x0870..0x08FF ||
                cp in 0xFB50..0xFDFF || cp in 0xFE70..0xFEFF) return true
            i++
        }
        return false
    }

    private fun hasAnyRtlCharacter(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val cp = text[i].code
            if (cp >= 0x0590 && (cp in 0x0590..0x08FF || cp in 0xFB1D..0xFEFF)) return true
            i++
        }
        return false
    }

    private fun CharSequence.stripDirectionalMarkers(): String {
        val hasMarker = (0 until length).any { isDirectionalMark(this[it]) }
        if (!hasMarker) return toString()
        val sb = StringBuilder(length)
        for (ch in this) { if (!isDirectionalMark(ch)) sb.append(ch) }
        return sb.toString()
    }

    private fun isDirectionalMark(c: Char): Boolean =
        c == '\u202A' || c == '\u202B' || c == '\u202C' || c == '\u200E' || c == '\u200F'

    private fun isRtlPunctuation(ch: Char, isEnd: Boolean): Boolean {
        if (isEnd && ch.isDigit()) return false
        return ch in RTL_PUNCTUATION || ch.isWhitespace()
    }

    private fun mirrorPunctuation(c: Char): Char = when (c) {
        '(' -> ')'; ')' -> '('; else -> c
    }

    private val RTL_PUNCTUATION = setOf('.', ',', '?', '!', '-', ':', ';', '…', ')', '(', '\'', '"') + ('0'..'9')
}
