package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeekPreviewCueTest {
    private val duration = 3_600_000L
    private val cue = SeekPreviewCue(2_260_000L, 2_270_000L)

    @Test fun `steps lock to preview grid`() {
        assertEquals(2_270_000L, SeekPreviewCueStepper.targetMs(cue, 2_264_000L, 10_000L, duration))
        assertEquals(2_250_000L, SeekPreviewCueStepper.targetMs(cue, 2_264_000L, -10_000L, duration))
        assertEquals(2_320_000L, SeekPreviewCueStepper.targetMs(cue, 2_260_000L, 60_000L, duration))
    }

    @Test fun `missing cue falls back to free seek and clamps bounds`() {
        assertEquals(2_274_000L, SeekPreviewCueStepper.targetMs(null, 2_264_000L, 10_000L, duration))
        assertEquals(0L, SeekPreviewCueStepper.targetMs(null, 1_000L, -10_000L, duration))
    }

    @Test fun `cue representation hands over after midpoint`() {
        val shown = SeekPreviewCue(1_110_000L, 1_120_000L)
        assertTrue(shown.represents(1_105_000L))
        assertFalse(shown.represents(1_104_999L))
        assertFalse(cue.prefersSuccessorFor(cue.startMs + 5_000L))
        assertTrue(cue.prefersSuccessorFor(cue.startMs + 5_001L))
    }

    @Test fun `alignment snaps to represented frame but preserves media ends`() {
        assertEquals(cue.startMs, SeekPreviewCueStepper.alignedTargetMs(cue, 2_264_000L, duration))
        assertNull(SeekPreviewCueStepper.alignedTargetMs(cue, cue.startMs, duration))
        val overhang = SeekPreviewCue(duration - 8_000L, duration + 2_000L)
        assertNull(SeekPreviewCueStepper.alignedTargetMs(overhang, duration, duration))
    }

    @Test fun `offset controls preserve Reshaped fine and coarse behavior`() {
        assertEquals(250, seekPreviewOffsetStepMs(0, true))
        assertEquals(-250, seekPreviewOffsetStepMs(2, false))
        assertEquals(2_000, seekPreviewOffsetStepMs(3, true))
        assertEquals("+1.250s", formatSeekPreviewOffset(1_250))
        assertEquals("-0.250s", formatSeekPreviewOffset(-250))
    }
}
