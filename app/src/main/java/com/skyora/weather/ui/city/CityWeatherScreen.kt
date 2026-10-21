package com.skyora.weather.ui.city

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.core.util.rememberNowMillis
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.ui.designsystem.AttributionDialog
import com.skyora.weather.ui.designsystem.SaveCityChip
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.SkyoraIconButton
import com.skyora.weather.ui.designsystem.SkyoraSecondaryButton
import com.skyora.weather.ui.designsystem.SkyoraSnackbarHost
import com.skyora.weather.ui.designsystem.SkyoraStatePanel
import com.skyora.weather.ui.designsystem.SkyoraStatusChip
import com.skyora.weather.ui.designsystem.SkyoraTopBar
import com.skyora.weather.ui.designsystem.WeatherErrorState
import com.skyora.weather.ui.designsystem.WeatherVisualFamily
import com.skyora.weather.ui.designsystem.visualFamily
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import com.skyora.weather.ui.weather.CachedWeatherBanner
import com.skyora.weather.ui.weather.WeatherDetailsContent
import com.skyora.weather.ui.weather.WeatherLoadingSkeleton
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Full forecast for a city opened from search or saved cities. It never touches
 * the device location: the Location passed in the route is the single source of
 * truth for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityWeatherScreen(container: AppContainer, location: Location?, onBack: () -> Unit) {
    if (location == null) {
        CityNotFoundScreen(onBack)
        return
    }
    val viewModel: CityWeatherViewModel = viewModel(
        key = "city-${location.id}",
        factory = CityWeatherViewModel.factory(container, location),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAttribution by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CityEvent.Snackbar -> {
                    val message = if (event.args.isEmpty()) {
                        context.getString(event.messageRes)
                    } else {
                        context.getString(event.messageRes, *event.args.toTypedArray())
                    }
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    val nowMillis = rememberNowMillis()
    val zone = remember(state.snapshot?.timezone, state.location.timezone) {
        state.snapshot?.zoneId()
            ?: state.location.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
    }
    val localNow = remember(nowMillis, zone) {
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
    }
    val snapshot = state.snapshot
    val family = snapshot?.currentCondition()?.visualFamily() ?: WeatherVisualFamily.CLEAR
    val isDay = snapshot?.current?.isDay ?: false

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
                SkyoraIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Spacer(Modifier.weight(1f))
                SaveCityChip(
                    saved = state.isSaved,
                    onClick = { viewModel.toggleSaved(state.location.name) },
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = SkyoraSpacing.screen,
                        end = SkyoraSpacing.screen,
                        top = SkyoraSpacing.xs,
                        bottom = SkyoraSpacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.lg),
                ) {
                    item(key = "title") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = state.location.name,
                                style = MaterialTheme.typography.headlineLarge,
                                color = SkyoraColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val subtitle = state.location.subtitle
                            if (subtitle.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyoraColors.TextSecondary,
                                )
                            }
                            Spacer(Modifier.height(SkyoraSpacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.xs)) {
                                if (state.isHomeCity) {
                                    SkyoraStatusChip(text = stringResource(R.string.city_home_label))
                                }
                                if (state.location.isDeviceLocation) {
                                    SkyoraStatusChip(text = stringResource(R.string.city_device_location_badge))
                                }
                            }
                        }
                    }

                    if (state.error != null && snapshot != null) {
                        item(key = "cached") {
                            CachedWeatherBanner(
                                error = state.error ?: WeatherError.Unknown,
                                updatedAtMillis = snapshot.fetchedAtEpochMillis,
                                zone = zone,
                                onRetry = viewModel::refresh,
                            )
                        }
                    }

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
                            if (!state.isHomeCity) {
                                item(key = "set-home") {
                                    SkyoraSecondaryButton(
                                        text = stringResource(R.string.city_set_home),
                                        onClick = { viewModel.setAsHome(state.location.name) },
                                        icon = Icons.Outlined.Home,
                                    )
                                }
                            }
                        }

                        state.isLoading -> {
                            item(key = "loading") { WeatherLoadingSkeleton() }
                        }

                        else -> {
                            item(key = "error") {
                                WeatherErrorState(
                                    error = state.error ?: WeatherError.Unknown,
                                    onRetry = viewModel::refresh,
                                )
                            }
                        }
                    }
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

    if (showAttribution) {
        AttributionDialog(onDismiss = { showAttribution = false })
    }
}

@Composable
private fun CityNotFoundScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Midnight),
    ) {
        SkyoraBackdrop(
            family = WeatherVisualFamily.CLEAR,
            isDay = false,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = SkyoraSpacing.screen),
        ) {
            SkyoraTopBar(
                title = stringResource(R.string.brand_wordmark),
                onBack = onBack,
            )
            SkyoraStatePanel(
                icon = Icons.Outlined.LocationOff,
                title = stringResource(R.string.search_no_results_title),
                message = stringResource(R.string.city_not_found),
                primaryLabel = stringResource(R.string.action_go_home),
                onPrimary = onBack,
            )
        }
    }
}
