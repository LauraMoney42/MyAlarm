package com.kindcode.alarmhub.kiosk

import android.app.admin.DeviceAdminReceiver

/**
 * Exists only so the app can be made device owner, which is what unlocks true
 * lock task mode. Nothing here enforces policy; see [Kiosk].
 */
class AdminReceiver : DeviceAdminReceiver()
