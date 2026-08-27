package com.kindcode.alarmhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.util.Locale

/**
 * The wake screen. The gradient sits low and off the bottom edge so the light
 * reads as a sunrise coming up past the horizon rather than a lamp switching on.
 */
@Composable
fun RingScreen(
    now: LocalDateTime,
    use24Hour: Boolean,
    label: String,
    soundName: String,
    wakeProgress: Float,
    baseColor: Color,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    val density = LocalDensity.current
    val hour = if (use24Hour) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val hh = if (use24Hour) String.format(Locale.US, "%02d", hour) else hour.toString()
    val mm = String.format(Locale.US, "%02d", now.minute)

    // Alternates roughly every 12 seconds, as in the design.
    val heading = if ((now.toLocalTime().toSecondOfDay() / 12) % 2 == 0) {
        label.uppercase(Locale.getDefault())
    } else {
        "GOOD MORNING, SUNSHINE!"
    }

    // The ramp is derived from one chosen hue rather than hardcoded, so the
    // wake light can be any colour and still travel the same path: pale and
    // desaturated at the top, saturated through the middle, nearly black at the
    // outer edge. Feeding it amber reproduces the original sunrise exactly.
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
    fun stop(hueShift: Float, sat: Float, value: Float) =
        Color.hsv((hsv[0] + hueShift + 360f) % 360f, sat, value)

    val glowOp = 0.05f + wakeProgress * wakeProgress * 0.95f
    val fieldOp = 0.18f + Math.pow(wakeProgress.toDouble(), 0.75).toFloat() * 0.82f

    Box(Modifier.fillMaxSize().background(Color(0xFF08060A))) {

        WakeField(baseColor = baseColor, progress = wakeProgress)

        Column(
            Modifier.fillMaxSize().padding(horizontal = du(70)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                heading,
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = su(18),
                    letterSpacing = su(18 * 0.4f),
                ),
            )
            Spacer(Modifier.height(du(34)))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BigTime(hh)
                Column(
                    Modifier.padding(horizontal = du(26)),
                    verticalArrangement = Arrangement.spacedBy(du(34)),
                ) {
                    ColonDot(); ColonDot()
                }
                BigTime(mm)
                if (!use24Hour) {
                    Spacer(Modifier.padding(horizontal = du(13)))
                    Text(
                        if (now.hour < 12) "AM" else "PM",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = su(66),
                            fontWeight = FontWeight.Light,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(du(34)))
            Text(
                "${soundName.uppercase(Locale.getDefault())}  ·  LIGHT ${(wakeProgress * 100).toInt()}%",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = su(22),
                    letterSpacing = su(22 * 0.24f),
                ),
            )

            Spacer(Modifier.height(du(60)))
            Row(horizontalArrangement = Arrangement.spacedBy(du(24))) {
                Pill(
                    "SNOOZE 9 MIN",
                    background = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    borderColor = Color.White.copy(alpha = 0.3f),
                    hPad = 58,
                    onClick = onSnooze,
                )
                Pill(
                    "STOP",
                    background = Color.White.copy(alpha = 0.92f),
                    contentColor = Color(0xFF3A1C08),
                    borderColor = null,
                    hPad = 68,
                    onClick = onStop,
                )
            }
        }

        Text(
            "WARM WAKE LIGHT · BRIGHTENS OVER THE LEAD TIME",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.5f),
                fontSize = su(13),
                letterSpacing = su(13 * 0.28f),
            ),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = du(34)),
        )
    }
}

@Composable
private fun BigTime(value: String) {
    Text(
        value,
        style = TextStyle(
            color = Color.White,
            fontSize = su(320),
            lineHeight = su(320 * 0.86f),
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = su(-320 * 0.05f),
        ),
    )
}

@Composable
private fun ColonDot() {
    Box(
        Modifier
            .size(du(20))
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.72f))
    )
}

@Composable
private fun Pill(
    label: String,
    background: Color,
    contentColor: Color,
    borderColor: Color?,
    hPad: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(du(52))
    Box(
        Modifier
            .clip(shape)
            .background(background)
            .then(if (borderColor != null) Modifier.border(du(1).coerceAtLeast(1.dp), borderColor, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = du(hPad), vertical = du(26)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = su(21),
                letterSpacing = su(21 * 0.12f),
            ),
        )
    }
}

/**
 * The warm light itself: a sunrise sitting low and mostly below the bottom
 * edge, so it reads as light coming up past a horizon rather than a lamp
 * switching on.
 *
 * Shared between the ring screen and the lead-in before it, because they are
 * the same light at different points in the same climb. Keeping them separate
 * meant the ramp only ever appeared once the alarm was already sounding, which
 * is exactly too late to be a wake light.
 */
@Composable
fun WakeField(
    baseColor: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
    fun stop(hueShift: Float, sat: Float, value: Float) =
        Color.hsv((hsv[0] + hueShift + 360f) % 360f, sat, value)

    val p = progress.coerceIn(0f, 1f)
    val fieldOp = 0.18f + Math.pow(p.toDouble(), 0.75).toFloat() * 0.82f
    val glowOp = 0.05f + p * p * 0.95f

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to stop(14f, 0.23f, 1.00f).copy(alpha = fieldOp),
                            0.22f to stop(8f, 0.60f, 1.00f).copy(alpha = fieldOp),
                            0.44f to stop(0f, 0.83f, 1.00f).copy(alpha = fieldOp),
                            0.66f to stop(-6f, 0.93f, 0.88f).copy(alpha = fieldOp),
                            0.86f to stop(-10f, 0.95f, 0.36f).copy(alpha = fieldOp),
                            1.00f to stop(-12f, 0.90f, 0.09f).copy(alpha = fieldOp),
                        ),
                        center = Offset(
                            with(density) { du(640).toPx() },
                            with(density) { du(928).toPx() },
                        ),
                        radius = with(density) { du(1050).toPx() },
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to stop(16f, 0.05f, 1.00f).copy(alpha = glowOp),
                            0.26f to stop(12f, 0.36f, 1.00f).copy(alpha = glowOp * 0.9f),
                            0.50f to stop(4f, 0.72f, 1.00f).copy(alpha = glowOp * 0.62f),
                            1.00f to stop(0f, 0.85f, 1.00f).copy(alpha = 0f),
                        ),
                        center = Offset(
                            with(density) { du(640).toPx() },
                            with(density) { du(1010).toPx() },
                        ),
                        radius = with(density) { du(760).toPx() },
                    )
                )
        )
    }
}
