package com.kindcode.alarmhub.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * A neon tube character: a hollow stroked glyph with the light bleeding out of
 * it.
 *
 * Real neon is a bright core inside a wide, dim halo, so this is drawn as
 * several passes of the same glyph rather than one. Widest and dimmest first,
 * then tighter and brighter, finishing with a nearly white core, which is what
 * a lit tube actually looks like: the gas glows coloured, the filament reads
 * white.
 *
 * Blur is only available from API 31. Below that the wide passes still draw,
 * just with hard edges, which reads as a chunky outline rather than broken.
 */
@Composable
private fun NeonGlyph(
    char: String,
    color: Color,
    size: TextUnit,
    strokeCore: Float,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun pass(width: Float, alpha: Float, tint: Color) {
        Text(
            text = char,
            style = TextStyle(
                color = tint.copy(alpha = alpha),
                fontSize = size,
                lineHeight = size,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                drawStyle = Stroke(
                    width = width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
        )
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        // Widest and faintest first, tightening and brightening to a nearly
        // white core, which is what a lit tube looks like: the gas glows
        // coloured and the filament reads white.
        //
        // Built from stacked strokes rather than Modifier.blur. Blur looked
        // marginally softer and cost 250ms a frame on this GPU, which for a
        // clock that redraws every second is a quarter of the device's time,
        // all night. Strokes cost almost nothing.
        // The falloff is smooth on purpose. Big jumps between passes show up
        // as concentric rings rather than a glow, and the jump into the core is
        // the one that shows most.
        pass(strokeCore * 5.6f, 0.045f, color)
        pass(strokeCore * 4.4f, 0.060f, color)
        pass(strokeCore * 3.5f, 0.085f, color)
        pass(strokeCore * 2.8f, 0.120f, color)
        pass(strokeCore * 2.2f, 0.175f, color)
        pass(strokeCore * 1.7f, 0.260f, color)
        pass(strokeCore * 1.3f, 0.420f, color)
        pass(strokeCore * 1.0f, 0.950f, color)
        pass(strokeCore * 0.34f, 0.950f, lerp(color, Color.White, 0.88f))
    }
}

/** One character of the neon clock. */
@Composable
fun NeonChar(
    char: String,
    color: Color,
    size: TextUnit,
    strokeCore: Float,
    modifier: Modifier = Modifier,
) {
    NeonGlyph(char, color, size, strokeCore, modifier)
}
