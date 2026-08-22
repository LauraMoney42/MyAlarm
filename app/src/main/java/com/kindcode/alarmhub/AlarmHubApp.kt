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
        nm.createNotificationChannel(
            NotificationChannel(CH_ALARM, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Wake alarm and warm wake light"
                // The activity plays the tone itself so the fade-in can be controlled.
                setSound(null, null)
                enableVibration(false)
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
