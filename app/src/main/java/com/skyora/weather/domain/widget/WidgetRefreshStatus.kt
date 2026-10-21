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

/**
 * What the app has already said about widget health, so it does not repeat
 * itself.
 *
 * [dismissedMarker] is the [WidgetNudgeRules.episodeMarker] that was on screen
 * when the user dismissed the banner. A *different* marker means the widget
 * recovered and broke again, which is worth one fresh mention.
 */
data class WidgetNudgeState(
    val dismissedAtMillis: Long = 0L,
    val dismissedMarker: Long = Long.MIN_VALUE,
)

/**
 * Decides when SKYORA should mention that the widget has stopped updating.
 *
 * The trigger is a symptom the app can observe (the background job has not
 * succeeded recently), never a guess about a manufacturer setting, and it is
 * silent on a healthy device.
 */
object WidgetNudgeRules {

    /** After a dismissal, stay quiet for a week before mentioning it again. */
    const val DISMISS_COOLDOWN_MS = 7L * 24L * 60L * 60L * 1000L

    /**
     * Identifies the current unhealthy run.
     *
     * The last success does not change while refreshes keep failing, so it works
     * as an episode id: dismissing "hides" that episode, while a later success
     * followed by a new failure produces a different value and is mentioned
     * again.
     */
    fun episodeMarker(status: WidgetRefreshStatus): Long =
        if (status.hasEverSucceeded) status.lastSuccessMillis else NEVER_SUCCEEDED

    /**
     * `true` when the widget deserves a quiet nudge.
     *
     * Requires an actual widget on the home screen: with none placed there is
     * nothing to update, and telling the user otherwise would be wrong. A
     * [WidgetRefreshHealth.NEVER_RAN] journal means no background attempt has
     * happened at all (fresh install, work not yet due), which is not a fault.
     *
     * [isOnline] is a hard gate: a failed refresh while the phone has no network
     * is explained by the network, and blaming the device's battery settings for
     * it would be a lie. The page stays silent until there is something it can
     * honestly say.
     */
    fun needsAttention(
        status: WidgetRefreshStatus,
        nowMillis: Long,
        requestedIntervalMinutes: Long,
        hasWidgets: Boolean,
        isOnline: Boolean = true,
    ): Boolean {
        if (!hasWidgets) return false
        if (!isOnline) return false
        return when (
            WidgetRefreshHealthRules.classify(status, nowMillis, requestedIntervalMinutes)
        ) {
            WidgetRefreshHealth.HEALTHY -> false
            WidgetRefreshHealth.NEVER_RAN -> false
            WidgetRefreshHealth.RETRYING, WidgetRefreshHealth.STALE -> true
        }
    }

    /**
     * Whether the Home banner should be on screen.
     *
     * It stays up while the problem persists (like the cached-data banner) and
     * disappears once dismissed, unless the widget has since recovered and
     * failed again, or a week has passed.
     */
    fun shouldShowBanner(
        status: WidgetRefreshStatus,
        nudge: WidgetNudgeState,
        nowMillis: Long,
        requestedIntervalMinutes: Long,
        hasWidgets: Boolean,
        isOnline: Boolean = true,
    ): Boolean {
        if (!needsAttention(status, nowMillis, requestedIntervalMinutes, hasWidgets, isOnline)) {
            return false
        }
        if (nudge.dismissedAtMillis <= 0L) return true
        if (episodeMarker(status) != nudge.dismissedMarker) return true
        return nowMillis - nudge.dismissedAtMillis >= DISMISS_COOLDOWN_MS
    }

    /** Sentinel for "the background job has never succeeded". */
    const val NEVER_SUCCEEDED = -1L
}
