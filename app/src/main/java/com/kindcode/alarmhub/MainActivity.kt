package com.kindcode.alarmhub

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kindcode.alarmhub.alarm.AlarmRinger
import com.kindcode.alarmhub.alarm.AlarmScheduler
import com.kindcode.alarmhub.audio.SleepAudioService
import com.kindcode.alarmhub.data.ClockStyle
import com.kindcode.alarmhub.kiosk.Kiosk
import com.kindcode.alarmhub.ui.AlarmsPanel
import com.kindcode.alarmhub.ui.CalendarPanel
import com.kindcode.alarmhub.ui.ClockPane
import com.kindcode.alarmhub.ui.DC
import com.kindcode.alarmhub.ui.LightMode
import com.kindcode.alarmhub.ui.LocalDesignScale
import com.kindcode.alarmhub.ui.RingScreen
import com.kindcode.alarmhub.ui.SettingsSheet
import com.kindcode.alarmhub.ui.SoundsPanel
import com.kindcode.alarmhub.ui.WakeField
import com.kindcode.alarmhub.ui.WeatherPanel
import com.kindcode.alarmhub.ui.computeLight
import com.kindcode.alarmhub.ui.dateLabel
import com.kindcode.alarmhub.ui.du
import com.kindcode.alarmhub.ui.su
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.abs

private enum class View { CLOCK, CALENDAR, WEATHER, ALARMS, SOUNDS }

/**
 * How long the display stays awake-bright after the last touch. Fifteen seconds
 * was long enough to read the clock and far too short to change a setting.
 */
private const val IDLE_BEFORE_DIM_MS = 60_000L

/** Fraction of a swipe that commits to opening or closing a panel. */
private const val COMMIT = 0.32f

private val SETTLE = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Whether the clock is actually on screen. The alarm receiver uses this
         * to decide if it needs to fall back to a notification, so it must be
         * accurate rather than merely close.
         */
        @Volatile
        var onScreen: Boolean = false
            private set
    }

    private val app: AlarmHubApp get() = application as AlarmHubApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the keyguard and power the panel on when the alarm fires.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        // Transient bars come back on any edge swipe and then linger. Re-hide
        // them as soon as they report themselves visible, or the clock ends up
        // framed by a status bar and a nav bar all night.
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            if (insets.isVisible(WindowInsetsCompat.Type.systemBars())) hideSystemBars()
            insets
        }

        setContent {
            HubRoot(
                app = app,
                setBrightness = ::applyBrightness,
                hideBars = ::hideSystemBars,
            )
        }
    }

    override fun onPause() {
        onScreen = false
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        onScreen = true
        hideSystemBars()
        // Only does anything once the app is device owner. Idempotent, so it is
        // safe to re-assert every time the clock comes forward.
        Kiosk.enter(this)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Window-level brightness, not system brightness. Needs no WRITE_SETTINGS
     * permission and reverts the moment the app loses focus, so the app can
     * never strand the tablet at 4%.
     */
    private fun applyBrightness(value: Float) {
        val lp = window.attributes
        lp.screenBrightness = value.coerceIn(0.01f, 1f)
        window.attributes = lp
    }
}

@Composable
private fun HubRoot(
    app: AlarmHubApp,
    setBrightness: (Float) -> Unit,
    hideBars: () -> Unit,
) {

    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val alarms by app.prefs.alarms.collectAsState()
    val display by app.prefs.display.collectAsState()
    val sleep by app.prefs.sleep.collectAsState()
    val firingId by app.prefs.firingId.collectAsState()
    val snoozeAt by app.prefs.snoozeAt.collectAsState()
    val snoozeId by app.prefs.snoozeId.collectAsState()

    // Rainbow targets cycle off the same phase, offset from each other so the
    // numbers and the accent are never the same hue at the same moment.
    val anyRainbow = display.digitRainbow || display.surfaceRainbow || display.accentRainbow
    val accent = if (display.accentRainbow) DC.accent else Color(display.accentColor)

    var playStartedAt by remember { mutableLongStateOf(0L) }
    var settingsOpen by remember { mutableStateOf(false) }
    var lastTouchAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Single clock source for the whole tree, aligned to the second boundary so
    // cards flip when the wall clock does, not 300ms later.
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L - (System.currentTimeMillis() % 1000L))
        }
    }

    val lux = rememberAmbientLux(enabled = display.autoDim)

    // Lock task always keeps a back affordance reachable by design, so it
    // cannot be switched off, and the system reveals it on an edge swipe
    // whatever the app asks for. It can only be put back.
    //
    // Nothing is polled at rest. A reveal can only follow a touch, so the
    // sweep is keyed to the last one: hide at once, then a few more times
    // across the second that follows, which covers the system's own reveal
    // animation finishing after the finger has already lifted.
    LaunchedEffect(lastTouchAt) {
        hideBars()
        for (wait in listOf(120L, 260L, 500L, 900L)) {
            delay(wait)
            hideBars()
        }
    }

    // Recompute the visible strings only when the minute actually turns.
    val minuteKey = now.hour * 60 + now.minute
    val clock = remember(minuteKey, display.use24Hour) {
        val hour = if (display.use24Hour) now.hour else {
            (now.hour % 12).let { if (it == 0) 12 else it }
        }
        Triple(
            "%02d".format(hour),
            "%02d".format(now.minute),
            if (display.use24Hour) null else if (now.hour < 12) "AM" else "PM",
        )
    }
    val dayLabel = remember(now.dayOfYear) { dateLabel(now) }

    // One full hue cycle every four minutes, stepped once per second off the
    // tick that already exists. An actual animation would redraw continuously,
    // which is precisely the cost this GPU cannot carry all night.
    val rainbowPhase = if (anyRainbow) {
        val period = display.rainbowSeconds.coerceAtLeast(10)
        (now.toLocalTime().toSecondOfDay() % period) / period.toFloat()
    } else 0f

    val liveAccent = if (display.accentRainbow) {
        Color.hsv(((rainbowPhase * 360f) + 180f) % 360f, 0.72f, 0.95f)
    } else accent

    // Kept dim on purpose. This is the surface the numbers sit on, so a fully
    // saturated cycle behind them would bury the time.
    val surface = if (display.surfaceRainbow) {
        Color.hsv((rainbowPhase * 360f) % 360f, 0.55f, 0.22f)
    } else Color(display.backgroundColor)

    val wakeBase = if (display.wakeRainbow) {
        Color.hsv(((rainbowPhase * 360f) + 90f) % 360f, 0.85f, 1f)
    } else Color(display.wakeColor)

    val next = remember(alarms, snoozeAt, snoozeId, now.minute, now.hour) {
        AlarmScheduler.nextFire(alarms, now, snoozeAt, snoozeId)
    }
    val firingAlarm = alarms.firstOrNull { it.id == firingId }

    var view by remember { mutableStateOf(View.CLOCK) }
    var candidate by remember { mutableStateOf<View?>(null) }
    // While a finger is down the raw drag value is authoritative; the Animatable
    // only runs the settle afterwards. Driving one Animatable from both would
    // let a per-event snapTo cancel the settle mid-flight, which strands the
    // panel half open and never commits the new view.
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val anim = remember { Animatable(0f) }

    // Recomputed on every tick, since `now` is what drives this recomposition.
    @Suppress("UNUSED_EXPRESSION") now
    val idleFor = System.currentTimeMillis() - lastTouchAt

    val light = computeLight(
        now = now,
        nextFire = next?.at,
        // A snooze gets no ramp: floodlighting the room nine minutes after
        // someone asked for more sleep is not a feature.
        leadMinutes = if (next != null && !next.isSnooze && next.alarm.wakeLight) {
            next.alarm.leadMinutes
        } else 0,
        display = display,
        lux = lux,
        // Never dim while a panel is open: being in Alarms or Sounds at all
        // means someone is working, and there is no way to undo a dim from
        // inside a panel because its controls swallow the tap.
        boosted = view != View.CLOCK || idleFor < IDLE_BEFORE_DIM_MS,
    )

    val brightness by animateFloatAsState(
        targetValue = if (firingAlarm != null) 1f else light.brightness,
        animationSpec = tween(2500),
        label = "brightness",
    )
    val scrim by animateFloatAsState(
        targetValue = if (firingAlarm != null) 0f else light.scrim,
        animationSpec = tween(2500),
        label = "scrim",
    )
    LaunchedEffect(brightness) { setBrightness(brightness) }

    // Ring only while the persisted firing id is set, so a process restart
    // mid-alarm still rings and a stop always silences it.
    DisposableEffect(firingId) {
        if (firingId != 0L) {
            // Sleep sounds and an alarm tone at once is just noise. Whatever was
            // masking the night gives way to the thing meant to wake you.
            if (playStartedAt > 0L) {
                SleepAudioService.stop(context)
                playStartedAt = 0L
            }
            AlarmRinger.start(context, firingAlarm?.tone)
            // The full-screen intent has done its job of bringing us forward;
            // leaving the notification up just stacks a banner over the alarm
            // screen the user is already looking at.
        } else {
            AlarmRinger.stop()
        }
        onDispose { }
    }

    val playing = playStartedAt > 0L
    val secondsLeft = if (playing && sleep.durationMinutes > 0) {
        (sleep.durationMinutes * 60 -
            ((System.currentTimeMillis() - playStartedAt) / 1000).toInt()).coerceAtLeast(0)
    } else 0
    LaunchedEffect(playing, secondsLeft == 0, sleep.durationMinutes) {
        if (playing && sleep.durationMinutes > 0 && secondsLeft == 0) playStartedAt = 0L
    }


    // In LED mode the chosen colour is the whole screen. In flip mode it is the
    // flaps, and the page behind them stays dark so the cards still read as
    // physical objects sitting on a surface.
    val pageColor = if (display.clockStyle == ClockStyle.FLIP) DC.bg else surface

    BoxWithConstraints(Modifier.fillMaxSize().background(pageColor)) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        // The design is authored on a 1280x800 canvas; scale it once here.
        val scale = minOf(maxWidth.value / 1280f, maxHeight.value / 800f)

        CompositionLocalProvider(LocalDesignScale provides scale) {

            val active = if (view != View.CLOCK) view else candidate
            val p = if (dragging) dragProgress else anim.value

            fun settle(open: Boolean, destination: View) {
                dragging = false
                scope.launch {
                    anim.snapTo(dragProgress)
                    // Commit the destination before animating, so a cancelled
                    // animation can never leave the view state behind.
                    view = if (open) destination else View.CLOCK
                    anim.animateTo(if (open) 1f else 0f, tween(440, easing = SETTLE))
                    if (!open) candidate = null
                }
            }

            // Back closes an open panel and otherwise does nothing at all. On a
            // wall clock there is no "up" to navigate to, and finishing the
            // activity just drops the user on a launcher they did not want.
            BackHandler(enabled = true) {
                if (firingId == 0L && view != View.CLOCK) settle(false, View.CLOCK)
            }


            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(view, firingId, settingsOpen) {
                        if (firingId != 0L || settingsOpen) return@pointerInput
                        var acc = Offset.Zero
                        detectDragGestures(
                            onDragStart = { acc = Offset.Zero },
                            onDrag = { change, delta ->
                                change.consume()
                                acc += delta
                                if (view == View.CLOCK) {
                                    if (candidate == null) {
                                        candidate = when {
                                            abs(acc.x) > abs(acc.y) && acc.x < -8f -> View.WEATHER
                                            abs(acc.x) > abs(acc.y) && acc.x > 8f -> View.CALENDAR
                                            acc.y < -8f -> View.ALARMS
                                            acc.y > 8f -> View.SOUNDS
                                            else -> null
                                        }
                                    }
                                    val raw = when (candidate) {
                                        View.WEATHER -> -acc.x / (wPx * 0.5f)
                                        View.CALENDAR -> acc.x / (wPx * 0.5f)
                                        View.ALARMS -> -acc.y / (hPx * 0.5f)
                                        View.SOUNDS -> acc.y / (hPx * 0.5f)
                                        else -> 0f
                                    }
                                    dragging = true
                                    dragProgress = raw.coerceIn(0f, 1f)
                                } else {
                                    // A panel only closes on a drag along its own
                                    // axis. Without this, a mostly-sideways drag
                                    // on an alarm row still fed the close gesture,
                                    // and the panel started sliding away instead
                                    // of the row revealing its bin.
                                    val horizontalPanel =
                                        view == View.WEATHER || view == View.CALENDAR
                                    val alongAxis = if (horizontalPanel) {
                                        abs(acc.x) > abs(acc.y)
                                    } else {
                                        abs(acc.y) > abs(acc.x)
                                    }
                                    if (!alongAxis) return@detectDragGestures

                                    val back = when (view) {
                                        View.WEATHER -> acc.x / (wPx * 0.5f)
                                        View.CALENDAR -> -acc.x / (wPx * 0.5f)
                                        View.ALARMS -> acc.y / (hPx * 0.5f)
                                        View.SOUNDS -> -acc.y / (hPx * 0.5f)
                                        else -> 0f
                                    }
                                    dragging = true
                                    dragProgress = (1f - back.coerceAtLeast(0f)).coerceIn(0f, 1f)
                                }
                            },
                            onDragEnd = {
                                val c = candidate
                                when {
                                    view == View.CLOCK && c != null && dragProgress > COMMIT ->
                                        settle(true, c)
                                    view == View.CLOCK -> settle(false, View.CLOCK)
                                    dragProgress < 1f - COMMIT -> settle(false, View.CLOCK)
                                    else -> settle(true, view)
                                }
                            },
                            onDragCancel = {
                                if (view == View.CLOCK) settle(false, View.CLOCK)
                                else settle(true, view)
                            },
                        )
                    }
                    // Watch the Initial pass so every touch counts as activity,
                    // including ones a panel control goes on to consume. Nothing
                    // is consumed here, so no gesture is affected.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                lastTouchAt = System.currentTimeMillis()
                                // An edge swipe is exactly what summons the bar,
                                // so put it away again on the same gesture.
                                hideBars()
                            }
                        }
                    }
            ) {
                // The wake light, before the alarm rather than only during it.
                // It sits under the clock so the time stays readable while the
                // room fills.
                if (light.mode == LightMode.WAKE) {
                    WakeField(baseColor = wakeBase, progress = light.wakeProgress)
                }

                // Clock recedes rather than disappearing, as in the design.
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - p * 0.31f }) {
                    ClockPane(
                        hh = clock.first,
                        mm = clock.second,
                        meridiem = clock.third,
                        dateLabel = dayLabel,
                        style = display.clockStyle,
                        digitColor = Color(display.digitColor),
                        surfaceColor = surface,
                        rainbow = display.digitRainbow,
                        rainbowPhase = rainbowPhase,
                        rainbowSpread = display.rainbowSpread,
                        rainbowStatic = display.rainbowStatic,
                        reducedMotion = display.reducedMotion,
                        accent = liveAccent,
                        nextAlarmLabel = nextLabel(next, display.use24Hour),
                        soundStatus = soundStatus(playing, sleep, secondsLeft),
                        soundPlaying = playing,
                        onOpenSettings = { settingsOpen = true },
                    )
                }

                EdgeHints(alpha = (0.9f - p * 100f / 60f).coerceAtLeast(0f))

                if (active == View.CALENDAR) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = -wPx * (1f - p) }
                    ) { CalendarPanel(now, accent) { settle(false, View.CLOCK) } }
                }
                if (active == View.WEATHER) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = wPx * (1f - p) }
                    ) { WeatherPanel(accent) { settle(false, View.CLOCK) } }
                }
                if (active == View.ALARMS) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = hPx * (1f - p) }
                    ) {
                        AlarmsPanel(
                            alarms = alarms,
                            accent = liveAccent,
                            use24Hour = display.use24Hour,
                            onChange = { updated ->
                                app.prefs.setAlarms(updated)
                                AlarmScheduler.reschedule(context, updated, snoozeAt, snoozeId)
                            },
                            onOpenSettings = { settingsOpen = true },
                            onClose = { settle(false, View.CLOCK) },
                        )
                    }
                }
                if (active == View.SOUNDS) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = -hPx * (1f - p) }
                    ) {
                        SoundsPanel(
                            cfg = sleep,
                            playing = playing,
                            secondsLeft = secondsLeft,
                            accent = liveAccent,
                            onChange = { updated ->
                                app.prefs.setSleep(updated)
                                if (playing) {
                                    SleepAudioService.start(
                                        context, updated.voice,
                                        updated.durationMinutes, updated.volume,
                                    )
                                    playStartedAt = System.currentTimeMillis()
                                }
                            },
                            onTogglePlay = {
                                if (playing) {
                                    SleepAudioService.stop(context)
                                    playStartedAt = 0L
                                } else {
                                    SleepAudioService.start(
                                        context, sleep.voice,
                                        sleep.durationMinutes, sleep.volume,
                                    )
                                    playStartedAt = System.currentTimeMillis()
                                }
                            },
                            onClose = { settle(false, View.CLOCK) },
                        )
                    }
                }

                // The dimmer. A plain Box consumes no pointer events, so a tap
                // still reaches the boost handler beneath it.
                if (scrim > 0.001f) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))
                }

                if (settingsOpen) {
                    SettingsSheet(
                        display = display,
                        accent = liveAccent,
                        previewHh = clock.first,
                        previewMm = clock.second,
                        previewMeridiem = clock.third,
                        rainbowPhase = rainbowPhase,
                        onChange = { app.prefs.setDisplay(it) },
                        onPreviewWake = {
                            settingsOpen = false
                            val t = alarms.firstOrNull { it.enabled } ?: alarms.firstOrNull()
                            if (t != null) app.prefs.setFiring(t.id)
                        },
                        onClose = { settingsOpen = false },
                    )
                }

                if (firingAlarm != null) {
                    RingScreen(
                        now = now,
                        use24Hour = display.use24Hour,
                        label = firingAlarm.label,
                        soundName = firingAlarm.tone,
                        wakeProgress = if (light.mode == LightMode.WAKE) light.wakeProgress else 1f,
                        baseColor = wakeBase,
                        onSnooze = {
                            val at = System.currentTimeMillis() + 9 * 60_000L
                            app.prefs.setSnooze(at, firingAlarm.id)
                            app.prefs.setFiring(0L)
                                            AlarmScheduler.reschedule(context, alarms, at, firingAlarm.id)
                        },
                        onStop = {
                            app.prefs.setSnooze(0L, 0L)
                            app.prefs.setFiring(0L)
                                            AlarmScheduler.reschedule(context, alarms, 0L, 0L)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EdgeHints(alpha: Float) {
    if (alpha <= 0.001f) return
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = du(14)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(du(4)),
        ) {
            Grip(horizontal = true)
            HintLabel("SOUNDS")
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = du(12)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(du(4)),
        ) {
            HintLabel("ALARMS")
            Grip(horizontal = true)
        }
        Row(
            Modifier.align(Alignment.CenterStart).padding(start = du(14)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(du(8)),
        ) {
            Grip(horizontal = false)
            HintLabel("C A L E N D A R", vertical = true)
        }
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = du(14)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(du(8)),
        ) {
            HintLabel("W E A T H E R", vertical = true)
            Grip(horizontal = false)
        }
    }
}

@Composable
private fun Grip(horizontal: Boolean) {
    Box(
        Modifier
            .size(if (horizontal) du(52) else du(4), if (horizontal) du(4) else du(52))
            .clip(RoundedCornerShape(50))
            .background(DC.ink(0.22f))
    )
}

@Composable
private fun HintLabel(text: String, vertical: Boolean = false) {
    Text(
        text = if (vertical) text.toCharArray().joinToString("\n") else text,
        style = TextStyle(
            color = DC.ink(0.3f),
            fontSize = su(10),
            lineHeight = su(14),
            letterSpacing = su(10 * 0.28f),
        ),
    )
}

/**
 * Reads the ambient light sensor, when the panel has one. Cheap tablets often
 * ship without it, so a null result simply means the fixed night dim applies.
 */
@Composable
private fun rememberAmbientLux(enabled: Boolean): Float? {
    val context = LocalContext.current
    var lux by remember { mutableFloatStateOf(Float.NaN) }

    DisposableEffect(enabled) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (!enabled || sensor == null) {
            lux = Float.NaN
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // Heavy smoothing: a hand passing over the tablet should not
                    // make the room brighten.
                    val v = event.values.firstOrNull() ?: return
                    lux = if (lux.isNaN()) v else lux * 0.9f + v * 0.1f
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { sm.unregisterListener(listener) }
        }
    }
    return if (lux.isNaN()) null else lux
}

private fun nextLabel(next: com.kindcode.alarmhub.alarm.NextFire?, use24: Boolean): String {
    if (next == null) return "No alarm set"
    val t = next.at
    val time = if (use24) {
        "%02d:%02d".format(t.hour, t.minute)
    } else {
        val h = (t.hour % 12).let { if (it == 0) 12 else it }
        "%d:%02d %s".format(h, t.minute, if (t.hour < 12) "AM" else "PM")
    }
    return if (next.isSnooze) "Snoozed · rings $time" else "Alarm $time"
}

private fun soundStatus(
    playing: Boolean,
    cfg: com.kindcode.alarmhub.data.SleepConfig,
    secondsLeft: Int,
): String = when {
    !playing -> "Sounds off"
    cfg.durationMinutes == 0 -> "${cfg.voice.displayName} · all night"
    else -> "${cfg.voice.displayName} · %d:%02d".format(secondsLeft / 60, secondsLeft % 60)
}

