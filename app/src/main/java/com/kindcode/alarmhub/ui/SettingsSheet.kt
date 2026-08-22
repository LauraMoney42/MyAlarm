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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onChange: (DisplayConfig) -> Unit,
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
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(du(700))
                .clip(RoundedCornerShape(du(22)))
                .background(Color(0xFF141418))
                .border(du(1).coerceAtLeast(1.dp), DC.ink(0.1f), RoundedCornerShape(du(22)))
                .padding(horizontal = du(44), vertical = du(34)),
            verticalArrangement = Arrangement.spacedBy(du(26)),
        ) {
            SectionLabel("DISPLAY", 15, 0.5f)

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

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Body("Accent")
                Row(horizontalArrangement = Arrangement.spacedBy(du(12))) {
                    DC.accents.forEachIndexed { index, colour ->
                        val on = display.accentIndex == index
                        Box(
                            Modifier
                                .size(du(38))
                                .clip(RoundedCornerShape(50))
                                .background(colour)
                                .border(
                                    du(if (on) 3 else 0).coerceAtLeast(0.dp),
                                    DC.ink,
                                    RoundedCornerShape(50),
                                )
                                .clickable { onChange(display.copy(accentIndex = index)) }
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.08f)))

            SectionLabel("KIOSK", 15, 0.5f)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(du(440)), verticalArrangement = Arrangement.spacedBy(du(6))) {
                    Body(if (deviceOwner) "Locked down" else "Not locked down")
                    Caption(
                        if (deviceOwner) {
                            "System bars are blocked entirely. Turning this off releases " +
                                "device owner and hands the tablet back."
                        } else {
                            "Home returns here and Back does nothing, but an edge swipe " +
                                "can still flash the system bars. Full lock needs device " +
                                "owner, set once over adb."
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
                    onChange((down.position.x / widthPx).coerceIn(0f, 0.92f))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        onChange((change.position.x / widthPx).coerceIn(0f, 0.92f))
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
