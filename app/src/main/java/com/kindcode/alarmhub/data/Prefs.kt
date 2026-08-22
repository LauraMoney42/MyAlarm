package com.kindcode.alarmhub.data

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/**
 * The eight synthesised sleep voices from the design, built sample by sample by
 * [com.kindcode.alarmhub.audio.NoiseGen]. Nothing here is a file and nothing
 * here repeats, which is still the right answer for anything noise-shaped.
 *
 * The recorded loops that sit alongside these live in
 * [com.kindcode.alarmhub.data.SleepLoop]; [SleepVoice] is the union the Sounds
 * panel actually renders. Where a synthesised voice and a recording cover the
 * same ground they are numbered rather than given competing prose names, so the
 * two rains read as "Rain 1" and "Rain 2" instead of "Rain on glass" and "Rain".
 */
enum class NoiseType(val displayName: String, val tag: String) {
    RAIN("Rain 1", "ON GLASS"),
    BROWN("Brown noise", "MASKING"),
    SURF("Ocean 4", "SLOW SURF"),
    FAN("Box fan", "MASKING"),
    OCEAN("Ocean 1", "NEARSHORE"),
    NIGHT("Summer night", "NATURE"),
    TRAIN("Night train", "MOTION"),
    DRONE("Deep drone", "TONAL"),
}

/** Display names of the bundled alarm tones. See [AlarmTone]. */
val ALARM_TONES: List<String> = AlarmTone.NAMES

data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    /** java.time DayOfWeek values, 1=Mon..7=Sun. Empty means fire once. */
    val days: Set<Int> = emptySet(),
    val label: String = "Alarm",
    val tone: String = ALARM_TONES.first(),
    val wakeLight: Boolean = true,
    val leadMinutes: Int = 15,
    val enabled: Boolean = true,
) {
    /** Next occurrence strictly after [from], or null if it can never fire. */
    fun nextTrigger(from: LocalDateTime): LocalDateTime? {
        for (offset in 0..8) {
            val candidate = from.toLocalDate().plusDays(offset.toLong()).atTime(hour, minute)
            if (!candidate.isAfter(from)) continue
            if (days.isEmpty() || candidate.dayOfWeek.value in days) return candidate
        }
        return null
    }

    /** "Every day", "Mon - Fri", "Sat, Sun", or "Once". */
    fun whenLabel(): String {
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        if (days == setOf(1, 2, 3, 4, 5)) return "Mon - Fri"
        if (days == setOf(6, 7)) return "Sat, Sun"
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return days.sorted().joinToString(", ") { names[it - 1] }
    }
}

/** Which face the clock wears. */
enum class ClockStyle { FLIP, SEGMENT }

data class DisplayConfig(
    val use24Hour: Boolean = false,
    /** Hour at which the display drops to night mode. */
    val nightStartHour: Int = 21,
    val dayBrightness: Float = 0.85f,
    val nightBrightness: Float = 0.04f,
    /**
     * Black scrim painted over everything at night, 0f to 0.92f. The panel's own
     * minimum backlight is still too bright at 3am, so real darkness has to come
     * from painting over pixels rather than from the backlight alone.
     */
    val nightDim: Float = 0.72f,
    /** Drive the scrim from the ambient light sensor instead of the fixed value. */
    val autoDim: Boolean = true,
    /** Crossfade the cards instead of flipping. Defaults on for low-RAM devices. */
    val reducedMotion: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.FLIP,
    /** ARGB. Ignored by the flip face, which draws its own card colours. */
    val digitColor: Int = 0xFFC6C6C9.toInt(),
    val backgroundColor: Int = 0xFF050506.toInt(),
    val accentColor: Int = 0xFFD8A15C.toInt(),
    /** Base hue for the wake light ramp. The stops are derived from it. */
    val wakeColor: Int = 0xFFFFA72B.toInt(),
    /**
     * Rainbow is a choice on each colour target rather than a mode, so the
     * numbers can cycle over a fixed background or the other way round.
     *
     * The drift is driven off the existing one-second tick rather than an
     * animation, because a continuously animating clock is exactly the cost
     * this device cannot pay all night: a full cycle spread over minutes is
     * smooth enough at one frame per second.
     */
    val digitRainbow: Boolean = false,
    val surfaceRainbow: Boolean = false,
    val accentRainbow: Boolean = false,
    val wakeRainbow: Boolean = false,
    /**
     * Degrees of hue between one digit and the next. Zero makes all four the
     * same colour, so "solid but cycling" and "gradient across the face" are
     * the same setting at two ends of one slider.
     */
    val rainbowSpread: Float = 38f,
    /** Seconds for one full trip round the hue wheel. */
    val rainbowSeconds: Int = 240,
    /**
     * Static lays the spectrum across the display and leaves it there, the way
     * a moulded RGB clock does. Cycling drifts the whole thing instead. Same
     * rainbow, two temperaments.
     */
    val rainbowStatic: Boolean = false,
)

data class SleepConfig(
    val voice: SleepVoice = SleepVoice.DEFAULT,
    /** Minutes of playback, 0 means run until stopped. */
    val durationMinutes: Int = 45,
    val volume: Float = 0.62f,
)

class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("alarmhub", Context.MODE_PRIVATE)

    /**
     * Android Go reports true here. Better signal than any RAM number, because
     * the OEM sets it and the platform itself uses it to decide how hard to lean
     * on the device.
     */
    val isLowRam: Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice

    private val _alarms = MutableStateFlow(readAlarms())
    val alarms: StateFlow<List<Alarm>> = _alarms

    private val _display = MutableStateFlow(readDisplay())
    val display: StateFlow<DisplayConfig> = _display

    private val _sleep = MutableStateFlow(readSleep())
    val sleep: StateFlow<SleepConfig> = _sleep

    /**
     * Id of an alarm that is currently ringing, or 0. Persisted so a process
     * killed mid-alarm still knows to show the ring screen when it restarts.
     */
    private val _firingId = MutableStateFlow(sp.getLong("firing_id", 0L))
    val firingId: StateFlow<Long> = _firingId

    private val _snoozeAt = MutableStateFlow(sp.getLong("snooze_at", 0L))
    val snoozeAt: StateFlow<Long> = _snoozeAt

    private val _snoozeId = MutableStateFlow(sp.getLong("snooze_id", 0L))
    val snoozeId: StateFlow<Long> = _snoozeId

    // ---- alarms ----

    private fun readAlarms(): List<Alarm> {
        val raw = sp.getString("alarms_json", null) ?: return defaultAlarms()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val daysArr = o.optJSONArray("days") ?: JSONArray()
                Alarm(
                    id = o.getLong("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    days = (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet(),
                    label = o.optString("label", "Alarm"),
                    // Normalised through AlarmTone so the six decorative names
                    // shipped before v0.2.2 resolve to something that can ring.
                    tone = AlarmTone.byName(o.optString("tone")).displayName,
                    wakeLight = o.optBoolean("wakeLight", true),
                    leadMinutes = o.optInt("leadMinutes", 15),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        }.getOrElse { defaultAlarms() }
    }

    private fun defaultAlarms() = listOf(
        Alarm(1L, 6, 30, setOf(1, 2, 3, 4, 5), "Weekday wake", "Clock bell", true, 15, true),
        Alarm(2L, 7, 45, setOf(6, 7), "Weekend", "Birds 1", true, 15, false),
    )

    fun setAlarms(list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("hour", a.hour)
                    put("minute", a.minute)
                    put("days", JSONArray().also { d -> a.days.sorted().forEach(d::put) })
                    put("label", a.label)
                    put("tone", a.tone)
                    put("wakeLight", a.wakeLight)
                    put("leadMinutes", a.leadMinutes)
                    put("enabled", a.enabled)
                }
            )
        }
        sp.edit().putString("alarms_json", arr.toString()).apply()
        _alarms.value = list
    }

    // ---- display ----

    private fun readDisplay() = DisplayConfig(
        use24Hour = sp.getBoolean("d_24h", false),
        nightStartHour = sp.getInt("d_night_start", 21),
        dayBrightness = sp.getFloat("d_day_b", 0.85f),
        nightBrightness = sp.getFloat("d_night_b", 0.04f),
        nightDim = sp.getFloat("d_night_dim", 0.72f),
        autoDim = sp.getBoolean("d_auto_dim", true),
        reducedMotion = sp.getBoolean("d_reduced_motion", isLowRam),
        clockStyle = runCatching {
            ClockStyle.valueOf(sp.getString("d_style", "FLIP")!!)
        }.getOrDefault(ClockStyle.FLIP),
        digitColor = sp.getInt("d_digit_color", 0xFFC6C6C9.toInt()),
        backgroundColor = sp.getInt("d_bg_color", 0xFF050506.toInt()),
        accentColor = sp.getInt("d_accent_color", 0xFFD8A15C.toInt()),
        wakeColor = sp.getInt("d_wake_color", 0xFFFFA72B.toInt()),
        digitRainbow = sp.getBoolean("d_rainbow", false),
        surfaceRainbow = sp.getBoolean("d_bg_rainbow", false),
        accentRainbow = sp.getBoolean("d_accent_rainbow", false),
        wakeRainbow = sp.getBoolean("d_wake_rainbow", false),
        rainbowSpread = sp.getFloat("d_rainbow_spread", 38f),
        rainbowSeconds = sp.getInt("d_rainbow_seconds", 240),
        rainbowStatic = sp.getBoolean("d_rainbow_static", false),
    )

    fun setDisplay(cfg: DisplayConfig) {
        sp.edit()
            .putBoolean("d_24h", cfg.use24Hour)
            .putInt("d_night_start", cfg.nightStartHour)
            .putFloat("d_day_b", cfg.dayBrightness)
            .putFloat("d_night_b", cfg.nightBrightness)
            .putFloat("d_night_dim", cfg.nightDim)
            .putBoolean("d_auto_dim", cfg.autoDim)
            .putBoolean("d_reduced_motion", cfg.reducedMotion)
            .putString("d_style", cfg.clockStyle.name)
            .putInt("d_digit_color", cfg.digitColor)
            .putInt("d_bg_color", cfg.backgroundColor)
            .putInt("d_accent_color", cfg.accentColor)
            .putInt("d_wake_color", cfg.wakeColor)
            .putBoolean("d_rainbow", cfg.digitRainbow)
            .putBoolean("d_bg_rainbow", cfg.surfaceRainbow)
            .putBoolean("d_accent_rainbow", cfg.accentRainbow)
            .putBoolean("d_wake_rainbow", cfg.wakeRainbow)
            .putFloat("d_rainbow_spread", cfg.rainbowSpread)
            .putInt("d_rainbow_seconds", cfg.rainbowSeconds)
            .putBoolean("d_rainbow_static", cfg.rainbowStatic)
            .apply()
        _display.value = cfg
    }

    // ---- sleep ----

    private fun readSleep() = SleepConfig(
        voice = SleepVoice.fromKey(sp.getString("s_noise", null)),
        durationMinutes = sp.getInt("s_duration", 45),
        volume = sp.getFloat("s_volume", 0.62f),
    )

    fun setSleep(cfg: SleepConfig) {
        sp.edit()
            .putString("s_noise", cfg.voice.key)
            .putInt("s_duration", cfg.durationMinutes)
            .putFloat("s_volume", cfg.volume)
            .apply()
        _sleep.value = cfg
    }

    // ---- transient alarm state ----

    fun setFiring(alarmId: Long) {
        sp.edit().putLong("firing_id", alarmId).apply()
        _firingId.value = alarmId
    }

    fun setSnooze(atMillis: Long, alarmId: Long) {
        sp.edit().putLong("snooze_at", atMillis).putLong("snooze_id", alarmId).apply()
        _snoozeAt.value = atMillis
        _snoozeId.value = alarmId
    }
}
