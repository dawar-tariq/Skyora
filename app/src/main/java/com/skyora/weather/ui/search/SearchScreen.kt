package com.skyora.weather.ui.search

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.core.util.SkyoraLinks
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.DefaultLocations
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.ui.designsystem.BannerTone
import com.skyora.weather.ui.designsystem.BrandLockup
import com.skyora.weather.ui.designsystem.DialogLink
import com.skyora.weather.ui.designsystem.ScreenHeader
import com.skyora.weather.ui.designsystem.SkeletonBlock
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.SkyoraBanner
import com.skyora.weather.ui.designsystem.SkyoraCard
import com.skyora.weather.ui.designsystem.SkyoraConfirmDialog
import com.skyora.weather.ui.designsystem.SkyoraInfoDialog
import com.skyora.weather.ui.designsystem.SkyoraSectionHeader
import com.skyora.weather.ui.designsystem.SkyoraStatePanel
import com.skyora.weather.ui.designsystem.SkyoraTextButton
import com.skyora.weather.ui.designsystem.WeatherErrorState
import com.skyora.weather.ui.designsystem.WeatherVisualFamily
import com.skyora.weather.ui.designsystem.screenBottomPadding
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraRadius
import com.skyora.weather.ui.theme.SkyoraSpacing
import com.skyora.weather.ui.theme.SkyoraTextStyles

/**
 * Search and selection screen. The mode decides what a result tap does:
 * browse opens the city forecast, pick sets the Home city, add saves the city.
 * The geocoding request itself is debounced inside the ViewModel.
 */
@Composable
fun SearchScreen(
    container: AppContainer,
    mode: SearchMode,
    onBack: (() -> Unit)?,
    onOpenCity: (Location) -> Unit,
    onDone: () -> Unit,
) {
    val viewModel: SearchViewModel = viewModel(
        key = "search-${mode.name}",
        factory = SearchViewModel.factory(container, mode),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    var confirmClearRecents by rememberSaveable { mutableStateOf(false) }
    var showAttribution by rememberSaveable { mutableStateOf(false) }

    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val title = when (mode) {
        SearchMode.BROWSE -> stringResource(R.string.search_title)
        SearchMode.PICK_HOME -> stringResource(R.string.search_pick_title)
        SearchMode.ADD_CITY -> stringResource(R.string.saved_add_city)
    }
    val subtitle = when (mode) {
        SearchMode.BROWSE -> null
        SearchMode.PICK_HOME -> stringResource(R.string.search_pick_body)
        SearchMode.ADD_CITY -> stringResource(R.string.saved_subtitle)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.CitySelected -> when (mode) {
                    SearchMode.BROWSE -> onOpenCity(event.location)
                    SearchMode.PICK_HOME, SearchMode.ADD_CITY -> onDone()
                }
            }
        }
    }

    fun select(location: Location) {
        keyboardController?.hide()
        viewModel.onCitySelected(location)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Ink),
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
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SkyoraSpacing.screen),
            ) {
                Spacer(Modifier.height(SkyoraSpacing.xs))
                if (onBack != null) {
                    BrandLockup(compact = true, markSize = 28.dp)
                    Spacer(Modifier.height(SkyoraSpacing.xs))
                }
                ScreenHeader(
                    title = title,
                    subtitle = subtitle,
                    onBack = onBack,
                )
                Spacer(Modifier.height(SkyoraSpacing.md))
                SkyoraSearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onClear = viewModel::clearQuery,
                    onSearchAction = { keyboardController?.hide() },
                )
                Spacer(Modifier.height(SkyoraSpacing.md))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = SkyoraSpacing.screen,
                    end = SkyoraSpacing.screen,
                    top = SkyoraSpacing.xxs,
                    bottom = screenBottomPadding() + SkyoraSpacing.xs,
                ),
                verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
            ) {
                if (!state.isOnline) {
                    item(key = "offline") {
                        SkyoraBanner(
                            title = stringResource(R.string.error_offline_title),
                            message = stringResource(
                                if (state.recentSearches.isEmpty()) {
                                    R.string.search_offline_body
                                } else {
                                    R.string.search_offline_cached_body
                                },
                            ),
                            tone = BannerTone.WARNING,
                        )
                    }
                }

                when {
                    state.results.isNotEmpty() -> {
                        item(key = "results-header") {
                            SkyoraSectionHeader(
                                title = stringResource(R.string.search_results),
                                trailing = {
                                    Text(
                                        text = stringResource(R.string.search_result_count, state.results.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SkyoraColors.TextTertiary,
                                    )
                                },
                            )
                        }
                        items(
                            items = state.results,
                            key = { location -> "result-${location.id}" },
                        ) { location ->
                            CityResultRow(location = location, icon = Icons.Outlined.Place) {
                                select(location)
                            }
                        }
                        if (state.isSearching) {
                            item(key = "results-updating") {
                                Text(
                                    text = stringResource(R.string.search_searching),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SkyoraColors.TextTertiary,
                                )
                            }
                        }
                    }

                    state.isSearching -> {
                        items(count = 4, key = { index -> "loading-$index" }) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(vertical = SkyoraSpacing.xxs),
                            )
                        }
                    }

                    state.error != null -> {
                        item(key = "search-error") {
                            WeatherErrorState(
                                error = state.error ?: WeatherError.Unknown,
                                onRetry = viewModel::retry,
                                secondaryLabel = stringResource(R.string.search_clear_query),
                                onSecondary = viewModel::clearQuery,
                            )
                        }
                    }

                    state.hasSearched -> {
                        item(key = "no-results") {
                            SkyoraStatePanel(
                                icon = Icons.Outlined.SearchOff,
                                title = stringResource(R.string.search_no_results_title),
                                message = stringResource(R.string.search_no_results_body, state.submittedQuery),
                                primaryLabel = stringResource(R.string.search_clear_query),
                                onPrimary = viewModel::clearQuery,
                            )
                        }
                    }

                    else -> {
                        item(key = "browse") {
                            BrowseContent(
                                recentSearches = state.recentSearches,
                                onClearRecents = { confirmClearRecents = true },
                                onSelect = ::select,
                                onAttributionClick = { showAttribution = true },
                                queryTooShort = state.query.isNotBlank() && state.query.trim().length < 2,
                            )
                        }
                    }
                }

                item(key = "spacer") { Spacer(Modifier.size(1.dp)) }
            }
        }
    }

    if (confirmClearRecents) {
        SkyoraConfirmDialog(
            title = stringResource(R.string.search_recent_clear_title),
            body = stringResource(R.string.search_recent_clear_body),
            confirmLabel = stringResource(R.string.action_clear_all),
            onConfirm = {
                viewModel.clearRecentSearches()
                confirmClearRecents = false
            },
            onDismiss = { confirmClearRecents = false },
            destructive = true,
        )
    }

    if (showAttribution) {
        SkyoraInfoDialog(
            title = stringResource(R.string.search_attribution),
            body = stringResource(R.string.dialog_geocoding_attribution),
            onDismiss = { showAttribution = false },
            links = listOf(
                DialogLink(stringResource(R.string.link_open_meteo), SkyoraLinks.OPEN_METEO),
                DialogLink(stringResource(R.string.link_cc_by_nc), SkyoraLinks.CC_BY_NC_4),
                DialogLink(stringResource(R.string.link_terms), SkyoraLinks.OPEN_METEO_TERMS),
            ),
        )
    }
}

@Composable
private fun SkyoraSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearchAction: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                color = SkyoraColors.TextTertiary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = SkyoraColors.TextTertiary,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.search_clear_query),
                        tint = SkyoraColors.TextSecondary,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(SkyoraRadius.field),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SkyoraColors.TextPrimary,
            unfocusedTextColor = SkyoraColors.TextPrimary,
            cursorColor = SkyoraColors.Cyan,
            focusedContainerColor = SkyoraColors.Navy.copy(alpha = 0.62f),
            unfocusedContainerColor = SkyoraColors.Navy.copy(alpha = 0.62f),
            focusedBorderColor = SkyoraColors.Cyan.copy(alpha = 0.55f),
            unfocusedBorderColor = SkyoraColors.Border,
            focusedPlaceholderColor = SkyoraColors.TextTertiary,
            unfocusedPlaceholderColor = SkyoraColors.TextTertiary,
            focusedLeadingIconColor = SkyoraColors.TextTertiary,
            unfocusedLeadingIconColor = SkyoraColors.TextTertiary,
            focusedTrailingIconColor = SkyoraColors.TextSecondary,
            unfocusedTrailingIconColor = SkyoraColors.TextSecondary,
        ),
    )
}

@Composable
private fun BrowseContent(
    recentSearches: List<Location>,
    onClearRecents: () -> Unit,
    onSelect: (Location) -> Unit,
    onAttributionClick: () -> Unit,
    queryTooShort: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.lg)) {
        if (queryTooShort) {
            SkyoraBanner(
                title = stringResource(R.string.search_title),
                message = stringResource(R.string.search_hint_short),
                icon = Icons.Outlined.Search,
                tone = BannerTone.INFO,
            )
        }

        SkyoraSectionHeader(
            title = stringResource(R.string.search_recent),
            trailing = {
                if (recentSearches.isNotEmpty()) {
                    SkyoraTextButton(
                        text = stringResource(R.string.action_clear_all),
                        onClick = onClearRecents,
                    )
                }
            },
        )
        if (recentSearches.isEmpty()) {
            Text(
                text = stringResource(R.string.search_no_recent),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.xs)) {
                recentSearches.forEach { location ->
                    CityResultRow(
                        location = location,
                        icon = Icons.Outlined.History,
                        onClick = { onSelect(location) },
                    )
                }
            }
        }

        SkyoraSectionHeader(title = stringResource(R.string.search_popular))
        Column(verticalArrangement = Arrangement.spacedBy(SkyoraSpacing.xs)) {
            DefaultLocations.popular.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.xs)) {
                    row.forEach { location ->
                        PopularCityTile(
                            location = location,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(location) },
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        TextButton(
            onClick = onAttributionClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.search_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun CityResultRow(
    location: Location,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    SkyoraCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SkyoraColors.Cyan,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleSmall,
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
            }
            Text(
                text = location.countryCode.orEmpty(),
                style = SkyoraTextStyles.SectionLabel,
                color = SkyoraColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun PopularCityTile(
    location: Location,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkyoraCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = location.name,
            style = MaterialTheme.typography.titleSmall,
            color = SkyoraColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = location.country,
            style = MaterialTheme.typography.bodySmall,
            color = SkyoraColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
