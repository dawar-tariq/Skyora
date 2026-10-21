package com.skyora.weather.ui.onboarding

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.di.AppContainer
import com.skyora.weather.ui.designsystem.EmptyCitiesArt
import com.skyora.weather.ui.designsystem.ForecastPreviewArt
import com.skyora.weather.ui.designsystem.LocationRingsArt
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.SkyoraPrimaryButton
import com.skyora.weather.ui.designsystem.SkyoraStatusChip
import com.skyora.weather.ui.designsystem.SkyoraTextButton
import com.skyora.weather.ui.designsystem.WeatherVisualFamily
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val badgeRes: Int?,
    val illustrationDescriptionRes: Int,
    val art: @Composable (Modifier) -> Unit,
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
)

/**
 * Three first-launch slides. Completion is written through the ViewModel only
 * when the user skips or reaches the end, so a cold start mid-onboarding shows
 * the flow again rather than dumping the user on Home.
 */
@Composable
fun OnboardingScreen(
    container: AppContainer,
    onComplete: () -> Unit,
) {
    val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(container))
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()

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
            family = WeatherVisualFamily.CLEAR,
            isDay = false,
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
