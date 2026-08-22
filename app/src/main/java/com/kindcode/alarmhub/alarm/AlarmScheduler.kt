package com.kindcode.alarmhub.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kindcode.alarmhub.MainActivity
import com.kindcode.alarmhub.data.Alarm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** The next thing that will happen, and which alarm it belongs to. */
data class NextFire(val at: LocalDateTime, val alarm: Alarm, val isSnooze: Boolean)

/**
 * Scheduling rules that matter on Android:
 *  - setAlarmClock() is the only exact-alarm variant Doze will never defer, and
 *    it is what puts the alarm icon in the status bar.
 *  - The wake light gets its own setAlarmClock so the screen is already awake
 *    and ramping before the ring time, even from a cold process.
 */
object AlarmScheduler {

    const val ACTION_RING = "com.kindcode.alarmhub.RING"
    const val ACTION_WAKELIGHT = "com.kindcode.alarmhub.WAKELIGHT"

    private const val RC_RING = 1001
    private const val RC_LIGHT = 1002
    private const val RC_SHOW = 1003

    fun millisOf(t: LocalDateTime): Long =
        t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun timeOf(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

    /**
     * Earliest of: an active snooze, or the soonest enabled alarm. Pure, so the
     * UI and the scheduler can never disagree about what fires next.
     */
    fun nextFire(
        alarms: List<Alarm>,
        now: LocalDateTime,
        snoozeAt: Long,
        snoozeId: Long,
    ): NextFire? {
        val snoozed = if (snoozeAt > System.currentTimeMillis()) {
            alarms.firstOrNull { it.id == snoozeId }
                ?.let { NextFire(timeOf(snoozeAt), it, true) }
        } else null

        val scheduled = alarms
            .filter { it.enabled }
            .mapNotNull { a -> a.nextTrigger(now)?.let { NextFire(it, a, false) } }
            .minByOrNull { it.at }

        return listOfNotNull(snoozed, scheduled).minByOrNull { it.at }
    }

    fun reschedule(ctx: Context, alarms: List<Alarm>, snoozeAt: Long, snoozeId: Long) {
        cancel(ctx)
        val next = nextFire(alarms, LocalDateTime.now(), snoozeAt, snoozeId) ?: return
        val ringMillis = millisOf(next.at)

        am(ctx).setAlarmClock(
            AlarmManager.AlarmClockInfo(ringMillis, showIntent(ctx)),
            operation(ctx, ACTION_RING, RC_RING, next.alarm.id),
        )

        // A snooze gets no light ramp. Running a 15 minute sunrise for a nine
        // minute snooze floodlights the room seconds after asking for more sleep.
        if (!next.isSnooze && next.alarm.wakeLight && next.alarm.leadMinutes > 0) {
            val lightMillis = ringMillis - next.alarm.leadMinutes * 60_000L
            if (lightMillis > System.currentTimeMillis()) {
                am(ctx).setAlarmClock(
                    AlarmManager.AlarmClockInfo(lightMillis, showIntent(ctx)),
                    operation(ctx, ACTION_WAKELIGHT, RC_LIGHT, next.alarm.id),
                )
            }
        }
    }

    fun cancel(ctx: Context) {
        am(ctx).cancel(operation(ctx, ACTION_RING, RC_RING, 0))
        am(ctx).cancel(operation(ctx, ACTION_WAKELIGHT, RC_LIGHT, 0))
    }

    private fun am(ctx: Context) =
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun operation(ctx: Context, action: String, rc: Int, alarmId: Long) =
        PendingIntent.getBroadcast(
            ctx, rc,
            Intent(ctx, AlarmReceiver::class.java).setAction(action)
                .putExtra("alarmId", alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun showIntent(ctx: Context) = PendingIntent.getActivity(
        ctx, RC_SHOW,
        Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
