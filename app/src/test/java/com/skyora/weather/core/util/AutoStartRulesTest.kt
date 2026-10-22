package com.skyora.weather.core.util

import com.skyora.weather.domain.widget.DeviceVendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which settings screen the onboarding "background updates" button opens.
 *
 * The vendor table is plain strings, so the whole decision can be tested here
 * without a device: which manufacturer maps where, that Samsung/Pixel get no
 * OEM candidate at all (and therefore the app-info fallback), and that nothing
 * can accidentally point at SKYORA itself.
 */
class AutoStartRulesTest {

    private fun vendorOf(manufacturer: String, brand: String = "") =
        AutoStartRules.vendorFor(manufacturer, brand)

    @Test
    fun `xiaomi opens the miui auto-start manager`() {
        val screens = AutoStartRules.screensFor(vendorOf("Xiaomi"))
        assertEquals("com.miui.securitycenter", screens.first().packageName)
        assertEquals(
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
            screens.first().className,
        )
        assertTrue(AutoStartRules.hasAutoStartScreen(vendorOf("Xiaomi")))
    }

    @Test
    fun `redmi and poco are xiaomi too`() {
        assertEquals(DeviceVendor.XIAOMI, vendorOf("Xiaomi", "Redmi"))
        assertEquals(DeviceVendor.XIAOMI, vendorOf("Xiaomi", "POCO"))
        assertEquals(DeviceVendor.XIAOMI, vendorOf("Xiaomi", "poco"))
    }

    @Test
    fun `oppo, realme and oneplus use the coloros startup list`() {
        for (manufacturer in listOf("OPPO", "realme", "OnePlus")) {
            val screens = AutoStartRules.screensFor(vendorOf(manufacturer))
            assertEquals(manufacturer, "com.coloros.safecenter", screens.first().packageName)
            assertTrue(screens.any { it.className.contains("StartupAppListActivity") })
        }
    }

    @Test
    fun `vivo and iqoo use the background startup manager`() {
        val screens = AutoStartRules.screensFor(vendorOf("vivo"))
        assertEquals("com.vivo.permissionmanager", screens.first().packageName)
        assertEquals(
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            screens.first().className,
        )
        assertEquals(DeviceVendor.VIVO, vendorOf("iQOO"))
    }

    @Test
    fun `huawei and honor get launch management`() {
        val screens = AutoStartRules.screensFor(vendorOf("HUAWEI"))
        assertTrue(screens.first().packageName.contains("systemmanager"))
        assertTrue(screens.first().className.contains("startupmgr"))
        // Newer Honor firmware moved the same screen to its own package.
        assertTrue(screens.any { it.packageName == "com.hihonor.systemmanager" })
        assertEquals(DeviceVendor.HUAWEI, vendorOf("HONOR"))
    }

    @Test
    fun `samsung gets no auto-start screen and falls back to app info`() {
        val vendor = vendorOf("samsung")
        assertEquals(DeviceVendor.SAMSUNG, vendor)
        assertTrue(AutoStartRules.screensFor(vendor).isEmpty())
        assertFalse(AutoStartRules.hasAutoStartScreen(vendor))
    }

    @Test
    fun `pixel and stock android fall back to app info`() {
        for (manufacturer in listOf("Google", "unknown", "")) {
            val vendor = vendorOf(manufacturer)
            assertEquals(DeviceVendor.OTHER, vendor)
            assertTrue(AutoStartRules.screensFor(vendor).isEmpty())
            assertFalse(AutoStartRules.hasAutoStartScreen(vendor))
        }
    }

    @Test
    fun `every oem candidate names a real activity somewhere else`() {
        DeviceVendor.entries.forEach { vendor ->
            val screens = AutoStartRules.screensFor(vendor)
            screens.forEach { screen ->
                assertTrue("$vendor has a blank package", screen.packageName.isNotBlank())
                // A blank class is only allowed when an action is tried instead.
                assertTrue(
                    "$vendor has an empty component",
                    screen.className.isNotBlank() || screen.action != null,
                )
                assertTrue(
                    "$vendor points at SKYORA itself",
                    screen.packageName != "com.skyora.weather",
                )
            }
            // Trying the same component twice would just be a wasted launch.
            assertEquals("$vendor repeats a screen", screens.size, screens.distinct().size)
        }
    }

    @Test
    fun `the xiaomi action fallback has no component`() {
        val actionCandidate = AutoStartRules.screensFor(DeviceVendor.XIAOMI)
            .single { it.action != null }
        assertEquals("miui.intent.action.OP_AUTO_START", actionCandidate.action)
        assertTrue(actionCandidate.className.isEmpty())
    }

    @Test
    fun `samsung and pixel are the only vendors without a native shortcut`() {
        val withoutShortcut = DeviceVendor.entries.filterNot(AutoStartRules::hasAutoStartScreen)
        assertEquals(listOf(DeviceVendor.SAMSUNG, DeviceVendor.OTHER), withoutShortcut)
    }
}
