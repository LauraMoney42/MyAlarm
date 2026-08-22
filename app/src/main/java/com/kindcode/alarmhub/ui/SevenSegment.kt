package com.kindcode.alarmhub.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

/*
 * Segment names follow the usual convention so the digit table below can be
 * read against any datasheet:
 *
 *      aaaa
 *     f    b
 *     f    b
 *      gggg
 *     e    c
 *     e    c
 *      dddd
 */
private const val SEG_A = 1 shl 0
private const val SEG_B = 1 shl 1
private const val SEG_C = 1 shl 2
private const val SEG_D = 1 shl 3
private const val SEG_E = 1 shl 4
private const val SEG_F = 1 shl 5
private const val SEG_G = 1 shl 6

/** Which segments each digit lights. Index is the digit itself. */
private val DIGIT_SEGMENTS = intArrayOf(
    SEG_A or SEG_B or SEG_C or SEG_D or SEG_E or SEG_F,          // 0
    SEG_B or SEG_C,                                              // 1
    SEG_A or SEG_B or SEG_G or SEG_E or SEG_D,                   // 2
    SEG_A or SEG_B or SEG_G or SEG_C or SEG_D,                   // 3
    SEG_F or SEG_G or SEG_B or SEG_C,                            // 4
    SEG_A or SEG_F or SEG_G or SEG_C or SEG_D,                   // 5
    SEG_A or SEG_F or SEG_G or SEG_E or SEG_C or SEG_D,          // 6
    SEG_A or SEG_B or SEG_C,                                     // 7
    SEG_A or SEG_B or SEG_C or SEG_D or SEG_E or SEG_F or SEG_G, // 8
    SEG_A or SEG_B or SEG_C or SEG_D or SEG_F or SEG_G,          // 9
)

// Everything below is a fraction of the box handed in, so a digit drawn at any
// size keeps the same proportions.

/** Segment thickness. Capped against the width so narrow boxes stay legible. */
private const val THICKNESS_OF_HEIGHT = 0.145f
private const val THICKNESS_OF_WIDTH = 0.30f

/**
 * How far each segment pulls back from the corner it shares with its neighbour.
 * Real LED panels leave a hairline of dark between segments, and that hairline
 * is most of what makes the shape read as a bulb array rather than a glyph.
 */
private const val GAP_OF_THICKNESS = 0.16f

/** Dot size and corner rounding for the colon, both as fractions of height. */
private const val COLON_DOT_OF_HEIGHT = 0.115f
private const val COLON_CORNER_OF_DOT = 0.28f

/**
 * Builds a chamfered hexagon: a bar of length [length] and thickness [thickness]
 * whose two ends are mitred at 45 degrees.
 *
 * The mitre is not decoration. Adjacent segments meet at right angles, and only
 * a 45 degree end lets the corner close cleanly, which is exactly how the mask
 * on a physical seven segment display is cut.
 */
private fun Path.addSegment(
    centerX: Float,
    centerY: Float,
    length: Float,
    thickness: Float,
    vertical: Boolean,
) {
    val half = length / 2f
    val t = thickness / 2f
    if (vertical) {
        moveTo(centerX, centerY - half)
        lineTo(centerX + t, centerY - half + t)
        lineTo(centerX + t, centerY + half - t)
        lineTo(centerX, centerY + half)
        lineTo(centerX - t, centerY + half - t)
        lineTo(centerX - t, centerY - half + t)
    } else {
        moveTo(centerX - half, centerY)
        lineTo(centerX - half + t, centerY - t)
        lineTo(centerX + half - t, centerY - t)
        lineTo(centerX + half, centerY)
        lineTo(centerX + half - t, centerY + t)
        lineTo(centerX - half + t, centerY + t)
    }
    close()
}

/**
 * Draws one segment into [path], reusing the same Path instance across all seven
 * so a redraw costs no allocations.
 */
private fun DrawScope.drawSegment(
    path: Path,
    centerX: Float,
    centerY: Float,
    length: Float,
    thickness: Float,
    vertical: Boolean,
    color: Color,
) {
    if (color.alpha == 0f || length <= 0f) return
    path.reset()
    path.addSegment(centerX, centerY, length, thickness, vertical)
    drawPath(path, color)
}

/**
 * One digit of a bedside LED clock, drawn as seven mitred bars.
 *
 * Every segment is painted on every frame: the unlit ones in [dimColor], the lit
 * ones in [litColor]. That ghosting is the whole illusion. A real panel always
 * shows the dark bars of the digits it is not currently displaying, and drawing
 * only the lit segments produces something that reads as a stencil font instead
 * of hardware. It also means "1" and "8" occupy visibly the same cell, so the
 * time never appears to shift as the digits change.
 *
 * Pass null for [value] to render the cell entirely unlit, which is what a
 * blanked leading digit or a blinking display wants.
 *
 * Geometry is derived from [width] and [height] alone, so the same composable
 * serves a full screen clock and a list row preview.
 */
@Composable
fun SevenSegmentDigit(
    value: Int?,
    litColor: Color,
    dimColor: Color,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    // Hoisted out of the draw lambda: the path is scratch space, not state, and
    // a clock face redraws often enough that the churn is worth avoiding.
    val path = remember { Path() }
    val mask = value?.takeIf { it in 0..9 }?.let { DIGIT_SEGMENTS[it] } ?: 0

    Canvas(modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val thickness = minOf(h * THICKNESS_OF_HEIGHT, w * THICKNESS_OF_WIDTH)
        val gap = thickness * GAP_OF_THICKNESS

        // Horizontal bars span the full width; vertical bars run from an outer
        // edge to the centre line, which is where the middle bar's own centre
        // sits. Each is then pulled in by `gap` at both ends to open the seams.
        val hLength = w - gap * 2f
        val vLength = h / 2f - gap * 2f
        val halfT = thickness / 2f

        fun color(bit: Int) = if (mask and bit != 0) litColor else dimColor

        drawSegment(path, w / 2f, halfT, hLength, thickness, false, color(SEG_A))
        drawSegment(path, w / 2f, h / 2f, hLength, thickness, false, color(SEG_G))
        drawSegment(path, w / 2f, h - halfT, hLength, thickness, false, color(SEG_D))

        drawSegment(path, halfT, h / 4f, vLength, thickness, true, color(SEG_F))
        drawSegment(path, w - halfT, h / 4f, vLength, thickness, true, color(SEG_B))
        drawSegment(path, halfT, h * 3f / 4f, vLength, thickness, true, color(SEG_E))
        drawSegment(path, w - halfT, h * 3f / 4f, vLength, thickness, true, color(SEG_C))
    }
}

/**
 * The colon between hours and minutes, sized to sit beside a digit of [height].
 *
 * The dots land at a third and two thirds of the height rather than straddling
 * the midpoint, which puts them in the two openings the digits leave either side
 * of their middle segment. They are near square with softened corners because
 * that is the shape an LED window actually is, and a true circle looks out of
 * family next to the mitred bars.
 *
 * There is no dim state here: unlike a digit, a colon has nothing to disambiguate
 * by ghosting, so a caller that wants it blinking simply stops composing it or
 * hands in a transparent colour.
 */
@Composable
fun SevenSegmentColon(
    litColor: Color,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val dot = height * COLON_DOT_OF_HEIGHT

    Canvas(modifier.size(dot, height)) {
        val h = size.height
        val d = size.width
        if (h <= 0f || d <= 0f) return@Canvas

        val radius = CornerRadius(d * COLON_CORNER_OF_DOT, d * COLON_CORNER_OF_DOT)
        val boxSize = Size(d, d)

        drawRoundRect(
            color = litColor,
            topLeft = Offset(0f, h / 3f - d / 2f),
            size = boxSize,
            cornerRadius = radius,
        )
        drawRoundRect(
            color = litColor,
            topLeft = Offset(0f, h * 2f / 3f - d / 2f),
            size = boxSize,
            cornerRadius = radius,
        )
    }
}
