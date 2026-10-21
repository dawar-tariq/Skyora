package com.skyora.weather.ui.saved

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.core.util.formatTemperature
import com.skyora.weather.core.util.rememberNowMillis
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.MoveDirection
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.ui.designsystem.AttributionDialog
import com.skyora.weather.ui.designsystem.BannerTone
import com.skyora.weather.ui.designsystem.EmptyCitiesArt
import com.skyora.weather.ui.designsystem.ScreenHeader
import com.skyora.weather.ui.designsystem.SkeletonBlock
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.rememberSkyMoment
import com.skyora.weather.ui.designsystem.SkyoraBanner
import com.skyora.weather.ui.designsystem.SkyoraCard
import com.skyora.weather.ui.designsystem.SkyoraConfirmDialog
import com.skyora.weather.ui.designsystem.SkyoraIconButton
import com.skyora.weather.ui.designsystem.SkyoraPrimaryButton
import com.skyora.weather.ui.designsystem.SkyoraSnackbarHost
import com.skyora.weather.ui.designsystem.SkyoraTextButton
import com.skyora.weather.ui.designsystem.WeatherIcon
import com.skyora.weather.ui.designsystem.weatherIconRes
import com.skyora.weather.ui.designsystem.screenBottomPadding
import com.skyora.weather.ui.theme.LocalBottomBarInset
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import com.skyora.weather.ui.weather.ShortConditionLabel

/**
 * Saved cities are a user-owned list: add, remove (with undo), reorder and open.
 * The list is persisted in DataStore and de-duplicated by stable location id, so
 * the same city can never appear twice after a search or a GPS fix.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedCitiesScreen(
    container: AppContainer,
    onOpenCity: (com.skyora.weather.domain.model.Location) -> Unit,
    onAddCity: () -> Unit,
) {
    val viewModel: SavedCitiesViewModel = viewModel(factory = SavedCitiesViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<SavedCityRow?>(null) }
    var showAttribution by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SavedEvent.Removed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.saved_undo_removed, event.displayName),
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoRemove()
                    }
                }

                SavedEvent.Restored -> Unit
            }
        }
    }

    val nowMillis = rememberNowMillis()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Ink),
    ) {
        SkyoraBackdrop(
            moment = rememberSkyMoment(
                snapshot = state.rows.firstOrNull { it.snapshot != null }?.snapshot,
                nowMillis = nowMillis,
                fallbackZone = state.rows.firstOrNull()?.saved?.location?.timezone
                    ?.let { runCatching { ZoneId.of(it) }.getOrNull() },
            ),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SkyoraSpacing.screen),
            ) {
                Spacer(Modifier.height(SkyoraSpacing.xs))
                ScreenHeader(
                    title = stringResource(R.string.saved_title),
                    subtitle = stringResource(R.string.saved_subtitle),
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.rows.isNotEmpty()) {
                                SkyoraTextButton(
                                    text = stringResource(
                                        if (isEditing) R.string.saved_done else R.string.saved_edit,
                                    ),
                                    onClick = { isEditing = !isEditing },
                                )
                            }
                            SkyoraIconButton(
                                icon = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.saved_add_city),
                                onClick = onAddCity,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(SkyoraSpacing.sm))
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = SkyoraSpacing.screen,
                        end = SkyoraSpacing.screen,
                        top = SkyoraSpacing.xxs,
                        bottom = screenBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
                ) {
                    if (state.hasPartialFailures) {
                        item(key = "partial-error") {
                            SkyoraBanner(
                                title = stringResource(R.string.error_saved_refresh),
                                message = stringResource(R.string.saved_partial_error),
                                tone = BannerTone.WARNING,
                                actionLabel = stringResource(R.string.action_retry),
                                onAction = viewModel::refresh,
                            )
                        }
                    }

                    if (isEditing && state.rows.isNotEmpty()) {
                        item(key = "reorder-hint") {
                            Text(
                                text = stringResource(R.string.saved_reorder_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = SkyoraColors.TextTertiary,
                            )
                        }
                    }

                    when {
                        state.isLoading && state.rows.isEmpty() -> {
                            items(count = 3, key = { index -> "skeleton-$index" }) {
                                SkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(88.dp),
                                    cornerRadius = 24.dp,
                                )
                            }
                        }

                        state.rows.isEmpty() -> {
                            item(key = "empty") {
                                EmptySavedCities(onAddCity = onAddCity)
                            }
                        }

                        else -> {
                            items(
                                items = state.rows,
                                key = { row -> row.saved.location.id },
                            ) { row ->
                                val index = state.rows.indexOf(row)
                                SavedCityCard(
                                    row = row,
                                    unit = state.unit,
                                    isEditing = isEditing,
                                    isFirst = index == 0,
                                    isLast = index == state.rows.lastIndex,
                                    onClick = { onOpenCity(row.saved.location) },
                                    onMoveUp = {
                                        viewModel.move(row.saved.location.id, MoveDirection.UP)
                                    },
                                    onMoveDown = {
                                        viewModel.move(row.saved.location.id, MoveDirection.DOWN)
                                    },
                                    onRemove = { pendingRemoval = row },
                                )
                            }

                            item(key = "attribution") {
                                TextButton(
                                    onClick = { showAttribution = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(R.string.saved_attribution),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SkyoraColors.TextTertiary,
                                    )
                                }
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
                .padding(horizontal = SkyoraSpacing.md)
                .padding(bottom = LocalBottomBarInset.current + SkyoraSpacing.sm),
        )
    }

    pendingRemoval?.let { row ->
        SkyoraConfirmDialog(
            title = stringResource(R.string.saved_remove_title, row.saved.location.name),
            body = stringResource(R.string.saved_remove_body),
            confirmLabel = stringResource(R.string.action_remove),
            onConfirm = {
                viewModel.remove(row.saved, row.saved.location.name)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
            destructive = true,
        )
    }

    if (showAttribution) {
        AttributionDialog(onDismiss = { showAttribution = false })
    }
}

@Composable
private fun EmptySavedCities(onAddCity: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SkyoraSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyCitiesArt(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        )
        Spacer(Modifier.height(SkyoraSpacing.lg))
        Text(
            text = stringResource(R.string.saved_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = SkyoraColors.TextPrimary,
        )
        Spacer(Modifier.height(SkyoraSpacing.xs))
        Text(
            text = stringResource(R.string.saved_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SkyoraColors.TextSecondary,
            modifier = Modifier.padding(horizontal = SkyoraSpacing.sm),
        )
        Spacer(Modifier.height(SkyoraSpacing.xl))
        SkyoraPrimaryButton(
            text = stringResource(R.string.saved_find_city),
            onClick = onAddCity,
            icon = Icons.Outlined.Add,
        )
    }
}

@Composable
private fun SavedCityCard(
    row: SavedCityRow,
    unit: TemperatureUnit,
    isEditing: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val location = row.saved.location
    val snapshot = row.snapshot
    val temperature = snapshot?.let { formatTemperature(it.current.temperatureC, unit) }
    val condition = snapshot?.let { ShortConditionLabel(it.current.weatherCode) }
    val high = snapshot?.today()?.let { formatTemperature(it.tempMaxC, unit) }
    val low = snapshot?.today()?.let { formatTemperature(it.tempMinC, unit) }

    SkyoraCard(
        onClick = if (isEditing) null else onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = SkyoraColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = location.subtitle
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyoraColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (high != null && low != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_high_low_compact, high, low),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyoraColors.TextTertiary,
                    )
                }
            }
            Spacer(Modifier.width(SkyoraSpacing.sm))
            when {
                snapshot != null -> {
                    WeatherIcon(
                        resId = weatherIconRes(snapshot),
                        size = 30.dp,
                        contentDescription = condition,
                    )
                    Spacer(Modifier.width(SkyoraSpacing.xs))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = temperature ?: stringResource(R.string.value_unavailable),
                            style = MaterialTheme.typography.headlineSmall,
                            color = SkyoraColors.TextPrimary,
                        )
                        if (condition != null) {
                            Text(
                                text = condition,
                                style = MaterialTheme.typography.bodySmall,
                                color = SkyoraColors.TextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }

                row.error != null -> {
                    Text(
                        text = stringResource(R.string.value_unavailable),
                        style = MaterialTheme.typography.headlineSmall,
                        color = SkyoraColors.TextTertiary,
                    )
                }

                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = SkyoraColors.Cyan,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        if (isEditing) {
            Spacer(Modifier.height(SkyoraSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkyoraIconButton(
                    icon = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.saved_move_up, location.name),
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    containerColor = Color.Transparent,
                    showBorder = false,
                )
                SkyoraIconButton(
                    icon = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.saved_move_down, location.name),
                    onClick = onMoveDown,
                    enabled = !isLast,
                    containerColor = Color.Transparent,
                    showBorder = false,
                )
                SkyoraIconButton(
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.saved_delete, location.name),
                    onClick = onRemove,
                    tint = SkyoraColors.Error,
                    containerColor = SkyoraColors.Error.copy(alpha = 0.10f),
                    showBorder = false,
                )
            }
        }
    }
}
