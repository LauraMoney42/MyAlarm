package com.kindcode.alarmhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kindcode.alarmhub.data.ClockStyle
import java.time.LocalDateTime
import java.util.Locale

/**
 * Takes pre-derived strings rather than a LocalDateTime. The clock ticks every
 * second but only changes every minute, and passing `now` straight in made the
 * two large cards re-execute 60 times more often than they had any reason to.
 * With stable String parameters they skip instead.
 */
@Composable
fun ClockPane(
    hh: String,
    mm: String,
    meridiem: String?,
    dateLabel: String,
    style: ClockStyle,
    digitColor: Color,
    surfaceColor: Color,
    rainbow: Boolean,
    rainbowPhase: Float,
    rainbowSpread: Float,
    rainbowStatic: Boolean,
    reducedMotion: Boolean,
    accent: Color,
    nextAlarmLabel: String,
    soundStatus: String,
    soundPlaying: Boolean,
    onOpenSettings: () -> Unit,
) {

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Fixed height for both faces. Letting each face size itself moved
            // the status line up and down when the style changed.
            Box(Modifier.height(du(684)), contentAlignment = Alignment.Center) {
                ClockFace(
                    hh = hh, mm = mm, meridiem = meridiem, style = style,
                    digitColor = digitColor, surfaceColor = surfaceColor,
                    rainbow = rainbow, rainbowPhase = rainbowPhase,
                    rainbowSpread = rainbowSpread, rainbowStatic = rainbowStatic,
                    reducedMotion = reducedMotion,
                )
            }

            Spacer(Modifier.height(du(20)))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(du(22)),
            ) {
                StatusText(dateLabel)
                Dot()
                StatusText(nextAlarmLabel)
                Dot()
                StatusText(soundStatus, if (soundPlaying) accent else DC.ink(0.34f))
            }
        }

        // Deliberately small and dim. It is the only thing on the clock face
        // that is not the time, and it should never compete with it.
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = du(20), bottom = du(12))
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenSettings)
                .padding(du(12)),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2699", style = TextStyle(color = DC.ink(0.26f), fontSize = su(22)))
        }
    }
}

/**
 * The LED face: four seven-segment digits with the meridiem tucked to the left,
 * the way a moulded bedside clock lays it out.
 *
 * In rainbow mode each digit takes its own hue and the whole set drifts. The
 * drift is a pure function of [rainbowPhase], which the caller steps once per
 * second, so this stays as cheap to draw as the fixed-colour version.
 */
/**
 * Just the face, with no status line and no fixed height, so both the clock
 * itself and the live preview in settings render the identical thing. A
 * hand-drawn approximation in the settings sheet would drift out of step with
 * the real one the first time either changed.
 */
@Composable
fun ClockFace(
    hh: String,
    mm: String,
    meridiem: String?,
    style: ClockStyle,
    digitColor: Color,
    surfaceColor: Color,
    rainbow: Boolean,
    rainbowPhase: Float,
    rainbowSpread: Float,
    rainbowStatic: Boolean,
    reducedMotion: Boolean,
) {
    when (style) {
        ClockStyle.FLIP -> {
            fun cardColour(step: Int): Color = when {
                !rainbow -> digitColor
                rainbowStatic -> Color.hsv(
        (hueOf(digitColor) + step * rainbowSpread) % 360f, 0.85f, 1f,
                )
                else -> Color.hsv(
        ((rainbowPhase * 360f) + step * rainbowSpread) % 360f,
        0.85f, 1f,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(du(20)),
            ) {
                // Steps 0 and 2 so the two cards span the same slice
                // of the wheel the four LED digits do.
                FlipCard(
        hh, corner = meridiem, reducedMotion = reducedMotion,
        digitColor = cardColour(0), cardColor = surfaceColor,
                )
                FlipCard(
        mm, corner = null, reducedMotion = reducedMotion,
        digitColor = cardColour(2), cardColor = surfaceColor,
                )
            }
        }

        ClockStyle.SEGMENT -> SegmentFace(
            hh = hh,
            mm = mm,
            meridiem = meridiem,
            digitColor = digitColor,
            rainbow = rainbow,
            rainbowPhase = rainbowPhase,
            rainbowSpread = rainbowSpread,
            rainbowStatic = rainbowStatic,
        )
    }

}

/**
 * A scaled-down live face for the settings sheet. Changing a colour behind a
 * near-opaque sheet gave no feedback at all, so the sheet shows the result.
 */
@Composable
fun ClockFacePreview(
    hh: String,
    mm: String,
    meridiem: String?,
    style: ClockStyle,
    digitColor: Color,
    surfaceColor: Color,
    pageColor: Color,
    rainbow: Boolean,
    rainbowPhase: Float,
    rainbowSpread: Float,
    rainbowStatic: Boolean,
    reducedMotion: Boolean,
) {
    val boxH = du(250)
    Box(
        Modifier
            .fillMaxWidth()
            .height(boxH)
            .clip(RoundedCornerShape(du(16)))
            .background(pageColor),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Render at full size and scale the layer down, so the preview is
            // the same composable the clock uses rather than a smaller variant
            // with its own layout rules.
            val fullW = du(1240)
            val fullH = du(700)
            val scale = minOf(
                maxWidth.value / fullW.value,
                maxHeight.value / fullH.value,
            )
            Box(
                Modifier
                    .requiredSize(fullW, fullH)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                ClockFace(
                    hh = hh, mm = mm, meridiem = meridiem, style = style,
                    digitColor = digitColor, surfaceColor = surfaceColor,
                    rainbow = rainbow, rainbowPhase = rainbowPhase,
                    rainbowSpread = rainbowSpread, rainbowStatic = rainbowStatic,
                    reducedMotion = reducedMotion,
                )
            }
        }
    }
}

@Composable
private fun SegmentFace(
    hh: String,
    mm: String,
    meridiem: String?,
    digitColor: Color,
    rainbow: Boolean,
    rainbowPhase: Float,
    rainbowSpread: Float,
    rainbowStatic: Boolean,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // Everything is a multiple of digit height, and the height itself is
        // whatever lets four digits, a colon and the meridiem fit the width.
        val wRatio = 0.46f          // real LED digits are notably taller than wide
        val gapRatio = 0.09f
        val colonRatio = 0.14f
        val merRatio = if (meridiem == null) 0f else 0.30f

        val unitsWide = 4 * wRatio + 3 * gapRatio + colonRatio + merRatio +
            if (meridiem == null) 0f else 0.05f
        val avail = maxWidth.value * 0.90f
        val h = minOf(avail / unitsWide, maxHeight.value * 0.98f).dp

        val w = h * wRatio
        val gap = h * gapRatio

        val digits = listOf(hh.getOrNull(0), hh.getOrNull(1), mm.getOrNull(0), mm.getOrNull(1))
            .map { it?.digitToIntOrNull() }

        // Static mode tints the finished row with a single gradient, so the
        // spectrum runs continuously across the digits instead of stepping from
        // one solid digit to the next. The segments are drawn white first so the
        // tint lands at full strength.
        val span = (rainbowSpread * 4f).coerceIn(0f, 360f)
        val staticBrush = if (rainbow && rainbowStatic) {
            val start = hueOf(digitColor)
            Brush.horizontalGradient(
                (0..8).map { Color.hsv((start + it * span / 8f) % 360f, 0.88f, 1f) }
            )
        } else null

        fun colourFor(index: Int): Color = if (staticBrush != null) Color.White
        else if (!rainbow) digitColor else {
            // Spread the four digits across part of the wheel so they read as
            // one gradient rather than four unrelated colours.
            Color.hsv(((rainbowPhase * 360f) + index * rainbowSpread) % 360f, 0.85f, 1f)
        }

        // The meridiem rides inside the centred row, the way it is moulded into
        // the panel on a real clock. Positioning it absolutely to the left made
        // it drift into the screen-edge labels.
        // Nudged left. The meridiem block sits on the left of the row, so a
        // perfectly centred row still reads as sitting right of centre.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset(x = -h * 0.12f)
                .then(
                    if (staticBrush == null) Modifier else Modifier
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = staticBrush, blendMode = BlendMode.SrcIn)
                        }
                ),
        ) {
            if (meridiem != null) {
                // offset shifts where this draws without changing the width the
                // row reserves for it, so the digits do not move with it.
                Box(
                    Modifier
                        .width(h * merRatio)
                        .offset(x = h * 0.05f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        meridiem,
                        style = TextStyle(
                            color = colourFor(0),
                            fontSize = with(LocalDensity.current) { (h * 0.15f).toSp() },
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Spacer(Modifier.width(h * 0.05f))
            }
            // Unlit segments are drawn fully transparent. The ghost bars are
            // faithful to real hardware but read as smudges on a backlit LCD.
            val off = Color.Transparent
            SevenSegmentDigit(digits[0], colourFor(0), off, w, h)
            Spacer(Modifier.width(gap))
            SevenSegmentDigit(digits[1], colourFor(1), off, w, h)
            Spacer(Modifier.width(gap * 0.8f))
            SevenSegmentColon(colourFor(2), h)
            Spacer(Modifier.width(gap * 0.8f))
            SevenSegmentDigit(digits[2], colourFor(2), off, w, h)
            Spacer(Modifier.width(gap))
            SevenSegmentDigit(digits[3], colourFor(3), off, w, h)
        }
    }
}

@Composable
private fun StatusText(
    text: String,
    color: Color = DC.ink(0.34f),
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = su(15),
            fontWeight = FontWeight.Normal,
            letterSpacing = su(15 * 0.22f),
        ),
    )
}

@Composable
private fun Dot() {
    Box(
        Modifier
            .size(du(4))
            .clip(RoundedCornerShape(50))
            .background(DC.ink(0.24f))
    )
}

private val DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val MONTHS = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
)

fun dateLabel(now: LocalDateTime): String =
    "${DAYS[now.dayOfWeek.value - 1]} ${MONTHS[now.monthValue - 1]} ${now.dayOfMonth}"

/** Hue in degrees, used as the starting point for a static spread. */
private fun hueOf(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}
