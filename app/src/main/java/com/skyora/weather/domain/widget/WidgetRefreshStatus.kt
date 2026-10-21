package com.skyora.weather.domain.widget

/**
 * What the last background widget refresh did, in the user's terms.
 *
 * Only facts the app can actually observe are recorded. There is no attempt to
 * guess whether MIUI, HyperOS or any other OEM has silently restricted the
 * process — that cannot be read reliably, so the UI says so instead.
 */
enum class WidgetRefreshOutcome {
    /** No refresh has run since the status store was created. */
    NEVER,

    /** The last run fetched and rendered at least one widget. */
    SUCCESS,

    /** The last run could not reach the network or the API failed. */
    FAILED,
}

data class WidgetRefreshStatus(
    val outcome: WidgetRefreshOutcome = WidgetRefreshOutcome.NEVER,
    val lastAttemptMillis: Long = 0L,
    val lastSuccessMillis: Long = 0L,
) {
    val hasEverSucceeded: Boolean get() = lastSuccessMillis > 0L
}

/**
 * Which OEM guidance to show.
 *
 * Matching is by manufacturer/brand string, but the copy never claims a device
 * *has* a restriction — only that these devices are known to ship extra
 * background limits worth checking.
 */
enum class DeviceVendor { XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, OTHER }

object DeviceVendorRules {
    fun detect(manufacturer: String, brand: String): DeviceVendor {
        val id = "$manufacturer $brand".lowercase()
        return when {
            listOf("xiaomi", "redmi", "poco").any(id::contains) -> DeviceVendor.XIAOMI
            listOf("huawei", "honor").any(id::contains) -> DeviceVendor.HUAWEI
            listOf("oppo", "realme", "oneplus").any(id::contains) -> DeviceVendor.OPPO
            listOf("vivo", "iqoo").any(id::contains) -> DeviceVendor.VIVO
            id.contains("samsung") -> DeviceVendor.SAMSUNG
            else -> DeviceVendor.OTHER
        }
    }
}

/** How healthy the background refresh looks from what the app can observe. */
enum class WidgetRefreshHealth {
    /** Succeeded recently; nothing to explain. */
    HEALTHY,

    /** Last run failed but an earlier one worked: widget shows cached data. */
    RETRYING,

    /** Nothing has run yet (fresh install, or the job never got to run). */
    NEVER_RAN,

    /** Last success is older than the interval the app requested. */
    STALE,
}

/**
 * Classifies a refresh journal without touching Android APIs, so the rules are
 * unit-testable.
 *
 * [requestedIntervalMinutes] is the interval SKYORA *asked* WorkManager for.
 * Android is free to delay the job (Doze, app standby, OEM restrictions), so
 * [STALE] only means "older than requested", never "the device is broken".
 */
object WidgetRefreshHealthRules {

    /** Allow one missed window before calling the data stale. */
    const val STALE_AFTER_INTERVALS = 2

    fun classify(
        status: WidgetRefreshStatus,
        nowMillis: Long,
        requestedIntervalMinutes: Long,
    ): WidgetRefreshHealth {
        if (!status.hasEverSucceeded) {
            return if (status.lastAttemptMillis > 0L) {
                WidgetRefreshHealth.RETRYING
            } else {
                WidgetRefreshHealth.NEVER_RAN
            }
        }
        val age = (nowMillis - status.lastSuccessMillis).coerceAtLeast(0L)
        val limit = requestedIntervalMinutes * 60_000L * STALE_AFTER_INTERVALS
        if (age > limit) return WidgetRefreshHealth.STALE
        return if (status.outcome == WidgetRefreshOutcome.FAILED) {
            WidgetRefreshHealth.RETRYING
        } else {
            WidgetRefreshHealth.HEALTHY
        }
    }
}
