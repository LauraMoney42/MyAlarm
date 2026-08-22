package com.kindcode.alarmhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** The design's selectable pill, used for durations, tones and lead times. */
@Composable
fun Chip(
    label: String,
    selected: Boolean,
    accent: Color,
    hPad: Int = 24,
    vPad: Int = 13,
    fontSize: Int = 15,
    tracking: Float = 0.1f,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(du(30))
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) accent else Color.Transparent)
            .border(
                du(1).coerceAtLeast(1.dp),
                if (selected) accent else DC.ink(0.16f),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = du(hPad), vertical = du(vPad)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                color = if (selected) Color(0xFF0B0B0C) else DC.ink(0.6f),
                fontSize = su(fontSize),
                letterSpacing = su(fontSize * tracking),
            ),
        )
    }
}

/** Section heading: small, wide-tracked, dim. */
@Composable
fun SectionLabel(text: String, size: Int = 14, alpha: Float = 0.5f) {
    Text(
        text,
        style = TextStyle(
            color = DC.ink(alpha),
            fontSize = su(size),
            letterSpacing = su(size * 0.28f),
        ),
    )
}

/** The design's pill toggle. */
@Composable
fun Toggle(checked: Boolean, accent: Color, width: Int = 74, onToggle: () -> Unit) {
    val h = width * 36f / 74f
    val knob = h * 26f / 36f
    val pad = (h - knob) / 2f
    // Explicit size: sizing from content made the "off" state collapse to just
    // the knob, so the two states did not read as the same control.
    Box(
        Modifier
            .size(du(width), du(h))
            .clip(RoundedCornerShape(50))
            .background(if (checked) accent else DC.ink(0.14f))
            .clickable(onClick = onToggle),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = if (checked) du(width - knob - pad) else du(pad))
                .size(du(knob))
                .clip(RoundedCornerShape(50))
                .background(if (checked) Color(0xFF0B0B0C) else DC.ink(0.55f))
        )
    }
}

/**
 * The close affordance on every panel: the same grip the clock shows at its
 * edges, on the edge this panel closes toward. No wording, because the gesture
 * is the same one that opened it. Tappable as a fallback.
 */
@Composable
fun CloseGrip(vertical: Boolean, modifier: Modifier = Modifier, onClose: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClose)
            .padding(du(12)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(
                    if (vertical) du(4).coerceAtLeast(2.dp) else du(52),
                    if (vertical) du(52) else du(4).coerceAtLeast(2.dp),
                )
                .clip(RoundedCornerShape(50))
                .background(DC.ink(0.22f))
        )
    }
}
