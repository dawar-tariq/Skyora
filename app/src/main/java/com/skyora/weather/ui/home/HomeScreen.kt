package com.skyora.weather.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.core.util.LOCATION_PERMISSIONS
import com.skyora.weather.core.util.findActivity
import com.skyora.weather.core.util.formatDateHeader
import com.skyora.weather.core.util.formatTimeOfDay
import com.skyora.weather.core.util.hasAnyLocationPermission
import com.skyora.weather.core.util.hasFineLocationPermission
import com.skyora.weather.core.util.openAppSettings
import com.skyora.weather.core.util.rememberNowMillis
import com.skyora.weather.core.util.shouldShowLocationRationale
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.LocationAccessState
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.ui.designsystem.AttributionDialog
import com.skyora.weather.ui.designsystem.BannerTone
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.SkyoraBanner
import com.skyora.weather.ui.designsystem.SkyoraIconButton
import com.skyora.weather.ui.designsystem.SkyoraLoadingRow
import com.skyora.weather.ui.designsystem.SkyoraMark
import com.skyora.weather.ui.designsystem.SkyoraSnackbarHost
import com.skyora.weather.ui.designsystem.SkyoraStatusChip
import com.skyora.weather.ui.designsystem.WeatherErrorState
import com.skyora.weather.ui.theme.LocalBottomBarInset
import com.skyora.weather.ui.theme.Manrope
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import com.skyora.weather.ui.theme.SkyoraTextStyles
import com.skyora.weather.ui.weather.WeatherDetailsContent
import com.skyora.weather.ui.weather.WeatherLoadingSkeleton
import com.skyora.weather.ui.weather.CachedWeatherBanner
import com.skyora.weather.ui.designsystem.WeatherVisualFamily
import com.skyora.weather.ui.designsystem.screenBottomPadding
import com.skyora.weather.ui.designsystem.visualFamily
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Home is a single scrollable column: brand bar, city header, status banners and
 * the shared weather dashboard. Pull-to-refresh wraps the whole list so the
 * gesture works even when an error state is showing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onChooseCity: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarHostState = remember { SnackbarHostState() }
    var showAttribution by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val fine = grants[com.skyora.weather.core.util.LOCATION_FINE] == true
        val coarse = grants[com.skyora.weather.core.util.LOCATION_COARSE] == true
        if (fine || coarse) {
            viewModel.onPermissionResult(fine, coarse)
        } else {
            viewModel.onPermissionDenied(
                permanentDenial = activity != null && !activity.shouldShowLocationRationale(),
            )
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResumed(
            hasFinePermission = context.hasFineLocationPermission(),
            hasCoarsePermission = context.hasAnyLocationPermission(),
        )
    }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.Snackbar -> {
                    val message = if (event.args.isEmpty()) {
                        context.getString(event.messageRes)
                    } else {
                        context.getString(event.messageRes, *event.args.toTypedArray())
                    }
                    val actionLabel = event.actionLabelRes?.let(context::getString)
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed &&
                        event.action == SnackbarAction.OPEN_APP_SETTINGS
                    ) {
                        context.openAppSettings()
                    }
                }
            }
        }
    }

    val nowMillis = rememberNowMillis()
    val zone = remember(state.snapshot?.timezone, state.location.timezone, state.snapshot?.utcOffsetSeconds) {
        state.snapshot?.zoneId()
            ?: state.location.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
    }
    val localNow = remember(nowMillis, zone) {
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
    }

    val family = state.snapshot?.currentCondition()?.visualFamily() ?: WeatherVisualFamily.CLEAR
    val isDay = state.snapshot?.current?.isDay ?: false
    val hasLocationPermission = context.hasAnyLocationPermission()
    val showSetupBanner = state.automaticLocationEnabled &&
        !state.location.isDeviceLocation &&
        !hasLocationPermission &&
        state.location.isSamePlaceAs(state.defaultCity) &&
        state.snapshot != null
    val approximateLocation = state.locationAccess is LocationAccessState.Success &&
        (state.locationAccess as LocationAccessState.Success).isApproximate

    fun requestLocation() {
        if (context.hasAnyLocationPermission()) {
            viewModel.locateNow(context.hasFineLocationPermission())
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Ink),
    ) {
        SkyoraBackdrop(
            family = family,
            isDay = isDay,
            modifier = Modifier.fillMaxSize(),
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = SkyoraSpacing.screen,
                    end = SkyoraSpacing.screen,
                    top = SkyoraSpacing.xs,
                    bottom = screenBottomPadding(extra = SkyoraSpacing.xxl),
                ),
                verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.lg),
            ) {
                item(key = "header") {
                    HomeHeader(
                        state = state,
                        localNow = localNow,
                        hasSnapshot = state.snapshot != null,
                        onOpenSearch = onOpenSearch,
                        onOpenSettings = onOpenSettings,
                        onLocate = ::requestLocation,
                    )
                }

                if (state.isLocating) {
                    item(key = "locating") {
                        SkyoraLoadingRow(text = stringResource(R.string.loading_location))
                    }
                }

                if (state.error != null && state.snapshot != null) {
                    item(key = "cached-banner") {
                        CachedWeatherBanner(
                            error = state.error ?: WeatherError.Unknown,
                            updatedAtMillis = state.snapshot?.fetchedAtEpochMillis ?: 0L,
                            zone = zone,
                            onRetry = viewModel::refresh,
                        )
                    }
                }

                if (showSetupBanner) {
                    item(key = "setup-banner") {
                        SkyoraBanner(
                            title = stringResource(R.string.home_default_banner_title),
                            message = stringResource(R.string.home_default_banner_body),
                            icon = Icons.Outlined.CloudOff,
                            tone = BannerTone.INFO,
                            actionLabel = stringResource(R.string.home_default_banner_action),
                            onAction = ::requestLocation,
                        )
                    }
                }

                if (approximateLocation && state.snapshot != null) {
                    item(key = "approximate-banner") {
                        SkyoraBanner(
                            title = stringResource(R.string.home_current_location),
                            message = stringResource(R.string.permission_approximate_note),
                            icon = Icons.Outlined.MyLocation,
                            tone = BannerTone.INFO,
                        )
                    }
                }

                val snapshot = state.snapshot
                when {
                    snapshot != null -> {
                        item(key = "weather") {
                            WeatherDetailsContent(
                                snapshot = snapshot,
                                unit = state.unit,
                                now = localNow,
                                nowMillis = nowMillis,
                                isSaved = state.isSaved,
                                onToggleSaved = { viewModel.toggleSaved(state.location.name) },
                                onAttributionClick = { showAttribution = true },
                                isStale = state.error != null,
                            )
                        }
                    }

                    state.isLoading -> {
                        item(key = "loading") {
                            WeatherLoadingSkeleton()
                        }
                    }

                    state.error != null -> {
                        item(key = "error") {
                            WeatherErrorState(
                                error = state.error ?: WeatherError.Unknown,
                                onRetry = viewModel::retry,
                                secondaryLabel = stringResource(R.string.action_find_city),
                                onSecondary = onChooseCity,
                            )
                        }
                    }

                    else -> {
                        item(key = "loading-fallback") {
                            WeatherLoadingSkeleton()
                        }
                    }
                }
            }
        }

        SkyoraSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = SkyoraSpacing.md)
                .padding(bottom = LocalBottomBarInset.current + SkyoraSpacing.sm),
        )
    }

    if (showAttribution) {
        AttributionDialog(onDismiss = { showAttribution = false })
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    localNow: LocalDateTime,
    hasSnapshot: Boolean,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onLocate: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkyoraMark(size = 26.dp)
            Spacer(Modifier.width(SkyoraSpacing.xs))
            Text(
                text = stringResource(R.string.brand_wordmark),
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 3.2.sp,
                color = SkyoraColors.TextSecondary,
            )
            Spacer(Modifier.weight(1f))
            SkyoraIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.cd_search),
                onClick = onOpenSearch,
            )
            Spacer(Modifier.width(SkyoraSpacing.xxs))
            SkyoraIconButton(
                icon = Icons.Outlined.MyLocation,
                contentDescription = stringResource(R.string.cd_use_location),
                onClick = onLocate,
                tint = if (state.isLocating) SkyoraColors.Cyan else SkyoraColors.TextPrimary,
                enabled = !state.isLocating,
            )
            Spacer(Modifier.width(SkyoraSpacing.xxs))
            SkyoraIconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                onClick = onOpenSettings,
            )
        }

        Spacer(Modifier.height(SkyoraSpacing.lg))

        Text(
            text = state.location.name,
            style = MaterialTheme.typography.headlineLarge,
            color = SkyoraColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = state.location.subtitle
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SkyoraColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!hasSnapshot) {
            Spacer(Modifier.height(SkyoraSpacing.xs))
            Text(
                text = formatDateHeader(localNow) + " · " + formatTimeOfDay(localNow),
                style = SkyoraTextStyles.SectionLabel,
                color = SkyoraColors.TextSecondary,
            )
        }

        val chipLabel = when {
            state.location.isDeviceLocation -> R.string.home_current_location
            !state.automaticLocationEnabled && state.location.isSamePlaceAs(state.defaultCity) -> R.string.city_home_label
            else -> null
        }
        if (chipLabel != null) {
            Spacer(Modifier.height(SkyoraSpacing.sm))
            SkyoraStatusChip(text = stringResource(chipLabel))
        }
    }
}
