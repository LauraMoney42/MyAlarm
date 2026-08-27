package com.kindcode.alarmhub.ui

import com.kindcode.alarmhub.data.DisplayConfig
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ln
import kotlin.math.pow

enum class LightMode { DAY, NIGHT, WAKE }

data class LightState(
    val mode: LightMode,
    /** 0..1 through the wake light ramp. Meaningless outside WAKE. */
    val wakeProgress: Float,
    /** Backlight level handed to the window. */
    val brightness: Float,
    /** Black scrim painted over the UI, 0f..0.92f. */
    val scrim: Float,
)

/**
 * Everything visual is a pure function of the clock: given "now" and "when the
 * next alarm rings", the display state falls out. There is no "ramp in
 * progress" flag to get out of sync with reality.
 *
 * The scrim matters more than the backlight. A panel at its minimum backlight
 * is still far too bright at 3am, so real darkness comes from painting over the
 * pixels. The same knob run in reverse is what opens the wake light.
 */
fun computeLight(
    now: LocalDateTime,
    nextFire: LocalDateTime?,
    leadMinutes: Int,
    display: DisplayConfig,
    lux: Float?,
    boosted: Boolean,
): LightState {

    if (nextFire != null && leadMinutes > 0) {
        val start = nextFire.minusMinutes(leadMinutes.toLong())
        if (!now.isBefore(start) && now.isBefore(nextFire)) {
            val total = Duration.between(start, nextFire).toMillis().toDouble()
            val done = Duration.between(start, now).toMillis().toDouble()
            val p = (done / total).coerceIn(0.0, 1.0).toFloat()
            return LightState(
                mode = LightMode.WAKE,
                wakeProgress = p,
                // Curved so the last third does most of the visible lifting.
                brightness = 0.02f + 0.98f * p.toDouble().pow(1.7).toFloat(),
                // Opens from real darkness to fully clear. Starting from the
                // night dim setting meant that with a light dim value there was
                // barely anything to lift, so the sunrise had nothing to do.
                scrim = (0.88f * (1f - p.toDouble().pow(0.55).toFloat()))
                    .coerceIn(0f, 0.92f),
            )
        }
    }

    val isNight = now.hour >= display.nightStartHour || now.hour < 6
    if (!isNight) {
        return LightState(LightMode.DAY, 0f, display.dayBrightness, 0f)
    }

    val autoScrim = if (display.autoDim && lux != null) {
        // Log scale: a dark bedroom is under 1 lux, a lamp is tens, daylight is
        // thousands. Anything past ~60 lux means the room is lit, so stop dimming.
        val f = (ln((lux + 1f).toDouble()) / ln(60.0)).toFloat().coerceIn(0f, 1f)
        (0.90f * (1f - f)).coerceIn(0f, 0.92f)
    } else {
        display.nightDim
    }

    // `boosted` means someone is using the tablet right now. Dimming underneath
    // an active hand is the single most irritating thing a night mode can do, so
    // while it is set the scrim comes off entirely rather than being thinned.
    return LightState(
        mode = LightMode.NIGHT,
        wakeProgress = 0f,
        brightness = if (boosted) 0.38f else display.nightBrightness,
        scrim = if (boosted) 0f else autoScrim,
    )
}
