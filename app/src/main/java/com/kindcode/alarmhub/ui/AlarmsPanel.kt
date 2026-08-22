package com.kindcode.alarmhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kindcode.alarmhub.audio.TonePreview
import com.kindcode.alarmhub.data.AlarmTone
import com.kindcode.alarmhub.data.ALARM_TONES
import com.kindcode.alarmhub.data.Alarm
import kotlinx.coroutines.delay
import java.util.Locale

private val DAY_INITIALS = listOf("M", "T", "W", "T", "F", "S", "S")
private val LEADS = listOf(5, 10, 15, 30)

@Composable
fun AlarmsPanel(
    alarms: List<Alarm>,
    accent: Color,
    use24Hour: Boolean,
    onChange: (List<Alarm>) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    var editing by remember { mutableStateOf<Alarm?>(null) }

    Box(Modifier.fillMaxSize().background(DC.panel)) {
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier.fillMaxWidth().padding(horizontal = du(64), vertical = du(26)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("ALARMS", 15, 0.5f)
            }

            // Only scroll once the list genuinely overflows. A scroll container
            // consumes every vertical drag inside it, which silently ate the
            // "swipe down to close" gesture the header advertises. Four rows fit
            // the panel, and that covers essentially every real alarm list.
            val scrollable = alarms.size > 4
            Column(
                Modifier
                    .weight(1f)
                    .then(
                        if (scrollable) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    )
                    .padding(horizontal = du(64)),
            ) {
                alarms.forEach { alarm ->
                    SwipeToDelete(
                        onDelete = { onChange(alarms.filterNot { it.id == alarm.id }) },
                        onTap = { editing = alarm },
                    ) {
                        AlarmRow(alarm, accent, use24Hour) {
                            onChange(
                                alarms.map {
                                    if (it.id == alarm.id) it.copy(enabled = !it.enabled) else it
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(du(40)))
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = du(64), vertical = du(30)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(du(14)),
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(du(12)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(du(34)))
                        .background(accent)
                        .clickable {
                            editing = Alarm(
                                id = 0L, hour = 6, minute = 45,
                                days = setOf(1, 2, 3, 4, 5), label = "New alarm",
                            )
                        }
                        .padding(start = du(22), end = du(28), top = du(16), bottom = du(16)),
                ) {
                    Box(
                        Modifier
                            .size(du(18))
                            .clip(RoundedCornerShape(50))
                            .border(du(2).coerceAtLeast(1.dp), Color(0xFF0B0B0C), RoundedCornerShape(50))
                    )
                    Text(
                        "Set an alarm",
                        style = TextStyle(color = Color(0xFF0B0B0C), fontSize = su(20)),
                    )
                }
                Box(
                    Modifier
                        .size(du(54))
                        .clip(RoundedCornerShape(50))
                        .background(DC.ink(0.09f))
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\u2699", style = TextStyle(color = DC.ink(0.6f), fontSize = su(21)))
                }
                }
                Text(
                    "FADE-IN 30S · WAKE LIGHT PER ALARM",
                    style = TextStyle(
                        color = DC.ink(0.3f),
                        fontSize = su(14),
                        letterSpacing = su(14 * 0.16f),
                    ),
                )
            }
        }

        CloseGrip(vertical = false, modifier = Modifier.align(Alignment.TopCenter)) { onClose() }

        editing?.let { draft ->
            AlarmEditor(
                draft = draft,
                accent = accent,
                onCancel = { editing = null },
                onSave = { saved ->
                    onChange(
                        if (saved.id == 0L) {
                            alarms + saved.copy(id = System.currentTimeMillis())
                        } else {
                            alarms.map { if (it.id == saved.id) saved else it }
                        }
                    )
                    editing = null
                },
                onDelete = if (draft.id == 0L) null else {
                    {
                        onChange(alarms.filterNot { it.id == draft.id })
                        editing = null
                    }
                },
            )
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    accent: Color,
    use24Hour: Boolean,
    onToggle: () -> Unit,
) {
    val on = alarm.enabled
    val hour = if (use24Hour) alarm.hour else (alarm.hour % 12).let { if (it == 0) 12 else it }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = du(8), vertical = du(22)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.width(du(340)),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(du(12)),
            ) {
                Text(
                    "$hour:${"%02d".format(alarm.minute)}",
                    style = TextStyle(
                        color = if (on) DC.ink else DC.ink(0.3f),
                        fontSize = su(86),
                        lineHeight = su(86),
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = su(-86 * 0.035f),
                    ),
                )
                if (!use24Hour) {
                    Text(
                        if (alarm.hour < 12) "AM" else "PM",
                        style = TextStyle(
                            color = if (on) DC.ink(0.55f) else DC.ink(0.26f),
                            fontSize = su(26),
                            fontWeight = FontWeight.Light,
                        ),
                        modifier = Modifier.padding(bottom = du(10)),
                    )
                }
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(du(5)),
            ) {
                Text(
                    alarm.label.ifBlank { alarm.whenLabel() },
                    style = TextStyle(
                        color = if (on) DC.ink(0.85f) else DC.ink(0.34f),
                        fontSize = su(26),
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
                Text(
                    alarm.whenLabel().uppercase(Locale.getDefault()) + " · " +
                        alarm.tone.uppercase(Locale.getDefault()) +
                        if (alarm.wakeLight) " · LIGHT ${alarm.leadMinutes}M" else "",
                    style = TextStyle(
                        color = DC.ink(0.36f),
                        fontSize = su(14),
                        letterSpacing = su(14 * 0.2f),
                    ),
                )
            }
            Box(Modifier.width(du(92)), contentAlignment = Alignment.CenterEnd) {
                Toggle(on, accent, onToggle = onToggle)
            }
        }
        Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.09f)))
    }
}

@Composable
private fun AlarmEditor(
    draft: Alarm,
    accent: Color,
    onCancel: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var a by remember(draft.id) { mutableStateOf(draft) }

    // Whatever is previewing has to die with the sheet, or a tone keeps playing
    // over an app the user has already moved on from.
    DisposableEffect(Unit) { onDispose { TonePreview.stop() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF050506).copy(alpha = 0.86f))
            // Swallow taps so they never reach the list behind the sheet.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(du(1120))
                .clip(RoundedCornerShape(du(22)))
                .background(Color(0xFF141418))
                .border(du(1).coerceAtLeast(1.dp), DC.ink(0.1f), RoundedCornerShape(du(22)))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = du(44), vertical = du(30)),
            verticalArrangement = Arrangement.spacedBy(du(20)),
        ) {
            SectionLabel(if (draft.id == 0L) "NEW ALARM" else "EDIT ALARM", 19, 0.5f)

            BasicTextField(
                value = a.label,
                onValueChange = { a = a.copy(label = it.take(28)) },
                singleLine = true,
                textStyle = TextStyle(color = DC.ink, fontSize = su(30)),
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(du(12)))
                    .background(DC.ink(0.06f))
                    .padding(horizontal = du(20), vertical = du(16)),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (a.label.isEmpty()) {
                                Text(
                                    "Name this alarm, for example SCHOOL",
                                    style = TextStyle(color = DC.ink(0.3f), fontSize = su(30)),
                                )
                            }
                            inner()
                        }
                        if (a.label.isNotEmpty()) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { a = a.copy(label = "") }
                                    .padding(du(8)),
                            ) {
                                ClearIcon(du(30))
                            }
                        }
                    }
                },
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Stepper(
                    value = "%02d".format((a.hour % 12).let { if (it == 0) 12 else it }),
                    onUp = { a = a.copy(hour = (a.hour + 1) % 24) },
                    onDown = { a = a.copy(hour = (a.hour + 23) % 24) },
                )
                Text(
                    ":",
                    style = TextStyle(
                        color = DC.ink(0.24f),
                        fontSize = su(80),
                        fontWeight = FontWeight.ExtraLight,
                    ),
                    modifier = Modifier.padding(horizontal = du(11)),
                )
                Stepper(
                    value = "%02d".format(a.minute),
                    onUp = { a = a.copy(minute = (a.minute + 1) % 60) },
                    onDown = { a = a.copy(minute = (a.minute + 59) % 60) },
                )
                Column(
                    Modifier.padding(start = du(14)),
                    verticalArrangement = Arrangement.spacedBy(du(8)),
                ) {
                    MeridiemButton("AM", a.hour < 12, accent) {
                        if (a.hour >= 12) a = a.copy(hour = a.hour - 12)
                    }
                    MeridiemButton("PM", a.hour >= 12, accent) {
                        if (a.hour < 12) a = a.copy(hour = a.hour + 12)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(du(10), Alignment.CenterHorizontally),
            ) {
                DAY_INITIALS.forEachIndexed { index, initial ->
                    val day = index + 1
                    val on = day in a.days
                    Box(
                        Modifier
                            .size(du(68))
                            .clip(RoundedCornerShape(50))
                            .background(if (on) accent else Color.Transparent)
                            .border(
                                du(1).coerceAtLeast(1.dp),
                                if (on) accent else DC.ink(0.16f),
                                RoundedCornerShape(50),
                            )
                            .clickable {
                                a = a.copy(
                                    days = a.days.toMutableSet().apply {
                                        if (on) remove(day) else add(day)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initial,
                            style = TextStyle(
                                color = if (on) Color(0xFF0B0B0C) else DC.ink(0.5f),
                                fontSize = su(22),
                            ),
                        )
                    }
                }
            }

            Divider()
            SectionLabel("ALARM SOUND", 18, 0.5f)
            val ctx = LocalContext.current
            FlowChips(ALARM_TONES, a.tone, accent) { name ->
                a = a.copy(tone = name)
                TonePreview.play(ctx, AlarmTone.byName(name))
            }
            Text(
                "${a.tone} · fades in over 30s starting at low volume",
                style = TextStyle(color = DC.ink(0.4f), fontSize = su(19)),
            )

            Divider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("WARM WAKE LIGHT", 18, 0.5f)
                Toggle(a.wakeLight, accent, width = 64) { a = a.copy(wakeLight = !a.wakeLight) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(du(12)),
            ) {
                Text(
                    "Starts before",
                    style = TextStyle(color = DC.ink(0.5f), fontSize = su(20)),
                    modifier = Modifier.width(du(170)),
                )
                LEADS.forEach { m ->
                    Chip(
                        label = "$m MIN",
                        selected = a.wakeLight && a.leadMinutes == m,
                        accent = accent,
                        hPad = 28, vPad = 16, fontSize = 22, tracking = 0.06f,
                    ) { a = a.copy(wakeLight = true, leadMinutes = m) }
                }
            }
            Text(
                if (a.wakeLight) {
                    val start = ((a.hour * 60 + a.minute) - a.leadMinutes + 1440) % 1440
                    val sh = (start / 60 % 12).let { if (it == 0) 12 else it }
                    val mer = if (start / 60 < 12) "AM" else "PM"
                    "Light begins at $sh:${"%02d".format(start % 60)} $mer in near darkness " +
                        "and reaches full warmth as the alarm sounds"
                } else {
                    "Screen stays dark until the alarm sounds"
                },
                style = TextStyle(color = DC.ink(0.4f), fontSize = su(19)),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = du(6)),
                horizontalArrangement = Arrangement.spacedBy(du(14), Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(du(40)))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = du(28), vertical = du(14)),
                    ) {
                        Text(
                            "DELETE",
                            style = TextStyle(
                                color = Color(0xFFC47B6A),
                                fontSize = su(20),
                                letterSpacing = su(20 * 0.2f),
                            ),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(du(40)))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = du(28), vertical = du(14)),
                ) {
                    Text(
                        "CANCEL",
                        style = TextStyle(
                            color = DC.ink(0.55f),
                            fontSize = su(16),
                            letterSpacing = su(16 * 0.2f),
                        ),
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(du(40)))
                        .background(accent)
                        .clickable { onSave(a) }
                        .padding(horizontal = du(32), vertical = du(14)),
                ) {
                    Text(
                        "SAVE ALARM",
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
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(du(1).coerceAtLeast(1.dp)).background(DC.ink(0.08f)))
}

@Composable
private fun MeridiemButton(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(du(8)))
            .background(if (selected) accent else DC.ink(0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = du(26), vertical = du(14)),
    ) {
        Text(
            label,
            style = TextStyle(
                color = if (selected) Color(0xFF0B0B0C) else DC.ink(0.6f),
                fontSize = su(22),
                letterSpacing = su(22 * 0.14f),
            ),
        )
    }
}

@Composable
private fun FlowChips(
    options: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    // Two hand-laid rows rather than a flow layout: the tone list is fixed.
    Column(verticalArrangement = Arrangement.spacedBy(du(10))) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(du(10))) {
                row.forEach { name ->
                    Chip(
                        label = name,
                        selected = selected == name,
                        accent = accent,
                        hPad = 28, vPad = 16, fontSize = 22, tracking = 0f,
                    ) { onSelect(name) }
                }
            }
        }
    }
}

/**
 * Steps once on tap and auto-repeats while held. The first step fires straight
 * from the gesture: a tap short enough to flip the pressed flag inside a single
 * frame would otherwise be swallowed entirely.
 */
@Composable
private fun Stepper(value: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RepeatArrow("⌃", onUp)
        Text(
            value,
            style = TextStyle(
                color = DC.ink,
                fontSize = su(104),
                lineHeight = su(104),
                fontWeight = FontWeight.Light,
            ),
        )
        RepeatArrow("⌄", onDown)
    }
}

@Composable
private fun RepeatArrow(glyph: String, onClick: () -> Unit) {
    val action by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(420)
            while (pressed) {
                action()
                delay(70)
            }
        }
    }

    Box(
        Modifier
            .size(du(120), du(60))
            .clip(RoundedCornerShape(du(8)))
            .background(if (pressed) DC.ink(0.08f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        action()
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = TextStyle(color = DC.ink(0.4f), fontSize = su(30)))
    }
}

/**
 * Swipe an alarm left to reveal a bin, then either keep going to delete or tap
 * the bin. Matches the gesture people already know from iOS mail and alarms.
 *
 * The drag consumes from the first press so it never reaches the panel
 * navigation underneath, which would otherwise read a horizontal swipe here as
 * a request to change screens.
 */
@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    onTap: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { du(150).toPx() }
    val commitPx = with(density) { du(520).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val remove by rememberUpdatedState(onDelete)
    val tap by rememberUpdatedState(onTap)

    Box(Modifier.fillMaxWidth()) {
        // The bin only earns its space once the row has actually moved.
        if (offset.value < -2f) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(vertical = du(8))
                    .clip(RoundedCornerShape(du(12)))
                    .background(Color(0xFF7A2B24)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .padding(end = du(46))
                        .clip(RoundedCornerShape(50))
                        .clickable { remove() }
                        .padding(du(16)),
                ) {
                    BinIcon(size = du(44))
                }
            }
        }

        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                // detectHorizontalDragGestures rather than a hand-rolled loop:
                // it owns touch slop and consumption properly, which a manual
                // pointer loop here did not, so the drag never started.
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offset.value <= -commitPx -> remove()
                                offset.value <= -revealPx / 2f ->
                                    scope.launch { offset.animateTo(-revealPx) }
                                else -> scope.launch { offset.animateTo(0f) }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offset.snapTo(
                                (offset.value + dragAmount)
                                    .coerceIn(-commitPx * 1.2f, 0f)
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        // A tap closes the bin if it is showing, and otherwise
                        // opens the alarm for editing.
                        if (offset.value < -2f) scope.launch { offset.animateTo(0f) }
                        else tap()
                    }
                }
        ) { content() }
    }
}

/** Drawn rather than a glyph, so it cannot fall back to a missing-font box. */
@Composable
private fun BinIcon(size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f
        val bodyTop = h * 0.28f
        drawLine(Color.White, Offset(w * 0.10f, bodyTop), Offset(w * 0.90f, bodyTop), stroke)
        drawLine(Color.White, Offset(w * 0.38f, h * 0.14f), Offset(w * 0.62f, h * 0.14f), stroke)
        drawLine(Color.White, Offset(w * 0.38f, h * 0.14f), Offset(w * 0.38f, bodyTop), stroke)
        drawLine(Color.White, Offset(w * 0.62f, h * 0.14f), Offset(w * 0.62f, bodyTop), stroke)
        drawLine(Color.White, Offset(w * 0.20f, bodyTop), Offset(w * 0.27f, h * 0.90f), stroke)
        drawLine(Color.White, Offset(w * 0.80f, bodyTop), Offset(w * 0.73f, h * 0.90f), stroke)
        drawLine(Color.White, Offset(w * 0.27f, h * 0.90f), Offset(w * 0.73f, h * 0.90f), stroke)
        drawLine(Color.White, Offset(w * 0.42f, h * 0.42f), Offset(w * 0.44f, h * 0.78f), stroke * 0.8f)
        drawLine(Color.White, Offset(w * 0.58f, h * 0.42f), Offset(w * 0.56f, h * 0.78f), stroke * 0.8f)
    }
}

/** Drawn rather than a glyph, for the same reason as the bin. */
@Composable
private fun ClearIcon(size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val r = d / 2f
        val inset = d * 0.30f
        drawCircle(DC.ink(0.22f), radius = r)
        drawLine(
            DC.ink(0.75f),
            Offset(inset, inset), Offset(d - inset, d - inset),
            strokeWidth = d * 0.09f,
        )
        drawLine(
            DC.ink(0.75f),
            Offset(d - inset, inset), Offset(inset, d - inset),
            strokeWidth = d * 0.09f,
        )
    }
}
