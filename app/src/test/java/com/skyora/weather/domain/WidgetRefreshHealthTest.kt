package com.skyora.weather.domain

import com.skyora.weather.domain.widget.DeviceVendor
import com.skyora.weather.domain.widget.DeviceVendorRules
import com.skyora.weather.domain.widget.WidgetRefreshHealth
import com.skyora.weather.domain.widget.WidgetRefreshHealthRules
import com.skyora.weather.domain.widget.WidgetRefreshOutcome
import com.skyora.weather.domain.widget.WidgetRefreshStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Widget reliability screen must never overstate what the app knows: these
 * cases pin the "healthy / retrying / stale / never ran" rules and the vendor
 * matching that picks which OEM guidance to show.
 */
class WidgetRefreshHealthTest {

    private val now = 1_800_000_000_000L
    private val minute = 60_000L
    private val interval = 30L

    private fun classify(
        outcome: WidgetRefreshOutcome,
        lastSuccessAgeMinutes: Long?,
        lastAttemptAgeMinutes: Long? = lastSuccessAgeMinutes,
    ): WidgetRefreshHealth = WidgetRefreshHealthRules.classify(
        status = WidgetRefreshStatus(
            outcome = outcome,
            lastAttemptMillis = lastAttemptAgeMinutes?.let { now - it * minute } ?: 0L,
            lastSuccessMillis = lastSuccessAgeMinutes?.let { now - it * minute } ?: 0L,
        ),
        nowMillis = now,
        requestedIntervalMinutes = interval,
    )

    @Test
    fun `a fresh success is healthy`() {
        assertEquals(
            WidgetRefreshHealth.HEALTHY,
            classify(WidgetRefreshOutcome.SUCCESS, lastSuccessAgeMinutes = 5),
        )
    }

    @Test
    fun `a success inside one missed window is still healthy`() {
        assertEquals(
            WidgetRefreshHealth.HEALTHY,
            classify(WidgetRefreshOutcome.SUCCESS, lastSuccessAgeMinutes = interval),
        )
    }

    @Test
    fun `older than two windows is stale`() {
        assertEquals(
            WidgetRefreshHealth.STALE,
            classify(WidgetRefreshOutcome.SUCCESS, lastSuccessAgeMinutes = interval * 2 + 1),
        )
    }

    @Test
    fun `a failed attempt after an earlier success is retrying`() {
        assertEquals(
            WidgetRefreshHealth.RETRYING,
            classify(
                WidgetRefreshOutcome.FAILED,
                lastSuccessAgeMinutes = 10,
                lastAttemptAgeMinutes = 1,
            ),
        )
    }

    @Test
    fun `a failure before any success is retrying, not never-ran`() {
        assertEquals(
            WidgetRefreshHealth.RETRYING,
            classify(WidgetRefreshOutcome.FAILED, lastSuccessAgeMinutes = null, lastAttemptAgeMinutes = 3),
        )
    }

    @Test
    fun `nothing recorded is never-ran`() {
        assertEquals(
            WidgetRefreshHealth.NEVER_RAN,
            classify(WidgetRefreshOutcome.NEVER, lastSuccessAgeMinutes = null, lastAttemptAgeMinutes = null),
        )
    }

    @Test
    fun `a future clock cannot make the data look fresher than it is`() {
        // lastSuccess in the future (clock skew) must not throw or go negative.
        val status = WidgetRefreshStatus(
            outcome = WidgetRefreshOutcome.SUCCESS,
            lastAttemptMillis = now + 10 * minute,
            lastSuccessMillis = now + 10 * minute,
        )
        assertEquals(
            WidgetRefreshHealth.HEALTHY,
            WidgetRefreshHealthRules.classify(status, now, interval),
        )
    }

    @Test
    fun `xiaomi brands all map to xiaomi guidance`() {
        listOf("Xiaomi", "Redmi", "POCO").forEach { brand ->
            assertEquals(
                brand,
                DeviceVendor.XIAOMI,
                DeviceVendorRules.detect(manufacturer = brand, brand = brand),
            )
        }
    }

    @Test
    fun `honor is grouped with huawei and iqoo with vivo`() {
        assertEquals(
            DeviceVendor.HUAWEI,
            DeviceVendorRules.detect(manufacturer = "HONOR", brand = "HONOR"),
        )
        assertEquals(
            DeviceVendor.VIVO,
            DeviceVendorRules.detect(manufacturer = "vivo", brand = "iQOO"),
        )
        assertEquals(
            DeviceVendor.OPPO,
            DeviceVendorRules.detect(manufacturer = "OnePlus", brand = "OnePlus"),
        )
    }

    @Test
    fun `an unknown manufacturer gets generic guidance rather than a guess`() {
        assertEquals(
            DeviceVendor.OTHER,
            DeviceVendorRules.detect(manufacturer = "Fairphone", brand = "Fairphone"),
        )
        assertEquals(
            DeviceVendor.OTHER,
            DeviceVendorRules.detect(manufacturer = "", brand = ""),
        )
    }
}
