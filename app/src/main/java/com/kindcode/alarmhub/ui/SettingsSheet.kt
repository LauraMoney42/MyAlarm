package com.kindcode.alarmhub.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kindcode.alarmhub.data.ClockStyle
import com.kindcode.alarmhub.data.DisplayConfig
import com.kindcode.alarmhub.kiosk.Kiosk

/**
 * Shown as plain text rather than a link: in lock task mode the app cannot hand
 * off to a browser or mail client, so a tappable link would be a dead end.
 * Change this one constant to change the address the app advertises.
 */
private const val SUPPORT_CONTACT = "kindcodedevelopment@gmail.com"

@Composable
fun SettingsSheet(
    display: DisplayConfig,
    accent: Color,
    previewHh: String,
    previewMm: String,
    previewMeridiem: String?,
    rainbowPhase: Float,
    onChange: (DisplayConfig) -> Unit,
    onPreviewWake: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val deviceOwner = activity?.let { Kiosk.isDeviceOwner(it) } == true
    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF050506).copy(alpha = 0.86f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(du(760)).heightIn(max = du(740))
                .clip(RoundedCornerShape(du(22)))
                .background(Color(0xFF141418))
                .pointerInput(Unit) { detectTapGestures { } }
                .border(du(1).coerceAtLeast(1.dp), DC.ink(0.1f), RoundedCornerShape(du(22)))
                .padding(horizontal = du(44), vertical = du(34)),
            verticalArrangement = Arrangement.spacedBy(du(26)),
        ) {
            ClockFacePreview(
                hh = previewHh,
                mm = previewMm,
                meridiem = previewMeridiem,
                style = display.clockStyle,
                digitColor = Color(display.digitColor),
                surfaceColor = Color(display.backgroundColor),
                pageColor = if (display.clockStyle == ClockStyle.FLIP) {
                    DC.bg
                } else Color(display.backgroundColor),
                rainbow = display.digitRainbow,
                rainbowPhase = rainbowPhase,
                rainbowSpread = display.rainbowSpread,
                rainbowStatic = display.rainbowStatic,
                reducedMotion = display.reducedMotion,
            )

            // Pinned above the scroll. The whole point is to watch the face
            // while changing it, which does not work if it scrolls away.
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(du(26)),
            ) {

                SectionLabel("CLOCK FACE", 15, 0.5f)
                Row(horizontalArrangement = Arrangement.spacedBy(du(12))) {
                    Chip("Flip", display.clockStyle == ClockStyle.FLIP, accent, hPad = 30, fontSize = 17) {
                        onChange(display.copy(clockStyle = ClockStyle.FLIP))
                    }
                    Chip("LED", display.clockStyle == ClockStyle.SEGMENT, accent, hPad = 30, fontSize = 17) {
                        onChange(display.copy(clockStyle = ClockStyle.SEGMENT))
                    }
                    Chip("Neon", display.clockStyle == ClockStyle.NEON, accent, hPad = 30, fontSize = 17) {
                        onChange(display.copy(clockStyle = ClockStyle.NEON))
                    }
                }

                Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.08f)))

                SectionLabel("DISPLAY", 15, 0.5f)

                Column(verticalArrangement = Arrangement.spacedBy(du(10))) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Body("Brightness")
                        Body("${(display.dayBrightness * 100).toInt()}%", DC.ink(0.45f))
                    }
                    Track(display.dayBrightness, accent) {
                        // Below about 5% the panel is unreadable in daylight, and the
                        // night scrim is the control for getting darker than that.
                        onChange(display.copy(dayBrightness = it.coerceIn(0.05f, 1f)))
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Body("24 hour clock")
                    Toggle(display.use24Hour, accent, width = 64) {
                        onChange(display.copy(use24Hour = !display.use24Hour))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(du(10))) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Body("Night darkness")
                        Body("${(display.nightDim * 100).toInt()}%", DC.ink(0.45f))
                    }
                    Track(display.nightDim, accent) { onChange(display.copy(nightDim = it)) }
                    Caption("How far the screen is painted over once the clock sits idle at night.")
                }

                Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.08f)))

                SectionLabel("COLORS", 15, 0.5f)

                // One picker, three targets. Three stacked pickers would be the
                // obvious layout and would push everything else off the sheet.
                var target by remember { mutableIntStateOf(0) }
                Row(horizontalArrangement = Arrangement.spacedBy(du(10))) {
                    listOf("Numbers", "Background", "Accent", "Wake light").forEachIndexed { index, name ->
                        Chip(name, target == index, accent, hPad = 22, fontSize = 16) { target = index }
                    }
                }

                val current = when (target) {
                    0 -> Color(display.digitColor)
                    1 -> Color(display.backgroundColor)
                    2 -> Color(display.accentColor)
                    else -> Color(display.wakeColor)
                }
                val rainbowOn = when (target) {
                    0 -> display.digitRainbow
                    1 -> display.surfaceRainbow
                    2 -> display.accentRainbow
                    else -> display.wakeRainbow
                }

                // Choosing a colour always turns rainbow off for that target: the
                // two are the same setting, not two settings that could disagree.
                fun apply(c: Color) {
                    onChange(
                        when (target) {
                            0 -> display.copy(digitColor = c.toArgb(), digitRainbow = false)
                            1 -> display.copy(backgroundColor = c.toArgb(), surfaceRainbow = false)
                            2 -> display.copy(accentColor = c.toArgb(), accentRainbow = false)
                            else -> display.copy(wakeColor = c.toArgb(), wakeRainbow = false)
                        }
                    )
                }

                // Opacity only means something for the numbers, where it dims them
                // against the background. On the background itself and on the accent
                // it would be a control that visibly does nothing.
                ColorOpacityPicker(
                    color = current,
                    onChange = ::apply,
                    showOpacity = target == 0,
                )
                ColorSwatchRow(
                    selected = current,
                    onPick = ::apply,
                    rainbowSelected = rainbowOn,
                    onPickRainbow = {
                        onChange(
                            when (target) {
                                0 -> display.copy(digitRainbow = true)
                                1 -> display.copy(surfaceRainbow = true)
                                2 -> display.copy(accentRainbow = true)
                                else -> display.copy(wakeRainbow = true)
                            }
                        )
                    },
                )
                if (rainbowOn) {
                    val isStatic = target == 0 && display.rainbowStatic

                    if (target == 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(du(10))) {
                            Chip("Cycling", !display.rainbowStatic, accent, hPad = 24, fontSize = 16) {
                                onChange(display.copy(rainbowStatic = false))
                            }
                            Chip("Static", display.rainbowStatic, accent, hPad = 24, fontSize = 16) {
                                onChange(display.copy(rainbowStatic = true))
                            }
                        }
                        Caption(
                            if (isStatic) {
                                "The spectrum is laid across the display and stays put."
                            } else {
                                "The whole spread drifts round the wheel. Pick a colour to stop."
                            }
                        )
                    } else {
                        Caption("Cycling through the spectrum. Pick a colour to stop.")
                    }

                    if (!isStatic) {
                        Column(verticalArrangement = Arrangement.spacedBy(du(10))) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Body("Cycle")
                                Body(
                                    if (display.rainbowSeconds >= 60) {
                                        "%.1f min".format(display.rainbowSeconds / 60f)
                                    } else "${display.rainbowSeconds}s",
                                    DC.ink(0.45f),
                                )
                            }
                            Track(((display.rainbowSeconds - 20) / 880f).coerceIn(0f, 1f), accent) {
                                onChange(display.copy(rainbowSeconds = (20 + it * 880f).toInt()))
                            }
                        }
                    }

                    if (target == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(du(10))) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Body(if (isStatic) "Width" else "Spread")
                                Body(
                                    if (display.rainbowSpread < 1f) "solid"
                                    else if (isStatic) "${(display.rainbowSpread * 4f).toInt()}\u00B0"
                                    else "${display.rainbowSpread.toInt()}\u00B0",
                                    DC.ink(0.45f),
                                )
                            }
                            Track((display.rainbowSpread / 90f).coerceIn(0f, 1f), accent) {
                                onChange(display.copy(rainbowSpread = it * 90f))
                            }
                            Caption(
                                when {
                                    display.rainbowSpread < 1f -> "All digits share one colour."
                                    isStatic -> "How much of the wheel the spread covers, starting from the colour above."
                                    else -> "How far apart the digits sit on the colour wheel."
                                }
                            )
                        }
                    }
                }

                if (target == 3) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(du(16)),
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(du(40)))
                                .background(accent)
                                .clickable(onClick = onPreviewWake)
                                .padding(horizontal = du(30), vertical = du(14)),
                        ) {
                            Text(
                                "PREVIEW WAKE LIGHT",
                                style = TextStyle(
                                    color = Color(0xFF0B0B0C),
                                    fontSize = su(16),
                                    letterSpacing = su(16 * 0.16f),
                                ),
                            )
                        }
                        Caption("Opens the wake screen so you can see the whole ramp.")
                    }
                }

                Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.08f)))

                SectionLabel("KIOSK", 15, 0.5f)

                var homeAlias by remember(activity) {
                    mutableStateOf(activity?.let { Kiosk.isHomeAliasEnabled(it) } == true)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(du(500)), verticalArrangement = Arrangement.spacedBy(du(6))) {
                        Body("Kiosk mode")
                        Caption(
                            if (homeAlias) {
                                "The clock is the home screen. Home returns here and Back " +
                                    "does nothing, so it cannot be closed by accident."
                            } else {
                                "Off. The tablet uses its normal launcher and the clock can " +
                                    "be exited like any other app."
                            }
                        )
                    }
                    Toggle(homeAlias, accent, width = 64) {
                        val next = !homeAlias
                        activity?.let { Kiosk.setKiosk(it, next) }
                        homeAlias = next
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(du(500)), verticalArrangement = Arrangement.spacedBy(du(6))) {
                        Body(if (deviceOwner) "System bars blocked" else "System bars can still flash")
                        Caption(
                            if (deviceOwner) {
                                "Lock task is active. Releasing hands the tablet back and " +
                                    "cannot be undone from here."
                            } else {
                                "An edge swipe can still reveal the navigation bar. Blocking " +
                                    "it entirely needs device owner, granted once over adb."
                            }
                        )
                    }
                    if (deviceOwner && activity != null) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(du(40)))
                                .border(
                                    du(1).coerceAtLeast(1.dp),
                                    Color(0xFFC47B6A),
                                    RoundedCornerShape(du(40)),
                                )
                                .clickable { Kiosk.release(activity) }
                                .padding(horizontal = du(26), vertical = du(14)),
                        ) {
                            Text(
                                "RELEASE",
                                style = TextStyle(
                                    color = Color(0xFFC47B6A),
                                    fontSize = su(15),
                                    letterSpacing = su(15 * 0.2f),
                                ),
                            )
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.08f)))

                SectionLabel("ABOUT", 15, 0.5f)
                Column(verticalArrangement = Arrangement.spacedBy(du(8))) {
                    Body("AlarmHub $version")
                    Caption("Created by KindCode")
                    Caption(
                        "Bugs and feature requests: $SUPPORT_CONTACT"
                    )
                    Caption(
                        "Runs entirely offline. No internet, location, camera or " +
                            "microphone permission is requested."
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(du(40)))
                            .background(accent)
                            .clickable(onClick = onClose)
                            .padding(horizontal = du(32), vertical = du(14)),
                    ) {
                        Text(
                            "DONE",
                            style = TextStyle(
                                color = Color(0xFF0B0B0C),
                                fontSize = su(16),
                                letterSpacing = su(16 * 0.2f),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Body(text: String, colour: Color = DC.ink) {
    Text(text, style = TextStyle(color = colour, fontSize = su(20), fontWeight = FontWeight.Light))
}

@Composable
private fun Caption(text: String) {
    Text(text, style = TextStyle(color = DC.ink(0.4f), fontSize = su(15), fontWeight = FontWeight.Light))
}

@Composable
private fun Track(value: Float, accent: Color, onChange: (Float) -> Unit) {
    // pointerInput(Unit) remembers its block from the first composition, so a
    // callback captured directly inside it keeps writing an old snapshot of the
    // settings back. That is what made changing one slider silently revert
    // another setting. rememberUpdatedState keeps the latest one in reach.
    val emit by rememberUpdatedState(onChange)

    var widthPx by remember { mutableIntStateOf(1) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(du(44))
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    emit((down.position.x / widthPx).coerceIn(0f, 0.92f))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        emit((change.position.x / widthPx).coerceIn(0f, 0.92f))
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(du(4).coerceAtLeast(2.dp))
                .clip(RoundedCornerShape(50))
                .background(DC.ink(0.1f))
        )
        Box(
            Modifier
                .fillMaxWidth(value)
                .height(du(4).coerceAtLeast(2.dp))
                .clip(RoundedCornerShape(50))
                .background(accent)
        )
        Box(Modifier.fillMaxWidth(value), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.size(du(18)).clip(RoundedCornerShape(50)).background(DC.ink))
        }
    }
}
