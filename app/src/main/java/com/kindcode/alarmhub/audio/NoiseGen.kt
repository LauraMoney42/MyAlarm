package com.kindcode.alarmhub.audio

import com.kindcode.alarmhub.data.NoiseType
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Synthesises every sleep sound sample by sample rather than looping a file.
 * No assets to ship or license, and nothing repeats over eight hours next to
 * your head.
 *
 * One instance per channel. Give each a different seed so the stereo image is
 * decorrelated and sounds wide rather than centred in your skull.
 */
class NoiseGen(private val type: NoiseType, seed: Long, private val sampleRate: Int) {

    private val rnd = Random(seed)

    // Leaky integrator state for brown noise.
    private var brown = 0f

    // Paul Kellet pink noise filter state.
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var b3 = 0f
    private var b4 = 0f
    private var b5 = 0f
    private var b6 = 0f

    // One-pole lowpass state, reused by the darker voices.
    private var lp1 = 0f
    private var lp2 = 0f

    // Slow modulators. Each voice picks its own rate.
    private var phaseA = 0.0
    private var phaseB = 0.0
    private var tone = 0.0

    private fun white(): Float = rnd.nextFloat() * 2f - 1f

    private fun brownSample(w: Float): Float {
        brown = (brown + 0.02f * w) / 1.02f
        return brown * 3.5f
    }

    private fun pinkSample(w: Float): Float {
        b0 = 0.99886f * b0 + w * 0.0555179f
        b1 = 0.99332f * b1 + w * 0.0750759f
        b2 = 0.96900f * b2 + w * 0.1538520f
        b3 = 0.86650f * b3 + w * 0.3104856f
        b4 = 0.55000f * b4 + w * 0.5329522f
        b5 = -0.7616f * b5 - w * 0.0168980f
        val out = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f) * 0.11f
        b6 = w * 0.115926f
        return out
    }

    /** One-pole lowpass. [k] near 1 is very dark, near 0 passes everything. */
    private fun lowpass(x: Float, k: Float, stage: Int): Float {
        return if (stage == 1) {
            lp1 = lp1 * k + x * (1f - k); lp1
        } else {
            lp2 = lp2 * k + x * (1f - k); lp2
        }
    }

    private fun advance(hzA: Double, hzB: Double) {
        phaseA += 2.0 * PI * hzA / sampleRate
        phaseB += 2.0 * PI * hzB / sampleRate
    }

    fun next(): Float {
        val w = white()
        val s = when (type) {
            // Brown body for the rumble, a little white on top for droplet
            // hiss, gently swelling in and out.
            NoiseType.RAIN -> {
                advance(0.037, 0.0)
                val swell = 0.82f + 0.18f * sin(phaseA).toFloat()
                (brownSample(w) * 0.75f + w * 0.13f) * swell
            }

            NoiseType.BROWN -> brownSample(w)

            // Deep, slow breathing swell. Waves arrive roughly every 11s.
            NoiseType.SURF -> {
                advance(0.09, 0.013)
                val env = 0.30f + 0.70f * ((sin(phaseA) + 1.0) / 2.0).toFloat()
                val body = brownSample(w) * 0.8f + pinkSample(w) * 0.25f
                body * env * (0.9f + 0.1f * sin(phaseB).toFloat())
            }

            // Pink noise pushed through a lowpass, plus a faint motor beat.
            NoiseType.FAN -> {
                advance(11.0, 0.0)
                val body = lowpass(pinkSample(w) * 1.4f, 0.62f, 1)
                body * (0.94f + 0.06f * sin(phaseA).toFloat())
            }

            // Longer period than SURF and brighter foam on the crest.
            NoiseType.OCEAN -> {
                advance(0.055, 0.021)
                val wave = ((sin(phaseA) + 1.0) / 2.0).toFloat()
                val env = 0.24f + 0.76f * wave * wave
                (brownSample(w) * 0.9f + w * 0.10f * wave) * env *
                    (0.92f + 0.08f * sin(phaseB).toFloat())
            }

            // Quiet air with a high shimmer that pulses like crickets.
            NoiseType.NIGHT -> {
                advance(4.4, 0.11)
                val chirp = abs(sin(phaseA)).toFloat()
                val shimmer = (w - lowpass(w, 0.86f, 2)) * 0.30f * (chirp * chirp)
                brownSample(w) * 0.42f + shimmer * (0.55f + 0.45f * sin(phaseB).toFloat())
            }

            // Rolling stock: dark rumble with a two-beat sway over the joints.
            NoiseType.TRAIN -> {
                advance(1.6, 0.043)
                val clack = 0.86f + 0.14f * sin(phaseA).toFloat()
                val sway = 0.90f + 0.10f * sin(phaseB).toFloat()
                lowpass(brownSample(w) * 1.15f, 0.35f, 1) * clack * sway
            }

            // A low tonal cluster sitting under a bed of dark noise.
            NoiseType.DRONE -> {
                tone += 2.0 * PI * 58.0 / sampleRate
                advance(87.0, 0.02)
                val cluster = (sin(tone) * 0.55 + sin(phaseA) * 0.22).toFloat()
                cluster * 0.30f + lowpass(brownSample(w), 0.55f, 2) * 0.75f *
                    (0.9f + 0.1f * sin(phaseB).toFloat())
            }
        }
        return s.coerceIn(-1f, 1f)
    }
}
