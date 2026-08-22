package com.kindcode.alarmhub.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kindcode.alarmhub.AlarmHubApp

/**
 * Re-arms alarms from persisted config whenever the mapping between wall clock
 * and absolute time could have moved: a reboot, an app update, a timezone
 * change, or the clock being set.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AlarmHubApp
        AlarmScheduler.reschedule(
            context,
            app.prefs.alarms.value,
            app.prefs.snoozeAt.value,
            app.prefs.snoozeId.value,
        )
    }
}
