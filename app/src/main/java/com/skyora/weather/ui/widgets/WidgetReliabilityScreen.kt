package com.skyora.weather.ui.widgets

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import com.skyora.weather.R
import com.skyora.weather.core.util.UpdateAge
import com.skyora.weather.core.util.openAppSettings
import com.skyora.weather.core.util.updateAge
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.SkyCondition
import com.skyora.weather.domain.model.SkyMoment
import com.skyora.weather.domain.model.SkyPhase
import com.skyora.weather.domain.widget.DeviceVendor
import com.skyora.weather.domain.widget.WidgetRefreshHealth
import com.skyora.weather.domain.widget.WidgetRefreshOutcome
import com.skyora.weather.ui.designsystem.ScreenHeader
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.SkyoraCard
import com.skyora.weather.ui.designsystem.SkyoraDivider
import com.skyora.weather.ui.designsystem.SkyoraPrimaryButton
import com.skyora.weather.ui.designsystem.SkyoraSectionLabel
import com.skyora.weather.ui.designsystem.SkyoraSnackbarHost
import com.skyora.weather.ui.designsystem.SkyoraTextButton
import com.skyora.weather.ui.designsystem.screenBottomPadding
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import kotlinx.coroutines.launch

/**
 * "Widget reliability": what SKYORA can observe about background refresh, in
 * plain language, plus per-manufacturer guidance the user may choose to follow.
 *
 * Deliberately not a wizard: nothing here is required for the app to work, no
 * exclusion from battery optimisation is requested automatically, and the page
 * states plainly which restrictions it cannot detect.
 */
@Composable
fun WidgetReliabilityScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: WidgetReliabilityViewModel = viewModel(
        factory = WidgetReliabilityViewModel.factory(container),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val needsHelp = state.health == WidgetRefreshHealth.STALE ||
        state.health == WidgetRefreshHealth.RETRYING

    fun report(count: Int) {
        scope.launch {
            val message = when {
                count <= 0 -> context.getString(R.string.reliability_no_widgets)
                count == 1 -> context.getString(R.string.reliability_refreshed_one, count)
                else -> context.getString(R.string.reliability_refreshed_many, count)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Ink),
    ) {
        // Same ambient treatment as the other utility screens: time-aware, but
        // never claiming a condition the app has not fetched.
        SkyoraBackdrop(
            moment = SkyMoment(SkyPhase.NIGHT, SkyCondition.CLOUDY),
            modifier = Modifier.fillMaxSize(),
            showLandscape = false,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = SkyoraSpacing.screen,
                end = SkyoraSpacing.screen,
                top = SkyoraSpacing.xs,
                bottom = screenBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.md),
        ) {
            item(key = "header") {
                ScreenHeader(title = stringResource(R.string.reliability_title), onBack = onBack)
            }

            item(key = "intro") {
                Column(verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.xs)) {
                    Text(
                        text = stringResource(R.string.reliability_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyoraColors.TextTertiary,
                    )
                    if (needsHelp) {
                        // Say the symptom in the same words the Home banner used,
                        // so the user knows they are in the right place.
                        Text(
                            text = healthLabel(state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyoraColors.Warning,
                        )
                    }
                }
            }

            // Ordering is the point: someone who arrived because the widget
            // stopped updating needs the fix first, while someone browsing the
            // page wants the numbers first.
            val order = if (needsHelp) {
                listOf(Section.HELP, Section.ACTION, Section.REFRESH, Section.SYSTEM)
            } else {
                listOf(Section.REFRESH, Section.SYSTEM, Section.HELP, Section.ACTION)
            }
            order.forEach { section ->
                item(key = section.name) {
                    when (section) {
                        Section.HELP -> HelpCard(
                            state = state,
                            onOpenBatterySettings = {
                                val intent = Intent(
                                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                                )
                                runCatching { context.startActivity(intent) }
                                    .onFailure { context.openAppSettings() }
                            },
                            onOpenAppInfo = { context.openAppSettings() },
                            onDismiss = viewModel::dismissGuide,
                            onRestore = viewModel::restoreGuide,
                        )

                        Section.ACTION -> SkyoraPrimaryButton(
                            text = stringResource(R.string.reliability_refresh_now),
                            onClick = { viewModel.refreshWidgetsNow { count -> report(count) } },
                            loading = state.isRefreshing,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Section.REFRESH -> RefreshStatusCard(state)
                        Section.SYSTEM -> SystemStatusCard(state)
                    }
                }
            }
        }

        SkyoraSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = SkyoraSpacing.md)
                .padding(bottom = SkyoraSpacing.xl),
        )
    }
}

@Composable
private fun StatusLine(
    icon: ImageVector,
    label: String,
    value: String,
    tone: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SkyoraColors.Cyan.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SkyoraColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        // Both sides get a weight so a long value ("Optimized (may delay
        // refresh)") cannot push the label to zero width or overlap it; it wraps
        // to a second line instead.
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = tone,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun lastSuccessLabel(state: WidgetReliabilityState): String {
    if (!state.status.hasEverSucceeded) return stringResource(R.string.reliability_never)
    return when (val age = updateAge(state.status.lastSuccessMillis, state.nowMillis)) {
        UpdateAge.JustNow -> stringResource(R.string.widget_updated_just_now)
        is UpdateAge.Minutes -> stringResource(R.string.widget_updated_minutes, age.minutes)
        is UpdateAge.Hours -> stringResource(R.string.widget_updated_hours, age.hours)
        UpdateAge.Older -> stringResource(R.string.widget_updated_older)
    }
}

@Composable
private fun outcomeLabel(state: WidgetReliabilityState): String = when (state.status.outcome) {
    WidgetRefreshOutcome.NEVER -> stringResource(R.string.reliability_outcome_never)
    WidgetRefreshOutcome.SUCCESS -> stringResource(R.string.reliability_outcome_success)
    WidgetRefreshOutcome.FAILED -> stringResource(R.string.reliability_outcome_failed)
}

@Composable
private fun workStateLabel(state: WidgetReliabilityState): String = when (state.periodicWorkState) {
    WorkInfo.State.ENQUEUED -> stringResource(R.string.reliability_work_enqueued)
    WorkInfo.State.RUNNING -> stringResource(R.string.reliability_work_running)
    WorkInfo.State.SUCCEEDED -> stringResource(R.string.reliability_work_succeeded)
    WorkInfo.State.FAILED -> stringResource(R.string.reliability_work_failed)
    WorkInfo.State.CANCELLED, WorkInfo.State.BLOCKED -> stringResource(R.string.reliability_work_cancelled)
    null -> stringResource(R.string.reliability_work_cancelled)
}

@Composable
private fun healthLabel(state: WidgetReliabilityState): String = when (state.health) {
    WidgetRefreshHealth.HEALTHY -> stringResource(R.string.reliability_health_healthy)
    WidgetRefreshHealth.RETRYING -> stringResource(R.string.reliability_health_retrying)
    WidgetRefreshHealth.STALE -> stringResource(
        R.string.reliability_health_stale,
        (state.intervalMinutes * 2).toInt(),
    )

    WidgetRefreshHealth.NEVER_RAN -> stringResource(R.string.reliability_health_never)
}

@Composable
private fun vendorGuidance(vendor: DeviceVendor): String = when (vendor) {
    DeviceVendor.XIAOMI -> stringResource(R.string.reliability_help_xiaomi)
    DeviceVendor.HUAWEI -> stringResource(R.string.reliability_help_huawei)
    DeviceVendor.OPPO -> stringResource(R.string.reliability_help_oppo)
    DeviceVendor.VIVO -> stringResource(R.string.reliability_help_vivo)
    DeviceVendor.SAMSUNG -> stringResource(R.string.reliability_help_samsung)
    DeviceVendor.OTHER -> stringResource(R.string.reliability_help_generic)
}

/** Which card is showing; also the order they appear in. */
private enum class Section { HELP, ACTION, REFRESH, SYSTEM }

@Composable
private fun RefreshStatusCard(state: WidgetReliabilityState) {
    SkyoraCard(modifier = Modifier.fillMaxWidth()) {
        SkyoraSectionLabel(text = stringResource(R.string.reliability_section_refresh))
        Spacer(Modifier.height(SkyoraSpacing.xs))
        StatusLine(
            icon = Icons.Outlined.Refresh,
            label = stringResource(R.string.reliability_last_success),
            value = lastSuccessLabel(state),
            tone = when (state.health) {
                WidgetRefreshHealth.HEALTHY -> SkyoraColors.CyanSoft
                WidgetRefreshHealth.RETRYING,
                WidgetRefreshHealth.STALE,
                -> SkyoraColors.Warning

                WidgetRefreshHealth.NEVER_RAN -> SkyoraColors.TextSecondary
            },
        )
        SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xs))
        StatusLine(
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.reliability_last_outcome),
            value = outcomeLabel(state),
            tone = if (state.status.outcome == WidgetRefreshOutcome.FAILED) {
                SkyoraColors.Warning
            } else {
                SkyoraColors.TextSecondary
            },
        )
        SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xs))
        StatusLine(
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.reliability_work_state),
            value = workStateLabel(state),
            tone = SkyoraColors.TextSecondary,
        )
        SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xs))
        StatusLine(
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.reliability_interval),
            value = stringResource(
                R.string.reliability_interval_value,
                state.intervalMinutes.toInt(),
            ),
            tone = SkyoraColors.TextSecondary,
        )
        Spacer(Modifier.height(SkyoraSpacing.xs))
        Text(
            text = stringResource(R.string.reliability_interval_note),
            style = MaterialTheme.typography.bodySmall,
            color = SkyoraColors.TextTertiary,
        )
    }
}

@Composable
private fun SystemStatusCard(state: WidgetReliabilityState) {
    SkyoraCard(modifier = Modifier.fillMaxWidth()) {
        SkyoraSectionLabel(text = stringResource(R.string.reliability_section_system))
        Spacer(Modifier.height(SkyoraSpacing.xs))
        StatusLine(
            icon = Icons.Outlined.BatteryAlert,
            label = stringResource(R.string.reliability_battery_optimized),
            value = when (state.batteryUnrestricted) {
                true -> stringResource(R.string.reliability_battery_unrestricted)
                false -> stringResource(R.string.reliability_battery_restricted)
                null -> stringResource(R.string.reliability_battery_unknown)
            },
            tone = if (state.batteryUnrestricted == false) {
                SkyoraColors.Warning
            } else {
                SkyoraColors.TextSecondary
            },
        )
        if (state.batteryUnrestricted == false) {
            Text(
                text = stringResource(R.string.reliability_battery_note),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
                modifier = Modifier.padding(top = SkyoraSpacing.xxs),
            )
        }
        if (state.backgroundRestricted != null) {
            SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xs))
            StatusLine(
                icon = Icons.Outlined.BatteryAlert,
                label = stringResource(R.string.reliability_background_usage),
                value = stringResource(
                    if (state.backgroundRestricted == true) {
                        R.string.reliability_background_restricted
                    } else {
                        R.string.reliability_background_allowed
                    },
                ),
                tone = if (state.backgroundRestricted == true) {
                    SkyoraColors.Warning
                } else {
                    SkyoraColors.TextSecondary
                },
            )
        }
        SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xs))
        StatusLine(
            icon = Icons.Outlined.Widgets,
            label = stringResource(R.string.reliability_widget_count),
            value = state.widgetCount.toString(),
            tone = SkyoraColors.TextSecondary,
        )
    }
}

@Composable
private fun HelpCard(
    state: WidgetReliabilityState,
    onOpenBatterySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    SkyoraCard(modifier = Modifier.fillMaxWidth()) {
        SkyoraSectionLabel(text = stringResource(R.string.reliability_section_help))
        Spacer(Modifier.height(SkyoraSpacing.xs))
        Text(
            text = stringResource(R.string.reliability_help_body),
            style = MaterialTheme.typography.bodySmall,
            color = SkyoraColors.TextSecondary,
        )
        if (!state.guideDismissed) {
            Spacer(Modifier.height(SkyoraSpacing.sm))
            Text(
                text = vendorGuidance(state.vendor),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextSecondary,
            )
            Spacer(Modifier.height(SkyoraSpacing.xs))
            Text(
                text = stringResource(R.string.reliability_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
            Spacer(Modifier.height(SkyoraSpacing.sm))
            // The list of apps with battery restrictions, not a direct
            // exemption request: the user picks SKYORA themselves.
            SkyoraTextButton(
                text = stringResource(R.string.reliability_open_settings),
                onClick = onOpenBatterySettings,
            )
            SkyoraTextButton(
                text = stringResource(R.string.reliability_open_app_info),
                onClick = onOpenAppInfo,
            )
            SkyoraTextButton(
                text = stringResource(R.string.reliability_dismiss),
                onClick = onDismiss,
                color = SkyoraColors.TextTertiary,
            )
        } else {
            SkyoraTextButton(
                text = stringResource(R.string.reliability_restore_help),
                onClick = onRestore,
            )
        }
    }
}
