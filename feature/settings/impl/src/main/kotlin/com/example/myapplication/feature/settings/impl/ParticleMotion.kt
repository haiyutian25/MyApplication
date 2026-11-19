// The choreography is expressed as raw seconds and curve coefficients, so the file level magic
// number rule is waived for it.
@file:Suppress("MagicNumber")

package com.example.myapplication.feature.settings.impl

import kotlin.math.pow

/**
 * The time, in seconds, at which the particles leave the galaxy and start flying into the word.
 */
internal const val ASSEMBLY_START = 1.15f

/**
 * The time, in seconds, at which the first particles have gathered into the word.
 */
internal const val ASSEMBLY_END = 2.25f

/**
 * The time, in seconds, at which the formed word starts inhaling before it bursts apart.
 */
internal const val INHALE_START = 2.65f

/**
 * The time, in seconds, at which the solid word lands and the particle cloud detonates.
 */
internal const val IMPACT_AT = 3.1f

/**
 * The time, in seconds, at which the trails of the particle cloud have faded out.
 */
internal const val PARTICLES_END = 4.6f

/**
 * Normalised progress of [time] through the window that runs from [start] to [end], clamped to
 * the 0..1 range.
 */
internal fun progress(time: Float, start: Float, end: Float): Float =
    ((time - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Linear interpolation from [start] to [end] by [amount].
 */
internal fun mix(start: Float, end: Float, amount: Float): Float =
    start + (end - start) * amount

/**
 * Decelerating exponential ease: almost all of the motion happens in the first moments.
 */
internal fun outExpo(value: Float): Float =
    if (value >= 1f) 1f else 1f - 2f.pow(-10f * value)

/**
 * Accelerating cubic ease.
 */
internal fun inCubic(value: Float): Float = value * value * value

/**
 * Symmetric cubic ease, slow at both ends.
 */
internal fun inOutCubic(value: Float): Float =
    if (value < 0.5f) 4f * value * value * value else 1f - (-2f * value + 2f).pow(3f) / 2f

/**
 * Symmetric exponential ease, extremely slow at both ends.
 */
internal fun inOutExpo(value: Float): Float = when {
    value <= 0f -> 0f
    value >= 1f -> 1f
    value < 0.5f -> 2f.pow(20f * value - 10f) / 2f
    else -> (2f - 2f.pow(-20f * value + 10f)) / 2f
}
