package com.kindcode.alarmhub.data

import androidx.annotation.RawRes
import com.kindcode.alarmhub.R

/**
 * The recorded loops in `res/raw`, and the credits they carry.
 *
 * These exist because synthesis cannot do all of it. `NoiseGen` still owns
 * anything noise-shaped, where it beats a recording outright: nothing to ship
 * and nothing that can repeat over an eight hour night. What it cannot fake is
 * surf with real wave rhythm, a wood at dusk, birdsong, or a struck bell. Those
 * are recordings, and every recording is a loop, so each one is crossfade-
 * wrapped by `audio/prep_sounds.sh` before it ships.
 *
 * Loop lengths are deliberately uneven. Rain and wind are stationary noise, so a
 * short loop is undetectable; surf and birdsong carry a rhythm the ear tracks
 * and need 75 to 120 seconds before the pattern starts announcing itself.
 */
enum class SleepLoop(
    val displayName: String,
    @RawRes val res: Int,
    /** Non-null when the licence requires the credit to be visible in the app. */
    val credit: String? = null,
) {
    RAIN_2("Rain 2", R.raw.sleep_rain, "Rain 2: lebaston100, CC BY 4.0"),
    OCEAN_2("Ocean 2", R.raw.sleep_ocean_2),
    OCEAN_3("Ocean 3", R.raw.sleep_ocean_3),
    WOODS("Woods", R.raw.sleep_woods),
    WIND("Wind", R.raw.sleep_wind),
}

/**
 * What the Sounds panel offers: the eight synthesised voices and the five
 * recorded loops, as one list. The panel does not care which is which beyond
 * the tag it prints on the card; `SleepAudioService` is where they diverge.
 */
sealed interface SleepVoice {

    val key: String
    val displayName: String
    val tag: String

    data class Synth(val type: NoiseType) : SleepVoice {
        override val key get() = "synth:${type.name}"
        override val displayName get() = type.displayName
        override val tag get() = type.tag
    }

    data class Loop(val loop: SleepLoop) : SleepVoice {
        override val key get() = "loop:${loop.name}"
        override val displayName get() = loop.displayName
        override val tag get() = "RECORDED"
    }

    companion object {
        val ALL: List<SleepVoice> =
            NoiseType.entries.map(::Synth) + SleepLoop.entries.map(::Loop)

        val DEFAULT: SleepVoice = Synth(NoiseType.RAIN)

        /**
         * Builds up to v0.2.1 stored a bare `NoiseType` name in `s_noise`. That
         * form is still parsed, so upgrading keeps whatever the device was set
         * to rather than silently resetting it to rain.
         */
        fun fromKey(raw: String?): SleepVoice = when {
            raw == null -> DEFAULT
            raw.startsWith("synth:") -> runCatching {
                Synth(NoiseType.valueOf(raw.removePrefix("synth:")))
            }.getOrDefault(DEFAULT)
            raw.startsWith("loop:") -> runCatching {
                Loop(SleepLoop.valueOf(raw.removePrefix("loop:")))
            }.getOrDefault(DEFAULT)
            else -> runCatching { Synth(NoiseType.valueOf(raw)) }.getOrDefault(DEFAULT)
        }
    }
}

/**
 * Alarm tones. [SYSTEM] keeps the old behaviour of deferring to whatever the
 * device has set, and is the safety net if a bundled tone ever fails to open.
 */
enum class AlarmTone(
    val displayName: String,
    @RawRes val res: Int?,
    val credit: String? = null,
) {
    CLOCK_BELL("Clock bell", R.raw.wake_clock_bell),
    CHIMES("Chimes", R.raw.wake_chimes, "Chimes: sonocrea, CC BY 4.0"),
    BIRDS_1("Birds 1", R.raw.wake_birds_1),
    BIRDS_2("Birds 2", R.raw.wake_birds_2),
    SYSTEM("Device default", null),
    ;

    companion object {
        val NAMES: List<String> = entries.map { it.displayName }

        /**
         * Tolerant on purpose. Alarms persist a display name, and v0.2.1 shipped
         * six decorative names that never reached the player ("Sunrise chime",
         * "Birdsong", and so on). Anything unrecognised lands on the clock bell
         * rather than throwing on an alarm that is trying to ring.
         */
        fun byName(name: String?): AlarmTone =
            entries.firstOrNull { it.displayName == name } ?: CLOCK_BELL
    }
}

/**
 * Every credit the bundled audio requires, for the line at the foot of the
 * Sounds panel. Seven of the nine recordings are CC0 and need nothing; these two
 * are CC BY 4.0 and the credit has to be visible. `audio/README.md` carries the
 * full provenance table.
 */
val AUDIO_CREDITS: List<String> =
    SleepLoop.entries.mapNotNull { it.credit } + AlarmTone.entries.mapNotNull { it.credit }
