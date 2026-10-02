package com.nuvio.app.features.player

/**
 * Filters SDH (Subtitles for the Deaf or Hard-of-Hearing) content from subtitle text.
 * Removes sound effects in square brackets, speaker labels, chevrons, and parenthetical
 * descriptions common in CEA-608 / YouTube-style captions.
 *
 * KMP-compatible version — no Android/Java NIO dependencies.
 */
internal object SubtitleSdhFilter {
    private val squareBrackets = Regex("\\[[^]]*][ \\t]*")
    // ">>" marks a speaker change and ">>>" a topic change in CEA-608 style
    // captions, which YouTube carries into its own caption tracks.
    private val speakerChevrons = Regex("[<>]{2,}[ \\t]*")
    private val parentheses = Regex(
        "(?:\\((?=[A-Za-z0-9 '#.,\\\\\"\\\\\\\\\\-\\r\\n]*\\))(?![0-9]*\\))[^)]*\\)|" +
            "\uFF08(?=[A-Za-z0-9 '#.,\\\\\"\\\\\\\\\\-\\r\\n]*\uFF09)(?![0-9]*\uFF09)[^\uFF09]*\uFF09)[ \\t]*"
    )
    private val speakerLabel = Regex(
        "(?m)^([ \\t]*-[ \\t]*)?(?:[A-Za-z0-9 ()'#.,]+|\\[[^]\\r\\n]*]):(?=\\s|$)[ \\t]*"
    )

    fun filterText(text: String): String? {
        // Runs before speakerLabel so that ">> NAME:" loses the chevrons first and
        // is then recognised as a speaker label.
        var filtered = speakerChevrons.replace(text, "")
        filtered = speakerLabel.replace(filtered) { match -> match.groups[1]?.value.orEmpty() }
        filtered = squareBrackets.replace(filtered, "")
        filtered = parentheses.replace(filtered, "")
        return filtered.lines()
            .filter { line -> line.any { !it.isWhitespace() && it != '-' } }
            .joinToString("\n")
            .takeIf(String::isNotBlank)
    }
}
