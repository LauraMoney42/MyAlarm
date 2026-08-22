package com.kindcode.alarmhub.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * The Claude Design source is authored on a fixed 1280x800 canvas. Rather than
 * re-deriving every size, the panes are transcribed in those canvas units and
 * scaled once here, so the layout stays faithful on any panel.
 */
val LocalDesignScale = compositionLocalOf { 1f }

@Composable
fun du(canvasPx: Number): Dp = (canvasPx.toFloat() * LocalDesignScale.current).dp

@Composable
fun su(canvasPx: Number): TextUnit = with(LocalDensity.current) {
    (canvasPx.toFloat() * LocalDesignScale.current).dp.toSp()
}

/** Tokens lifted verbatim from `Flip Clock Tablet.dc.html`. */
object DC {
    val bg = Color(0xFF050506)
    val panel = Color(0xFF0C0C0F)
    val ink = Color(0xFFE9E9EA)

    val cardBody = Color(0xFF191A1D)
    val cardTop = Color(0xFF1E1E22)
    val cardBottom = Color(0xFF191A1D)
    val digit = Color(0xFFC6C6C9)
    val hinge = Color(0xFF050506)

    val accent = Color(0xFFD8A15C)

    /** The design's four accent options, offered in settings. */
    val accents = listOf(
        Color(0xFFD8A15C),
        Color(0xFF8FA3C4),
        Color(0xFFC47B6A),
        Color(0xFF9DB894),
    )

    fun ink(alpha: Float): Color = ink.copy(alpha = alpha)
}
