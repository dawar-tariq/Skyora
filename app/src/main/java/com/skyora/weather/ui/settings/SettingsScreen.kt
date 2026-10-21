package com.skyora.weather.ui.settings

import android.Manifest
import android.os.Build
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.app.NotificationManagerCompat
import com.skyora.weather.core.util.openNotificationSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.core.util.LOCATION_PERMISSIONS
import com.skyora.weather.core.util.SkyoraLinks
import com.skyora.weather.core.util.hasAnyLocationPermission
import com.skyora.weather.core.util.hasFineLocationPermission
import com.skyora.weather.core.util.openAppSettings
import com.skyora.weather.core.util.openUrl
import com.skyora.weather.core.util.rememberNowMillis
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.widget.WidgetRefreshHealth
import com.skyora.weather.ui.designsystem.AttributionDialog
import com.skyora.weather.ui.designsystem.ScreenHeader
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.rememberAmbientSkyMoment
import com.skyora.weather.ui.designsystem.SkyoraCard
import com.skyora.weather.ui.designsystem.SkyoraConfirmDialog
import com.skyora.weather.ui.designsystem.SkyoraDialog
import com.skyora.weather.ui.designsystem.SkyoraDivider
import com.skyora.weather.ui.designsystem.SkyoraIconButton
import com.skyora.weather.ui.designsystem.SkyoraInfoDialog
import com.skyora.weather.ui.designsystem.SkyoraLoadingRow
import com.skyora.weather.ui.designsystem.SkyoraMark
import com.skyora.weather.ui.designsystem.SkyoraSectionLabel
import com.skyora.weather.ui.designsystem.SkyoraSegmentedControl
import com.skyora.weather.ui.designsystem.SkyoraSnackbarHost
import com.skyora.weather.ui.designsystem.SkyoraStatusChip
import com.skyora.weather.ui.designsystem.SkyoraTextButton
import com.skyora.weather.ui.designsystem.SkyoraWordmark
import com.skyora.weather.ui.designsystem.screenBottomPadding
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraRadius
import com.skyora.weather.ui.theme.SkyoraSpacing
import com.skyora.weather.ui.theme.SkyoraTextStyles
import kotlinx.coroutines.launch

private enum class SettingsDialog { ATTRIBUTION, PRIVACY, ABOUT, RESET }

/**
 * Settings covers units, location behaviour, data controls, notification
 * placeholders and the Open-Meteo attribution required by CC BY 4.0.
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onChangeHomeCity: () -> Unit,
    onOpenWidgetReliability: () -> Unit,
    onRestartOnboarding: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val widgetNeedsAttention = state.widgetHealth == WidgetRefreshHealth.STALE ||
        state.widgetHealth == WidgetRefreshHealth.RETRYING
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    var notificationsAllowed by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var locationAllowed by remember { mutableStateOf(context.hasAnyLocationPermission()) }
    var fineAllowed by remember { mutableStateOf(context.hasFineLocationPermission()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        locationAllowed = context.hasAnyLocationPermission()
        fineAllowed = context.hasFineLocationPermission()
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val fine = grants[com.skyora.weather.core.util.LOCATION_FINE] == true
        val coarse = grants[com.skyora.weather.core.util.LOCATION_COARSE] == true
        if (fine || coarse) {
            viewModel.enableAutomaticLocation(fine)
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (granted) {
            viewModel.enableNotifications()
        } else {
            viewModel.onNotificationPermissionDenied()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.SnackbarText -> snackbarHostState.showSnackbar(
                    context.getString(event.messageRes, *event.args.toTypedArray()),
                )

                is SettingsEvent.Snackbar -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
                SettingsEvent.OnboardingReset -> onRestartOnboarding()
            }
        }
    }

    fun openLink(url: String) {
        if (!context.openUrl(url)) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_link_failed)) }
        }
    }

    val celsiusLabel = stringResource(R.string.settings_celsius_short)
    val fahrenheitLabel = stringResource(R.string.settings_fahrenheit_short)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Ink),
    ) {
        SkyoraBackdrop(
            moment = rememberAmbientSkyMoment(nowMillis = rememberNowMillis()),
            modifier = Modifier.fillMaxSize(),
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
                ScreenHeader(title = stringResource(R.string.settings_title), onBack = onBack)
            }

            item(key = "units") {
                SettingsSection(title = stringResource(R.string.settings_units)) {
                    Text(
                        text = stringResource(R.string.settings_units_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyoraColors.TextTertiary,
                    )
                    Spacer(Modifier.height(SkyoraSpacing.sm))
                    SkyoraSegmentedControl(
                        options = listOf(TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT),
                        selected = state.unit,
                        onSelected = viewModel::setTemperatureUnit,
                        label = { unit ->
                            if (unit == TemperatureUnit.CELSIUS) celsiusLabel else fahrenheitLabel
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item(key = "location") {
                SettingsSection(title = stringResource(R.string.settings_location)) {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.MyLocation,
                        title = stringResource(R.string.settings_use_location),
                        description = stringResource(R.string.settings_use_location_desc),
                        checked = state.automaticLocationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (locationAllowed) {
                                    viewModel.enableAutomaticLocation(fineAllowed)
                                } else {
                                    permissionLauncher.launch(LOCATION_PERMISSIONS)
                                }
                            } else {
                                viewModel.disableAutomaticLocation()
                            }
                        },
                    )
                    if (state.isLocating) {
                        SkyoraLoadingRow(text = stringResource(R.string.loading_location))
                        Spacer(Modifier.height(SkyoraSpacing.xs))
                    }
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsValueRow(
                        icon = Icons.Outlined.Place,
                        title = stringResource(R.string.settings_home_city),
                        value = state.homeCity.name,
                        description = stringResource(R.string.settings_home_city_desc),
                        onClick = onChangeHomeCity,
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsValueRow(
                        icon = Icons.Outlined.LocationOn,
                        title = stringResource(R.string.settings_permission),
                        value = stringResource(
                            if (locationAllowed) {
                                R.string.settings_permission_granted
                            } else {
                                R.string.settings_permission_denied
                            },
                        ),
                        valueTint = if (locationAllowed) SkyoraColors.CyanSoft else SkyoraColors.Warning,
                        description = if (locationAllowed) {
                            null
                        } else {
                            stringResource(R.string.settings_open_system)
                        },
                        onClick = { context.openAppSettings() },
                    )
                }
            }

            item(key = "notifications") {
                SettingsSection(title = stringResource(R.string.settings_notifications_title)) {
                    Text(
                        text = stringResource(R.string.settings_notifications_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyoraColors.TextTertiary,
                    )
                    Spacer(Modifier.height(SkyoraSpacing.sm))

                    SettingsSwitchRow(
                        icon = Icons.Outlined.NotificationsNone,
                        title = stringResource(R.string.settings_notif_enable),
                        description = stringResource(R.string.settings_notif_enable_desc),
                        checked = state.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                viewModel.disableNotifications()
                            } else if (notificationsAllowed) {
                                viewModel.enableNotifications()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.enableNotifications()
                            }
                        },
                    )

                    val enabled = state.notificationsEnabled
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsSwitchRow(
                        icon = Icons.Outlined.WbSunny,
                        title = stringResource(R.string.settings_notif_daily),
                        description = stringResource(R.string.settings_notif_daily_desc),
                        checked = state.notifications.dailyForecastEnabled,
                        enabled = enabled,
                        onCheckedChange = viewModel::setDailyForecastEnabled,
                    )
                    SettingsValueRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.settings_notif_time),
                        value = state.notifications.dailyForecastTimeLabel,
                        description = stringResource(R.string.settings_notif_time_desc),
                        enabled = enabled && state.notifications.dailyForecastEnabled,
                        onClick = { showTimePicker = true },
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsSwitchRow(
                        icon = Icons.Outlined.WaterDrop,
                        title = stringResource(R.string.settings_notif_rain),
                        description = stringResource(R.string.settings_notif_rain_desc),
                        checked = state.notifications.rainAlertsEnabled,
                        enabled = enabled,
                        onCheckedChange = viewModel::setRainAlertsEnabled,
                    )
                    if (state.notifications.rainAlertsEnabled) {
                        Spacer(Modifier.height(SkyoraSpacing.sm))
                        Text(
                            text = stringResource(R.string.settings_notif_threshold) + " · " +
                                stringResource(
                                    R.string.settings_notif_threshold_value,
                                    state.notifications.rainThresholdPercent,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyoraColors.TextTertiary,
                        )
                        Spacer(Modifier.height(SkyoraSpacing.xs))
                        SkyoraSegmentedControl(
                            options = RAIN_THRESHOLDS,
                            selected = state.notifications.rainThresholdPercent,
                            onSelected = viewModel::setRainThreshold,
                            label = { percent ->
                                when (percent) {
                                    40 -> stringResource(R.string.settings_notif_threshold_light)
                                    60 -> stringResource(R.string.settings_notif_threshold_likely)
                                    else -> stringResource(R.string.settings_notif_threshold_very_likely)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsValueRow(
                        icon = Icons.Outlined.Place,
                        title = stringResource(R.string.settings_notif_city),
                        value = state.notifications.notificationLocation?.name
                            ?: stringResource(R.string.settings_notif_city_selected),
                        description = stringResource(R.string.settings_notif_city_desc),
                        enabled = enabled,
                        onClick = { showCityPicker = true },
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsBadgeRow(
                        icon = Icons.Outlined.Warning,
                        title = stringResource(R.string.settings_notif_severe),
                        description = stringResource(R.string.settings_notif_severe_unavailable),
                        badge = stringResource(R.string.settings_unavailable_badge),
                    )

                    if (!notificationsAllowed) {
                        Spacer(Modifier.height(SkyoraSpacing.sm))
                        Text(
                            text = stringResource(R.string.notif_permission_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyoraColors.Warning,
                        )
                        SkyoraTextButton(
                            text = stringResource(R.string.notif_permission_settings),
                            onClick = { context.openNotificationSettings() },
                        )
                    }
                }
            }

            item(key = "widgets") {
                SettingsSection(title = stringResource(R.string.settings_widgets)) {
                    SettingsNavRow(
                        icon = Icons.Outlined.Widgets,
                        title = stringResource(R.string.settings_widget_reliability),
                        // Only claim a problem when the journal actually shows
                        // one; otherwise stay descriptive.
                        description = if (widgetNeedsAttention) {
                            stringResource(R.string.settings_widget_reliability_attention)
                        } else {
                            stringResource(R.string.settings_widget_reliability_desc)
                        },
                        onClick = onOpenWidgetReliability,
                        attention = widgetNeedsAttention,
                    )
                }
            }

            item(key = "data") {
                SettingsSection(title = stringResource(R.string.settings_data)) {
                    SettingsNavRow(
                        icon = Icons.Outlined.DeleteSweep,
                        title = stringResource(R.string.settings_clear_cache),
                        description = stringResource(R.string.settings_clear_cache_desc),
                        onClick = viewModel::clearCachedWeather,
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsNavRow(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.settings_clear_recents),
                        description = stringResource(R.string.settings_clear_recents_desc),
                        onClick = viewModel::clearRecentSearches,
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsNavRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_license),
                        description = stringResource(R.string.settings_attribution_desc),
                        onClick = { dialog = SettingsDialog.ATTRIBUTION },
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsNavRow(
                        icon = Icons.Outlined.OpenInNew,
                        title = stringResource(R.string.settings_terms),
                        description = null,
                        onClick = { openLink(SkyoraLinks.OPEN_METEO_TERMS) },
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsNavRow(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.settings_privacy),
                        description = null,
                        onClick = { dialog = SettingsDialog.PRIVACY },
                    )
                    SkyoraDivider(modifier = Modifier.padding(vertical = SkyoraSpacing.xxs))
                    SettingsNavRow(
                        icon = Icons.Outlined.RestartAlt,
                        title = stringResource(R.string.settings_reset_onboarding),
                        description = stringResource(R.string.settings_reset_onboarding_desc),
                        onClick = { dialog = SettingsDialog.RESET },
                    )
                }
            }

            item(key = "about") {
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SkyoraMark(size = 44.dp)
                        Spacer(Modifier.height(SkyoraSpacing.xs))
                        SkyoraWordmark(fontSize = 22, letterSpacing = 8)
                        Spacer(Modifier.height(SkyoraSpacing.xxs))
                        Text(
                            text = stringResource(R.string.settings_version, state.versionName),
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyoraColors.TextTertiary,
                        )
                        Spacer(Modifier.height(SkyoraSpacing.sm))
                        Text(
                            text = stringResource(R.string.settings_api_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyoraColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(SkyoraSpacing.xxs))
                        SkyoraTextButton(
                            text = stringResource(R.string.settings_about_skyora),
                            onClick = { dialog = SettingsDialog.ABOUT },
                        )
                    }
                }
            }

            item(key = "footer") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onClick = { dialog = SettingsDialog.ATTRIBUTION }) {
                        Text(
                            text = stringResource(R.string.home_attribution),
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyoraColors.TextTertiary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyoraColors.TextTertiary,
                    )
                    Spacer(Modifier.height(SkyoraSpacing.sm))
                }
            }
        }

        SkyoraSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(SkyoraSpacing.md),
        )
    }

    if (showTimePicker) {
        NotificationTimeDialog(
            initialMinutes = state.notifications.dailyForecastMinutes,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes ->
                viewModel.setDailyForecastTime(minutes)
                showTimePicker = false
            },
        )
    }

    if (showCityPicker) {
        NotificationCityDialog(
            current = state.notifications.notificationLocation,
            selectedCity = state.selectedLocation,
            savedCities = state.savedCities,
            onDismiss = { showCityPicker = false },
            onSelect = { location ->
                viewModel.setNotificationLocation(location)
                showCityPicker = false
            },
            onUseSelected = {
                viewModel.setNotificationLocation(state.selectedLocation)
                showCityPicker = false
            },
        )
    }

    when (dialog) {
        SettingsDialog.ATTRIBUTION -> AttributionDialog(onDismiss = { dialog = null })
        SettingsDialog.PRIVACY -> SkyoraInfoDialog(
            title = stringResource(R.string.privacy_dialog_title),
            body = stringResource(R.string.privacy_dialog_body),
            onDismiss = { dialog = null },
        )

        SettingsDialog.ABOUT -> SkyoraInfoDialog(
            title = stringResource(R.string.about_dialog_title),
            body = stringResource(R.string.about_dialog_body),
            onDismiss = { dialog = null },
        )

        SettingsDialog.RESET -> SkyoraConfirmDialog(
            title = stringResource(R.string.settings_reset_confirm_title),
            body = stringResource(R.string.settings_reset_confirm_body),
            confirmLabel = stringResource(R.string.action_reset_onboarding),
            onConfirm = {
                dialog = null
                viewModel.resetOnboarding()
            },
            onDismiss = { dialog = null },
            destructive = true,
        )

        null -> Unit
    }
}

/** Rain alert thresholds offered in the UI, as precipitation probability. */
private val RAIN_THRESHOLDS = listOf(40, 60, 80)

@Composable
private fun NotificationTimeDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hour by rememberSaveable { mutableIntStateOf(initialMinutes / 60) }
    var minute by rememberSaveable { mutableIntStateOf((initialMinutes % 60) / 5 * 5) }
    SkyoraDialog(
        title = stringResource(R.string.settings_notif_time),
        body = stringResource(R.string.settings_notif_time_desc),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = { onConfirm(hour * 60 + minute) },
        dismissLabel = stringResource(R.string.action_cancel),
        onDismiss = onDismiss,
        extraContent = {
            StepperRow(
                label = stringResource(R.string.settings_notif_time),
                value = "%02d:%02d".format(hour, minute),
                onDecrease = {
                    if (minute == 0) {
                        minute = 55
                        hour = if (hour == 0) 23 else hour - 1
                    } else {
                        minute -= 5
                    }
                },
                onIncrease = {
                    if (minute >= 55) {
                        minute = 0
                        hour = (hour + 1) % 24
                    } else {
                        minute += 5
                    }
                },
            )
        },
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SkyoraIconButton(
            icon = Icons.Outlined.KeyboardArrowDown,
            contentDescription = label,
            onClick = onDecrease,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = SkyoraColors.TextPrimary,
        )
        SkyoraIconButton(
            icon = Icons.Outlined.KeyboardArrowUp,
            contentDescription = label,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun NotificationCityDialog(
    current: com.skyora.weather.domain.model.Location?,
    selectedCity: com.skyora.weather.domain.model.Location,
    savedCities: List<com.skyora.weather.domain.model.SavedCity>,
    onDismiss: () -> Unit,
    onSelect: (com.skyora.weather.domain.model.Location) -> Unit,
    onUseSelected: () -> Unit,
) {
    val candidates = buildList {
        add(selectedCity)
        savedCities.forEach { add(it.location) }
        current?.let { add(it) }
    }.distinctBy { it.id }

    SkyoraInfoDialog(
        title = stringResource(R.string.settings_notif_city_dialog_title),
        body = stringResource(R.string.settings_notif_city_desc),
        onDismiss = onDismiss,
        extraContent = {
            Column(verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.xxs)) {
                candidates.forEach { location ->
                    SettingsNavRow(
                        icon = Icons.Outlined.Place,
                        title = location.name,
                        description = location.subtitle.ifBlank { null },
                        onClick = { onSelect(location) },
                    )
                }
                SkyoraTextButton(
                    text = stringResource(R.string.settings_notif_city_selected),
                    onClick = onUseSelected,
                )
            }
        },
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SkyoraCard(modifier = Modifier.fillMaxWidth()) {
        SkyoraSectionLabel(text = title)
        Spacer(Modifier.height(SkyoraSpacing.xs))
        content()
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, tint: Color = SkyoraColors.Cyan) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SkyoraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
    ) {
        SettingsIcon(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SkyoraColors.TextPrimary,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyoraColors.TextTertiary,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SkyoraSwitchColors(),
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    description: String?,
    onClick: () -> Unit,
    attention: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = SkyoraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
    ) {
        SettingsIcon(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SkyoraColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (attention) SkyoraColors.Warning else SkyoraColors.TextTertiary,
                )
            }
        }
        if (attention) {
            // Same cyan dot as the Home gear: it survives dismissing the
            // banner, so the clue is still here next time the user looks.
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(SkyoraColors.Cyan, CircleShape),
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = SkyoraColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    value: String,
    description: String?,
    onClick: () -> Unit,
    valueTint: Color = SkyoraColors.CyanSoft,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = SkyoraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
    ) {
        SettingsIcon(icon, tint = if (enabled) SkyoraColors.Cyan else SkyoraColors.TextTertiary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SkyoraColors.TextPrimary,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyoraColors.TextTertiary,
                )
            }
        }
        Spacer(Modifier.width(SkyoraSpacing.xxs))
        Text(
            text = value,
            style = SkyoraTextStyles.SectionLabel.copy(letterSpacing = 0.4.sp),
            color = valueTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = SkyoraColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsBadgeRow(
    icon: ImageVector,
    title: String,
    description: String?,
    badge: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SkyoraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
    ) {
        SettingsIcon(icon, tint = SkyoraColors.TextTertiary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SkyoraColors.TextPrimary,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyoraColors.TextTertiary,
                )
            }
        }
        SkyoraStatusChip(text = badge)
    }
}

@Composable
private fun SkyoraSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = SkyoraColors.OnAccent,
    checkedTrackColor = SkyoraColors.Cyan,
    checkedBorderColor = SkyoraColors.Cyan,
    uncheckedThumbColor = SkyoraColors.TextTertiary,
    uncheckedTrackColor = SkyoraColors.NavySoft,
    uncheckedBorderColor = SkyoraColors.BorderStrong,
    disabledCheckedThumbColor = SkyoraColors.TextTertiary,
    disabledCheckedTrackColor = SkyoraColors.NavySoft,
    disabledCheckedBorderColor = SkyoraColors.Border,
    disabledUncheckedThumbColor = SkyoraColors.TextTertiary.copy(alpha = 0.5f),
    disabledUncheckedTrackColor = SkyoraColors.NavySoft.copy(alpha = 0.5f),
    disabledUncheckedBorderColor = SkyoraColors.Border,
)
