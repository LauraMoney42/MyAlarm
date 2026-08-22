package com.kindcode.alarmhub.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path

/**
 * Monoline geometric numerals, drawn as centre-lines rather than letter shapes.
 *
 * This exists because stroking a font glyph traces its outline, so every stem
 * comes out as two parallel tubes with a hollow gap between them. Real neon is
 * one bent tube per stroke, so the path here is the tube's centre-line and the
 * stroke width is the tube.
 *
 * Everything is authored on a unit box and scaled at draw time. The shapes are
 * deliberately geometric, circles and straight runs, which is what a bar-sign
 * face looks like.
 */
object NeonGlyphs {

    fun path(char: Char, size: Size, inset: Float): Path {
        val p = Path()
        val l = inset
        val t = inset
        val r = size.width - inset
        val b = size.height - inset
        val w = r - l
        val h = b - t
        fun x(u: Float) = l + u * w
        fun y(v: Float) = t + v * h
        fun rect(x0: Float, y0: Float, x1: Float, y1: Float) =
            Rect(x(x0), y(y0), x(x1), y(y1))

        when (char) {
            '0' -> p.addOval(rect(0.02f, 0f, 0.98f, 1f))

            '1' -> {
                p.moveTo(x(0.14f), y(0.24f))
                p.lineTo(x(0.58f), y(0f))
                p.lineTo(x(0.58f), y(1f))
            }

            '2' -> {
                // Shoulder over the top, a diagonal down, then the foot.
                p.arcTo(rect(0.04f, 0f, 0.96f, 0.54f), 180f, 190f, true)
                p.cubicTo(x(1.00f), y(0.52f), x(0.62f), y(0.72f), x(0.04f), y(1f))
                p.lineTo(x(0.98f), y(1f))
            }

            '3' -> {
                p.arcTo(rect(0.06f, 0f, 0.94f, 0.53f), 175f, 245f, true)
                p.arcTo(rect(0.06f, 0.47f, 0.94f, 1f), 290f, 245f, false)
            }

            '4' -> {
                p.moveTo(x(0.78f), y(0f))
                p.lineTo(x(0.03f), y(0.72f))
                p.lineTo(x(0.99f), y(0.72f))
                p.moveTo(x(0.78f), y(0.30f))
                p.lineTo(x(0.78f), y(1f))
            }

            '5' -> {
                p.moveTo(x(0.92f), y(0.02f))
                p.lineTo(x(0.16f), y(0.02f))
                p.lineTo(x(0.11f), y(0.44f))
                p.cubicTo(x(0.55f), y(0.30f), x(0.98f), y(0.46f), x(0.94f), y(0.70f))
                p.cubicTo(x(0.90f), y(0.96f), x(0.36f), y(1.06f), x(0.06f), y(0.86f))
            }

            '6' -> {
                p.moveTo(x(0.86f), y(0.06f))
                p.cubicTo(x(0.30f), y(0.10f), x(0.04f), y(0.40f), x(0.05f), y(0.70f))
                p.addOval(rect(0.05f, 0.42f, 0.95f, 1f))
            }

            '7' -> {
                p.moveTo(x(0.04f), y(0.02f))
                p.lineTo(x(0.96f), y(0.02f))
                p.lineTo(x(0.34f), y(1f))
            }

            '8' -> {
                p.addOval(rect(0.11f, 0f, 0.89f, 0.47f))
                p.addOval(rect(0.03f, 0.47f, 0.97f, 1f))
            }

            '9' -> {
                p.addOval(rect(0.05f, 0f, 0.95f, 0.58f))
                p.moveTo(x(0.95f), y(0.30f))
                p.cubicTo(x(0.96f), y(0.60f), x(0.70f), y(0.90f), x(0.14f), y(0.94f))
            }

            'A' -> {
                p.moveTo(x(0.02f), y(1f))
                p.lineTo(x(0.5f), y(0f))
                p.lineTo(x(0.98f), y(1f))
                p.moveTo(x(0.18f), y(0.66f))
                p.lineTo(x(0.82f), y(0.66f))
            }

            'M' -> {
                p.moveTo(x(0.03f), y(1f))
                p.lineTo(x(0.03f), y(0f))
                p.lineTo(x(0.5f), y(0.64f))
                p.lineTo(x(0.97f), y(0f))
                p.lineTo(x(0.97f), y(1f))
            }

            'P' -> {
                p.moveTo(x(0.08f), y(1f))
                p.lineTo(x(0.08f), y(0.02f))
                p.lineTo(x(0.58f), y(0.02f))
                p.cubicTo(x(0.98f), y(0.06f), x(0.98f), y(0.50f), x(0.58f), y(0.54f))
                p.lineTo(x(0.08f), y(0.54f))
            }

            ':' -> {
                val rr = w * 0.14f
                p.addOval(Rect(x(0.5f) - rr, y(0.30f) - rr, x(0.5f) + rr, y(0.30f) + rr))
                p.addOval(Rect(x(0.5f) - rr, y(0.72f) - rr, x(0.5f) + rr, y(0.72f) + rr))
            }
        }
        return p
    }

    /** Relative advance for each character, in units of glyph width. */
    fun advance(char: Char): Float = if (char == ':') 0.38f else 1f
}
