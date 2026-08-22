package com.kindcode.alarmhub.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The colour picker, ported from Tally's `ColorOpacitySliderControl` so the two
 * apps read as one family: a round preview swatch on the left, and on the right
 * a hue slider over a full rainbow and an opacity slider that fades the current
 * colour in over a checkerboard.
 *
 * Ported rather than shared, obviously, but the proportions, the dim tracked
 * labels and the tinted circular thumbs are all deliberately the iOS control's,
 * only re-expressed in this project's 1280x800 canvas units.
 */

/**
 * Tally's `TallyPalette.presetColorHexes`, in the same order. Kept as literal
 * ARGB rather than parsed hex strings because nothing on this side stores
 * colours as text, so the string round-trip the iOS app needs buys us nothing.
 */
private val PRESET_COLORS = listOf(
    Color(0xFFFFFFFF), // white, the default for clock numbers
    Color(0xFF12707B), // teal (Tally's default)
    Color(0xFF0A84FF), // blue
    Color(0xFF5E5CE6), // indigo
    Color(0xFFAF52DE), // purple
    Color(0xFFFF375F), // pink
    Color(0xFFFF453A), // red
    Color(0xFFFF9F0A), // orange
    Color(0xFFFFD60A), // yellow
    Color(0xFF32D74B), // green
    Color(0xFF63E6BE), // mint
    Color(0xFF64D2FF), // cyan
    Color(0xFF8E8E93), // gray
    Color(0xFFA2845E), // brown
    Color(0xFF052F38), // deep teal
    Color(0xFF1C1C3A), // midnight
    Color(0xFF6B2737), // wine
    Color(0xFF000000), // solid black
)

// Canvas units. The iOS control is 44x44 preview, 28pt track, 30pt thumb; those
// are point sizes on a phone, and this canvas is a tablet, so they are nudged up
// to stay the same apparent size at arm's length.
private const val PREVIEW = 56
private const val TRACK_H = 30
private const val THUMB = 34
private const val ROW_H = 52
private const val CHECKER_CELL = 7

/** Six evenly spaced stops is what SwiftUI's rainbow uses, and it is enough: the
 *  gradient interpolates the rest and no banding shows at slider width. */
private val HUE_STOPS = List(7) { i -> hueColor(i * 60f) }

/**
 * The hue plus opacity picker. `onChange` fires continuously during a drag, so
 * whatever it writes to must be cheap to update; the whole point of the control
 * is that the surface behind it previews live rather than waiting for a commit.
 *
 * @param showOpacity false hides the opacity row entirely, for the call sites
 *   where alpha is meaningless (an opaque accent, say) and a slider that appears
 *   to do nothing would be worse than no slider.
 */
@Composable
fun ColorOpacityPicker(
    color: Color,
    onChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    showOpacity: Boolean = true,
) {
    // The hue is this control's own state, not a function of `color`, because a
    // colour does not always remember the hue it was dragged from: drop the
    // opacity to zero, or pick a grey, and HSV has nothing left to recover. The
    // iOS original has exactly this quirk (it derives hue once, on appear) and
    // it has never bothered anyone, so it is not worth a more elaborate model.
    var hue by remember { mutableFloatStateOf(hueOf(color)) }

    // Re-derive only when the value changed from outside, for instance the user
    // tapped a preset swatch. Comparing against our own last write rather than
    // diffing hues avoids fighting the drag, where every frame comes back to us
    // as a "new" colour that we in fact just produced.
    var lastEmitted by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(color) {
        if (color != lastEmitted) hue = hueOf(color)
    }

    fun emit(next: Color) {
        lastEmitted = next
        onChange(next)
    }

    Row(modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(du(20))) {
        Box(
            Modifier
                .size(du(PREVIEW))
                .clip(CircleShape)
                .border(du(2).coerceAtLeast(1.dp), DC.ink(0.5f), CircleShape),
        ) {
            // Checkerboard first, so a half-transparent choice reads as
            // see-through rather than merely darker against the dark panel.
            Checkerboard(Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(color))
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(du(10))) {
            SliderLabel("COLOR")
            GradientSlider(
                value = hue / 360f,
                thumbColor = hueColor(hue),
                onValue = { f ->
                    hue = f * 360f
                    // Alpha survives a hue change: the two sliders each own one
                    // axis, and neither may quietly reset the other.
                    emit(hueColor(hue, color.alpha))
                },
            ) {
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(HUE_STOPS)))
            }

            if (showOpacity) {
                SliderLabel("OPACITY  ${(color.alpha * 100).roundToInt()}%")
                GradientSlider(
                    value = color.alpha,
                    thumbColor = color,
                    // Only alpha moves here. Carrying the colour's real RGB
                    // through, instead of rebuilding it from `hue`, keeps a
                    // muted preset muted when its opacity is adjusted.
                    onValue = { f -> emit(color.copy(alpha = f)) },
                ) {
                    Checkerboard(Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(color.copy(alpha = 0f), color.copy(alpha = 1f)),
                            ),
                        ),
                    )
                }
            }
        }
    }
}

/** The shared preset swatch row, ported from TallyPalette.presetColorHexes. */
@Composable
fun ColorSwatchRow(
    selected: Color,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
    rainbowSelected: Boolean = false,
    onPickRainbow: (() -> Unit)? = null,
) {
    // Wraps rather than running in one line. A single row overflowed the sheet
    // and silently clipped the dark end of the palette, so black and the deep
    // tones could not be picked at all.
    // Rainbow rides along as the last swatch rather than sitting on its own
    // row: it is one of the choices, not a separate mode.
    val entries: List<Color?> = PRESET_COLORS + if (onPickRainbow != null) listOf(null) else emptyList()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(du(12))) {
        entries.chunked(9).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(du(14))) {
        row.forEach { entry ->
            if (entry == null) {
                RainbowSwatch(selected = rainbowSelected, onPick = { onPickRainbow?.invoke() })
                return@forEach
            }
            val swatch = entry
            // Alpha is ignored in the comparison: the presets are a hue choice,
            // and a swatch should still show as the current one after the
            // opacity slider has been moved. A rainbow selection means no
            // swatch is current.
            val isSelected = !rainbowSelected &&
                swatch.toArgb() and 0x00FFFFFF == selected.toArgb() and 0x00FFFFFF
            Box(
                Modifier
                    .size(du(40))
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                        du(if (isSelected) 3 else 1).coerceAtLeast(1.dp),
                        if (isSelected) DC.accent else DC.ink(0.18f),
                        CircleShape,
                    )
                    .clickable { onPick(swatch.copy(alpha = selected.alpha)) },
            )
        }
        }
        }
    }
}

/** Small, uppercase, wide-tracked and dim, matching the iOS control's captions. */
@Composable
private fun SliderLabel(text: String) {
    Text(
        text,
        style = TextStyle(
            color = DC.ink(0.5f),
            fontSize = su(13),
            letterSpacing = su(13 * 0.16f),
        ),
    )
}

/**
 * One horizontal slider with a caller-drawn track, because neither track here
 * (a rainbow, a fade over a checkerboard) can be expressed as a Material slider's
 * colours.
 *
 * The gesture is the load-bearing part. This control lives inside a pane that
 * carries its own full-screen drag-to-navigate gesture, and Compose hands a
 * pointer to the ancestor as soon as a descendant leaves any of it unclaimed.
 * So the very first `awaitFirstDown` is consumed, before we know whether the
 * user meant a tap or a drag, and every subsequent change is consumed while the
 * pointer stays down. Consuming later, on slop or on the first move, is too
 * late: the pane has already taken the gesture and the slider answers taps only.
 *
 * @param value 0..1, always, whatever the caller's real unit is.
 */
@Composable
private fun GradientSlider(
    value: Float,
    thumbColor: Color,
    onValue: (Float) -> Unit,
    track: @Composable () -> Unit,
) {
    var widthPx by remember { mutableIntStateOf(1) }
    val shape = RoundedCornerShape(50)
    val thumbPx = with(LocalDensity.current) { du(THUMB).toPx() }

    Box(
        Modifier
            .fillMaxWidth()
            // The visible track is thin, but a thin touch target on a tablet at
            // arm's length is not a control. 44dp is the floor even when the
            // design scale shrinks everything else.
            .height(du(ROW_H).coerceAtLeast(44.dp))
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onValue((down.position.x / widthPx).coerceIn(0f, 1f))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        onValue((change.position.x / widthPx).coerceIn(0f, 1f))
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(du(TRACK_H).coerceAtLeast(12.dp))
                .clip(shape)
                .border(du(1).coerceAtLeast(1.dp), DC.ink(0.28f), shape),
        ) {
            track()
        }

        // Kept a half-thumb clear of each end so the thumb never hangs off the
        // track, which is what SwiftUI's own sliders do and why the extremes
        // still look deliberate.
        val half = thumbPx / 2f
        val centre = (value.coerceIn(0f, 1f) * widthPx).coerceIn(half, (widthPx - half).coerceAtLeast(half))
        Box(
            Modifier
                .offset { IntOffset((centre - half).roundToInt(), 0) }
                .size(du(THUMB))
                .clip(CircleShape)
                .background(thumbColor)
                .border(du(3).coerceAtLeast(2.dp), DC.ink, CircleShape),
        )
    }
}

/**
 * Drawn, not an asset: the pattern has to sit behind tracks of unknown width and
 * a round swatch, and a bitmap would either tile visibly or need a nine-patch
 * for something a dozen lines of Canvas already does exactly.
 */
@Composable
private fun Checkerboard(modifier: Modifier = Modifier) {
    val cell = with(LocalDensity.current) { du(CHECKER_CELL).toPx() }.coerceAtLeast(3f)
    Canvas(modifier) {
        drawRect(Color.Black.copy(alpha = 0.55f))
        var row = 0
        var y = 0f
        while (y < size.height) {
            var col = 0
            var x = 0f
            while (x < size.width) {
                if ((row + col) % 2 == 0) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.9f),
                        topLeft = Offset(x, y),
                        // Clipped at the edges so a partial cell does not spill
                        // outside the swatch's circular clip.
                        size = Size(min(cell, size.width - x), min(cell, size.height - y)),
                    )
                }
                x += cell
                col++
            }
            y += cell
            row++
        }
    }
}

/** Fully saturated, fully bright colour at `hue` degrees, the hue slider's own value. */
private fun hueColor(hue: Float, alpha: Float = 1f): Color {
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), 1f, 1f))
    return Color(argb).copy(alpha = alpha)
}

/** Lossy for greys and for black, which report hue 0 (red); see ColorOpacityPicker. */
private fun hueOf(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

/**
 * The rainbow choice, offered at the end of the palette because it is not a
 * colour so much as the absence of picking one: whatever it is applied to
 * cycles through the spectrum instead of holding still.
 */
@Composable
fun RainbowSwatch(
    selected: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(du(40))
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    (0..6).map { Color.hsv((it * 60f) % 360f, 0.85f, 1f) }
                )
            )
            .border(
                du(if (selected) 3 else 1).coerceAtLeast(1.dp),
                if (selected) DC.ink else DC.ink(0.18f),
                CircleShape,
            )
            .clickable(onClick = onPick),
    )
}
