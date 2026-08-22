package com.kindcode.alarmhub.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Plays one bundled loop indefinitely with no gap at the join.
 *
 * `MediaPlayer.isLooping` is the obvious choice and the wrong one here: it
 * restarts the decoder at the loop point, and the few milliseconds that costs is
 * an audible tick in a silent bedroom at 3am. `setNextMediaPlayer` is the
 * platform's own gapless handoff, so this keeps two players and passes between
 * them, re-arming the one that just finished to be the next one after that.
 *
 * That handles the playback seam. The waveform seam is handled earlier, by
 * `audio/prep_sounds.sh`, which crossfade-wraps every file so the sample either
 * side of the join is continuous. Both halves are needed: a gapless player
 * joining a butt-spliced file still clicks, and a seamless file played by a
 * gapping player still clicks.
 *
 * Memory cost is the decoder, not the audio: nothing is held decoded, which is
 * what makes a two minute stereo loop affordable on a 2 GB device.
 *
 * Not thread-safe by itself. [start] and [stop] must be called from the thread
 * that owns the Looper, which is the main thread, because that is where
 * MediaPlayer delivers completion callbacks. [setVolume] is safe from anywhere.
 */
class LoopPlayer(
    private val ctx: Context,
    @RawRes private val res: Int,
    /**
     * Alarms must pass [AudioAttributes.USAGE_ALARM]: it routes to the alarm
     * stream and sounds through Do Not Disturb and a silenced ringer, which is
     * the whole contract of an alarm clock. Sleep sounds are ordinary media.
     */
    private val usage: Int = AudioAttributes.USAGE_MEDIA,
    private val contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC,
) {

    private var current: MediaPlayer? = null
    private var next: MediaPlayer? = null

    @Volatile private var volume = 1f
    @Volatile private var stopped = false

    private fun build(): MediaPlayer? = runCatching {
        val afd = ctx.resources.openRawResourceFd(res) ?: return null
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(contentType)
                    .build()
            )
            afd.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            prepare()
            setVolume(volume, volume)
        }
    }.getOrNull()

    /** Returns false if the resource could not be opened, so the caller can fall back. */
    fun start(initialVolume: Float): Boolean {
        volume = initialVolume.coerceIn(0f, 1f)
        current = build() ?: return false
        next = build()
        arm()
        return runCatching { current?.start(); true }.getOrDefault(false)
    }

    /**
     * Points the running player at its successor and arranges for the pair to
     * swap when it finishes. The successor is already playing by the time the
     * callback arrives, which is the whole point of `setNextMediaPlayer`.
     */
    private fun arm() {
        val c = current ?: return
        val n = next
        if (n != null) runCatching { c.setNextMediaPlayer(n) }
        c.setOnCompletionListener { finished ->
            if (stopped) return@setOnCompletionListener
            current = next
            runCatching { finished.reset(); finished.release() }
            next = build()
            arm()
        }
    }

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        runCatching { current?.setVolume(volume, volume) }
        runCatching { next?.setVolume(volume, volume) }
    }

    fun stop() {
        stopped = true
        listOf(current, next).forEach { p ->
            p ?: return@forEach
            runCatching { p.setOnCompletionListener(null) }
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        current = null
        next = null
    }
}
