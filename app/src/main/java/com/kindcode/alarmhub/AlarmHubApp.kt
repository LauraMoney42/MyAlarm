package com.kindcode.alarmhub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.kindcode.alarmhub.alarm.AlarmScheduler
import com.kindcode.alarmhub.data.Prefs

class AlarmHubApp : Application() {

    companion object {
        const val CH_ALARM = "alarm"
        const val CH_SLEEP = "sleep"
    }

    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)

        val nm = getSystemService(NotificationManager::class.java)
        // Exists only to carry a full-screen intent when the clock is not
        // already on screen. Silent, and never posted in normal use.
        nm.createNotificationChannel(
            NotificationChannel(CH_ALARM, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Brings the alarm screen up when the clock is not showing"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SLEEP, "Sleep sounds", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ongoing sleep audio"
                setShowBadge(false)
            }
        )

        // Re-arm on every cold start, in case an alarm was missed or the app was
        // updated out from under a pending intent.
        AlarmScheduler.reschedule(
            this,
            prefs.alarms.value,
            prefs.snoozeAt.value,
            prefs.snoozeId.value,
        )
    }
}
