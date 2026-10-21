package com.skyora.weather.ui.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skyora.weather.R
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraRadius
import com.skyora.weather.ui.theme.SkyoraSpacing
import androidx.compose.ui.res.stringResource

enum class BannerTone { INFO, WARNING, ERROR }

@Composable
fun SkyoraBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    tone: BannerTone = BannerTone.INFO,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val accent = when (tone) {
        BannerTone.INFO -> SkyoraColors.Cyan
        BannerTone.WARNING -> SkyoraColors.Warning
        BannerTone.ERROR -> SkyoraColors.Error
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SkyoraColors.Navy.copy(alpha = 0.72f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = SkyoraColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextSecondary,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 36.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyoraColors.CyanSoft,
                    )
                }
            }
        }
        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_dismiss),
                    tint = SkyoraColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun SkyoraStatePanel(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    iconTint: Color = SkyoraColors.Cyan,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SkyoraSpacing.lg, vertical = SkyoraSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StateIcon(icon = icon, tint = iconTint)
        Spacer(Modifier.height(SkyoraSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = SkyoraColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SkyoraSpacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = SkyoraColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = SkyoraSpacing.sm),
        )
        if (primaryLabel != null && onPrimary != null) {
            Spacer(Modifier.height(SkyoraSpacing.xl))
            SkyoraPrimaryButton(
                text = primaryLabel,
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(SkyoraSpacing.xs))
            TextButton(onClick = onSecondary) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyoraColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun StateIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.04f)),
                ),
            )
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
fun WeatherErrorState(
    error: WeatherError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val title: String
    val message: String
    val icon: ImageVector
    when (error) {
        WeatherError.Offline -> {
            title = stringResource(R.string.error_offline_title)
            message = stringResource(R.string.error_offline_body)
            icon = Icons.Outlined.WifiOff
        }

        WeatherError.Timeout -> {
            title = stringResource(R.string.error_timeout_title)
            message = stringResource(R.string.error_timeout_body)
            icon = Icons.Outlined.Schedule
        }

        is WeatherError.Server -> {
            title = stringResource(R.string.error_server_title)
            message = stringResource(R.string.error_server_body)
            icon = Icons.Outlined.Cloud
        }

        WeatherError.InvalidData -> {
            title = stringResource(R.string.error_invalid_title)
            message = stringResource(R.string.error_invalid_body)
            icon = Icons.Outlined.Warning
        }

        WeatherError.Unknown -> {
            title = stringResource(R.string.error_unknown_title)
            message = stringResource(R.string.error_unknown_body)
            icon = Icons.Outlined.Cloud
        }
    }
    SkyoraStatePanel(
        icon = icon,
        title = title,
        message = message,
        modifier = modifier,
        primaryLabel = stringResource(R.string.action_retry),
        onPrimary = onRetry,
        secondaryLabel = secondaryLabel,
        onSecondary = onSecondary,
        iconTint = if (error == WeatherError.Offline) SkyoraColors.Warning else SkyoraColors.Cyan,
    )
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(SkyoraColors.TextSecondary.copy(alpha = alpha)),
    )
}

@Composable
fun SkyoraSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = SkyoraColors.NavySoft,
            contentColor = SkyoraColors.TextPrimary,
            actionColor = SkyoraColors.Cyan,
            dismissActionContentColor = SkyoraColors.TextSecondary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun SkyoraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String = stringResource(R.string.cd_back),
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkyoraSpacing.xs),
    ) {
        if (onBack != null) {
            SkyoraIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = backDescription,
                onClick = onBack,
            )
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = SkyoraColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        actions?.invoke()
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String = stringResource(R.string.cd_back),
    actions: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkyoraIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = backDescription,
                    onClick = onBack,
                )
                Spacer(Modifier.weight(1f))
                actions?.invoke()
            }
            Spacer(Modifier.height(SkyoraSpacing.md))
        } else if (actions != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                actions()
            }
            Spacer(Modifier.height(SkyoraSpacing.sm))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = SkyoraColors.TextPrimary,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(SkyoraSpacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SkyoraColors.TextSecondary,
            )
        }
    }
}
