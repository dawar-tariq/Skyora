package com.skyora.weather.ui.permission

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skyora.weather.R
import com.skyora.weather.core.util.LOCATION_PERMISSIONS
import com.skyora.weather.core.util.findActivity
import com.skyora.weather.core.util.hasAnyLocationPermission
import com.skyora.weather.core.util.hasFineLocationPermission
import com.skyora.weather.core.util.openAppSettings
import com.skyora.weather.core.util.openLocationSettings
import com.skyora.weather.core.util.shouldShowLocationRationale
import com.skyora.weather.core.util.rememberNowMillis
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.LocationAccessState
import com.skyora.weather.domain.model.LocationFailureReason
import com.skyora.weather.ui.designsystem.BannerTone
import com.skyora.weather.ui.designsystem.BrandLockup
import com.skyora.weather.ui.designsystem.LocationRingsArt
import com.skyora.weather.ui.designsystem.SkyoraBackdrop
import com.skyora.weather.ui.designsystem.rememberAmbientSkyMoment
import com.skyora.weather.ui.designsystem.SkyoraBanner
import com.skyora.weather.ui.designsystem.SkyoraLoadingRow
import com.skyora.weather.ui.designsystem.SkyoraPrimaryButton
import com.skyora.weather.ui.designsystem.SkyoraSecondaryButton
import com.skyora.weather.ui.designsystem.SkyoraTextButton
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraSpacing
import kotlinx.coroutines.delay

/**
 * Permission explainer shown during onboarding. The system dialog only appears
 * after an explicit tap on "Allow location access"; declining is a first-class
 * path that leaves the app fully usable with a manually chosen city.
 */
@Composable
fun LocationPermissionScreen(
    container: AppContainer,
    onFinished: () -> Unit,
    onChooseCity: () -> Unit,
) {
    val viewModel: LocationPermissionViewModel = viewModel(
        factory = LocationPermissionViewModel.factory(container),
    )
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun finish(action: () -> Unit) {
        pendingAction = action
        viewModel.completeOnboarding()
    }

    LaunchedEffect(viewModel) {
        viewModel.completed.collect {
            val action = pendingAction
            pendingAction = null
            action?.invoke()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val fine = grants[com.skyora.weather.core.util.LOCATION_FINE] == true
        val coarse = grants[com.skyora.weather.core.util.LOCATION_COARSE] == true
        if (fine || coarse) {
            viewModel.locate(fine)
        } else {
            viewModel.onPermissionDenied(
                permanentDenial = activity != null && !activity.shouldShowLocationRationale(),
            )
        }
    }

    fun requestLocation() {
        if (context.hasAnyLocationPermission()) {
            viewModel.locate(context.hasFineLocationPermission())
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    // If access was granted earlier, fetch the fix automatically. This is not a
    // permission prompt: the user already made that choice.
    LaunchedEffect(Unit) {
        if (context.hasAnyLocationPermission()) {
            viewModel.locate(context.hasFineLocationPermission())
        }
    }

    val success = locationState as? LocationAccessState.Success
    LaunchedEffect(success) {
        if (success != null) {
            delay(750)
            finish { onFinished() }
        }
    }

    BackHandler {
        finish { onFinished() }
    }

    val permanentDenial = (locationState as? LocationAccessState.PermissionRequired)?.permanentDenial == true
    val locating = locationState is LocationAccessState.Locating

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
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SkyoraSpacing.screen),
        ) {
            Spacer(Modifier.height(SkyoraSpacing.xs))
            BrandLockup(compact = true)
            Spacer(Modifier.height(SkyoraSpacing.lg))
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.displaySmall,
                color = SkyoraColors.TextPrimary,
            )
            Spacer(Modifier.height(SkyoraSpacing.xs))
            Text(
                text = stringResource(R.string.permission_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = SkyoraColors.TextSecondary,
            )
            Spacer(Modifier.height(SkyoraSpacing.md))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp),
                contentAlignment = Alignment.Center,
            ) {
                LocationRingsArt(Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(SkyoraSpacing.md))

            BenefitRow(
                icon = Icons.Outlined.WbSunny,
                title = stringResource(R.string.permission_benefit_realtime),
                description = stringResource(R.string.permission_benefit_realtime_desc),
            )
            BenefitRow(
                icon = Icons.Outlined.Place,
                title = stringResource(R.string.permission_benefit_local),
                description = stringResource(R.string.permission_benefit_local_desc),
            )
            BenefitRow(
                icon = Icons.Outlined.Star,
                title = stringResource(R.string.permission_benefit_experience),
                description = stringResource(R.string.permission_benefit_experience_desc),
            )

            when (val state = locationState) {
                is LocationAccessState.Locating -> {
                    Spacer(Modifier.height(SkyoraSpacing.md))
                    SkyoraLoadingRow(text = stringResource(R.string.permission_locating))
                }

                is LocationAccessState.Success -> {
                    Spacer(Modifier.height(SkyoraSpacing.md))
                    SkyoraBanner(
                        title = stringResource(R.string.permission_granted),
                        message = if (state.isApproximate) {
                            stringResource(R.string.permission_approximate_note)
                        } else {
                            stringResource(R.string.permission_granted_body)
                        },
                    )
                }

                is LocationAccessState.PermissionRequired -> {
                    Spacer(Modifier.height(SkyoraSpacing.md))
                    SkyoraBanner(
                        title = stringResource(R.string.permission_denied_title),
                        message = stringResource(
                            if (state.permanentDenial) {
                                R.string.permission_denied_permanent_body
                            } else {
                                R.string.permission_denied_body
                            },
                        ),
                        tone = BannerTone.WARNING,
                        actionLabel = if (state.permanentDenial) {
                            stringResource(R.string.action_open_settings)
                        } else {
                            null
                        },
                        onAction = if (state.permanentDenial) {
                            { context.openAppSettings() }
                        } else {
                            null
                        },
                    )
                }

                is LocationAccessState.Error -> {
                    Spacer(Modifier.height(SkyoraSpacing.md))
                    SkyoraBanner(
                        title = stringResource(R.string.permission_unavailable_title),
                        message = stringResource(R.string.permission_unavailable_body),
                        tone = BannerTone.WARNING,
                        actionLabel = if (state.reason == LocationFailureReason.SERVICES_DISABLED) {
                            stringResource(R.string.action_open_settings)
                        } else {
                            stringResource(R.string.permission_try_again)
                        },
                        onAction = {
                            if (state.reason == LocationFailureReason.SERVICES_DISABLED) {
                                context.openLocationSettings()
                            } else {
                                requestLocation()
                            }
                        },
                    )
                }

                LocationAccessState.Idle -> Unit
            }

            Spacer(Modifier.height(SkyoraSpacing.xl))
            SkyoraPrimaryButton(
                text = if (permanentDenial) {
                    stringResource(R.string.action_open_settings)
                } else {
                    stringResource(R.string.permission_allow)
                },
                onClick = {
                    if (permanentDenial) context.openAppSettings() else requestLocation()
                },
                loading = locating,
            )
            Spacer(Modifier.height(SkyoraSpacing.sm))
            SkyoraSecondaryButton(
                text = stringResource(R.string.permission_choose_manually),
                onClick = { finish { onChooseCity() } },
                enabled = !locating,
            )
            Spacer(Modifier.height(SkyoraSpacing.xxs))
            SkyoraTextButton(
                text = stringResource(R.string.permission_maybe_later),
                onClick = { finish { onFinished() } },
                modifier = Modifier.fillMaxWidth(),
                color = SkyoraColors.TextSecondary,
            )
            Spacer(Modifier.height(SkyoraSpacing.md))
            Text(
                text = stringResource(R.string.permission_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SkyoraSpacing.xs),
            )
            Spacer(Modifier.height(SkyoraSpacing.xxl))
        }
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SkyoraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SkyoraColors.Cyan.copy(alpha = 0.12f)),
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SkyoraColors.TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextSecondary,
            )
        }
    }
}
