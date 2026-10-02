package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleMojibakeSanitizerTest {

    @Test
    fun replacesMusicalNotesMojibake() {
        assertEquals("♪ lalala ♪", SubtitleMojibakeSanitizer.sanitize("â™ª lalala â™ª").toString())
        assertEquals("♫ song playing ♫", SubtitleMojibakeSanitizer.sanitize("â™« song playing â™«").toString())
        assertEquals("♪ melody ♪", SubtitleMojibakeSanitizer.sanitize("â™ melody â™").toString())
    }

    @Test
    fun replacesQuotesAndPunctuationMojibake() {
        assertEquals("It’s great!", SubtitleMojibakeSanitizer.sanitize("Itâ€™s great!").toString())
        assertEquals("‘Hello’", SubtitleMojibakeSanitizer.sanitize("â€˜Helloâ€™").toString())
        assertEquals("“Quote”", SubtitleMojibakeSanitizer.sanitize("â€œQuoteâ€").toString())
        assertEquals("“Quote”", SubtitleMojibakeSanitizer.sanitize("â€œQuoteâ€\u009D").toString())
        assertEquals("Wait – what — why…", SubtitleMojibakeSanitizer.sanitize("Wait â€“ what â€” whyâ€¦").toString())
    }

    @Test
    fun replacesSpanishPunctuationMojibake() {
        assertEquals("¿Cómo estás? ¡Bien!", SubtitleMojibakeSanitizer.sanitize("Â¿Cómo estás? Â¡Bien!").toString())
        assertEquals("«Hola»", SubtitleMojibakeSanitizer.sanitize("Â«HolaÂ»").toString())
        assertEquals("Hello world", SubtitleMojibakeSanitizer.sanitize("HelloÂ world").toString())
    }

    @Test
    fun leavesCleanTextUnchanged() {
        val clean = "Hello, world! 123 ♪ ♫ “test”"
        assertEquals(clean, SubtitleMojibakeSanitizer.sanitize(clean).toString())
    }

    @Test
    fun stripsReplacementCharacters() {
        assertEquals("Hello world", SubtitleMojibakeSanitizer.sanitize("Hello \uFFFDworld\uFFFD").toString())
    }
}
