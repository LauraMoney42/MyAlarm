package com.kindcode.alarmhub.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

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

    private const val HOME_ALIAS = "com.kindcode.alarmhub.HomeAlias"

    /**
     * The everyday kiosk switch: whether the clock is a home screen.
     *
     * An app is always allowed to enable and disable its own components, which
     * is what makes this a toggle the user can own. Lock task, below, is the
     * stronger lock and needs device owner granted once over adb.
     */
    fun isHomeAliasEnabled(ctx: Context): Boolean {
        val state = ctx.packageManager.getComponentEnabledSetting(
            ComponentName(ctx.packageName, HOME_ALIAS)
        )
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun setHomeAlias(ctx: Context, enabled: Boolean) {
        runCatching {
            ctx.packageManager.setComponentEnabledSetting(
                ComponentName(ctx.packageName, HOME_ALIAS),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

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
