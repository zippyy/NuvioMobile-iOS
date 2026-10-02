package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleSdhFilterTest {

    @Test
    fun removesSquareBracketSoundEffects() {
        assertEquals(
            "Hello world",
            SubtitleSdhFilter.filterText("[music playing] Hello world"),
        )
    }

    @Test
    fun removesSpeakerChevrons() {
        assertEquals(
            "Hello",
            SubtitleSdhFilter.filterText(">> Hello"),
        )
        assertEquals(
            "Hello world",
            SubtitleSdhFilter.filterText(">>> Hello world"),
        )
    }

    @Test
    fun removesParentheticalDescriptions() {
        assertEquals(
            "Hello",
            SubtitleSdhFilter.filterText("(laughing) Hello"),
        )
        assertEquals(
            "Hello world",
            SubtitleSdhFilter.filterText("Hello world (in a whisper)"),
        )
    }

    @Test
    fun removesSpeakerLabels() {
        assertEquals(
            "Hello world",
            SubtitleSdhFilter.filterText("MAN: Hello world"),
        )
        assertEquals(
            "Hello world",
            SubtitleSdhFilter.filterText("- WOMAN: Hello world"),
        )
        assertEquals(
            "Hello world",
            SubtitleSdhFilter.filterText("[NARRATOR]: Hello world"),
        )
    }

    @Test
    fun handlesChevronPlusSpeakerLabel() {
        assertEquals(
            "Hello world",
            SubtitleSdhFilter.filterText(">> MAN: Hello world"),
        )
    }

    @Test
    fun returnsNullWhenAllContentIsRemoved() {
        assertNull(SubtitleSdhFilter.filterText("[music]"))
        assertNull(SubtitleSdhFilter.filterText("(silence)"))
        assertNull(SubtitleSdhFilter.filterText(">>"))
    }

    @Test
    fun leavesPlainTextUnchanged() {
        assertEquals("Hello world", SubtitleSdhFilter.filterText("Hello world"))
    }
}
