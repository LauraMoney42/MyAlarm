package com.kindcode.alarmhub.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp

/**
 * One neon character: a monoline centre-line path stroked several times over.
 *
 * Widest and faintest first, tightening and brightening to a nearly white core,
 * which is what a lit tube looks like: the gas glows coloured and the filament
 * reads white. Round caps and joins are what make a stroke read as bent glass
 * rather than an outline.
 *
 * The falloff between passes is deliberately gentle. Large alpha steps show up
 * as concentric rings instead of a glow, and the step into the core shows most.
 *
 * Stacked strokes rather than Modifier.blur: blur looked marginally softer and
 * cost 250ms a frame on this GPU, which for a clock redrawing every second in
 * rainbow mode is a quarter of the device's time, all night.
 */
@Composable
fun NeonChar(
    char: Char,
    color: Color,
    width: Dp,
    height: Dp,
    tube: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(width, height)) {
        val core = tube.toPx()
        val path = NeonGlyphs.path(char, size, core * 0.5f + core * 0.6f)
        val white = lerp(color, Color.White, 0.88f)

        fun pass(scale: Float, alpha: Float, tint: Color) {
            drawPath(
                path = path,
                color = tint.copy(alpha = alpha),
                style = Stroke(
                    width = core * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // A generated ramp rather than a handful of hand-picked passes. Discrete
        // steps read as concentric rings around a thin tube, and the thinner the
        // tube the more they show, so the halo is built from many faint layers.
        val halo = 14
        for (i in halo downTo 1) {
            val f = i / halo.toFloat()
            pass(1f + f * 4.6f, 0.30f * (1f - f) * (1f - f) + 0.018f, color)
        }
        pass(1.0f, 0.95f, color)
        pass(0.34f, 0.95f, white)
    }
}
