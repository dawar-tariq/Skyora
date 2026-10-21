package com.skyora.weather.ui.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.core.util.NOTIFICATIONS
import com.skyora.weather.core.util.openNotificationSettings
import com.skyora.weather.core.util.rememberNowMillis
import com.skyora.weather.di.AppContainer
import com.skyora.weather.ui.designsystem.EmptyCitiesArt
import com.skyora.weather.ui.designsystem.ForecastPreviewArt
import com.skyora.weather.ui.designsystem.LocationRingsArt
import com.skyora.weather.ui.designsystem.NotificationAlertArt
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.SkyoraCard
import com.skyora.weather.ui.designsystem.SkyoraDivider
import com.skyora.weather.ui.designsystem.SkyoraIconButton
import com.skyora.weather.ui.designsystem.SkyoraPrimaryButton
import com.skyora.weather.ui.designsystem.SkyoraSegmentedControl
import com.skyora.weather.ui.designsystem.SkyoraStatusChip
import com.skyora.weather.ui.designsystem.SkyoraTextButton
import com.skyora.weather.ui.designsystem.rememberAmbientSkyMoment
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val badgeRes: Int?,
    val illustrationDescriptionRes: Int,
    val art: @Composable (Modifier) -> Unit,
    /** The last slide also hosts the real notification opt-in controls. */
    val hostsNotificationStep: Boolean = false,
)

private val Pages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_1_title,
        bodyRes = R.string.onboarding_1_body,
        badgeRes = R.string.onboarding_badge_forecast,
        illustrationDescriptionRes = R.string.onboarding_illustration_forecast,
        art = { modifier -> ForecastPreviewArt(modifier) },
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_2_title,
        bodyRes = R.string.onboarding_2_body,
        badgeRes = null,
        illustrationDescriptionRes = R.string.onboarding_illustration_location,
        art = { modifier -> LocationRingsArt(modifier) },
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_3_title,
        bodyRes = R.string.onboarding_3_body,
        badgeRes = null,
        illustrationDescriptionRes = R.string.onboarding_illustration_cities,
        art = { modifier -> EmptyCitiesArt(modifier) },
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_4_title,
        bodyRes = R.string.onboarding_4_body,
        badgeRes = R.string.onboarding_badge_notifications,
        illustrationDescriptionRes = R.string.onboarding_illustration_notifications,
        art = { modifier -> NotificationAlertArt(modifier) },
        hostsNotificationStep = true,
    ),
)

/** Which notification switches a permission request should turn on. */
private data class PendingEnable(val daily: Boolean, val rain: Boolean) {
    companion object {
        val NONE = PendingEnable(daily = false, rain = false)
    }
}

/**
 * First-launch slides. The last slide carries the real notification opt-in —
 * daily summary time, rain alerts and threshold — so the user never has to
 * discover these later in Settings. Completion is written through the
 * ViewModel only when the user skips or reaches the end, so a cold start
 * mid-onboarding shows the flow again rather than dumping the user on Home.
 */
@Composable
fun OnboardingScreen(
    container: AppContainer,
    onComplete: () -> Unit,
) {
    val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(container))
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()

    // The OS-level state, not just the runtime grant: the user may have turned
    // SKYORA notifications off in system settings.
    var notificationsAllowed by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    // Which switches the user asked for, so a granted permission can apply them.
    var pendingEnable by remember { mutableStateOf(PendingEnable.NONE) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (granted) {
            if (pendingEnable.daily) viewModel.enableDailyForecast(true)
            if (pendingEnable.rain) viewModel.setRainAlertsEnabled(true)
        }
        pendingEnable = PendingEnable.NONE
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun requestOrApply(pending: PendingEnable) {
        when {
            notificationsAllowed -> {
                if (pending.daily) viewModel.enableDailyForecast(true)
                if (pending.rain) viewModel.setRainAlertsEnabled(true)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                pendingEnable = pending
                notificationPermissionLauncher.launch(NOTIFICATIONS)
            }
            else -> {
                // Pre-13 there is no runtime dialog: notifications can only be
                // blocked from system settings, so send the user there.
                context.openNotificationSettings()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.completed.collect { onComplete() }
    }

    fun finish() {
        viewModel.completeOnboarding()
    }

    val isLastPage = pagerState.currentPage == Pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Midnight),
    ) {
        SkyoraBackdrop(
            moment = rememberAmbientSkyMoment(nowMillis = rememberNowMillis()),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SkyoraSpacing.screen, vertical = SkyoraSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                SkyoraTextButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = { finish() },
                    color = SkyoraColors.TextSecondary,
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                val page = Pages[pageIndex]
                val illustrationDescription = stringResource(page.illustrationDescriptionRes)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SkyoraSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(SkyoraSpacing.lg))
                    if (page.badgeRes != null) {
                        SkyoraStatusChip(text = stringResource(page.badgeRes))
                        Spacer(Modifier.height(SkyoraSpacing.xl))
                    } else {
                        Spacer(Modifier.height(SkyoraSpacing.xxl))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(232.dp)
                            .semantics { contentDescription = illustrationDescription },
                        contentAlignment = Alignment.Center,
                    ) {
                        page.art(Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(SkyoraSpacing.xxl))
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineLarge,
                        color = SkyoraColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(SkyoraSpacing.sm))
                    Text(
                        text = stringResource(page.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SkyoraColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    if (page.hostsNotificationStep) {
                        Spacer(Modifier.height(SkyoraSpacing.lg))
                        OnboardingNotificationStep(
                            state = notificationState,
                            allowed = notificationsAllowed,
                            onDailyToggle = { enabled ->
                                if (enabled) {
                                    requestOrApply(PendingEnable(daily = true, rain = false))
                                } else {
                                    viewModel.enableDailyForecast(false)
                                }
                            },
                            onTimeChange = viewModel::setDailyForecastTime,
                            onRainToggle = { enabled ->
                                if (enabled) {
                                    requestOrApply(PendingEnable(daily = false, rain = true))
                                } else {
                                    viewModel.setRainAlertsEnabled(false)
                                }
                            },
                            onThresholdChange = viewModel::setRainThreshold,
                            onOpenSettings = { context.openNotificationSettings() },
                        )
                    }
                    Spacer(Modifier.height(SkyoraSpacing.lg))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SkyoraSpacing.screen,
                        end = SkyoraSpacing.screen,
                        bottom = SkyoraSpacing.xl,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pages.indices.forEach { index ->
                        val selected = index == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (selected) 26.dp else 8.dp,
                            label = "dotWidth",
                        )
                        val color by animateColorAsState(
                            targetValue = if (selected) SkyoraColors.Cyan else SkyoraColors.BorderStrong,
                            label = "dotColor",
                        )
                        Box(
                            modifier = Modifier
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }
                Spacer(Modifier.height(SkyoraSpacing.lg))
                SkyoraPrimaryButton(
                    text = stringResource(
                        if (isLastPage) R.string.onboarding_start else R.string.onboarding_next,
                    ),
                    onClick = {
                        if (isLastPage) {
                            finish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                )
                Spacer(Modifier.height(SkyoraSpacing.xs))
                Text(
                    text = stringResource(
                        R.string.onboarding_page_indicator,
                        pagerState.currentPage + 1,
                        Pages.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyoraColors.TextTertiary,
                )
            }
        }
    }
}

/**
 * The real notification opt-in, embedded in onboarding.
 *
 * These are the same preferences the Settings screen edits; enabling a switch
 * here schedules work through the normal `NotificationScheduler` observer, so
 * there is no separate onboarding-only path. If permission is denied nothing
 * is enabled and the app stays fully usable.
 */
@Composable
private fun OnboardingNotificationStep(
    state: OnboardingNotificationState,
    allowed: Boolean,
    onDailyToggle: (Boolean) -> Unit,
    onTimeChange: (Int) -> Unit,
    onRainToggle: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    SkyoraCard(modifier = Modifier.fillMaxWidth()) {
        NotificationSwitchRow(
            title = stringResource(R.string.onboarding_notif_daily_title),
            description = stringResource(R.string.onboarding_notif_daily_body),
            checked = state.dailyForecastEnabled,
            onCheckedChange = onDailyToggle,
        )
        if (state.dailyForecastEnabled) {
            Spacer(Modifier.height(SkyoraSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                SkyoraIconButton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.onboarding_notif_earlier),
                    onClick = { onTimeChange(previousHalfHour(state.dailyMinutes)) },
                )
                Text(
                    text = stringResource(R.string.onboarding_notif_time, state.timeLabel),
                    style = MaterialTheme.typography.titleMedium,
                    color = SkyoraColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = SkyoraSpacing.sm),
                )
                SkyoraIconButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.onboarding_notif_later),
                    onClick = { onTimeChange(nextHalfHour(state.dailyMinutes)) },
                )
            }
        }

        SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xs))

        NotificationSwitchRow(
            title = stringResource(R.string.onboarding_notif_rain_title),
            description = stringResource(R.string.onboarding_notif_rain_body),
            checked = state.rainAlertsEnabled,
            onCheckedChange = onRainToggle,
        )
        if (state.rainAlertsEnabled) {
            Spacer(Modifier.height(SkyoraSpacing.xs))
            Text(
                text = stringResource(R.string.onboarding_notif_threshold, state.rainThresholdPercent),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
            Spacer(Modifier.height(SkyoraSpacing.xxs))
            SkyoraSegmentedControl(
                options = ONBOARDING_THRESHOLDS,
                selected = state.rainThresholdPercent,
                onSelected = onThresholdChange,
                label = { percent -> "$percent%" },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.cityName.isNotBlank()) {
            Spacer(Modifier.height(SkyoraSpacing.sm))
            Text(
                text = stringResource(R.string.onboarding_notif_city, state.cityName),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
        }

        if (state.anyEnabled && !allowed) {
            Spacer(Modifier.height(SkyoraSpacing.sm))
            Text(
                text = stringResource(R.string.onboarding_notif_denied),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.Warning,
            )
            SkyoraTextButton(
                text = stringResource(R.string.notif_permission_settings),
                onClick = onOpenSettings,
            )
        } else if (!allowed && !state.anyEnabled) {
            Spacer(Modifier.height(SkyoraSpacing.sm))
            Text(
                text = stringResource(R.string.onboarding_notif_permission_rationale),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
        }

        Spacer(Modifier.height(SkyoraSpacing.xs))
        Text(
            text = stringResource(R.string.onboarding_notif_local_note),
            style = MaterialTheme.typography.bodySmall,
            color = SkyoraColors.TextTertiary,
        )
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SkyoraColors.TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
        }
        Spacer(Modifier.width(SkyoraSpacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SkyoraColors.OnAccent,
                checkedTrackColor = SkyoraColors.Cyan,
                checkedBorderColor = SkyoraColors.Cyan,
                uncheckedThumbColor = SkyoraColors.TextTertiary,
                uncheckedTrackColor = SkyoraColors.NavySoft,
                uncheckedBorderColor = SkyoraColors.BorderStrong,
            ),
        )
    }
}

private fun nextHalfHour(minutes: Int): Int = ((minutes / 30 + 1) % 48) * 30

private fun previousHalfHour(minutes: Int): Int = ((minutes / 30 + 47) % 48) * 30

private val ONBOARDING_THRESHOLDS = listOf(40, 60, 80)
