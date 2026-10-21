package com.skyora.weather.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.SkyCondition
import com.skyora.weather.domain.model.SkyMoment
import com.skyora.weather.domain.model.SkyPhase
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.SkyoraMark
import com.skyora.weather.ui.designsystem.SkyoraWordmark
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import kotlinx.coroutines.launch

/**
 * In-app continuation of the Android 12 splash screen. The system splash uses
 * the same mark and colour, so the hand-off is seamless; the wordmark and
 * tagline fade in while the app reads the onboarding flag.
 */
@Composable
fun SplashScreen(
    container: AppContainer,
    onFinished: (StartDestination) -> Unit,
) {
    val viewModel: SplashViewModel = viewModel(factory = SplashViewModel.factory(container))
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    val fade = remember { Animatable(0f) }
    val scale = remember { Animatable(0.94f) }
    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, animationSpec = tween(durationMillis = 620)) }
        scale.animateTo(1f, animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(destination) {
        destination?.let(onFinished)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyoraColors.Ink),
    ) {
        // Deliberately not time-aware: the system splash window and the
        // launcher icon are brand navy, so a bright sky here would flash.
        SkyoraBackdrop(
            moment = SkyMoment(SkyPhase.NIGHT, SkyCondition.CLEAR),
            animate = false,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SkyoraSpacing.xxl)
                .graphicsLayer {
                    alpha = fade.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SkyoraMark(size = 104.dp)
            Spacer(Modifier.height(SkyoraSpacing.xl))
            SkyoraWordmark(fontSize = 38, letterSpacing = 13)
            Spacer(Modifier.height(SkyoraSpacing.sm))
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = SkyoraColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(SkyoraSpacing.xxxl))
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = SkyoraColors.Cyan,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(SkyoraSpacing.md))
            Text(
                text = stringResource(R.string.splash_loading),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
            )
        }
    }
}
