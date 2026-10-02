package com.nuvio.app.features.player

import kotlin.math.abs
import kotlin.math.roundToLong

data class SeekPreviewCue(val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
    val isValid: Boolean get() = durationMs > 0L
    fun contains(positionMs: Long): Boolean = positionMs >= startMs && positionMs < endMs
    fun represents(positionMs: Long): Boolean = positionMs >= startMs - durationMs / 2 && positionMs < endMs
    fun prefersSuccessorFor(positionMs: Long): Boolean = positionMs - startMs > endMs - positionMs
}

object SeekPreviewCueStepper {
    fun targetMs(cue: SeekPreviewCue?, fromMs: Long, deltaMs: Long, durationMs: Long): Long {
        val maxMs = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val free = { (fromMs + deltaMs).coerceIn(0L, maxMs) }
        if (cue == null || !cue.isValid || !cue.represents(fromMs) || deltaMs == 0L) return free()
        val cueMs = cue.durationMs
        val steps = (abs(deltaMs).toDouble() / cueMs).roundToLong().coerceAtLeast(1L)
        val target = if (deltaMs > 0L) cue.endMs + (steps - 1L) * cueMs else cue.startMs - steps * cueMs
        return target.coerceIn(0L, maxMs)
    }

    fun alignedTargetMs(
        cue: SeekPreviewCue?,
        pendingMs: Long?,
        durationMs: Long,
        snap: (Long) -> Long = { it },
    ): Long? {
        if (cue == null || !cue.isValid || pendingMs == null || !cue.represents(pendingMs)) return null
        if (pendingMs <= 0L || (durationMs > 0L && pendingMs >= durationMs)) return null
        val target = snap(cue.startMs)
        if (target < 0L || (durationMs > 0L && target > durationMs) || target == pendingMs) return null
        return target
    }
}

const val SEEK_PREVIEW_KEYFRAME_SNAP_MS = 1_500L
const val SEEK_PREVIEW_OFFSET_MIN_MS = -240_000
const val SEEK_PREVIEW_OFFSET_MAX_MS = 240_000
const val SEEK_PREVIEW_OFFSET_STEP_MS = 250
const val SEEK_PREVIEW_OFFSET_COARSE_STEP_MS = 2_000
const val SEEK_PREVIEW_OFFSET_COARSE_AFTER_REPEATS = 3

fun seekPreviewOffsetStepMs(repeatCount: Int, forward: Boolean): Int {
    val magnitude = if (repeatCount >= SEEK_PREVIEW_OFFSET_COARSE_AFTER_REPEATS) SEEK_PREVIEW_OFFSET_COARSE_STEP_MS else SEEK_PREVIEW_OFFSET_STEP_MS
    return if (forward) magnitude else -magnitude
}

fun formatSeekPreviewOffset(offsetMs: Int): String {
    val sign = if (offsetMs >= 0) "+" else "-"
    val absolute = abs(offsetMs)
    return sign + (absolute / 1000) + "." + (absolute % 1000).toString().padStart(3, '0') + "s"
}
