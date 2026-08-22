package com.kindcode.alarmhub.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.kindcode.alarmhub.AlarmHubApp
import com.kindcode.alarmhub.MainActivity
import com.kindcode.alarmhub.R
import com.kindcode.alarmhub.data.NoiseType
import com.kindcode.alarmhub.data.SleepVoice
import kotlin.concurrent.thread

class SleepAudioService : Service() {

    companion object {
        const val ACTION_START = "com.kindcode.alarmhub.SLEEP_START"
        const val ACTION_STOP = "com.kindcode.alarmhub.SLEEP_STOP"
        private const val SAMPLE_RATE = 44100
        private const val FADE_IN_SEC = 3f
        private const val FADE_OUT_SEC = 60f
        private const val NOTE_ID = 42

        fun start(ctx: Context, voice: SleepVoice, minutes: Int, volume: Float) {
            val i = Intent(ctx, SleepAudioService::class.java)
                .setAction(ACTION_START)
                .putExtra("voice", voice.key)
                .putExtra("minutes", minutes)
                .putExtra("volume", volume)
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.startService(
                Intent(ctx, SleepAudioService::class.java).setAction(ACTION_STOP)
            )
        }
    }

    @Volatile private var running = false
    private var worker: Thread? = null
    private var loop: LoopPlayer? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
            ACTION_START -> {
                val voice = SleepVoice.fromKey(intent.getStringExtra("voice"))
                val minutes = intent.getIntExtra("minutes", 60)
                val volume = intent.getFloatExtra("volume", 0.5f)
                startForeground(NOTE_ID, buildNote(voice, minutes))
                stopPlayback()
                when (voice) {
                    is SleepVoice.Synth -> startSynth(voice.type, minutes, volume)
                    is SleepVoice.Loop -> startLoop(voice, minutes, volume)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun buildNote(voice: SleepVoice, minutes: Int) =
        NotificationCompat.Builder(this, AlarmHubApp.CH_SLEEP)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sleep sounds")
            .setContentText(
                voice.displayName + if (minutes > 0) " for $minutes min" else ""
            )
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

    private fun stopPlayback() {
        running = false
        worker?.join(500)
        worker = null
        // Released here rather than on the worker: MediaPlayer delivers its
        // completion callbacks on the main Looper, and tearing it down from
        // another thread races them.
        loop?.stop()
        loop = null
    }

    /**
     * Fade in from silence, then out over the last minute, so neither end of a
     * session registers as a cut. Shared by both playback paths so a recording
     * and a synthesised voice behave identically.
     */
    private fun gainAt(elapsed: Double, total: Double, volume: Float): Float {
        var g = volume
        if (elapsed < FADE_IN_SEC) g *= (elapsed / FADE_IN_SEC).toFloat()
        if (total > 0) {
            val remaining = total - elapsed
            if (remaining < FADE_OUT_SEC) g *= (remaining / FADE_OUT_SEC).toFloat()
        }
        return g.coerceIn(0f, 1f)
    }

    /**
     * A recorded loop. The audio is handled entirely by [LoopPlayer]; this
     * thread exists only to walk the same fade curve the synthesised path uses,
     * and to notice when the timer runs out.
     */
    private fun startLoop(voice: SleepVoice.Loop, minutes: Int, volume: Float) {
        val player = LoopPlayer(this, voice.loop.res)
        if (!player.start(0f)) {
            // Could not open the asset. Better to fall back to a synthesised
            // voice than to leave a running notification over silence.
            player.stop()
            startSynth(NoiseType.RAIN, minutes, volume)
            return
        }
        loop = player
        running = true
        worker = thread(name = "sleep-loop", isDaemon = true) {
            val total = minutes * 60.0
            val startedAt = System.nanoTime()
            var expired = false
            while (running) {
                val elapsed = (System.nanoTime() - startedAt) / 1_000_000_000.0
                if (total > 0 && elapsed >= total) { expired = true; break }
                player.setVolume(gainAt(elapsed, total, volume))
                try { Thread.sleep(100) } catch (_: InterruptedException) { break }
            }
            if (expired) main.post {
                stopPlayback()
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    /** A synthesised voice, written sample by sample into an AudioTrack. */
    private fun startSynth(type: NoiseType, minutes: Int, volume: Float) {
        running = true
        worker = thread(name = "sleep-noise", isDaemon = true) {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val left = NoiseGen(type, 0x5EED_1234L, SAMPLE_RATE)
            val right = NoiseGen(type, 0x5EED_9876L, SAMPLE_RATE)

            val frames = 1024
            val buf = ShortArray(frames * 2)
            var elapsed = 0.0
            val total = minutes * 60.0

            track.play()
            while (running) {
                for (i in 0 until frames) {
                    buf[i * 2] = (left.next() * Short.MAX_VALUE * 0.9f).toInt().toShort()
                    buf[i * 2 + 1] = (right.next() * Short.MAX_VALUE * 0.9f).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
                elapsed += frames.toDouble() / SAMPLE_RATE

                if (total > 0 && total - elapsed <= 0) break
                track.setVolume(gainAt(elapsed, total, volume))
            }

            runCatching { track.stop() }
            track.release()
            if (running) {
                // Ran to the end of its timer rather than being stopped by hand.
                running = false
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
}
