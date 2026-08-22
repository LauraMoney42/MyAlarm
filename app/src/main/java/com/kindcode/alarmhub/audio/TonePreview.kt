package com.kindcode.alarmhub.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import com.kindcode.alarmhub.data.AlarmTone

/**
 * A few seconds of a tone, so picking one in the alarm editor is a choice you
 * can hear rather than a name you have to guess at.
 *
 * Deliberately not [LoopPlayer]: this never loops, never fades, and always
 * stops itself. Only one preview exists at a time, so choosing quickly through
 * the list cuts the previous one off instead of stacking.
 *
 * Plays on the alarm stream on purpose. A preview that comes out at media
 * volume tells you nothing about how loud the alarm will actually be.
 */
object TonePreview {

    private const val PREVIEW_MS = 4_000L

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val autoStop = Runnable { stop() }

    fun play(ctx: Context, tone: AlarmTone) {
        stop()
        val built = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val res = tone.res
                if (res != null) {
                    ctx.resources.openRawResourceFd(res).use {
                        setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                } else {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: return
                    setDataSource(ctx, uri)
                }
                isLooping = false
                prepare()
                start()
            }
        }.getOrNull() ?: return

        player = built
        built.setOnCompletionListener { stop() }
        handler.postDelayed(autoStop, PREVIEW_MS)
    }

    fun stop() {
        handler.removeCallbacks(autoStop)
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }
}
