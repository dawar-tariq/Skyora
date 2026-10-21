package com.skyora.weather.domain

import com.skyora.weather.domain.widget.WidgetNudgeRules
import com.skyora.weather.domain.widget.WidgetNudgeState
import com.skyora.weather.domain.widget.WidgetRefreshOutcome
import com.skyora.weather.domain.widget.WidgetRefreshStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When SKYORA is allowed to say "your widget stopped updating".
 *
 * The rule is deliberately narrow: it fires on an observable symptom, stays
 * silent for the cases it cannot explain (no widget, no network, fresh install),
 * and can be dismissed without being asked again all week.
 */
class WidgetNudgeRulesTest {

    private val now = 1_800_000_000_000L
    private val minute = 60_000L
    private val interval = 30L
    private val week = 7L * 24L * 60L * 60L * 1000L

    private fun status(
        outcome: WidgetRefreshOutcome = WidgetRefreshOutcome.SUCCESS,
        successAgeMinutes: Long? = 10L,
        attemptAgeMinutes: Long? = 10L,
    ) = WidgetRefreshStatus(
        outcome = outcome,
        lastAttemptMillis = attemptAgeMinutes?.let { now - it * minute } ?: 0L,
        lastSuccessMillis = successAgeMinutes?.let { now - it * minute } ?: 0L,
    )

    private fun attention(
        status: WidgetRefreshStatus,
        hasWidgets: Boolean = true,
        isOnline: Boolean = true,
    ) = WidgetNudgeRules.needsAttention(
        status = status,
        nowMillis = now,
        requestedIntervalMinutes = interval,
        hasWidgets = hasWidgets,
        isOnline = isOnline,
    )

    private fun banner(
        status: WidgetRefreshStatus,
        nudge: WidgetNudgeState = WidgetNudgeState(),
        hasWidgets: Boolean = true,
        isOnline: Boolean = true,
    ) = WidgetNudgeRules.shouldShowBanner(
        status = status,
        nudge = nudge,
        nowMillis = now,
        requestedIntervalMinutes = interval,
        hasWidgets = hasWidgets,
        isOnline = isOnline,
    )

    @Test
    fun `a healthy widget draws no attention`() {
        assertFalse(attention(status(successAgeMinutes = 5)))
        assertFalse(banner(status(successAgeMinutes = 5)))
    }

    @Test
    fun `a stale widget asks for attention`() {
        assertTrue(attention(status(successAgeMinutes = interval * 2 + 5)))
    }

    @Test
    fun `a failed refresh after an earlier success asks for attention`() {
        assertTrue(
            attention(
                status(WidgetRefreshOutcome.FAILED, successAgeMinutes = 20, attemptAgeMinutes = 1),
            ),
        )
    }

    @Test
    fun `no widget placed means nothing to talk about`() {
        val stale = status(successAgeMinutes = interval * 10)
        assertFalse(attention(stale, hasWidgets = false))
        assertFalse(banner(stale, hasWidgets = false))
    }

    @Test
    fun `without a network the app does not blame the phone`() {
        // The refresh failed, but the network is the obvious cause: stay quiet
        // rather than suggesting a battery-setting change.
        val failed = status(WidgetRefreshOutcome.FAILED, successAgeMinutes = 120)
        assertFalse(attention(failed, isOnline = false))
        assertFalse(banner(failed, isOnline = false))
    }

    @Test
    fun `a fresh install with no attempt yet stays quiet`() {
        val never = WidgetRefreshStatus(
            outcome = WidgetRefreshOutcome.NEVER,
            lastAttemptMillis = 0L,
            lastSuccessMillis = 0L,
        )
        assertFalse(attention(never))
        assertFalse(banner(never))
    }

    @Test
    fun `the banner stays up while the problem lasts`() {
        val stale = status(successAgeMinutes = interval * 3)
        assertTrue(banner(stale, nudge = WidgetNudgeState()))
    }

    @Test
    fun `dismissing hides the banner for the same episode`() {
        val stale = status(successAgeMinutes = interval * 3)
        val nudge = WidgetNudgeState(
            dismissedAtMillis = now - minute,
            dismissedMarker = WidgetNudgeRules.episodeMarker(stale),
        )
        assertFalse(banner(stale, nudge))
        // The gear dot is not a nag, so it keeps showing the state.
        assertTrue(attention(stale))
    }

    @Test
    fun `the banner returns when the widget recovered and broke again`() {
        val dismissedEpisode = status(successAgeMinutes = 300)
        val nudge = WidgetNudgeState(
            dismissedAtMillis = now - 10 * minute,
            dismissedMarker = WidgetNudgeRules.episodeMarker(dismissedEpisode),
        )
        // A later success moved the episode marker, and it is stale again.
        val brokenAgain = status(successAgeMinutes = interval * 3)
        assertTrue(banner(brokenAgain, nudge))
    }

    @Test
    fun `a week after dismissal it may mention the problem once more`() {
        val stale = status(successAgeMinutes = interval * 3)
        val nudge = WidgetNudgeState(
            dismissedAtMillis = now - week + minute,
            dismissedMarker = WidgetNudgeRules.episodeMarker(stale),
        )
        assertFalse("still in the quiet period", banner(stale, nudge))
        val later = WidgetNudgeState(
            dismissedAtMillis = now - week,
            dismissedMarker = WidgetNudgeRules.episodeMarker(stale),
        )
        assertTrue("quiet period is over", banner(stale, later))
    }

    @Test
    fun `never-succeeded has a stable episode marker`() {
        val failing = WidgetRefreshStatus(
            outcome = WidgetRefreshOutcome.FAILED,
            lastAttemptMillis = now - minute,
            lastSuccessMillis = 0L,
        )
        assertEquals(WidgetNudgeRules.NEVER_SUCCEEDED, WidgetNudgeRules.episodeMarker(failing))
        assertEquals(failing.lastSuccessMillis, 0L)
        // Marker is stable across repeated failures, so it is dismissed once.
        val nudge = WidgetNudgeState(
            dismissedAtMillis = now - minute,
            dismissedMarker = WidgetNudgeRules.NEVER_SUCCEEDED,
        )
        assertFalse(banner(failing, nudge))
    }

    @Test
    fun `an ageing success eventually becomes an episode`() {
        val recent = status(successAgeMinutes = 10)
        val old = status(successAgeMinutes = interval * 4)
        // Same journal shape, different age: only the old one is worth a word.
        assertFalse(attention(recent))
        assertTrue(attention(old))
    }
}
