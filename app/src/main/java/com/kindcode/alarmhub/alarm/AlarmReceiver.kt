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
 * the same: get MainActivity on screen. The tone and the light ramp are driven
 * by the activity, which is the resident app on this device.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AlarmHubApp
        val ringing = intent.action == AlarmScheduler.ACTION_RING
        val alarmId = intent.getLongExtra("alarmId", 0L)

        if (ringing) {
            app.prefs.setFiring(alarmId)
            app.prefs.setSnooze(0L, 0L)
        }

        val full = PendingIntent.getActivity(
            context, 2001,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val note = NotificationCompat.Builder(context, AlarmHubApp.CH_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (ringing) "Alarm" else "Wake light starting")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(ringing)
            .setAutoCancel(!ringing)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(if (ringing) 1 else 2, note)

        // Roll the schedule forward so the next occurrence is armed even if
        // nobody touches the tablet.
        if (ringing) {
            AlarmScheduler.reschedule(context, app.prefs.alarms.value, 0L, 0L)
        }
    }
}
