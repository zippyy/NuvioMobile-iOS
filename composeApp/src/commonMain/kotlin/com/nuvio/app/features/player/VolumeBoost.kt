package com.nuvio.app.features.player

import kotlin.math.abs
import kotlin.math.tanh

private const val VOLUME_BOOST_KNEE = 0.8f
private const val VOLUME_BOOST_HEADROOM = 1f - VOLUME_BOOST_KNEE

/** Reshaped soft clip used after gain so boosted peaks bend instead of hard-clipping. */
fun softClipBoostedSample(sample: Float): Float {
    val magnitude = abs(sample)
    if (magnitude <= VOLUME_BOOST_KNEE) return sample
    val bent = VOLUME_BOOST_KNEE + VOLUME_BOOST_HEADROOM * tanh((magnitude - VOLUME_BOOST_KNEE) / VOLUME_BOOST_HEADROOM)
    return if (sample < 0f) -bent else bent
}

/** Maps the Reshaped 0..200% control to mpv's volume scale. */
fun volumeBoostPercentToMpv(percent: Int): Double = percent.coerceIn(0, 200).toDouble()
