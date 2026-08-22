package com.kindcode.alarmhub.ui

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kindcode.alarmhub.data.AUDIO_CREDITS
import com.kindcode.alarmhub.data.SleepConfig
import com.kindcode.alarmhub.data.SleepVoice
import java.util.Locale

private val DURATIONS = listOf(15, 30, 45, 60, 90, 0)

/**
 * The design's grid was two rows of four, for eight synthesised voices. Five
 * recorded loops joined them, and thirteen cards do not divide into rows of
 * four without a fourth row the 800px canvas has no room for. Five columns over
 * three rows fits, at the cost of a slightly shorter, narrower card.
 */
private const val COLUMNS = 5
private const val CARD_HEIGHT = 112

@Composable
fun SoundsPanel(
    cfg: SleepConfig,
    playing: Boolean,
    secondsLeft: Int,
    accent: Color,
    onChange: (SleepConfig) -> Unit,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(DC.panel)) {
        Column(Modifier.fillMaxSize().padding(horizontal = du(64), vertical = du(56))) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("SOUNDS", 12, 0.4f)
            }

            Spacer(Modifier.height(du(32)))

            // Hand-laid rather than a lazy grid: thirteen fixed items never
            // need recycling, and a LazyVerticalGrid would cost a scroll
            // container this panel does not want.
            SleepVoice.ALL.chunked(COLUMNS).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = du(16)),
                    horizontalArrangement = Arrangement.spacedBy(du(16)),
                ) {
                    row.forEach { voice ->
                        SoundCard(
                            voice = voice,
                            selected = cfg.voice == voice,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        ) { onChange(cfg.copy(voice = voice)) }
                    }
                    // Keeps the short final row's cards the same width as the
                    // full rows above rather than letting three cards stretch.
                    repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(du(20)))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(du(110))) { SectionLabel("PLAY FOR", 12, 0.36f) }
                Row(horizontalArrangement = Arrangement.spacedBy(du(10))) {
                    DURATIONS.forEach { m ->
                        Chip(
                            label = if (m == 0) "ALL NIGHT" else "$m MIN",
                            selected = cfg.durationMinutes == m,
                            accent = accent,
                        ) { onChange(cfg.copy(durationMinutes = m)) }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Seven of the nine recordings are CC0 and need nothing. These two
            // are CC BY 4.0, where the credit is a licence condition, not a
            // courtesy. audio/README.md carries the full provenance table.
            Text(
                AUDIO_CREDITS.joinToString("   ·   "),
                style = TextStyle(
                    color = DC.ink(0.26f),
                    fontSize = su(11),
                    letterSpacing = su(11 * 0.06f),
                ),
            )
            Spacer(Modifier.height(du(14)))

            Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.1f)))
            Spacer(Modifier.height(du(28)))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(du(32)),
            ) {
                Box(
                    Modifier
                        .size(du(82))
                        .clip(RoundedCornerShape(50))
                        .background(if (playing) accent else DC.ink(0.08f))
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (playing) "❙❙" else "▶",
                        style = TextStyle(
                            color = if (playing) Color(0xFF0B0B0C) else DC.ink(0.7f),
                            fontSize = su(26),
                        ),
                    )
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(du(12))) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            if (playing) cfg.voice.displayName else "Nothing playing",
                            style = TextStyle(color = DC.ink, fontSize = su(22)),
                        )
                        Text(
                            when {
                                !playing -> "—"
                                cfg.durationMinutes == 0 -> "∞"
                                else -> "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)
                            },
                            style = TextStyle(color = accent, fontSize = su(22)),
                        )
                    }
                    val total = cfg.durationMinutes * 60
                    val progress = if (playing && total > 0) {
                        (1f - secondsLeft.toFloat() / total).coerceIn(0f, 1f)
                    } else 0f
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(du(4).coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(50))
                            .background(DC.ink(0.1f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(accent)
                        )
                    }
                    Text(
                        when {
                            !playing -> "PICK A SOUND, THEN A DURATION"
                            cfg.durationMinutes == 0 -> "PLAYS UNTIL THE FIRST ALARM"
                            else -> "FADES OUT OVER THE LAST MINUTE"
                        },
                        style = TextStyle(
                            color = DC.ink(0.34f),
                            fontSize = su(12),
                            letterSpacing = su(12 * 0.18f),
                        ),
                    )
                }

                Row(
                    Modifier.width(du(230)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(du(14)),
                ) {
                    Text(
                        "VOL",
                        style = TextStyle(
                            color = DC.ink(0.34f),
                            fontSize = su(11),
                            letterSpacing = su(11 * 0.2f),
                        ),
                    )
                    VolumeBar(cfg.volume, Modifier.weight(1f)) { onChange(cfg.copy(volume = it)) }
                }
            }
        }
        CloseGrip(vertical = false, modifier = Modifier.align(Alignment.BottomCenter)) { onClose() }

    }
}

@Composable
private fun SoundCard(
    voice: SleepVoice,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(du(16))
    Column(
        modifier
            .height(du(CARD_HEIGHT))
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.14f) else DC.ink(0.045f))
            .border(du(1).coerceAtLeast(1.dp), if (selected) accent else DC.ink(0.08f), shape)
            .clickable(onClick = onSelect)
            .padding(horizontal = du(18), vertical = du(17)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            voice.tag,
            style = TextStyle(
                color = if (selected) accent else DC.ink(0.32f),
                fontSize = su(11),
                letterSpacing = su(11 * 0.22f),
            ),
        )
        Text(
            voice.displayName,
            style = TextStyle(
                color = if (selected) DC.ink else DC.ink(0.72f),
                fontSize = su(21),
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun VolumeBar(value: Float, modifier: Modifier = Modifier, onChange: (Float) -> Unit) {
    var widthPx by remember { mutableIntStateOf(1) }
    Box(
        modifier
            // Taller than it looks: the visible track is 4dp, but a 4dp touch
            // target on a tablet at arm's length is not a control.
            .height(du(44))
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Consume from the very first press. Without this the drag
                    // travelled up to the panel navigation, so the slider only
                    // ever answered a clean tap and dragging it did nothing.
                    val down = awaitFirstDown()
                    down.consume()
                    onChange((down.position.x / widthPx).coerceIn(0.04f, 1f))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        onChange((change.position.x / widthPx).coerceIn(0.04f, 1f))
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
                .background(DC.ink(0.55f))
        )
        Box(Modifier.fillMaxWidth(value), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier
                    .size(du(18))
                    .clip(RoundedCornerShape(50))
                    .background(DC.ink)
            )
        }
    }
}
