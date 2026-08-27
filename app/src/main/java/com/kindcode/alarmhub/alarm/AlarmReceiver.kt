package com.kindcode.alarmhub.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kindcode.alarmhub.AlarmHubApp
import com.kindcode.alarmhub.MainActivity
import com.kindcode.alarmhub.R

/**
 * Fires for both the wake light and the ring itself. In both cases the job is
 * the same: get MainActivity on screen.
 *
 * When the clock is already on screen, which on a docked tablet running it as
 * the home app is essentially always, there is nothing to do. The firing state
 * is persisted and the UI is driven from it, so no notification is posted and
 * none is seen.
 *
 * When it is not on screen, a plain startActivity is not enough: Android blocks
 * background activity starts, and an alarm that is silently blocked is the
 * worst failure this app has. A full-screen intent is the sanctioned way
 * through, and it is delivered by posting a notification, so one is posted only
 * in that case.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AlarmHubApp
        val ringing = intent.action == AlarmScheduler.ACTION_RING
        val alarmId = intent.getLongExtra("alarmId", 0L)

        // Written before anything is asked to appear, so a cold start reads the
        // firing state on the way up rather than racing it.
        if (ringing) {
            app.prefs.setFiring(alarmId)
            app.prefs.setSnooze(0L, 0L)
        }

        if (!MainActivity.onScreen) {
            val show = PendingIntent.getActivity(
                context, 2001,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            runCatching { context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ) }

            val note = NotificationCompat.Builder(context, AlarmHubApp.CH_ALARM)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(if (ringing) "Alarm" else "Wake light")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setSilent(true)
                .setAutoCancel(true)
                .setFullScreenIntent(show, true)
                .setContentIntent(show)
                .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(if (ringing) 1 else 2, note)
        }

        // Roll the schedule forward so the next occurrence is armed even if
        // nobody touches the tablet.
        if (ringing) {
            AlarmScheduler.reschedule(context, app.prefs.alarms.value, 0L, 0L)
        }
    }
}
