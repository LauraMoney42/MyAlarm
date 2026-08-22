package com.kindcode.alarmhub.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import com.kindcode.alarmhub.audio.LoopPlayer
import com.kindcode.alarmhub.data.AlarmTone
import kotlin.math.min

/**
 * Plays the alarm's chosen tone, ramping up over 30 seconds. Paired with the
 * sunrise light, a gradual ring is the difference between waking up and being
 * startled awake.
 *
 * Until v0.2.2 the tone stored on each alarm was decorative: six names in the
 * picker, all of which played the system default. The bundled tones are real
 * now, and [AlarmTone.SYSTEM] is what keeps the old behaviour available.
 *
 * Bundled tones go through [LoopPlayer] rather than `MediaPlayer.isLooping`
 * because they were built to be looped: the clock bell in particular is exactly
 * four strike periods long, so a gapless join makes it read as one unbroken bell
 * tower rather than a phrase restarting.
 */
object AlarmRinger {

    private const val RAMP_MS = 30_000f
    private const val START_VOLUME = 0.08f

    private var loop: LoopPlayer? = null
    private var system: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var startedAt = 0L

    private val ramp = object : Runnable {
        override fun run() {
            if (loop == null && system == null) return
            val elapsed = (System.currentTimeMillis() - startedAt).toFloat()
            val v = min(1f, START_VOLUME + (1f - START_VOLUME) * (elapsed / RAMP_MS))
            loop?.setVolume(v)
            runCatching { system?.setVolume(v, v) }
            if (v < 1f) handler.postDelayed(this, 250)
        }
    }

    fun start(ctx: Context, toneName: String?) {
        if (loop != null || system != null) return

        val res = AlarmTone.byName(toneName).res
        if (res != null) {
            val player = LoopPlayer(
                ctx,
                res,
                AudioAttributes.USAGE_ALARM,
                AudioAttributes.CONTENT_TYPE_SONIFICATION,
            )
            if (player.start(START_VOLUME)) {
                loop = player
                begin()
                return
            }
            // Opening the bundled tone failed. An alarm that does not ring is
            // the worst bug this app can have, so fall through to the device's
            // own tone rather than giving up.
            player.stop()
        }

        startSystemTone(ctx)
    }

    private fun startSystemTone(ctx: Context) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return

        val p = MediaPlayer()
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        val ok = runCatching {
            p.setDataSource(ctx, uri)
            p.isLooping = true
            p.setVolume(START_VOLUME, START_VOLUME)
            p.prepare()
            p.start()
        }.isSuccess

        if (!ok) {
            runCatching { p.release() }
            return
        }
        system = p
        begin()
    }

    private fun begin() {
        startedAt = System.currentTimeMillis()
        handler.post(ramp)
    }

    fun stop() {
        handler.removeCallbacks(ramp)
        loop?.stop()
        loop = null
        system?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        system = null
    }
}
