package com.kindcode.alarmhub.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

/**
 * Lock task mode, the only way on stock Android to make the navigation and
 * status bars genuinely never appear. Immersive mode alone cannot do it: the
 * public insets API has no "never show" behaviour, only "show transiently on
 * swipe", which is exactly the flash we are trying to remove.
 *
 * All of this is inert unless the app has been made device owner:
 *
 *     adb shell dpm set-device-owner com.kindcode.alarmhub/.kiosk.AdminReceiver
 *
 * Without device owner, startLockTask() would prompt and still leave Back and
 * Overview on screen, so it is not worth entering at all.
 */
object Kiosk {

    private fun dpm(ctx: Context): DevicePolicyManager =
        ctx.getSystemService(DevicePolicyManager::class.java)

    private fun admin(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)

    fun isDeviceOwner(ctx: Context): Boolean =
        runCatching { dpm(ctx).isDeviceOwnerApp(ctx.packageName) }.getOrDefault(false)

    /** Enters lock task and blanks the status bar. Safe to call repeatedly. */
    fun enter(activity: Activity) {
        if (!isDeviceOwner(activity)) return
        val d = dpm(activity)
        runCatching {
            d.setLockTaskPackages(admin(activity), arrayOf(activity.packageName))
            d.setStatusBarDisabled(admin(activity), true)
        }
        if (runCatching { d.isLockTaskPermitted(activity.packageName) }.getOrDefault(false)) {
            runCatching { activity.startLockTask() }
        }
    }

    /**
     * The escape hatch. A device owner app cannot be uninstalled and adb cannot
     * remove it on a user build, so the app has to be able to release itself or
     * the tablet is stuck short of a factory reset.
     */
    fun release(activity: Activity) {
        runCatching { activity.stopLockTask() }
        if (!isDeviceOwner(activity)) return
        val d = dpm(activity)
        runCatching { d.setStatusBarDisabled(admin(activity), false) }
        runCatching { d.clearDeviceOwnerApp(activity.packageName) }
    }
}
