package com.skyora.weather.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.skyora.weather.domain.widget.DeviceVendor
import com.skyora.weather.domain.widget.DeviceVendorRules

/**
 * A settings screen owned by the phone manufacturer rather than by AOSP.
 *
 * Kept as plain strings instead of a [ComponentName] so the whole table can be
 * unit-tested on the JVM, where no Android framework is available.
 */
data class OemSettingsScreen(
    val packageName: String,
    val className: String,
    /** Some firmware builds expose only an action; tried if the component is absent. */
    val action: String? = null,
)

/**
 * Which manufacturer screen the "background updates" button should try.
 *
 * These manager activities are not part of any public API: their names come from
 * the ROMs themselves and change between versions, and a device can always be a
 * custom ROM that has none of them. So every entry is a *candidate* — the caller
 * tries them in order and falls back to the standard Android app-info screen
 * when none of them opens. Nothing here is a permission request, and none of it
 * asks Android to exempt SKYORA from battery optimisation
 * (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is deliberately never used).
 */
object AutoStartRules {

    /** Resolves the vendor from what the device reports about itself. */
    fun vendorFor(manufacturer: String, brand: String): DeviceVendor =
        DeviceVendorRules.detect(manufacturer, brand)

    /**
     * Candidate screens for [vendor], most likely first.
     *
     * Samsung and stock/Pixel devices return an empty list on purpose: neither
     * ships an auto-start manager, and the honest destination there is the
     * app-info screen where battery usage actually lives.
     */
    fun screensFor(vendor: DeviceVendor): List<OemSettingsScreen> = when (vendor) {
        DeviceVendor.XIAOMI -> listOf(
            OemSettingsScreen(
                packageName = "com.miui.securitycenter",
                className = "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
            OemSettingsScreen(
                packageName = "com.miui.securitycenter",
                className = "",
                action = "miui.intent.action.OP_AUTO_START",
            ),
        )

        DeviceVendor.HUAWEI -> listOf(
            OemSettingsScreen(
                packageName = "com.huawei.systemmanager",
                className = "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
            OemSettingsScreen(
                packageName = "com.huawei.systemmanager",
                className = "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
            ),
            // Newer Honor builds ship the same screen under their own package.
            OemSettingsScreen(
                packageName = "com.hihonor.systemmanager",
                className = "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
        )

        DeviceVendor.OPPO -> listOf(
            OemSettingsScreen(
                packageName = "com.coloros.safecenter",
                className = "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            ),
            OemSettingsScreen(
                packageName = "com.coloros.safecenter",
                className = "com.coloros.safecenter.startupapp.StartupAppListActivity",
            ),
            OemSettingsScreen(
                packageName = "com.oppo.safe",
                className = "com.oppo.safe.permission.startup.StartupAppListActivity",
            ),
        )

        DeviceVendor.VIVO -> listOf(
            OemSettingsScreen(
                packageName = "com.vivo.permissionmanager",
                className = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            ),
            OemSettingsScreen(
                packageName = "com.iqoo.secure",
                className = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            ),
        )

        DeviceVendor.SAMSUNG, DeviceVendor.OTHER -> emptyList()
    }

    /**
     * `true` when a manufacturer auto-start screen is expected, so the copy can
     * name it instead of promising a generic "settings" page.
     */
    fun hasAutoStartScreen(vendor: DeviceVendor): Boolean = screensFor(vendor).isNotEmpty()
}

private fun OemSettingsScreen.toIntent(): Intent {
    val intent = if (className.isNotEmpty()) {
        Intent().setComponent(ComponentName(packageName, className))
    } else {
        Intent().setPackage(packageName)
    }
    if (action != null) intent.action = action
    // Started from a screen, but NEW_TASK keeps this safe if the caller is ever
    // a widget or receiver.
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return intent
}

/**
 * Opens the best available background-usage / auto-start screen for [vendor].
 *
 * Each candidate is tried in turn; the first one that actually launches wins.
 * If none does — Samsung, Pixel, stock Android, a custom ROM, or a firmware
 * update that renamed the activity — it opens this app's info page through
 * [Settings.ACTION_APPLICATION_DETAILS_SETTINGS], which exists on every device.
 *
 * @return `true` if something opened, `false` if even the fallback failed.
 */
fun Context.openBackgroundSettings(vendor: DeviceVendor): Boolean {
    for (screen in AutoStartRules.screensFor(vendor)) {
        val intent = runCatching { screen.toIntent() }.getOrNull() ?: continue
        if (runCatching { startActivity(intent) }.isSuccess) return true
    }
    return runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
}
