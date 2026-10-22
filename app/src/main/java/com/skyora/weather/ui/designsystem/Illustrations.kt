package com.skyora.weather.ui.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyora.weather.domain.model.DefaultLocations
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraTextStyles
import androidx.compose.ui.res.stringResource
import com.skyora.weather.R

/** Pulsing location rings with a gradient pin, used on permission and onboarding. */
@Composable
fun LocationRingsArt(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "locationRings")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    val progress = if (animate) pulse else 0.35f

    Canvas(modifier = modifier) {
        val minDimension = size.minDimension
        val center = Offset(size.width / 2f, size.height * 0.44f)
        val maxRadius = minDimension * 0.42f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SkyoraColors.Cyan.copy(alpha = 0.20f), Color.Transparent),
                center = center,
                radius = maxRadius * 1.35f,
            ),
            radius = maxRadius * 1.35f,
            center = center,
        )

        repeat(3) { index ->
            val phase = ((progress + index / 3f) % 1f)
            drawCircle(
                color = SkyoraColors.Cyan.copy(alpha = (0.30f * (1f - phase)).coerceIn(0f, 1f)),
                radius = maxRadius * (0.34f + 0.66f * phase),
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
        drawCircle(
            color = SkyoraColors.Cyan.copy(alpha = 0.20f),
            radius = maxRadius * 0.62f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = SkyoraColors.Cyan.copy(alpha = 0.12f),
            radius = maxRadius * 0.86f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )

        val pinCenter = Offset(center.x, center.y - minDimension * 0.02f)
        val pinRadius = minDimension * 0.082f
        val pinBrush = Brush.verticalGradient(
            colors = listOf(SkyoraColors.CyanSoft, SkyoraColors.Blue),
            startY = pinCenter.y - pinRadius,
            endY = pinCenter.y + pinRadius * 2,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SkyoraColors.Cyan.copy(alpha = 0.35f), Color.Transparent),
                center = pinCenter,
                radius = pinRadius * 2.2f,
            ),
            radius = pinRadius * 2.2f,
            center = pinCenter,
        )
        drawCircle(brush = pinBrush, radius = pinRadius, center = pinCenter)
        val tip = Path().apply {
            moveTo(pinCenter.x - pinRadius * 0.52f, pinCenter.y + pinRadius * 0.78f)
            lineTo(pinCenter.x, pinCenter.y + pinRadius * 2.05f)
            lineTo(pinCenter.x + pinRadius * 0.52f, pinCenter.y + pinRadius * 0.78f)
            close()
        }
        drawPath(tip, pinBrush)
        drawCircle(color = SkyoraColors.Ink, radius = pinRadius * 0.34f, center = pinCenter)

        // Small orbit accents.
        drawCircle(
            color = SkyoraColors.CyanSoft.copy(alpha = 0.5f),
            radius = minDimension * 0.008f,
            center = Offset(center.x + maxRadius * 0.9f, center.y - maxRadius * 0.35f),
        )
        drawCircle(
            color = SkyoraColors.CyanSoft.copy(alpha = 0.3f),
            radius = minDimension * 0.005f,
            center = Offset(center.x - maxRadius * 0.75f, center.y + maxRadius * 0.45f),
        )
    }
}

/** Abstract saved-city cards used in the empty state and onboarding. */
@Composable
fun EmptyCitiesArt(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        MiniCityCard(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = -9f
                    translationY = 26.dp.toPx()
                    scaleX = 0.9f
                    scaleY = 0.9f
                    alpha = 0.45f
                },
        )
        MiniCityCard(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 7f
                    translationY = 14.dp.toPx()
                    scaleX = 0.95f
                    scaleY = 0.95f
                    alpha = 0.7f
                },
        )
        MiniCityCard()
    }
}

@Composable
private fun MiniCityCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .width(196.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SkyoraColors.Navy.copy(alpha = 0.92f))
            .border(1.dp, SkyoraColors.BorderStrong, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SkyoraColors.Cyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Cloud,
                contentDescription = null,
                tint = SkyoraColors.CyanSoft,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SkyoraColors.TextSecondary.copy(alpha = 0.35f)),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.46f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SkyoraColors.TextTertiary.copy(alpha = 0.30f)),
            )
        }
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = SkyoraColors.Cyan.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp),
        )
    }
}

/** A miniature weather screen used on the first onboarding slide. */
@Composable
fun ForecastPreviewArt(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(226.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SkyoraColors.Navy.copy(alpha = 0.92f))
            .border(1.dp, SkyoraColors.BorderStrong, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = DefaultLocations.Srinagar.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = SkyoraColors.TextSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "24°",
                    style = SkyoraTextStyles.HeroTemperature.copy(
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-1).sp,
                    ),
                    color = SkyoraColors.TextPrimary,
                )
            }
            WeatherIcon(
                resId = R.drawable.weather_partly_cloudy_day,
                size = 54.dp,
                contentDescription = stringResource(R.string.weather_partly_cloudy),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.weather_partly_cloudy),
            style = MaterialTheme.typography.bodySmall,
            color = SkyoraColors.TextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SkyoraColors.Border),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            listOf("09", "12", "15", "18").forEachIndexed { index, hour ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hour,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyoraColors.TextTertiary,
                    )
                    Spacer(Modifier.height(5.dp))
                    WeatherIcon(
                        resId = if (index == 2) {
                            R.drawable.weather_clear_day
                        } else {
                            R.drawable.weather_partly_cloudy_day
                        },
                        size = 20.dp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${24 + index}°",
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyoraColors.TextPrimary,
                    )
                }
            }
        }
    }
}

/**
 * Abstract notification cards used on the onboarding notification step.
 *
 * Deliberately a *mock* alert card rather than live data: onboarding runs
 * before any forecast is fetched, and showing fabricated temperatures here
 * would be exactly the kind of fake content the app avoids elsewhere.
 */
@Composable
fun NotificationAlertArt(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        NotificationCardMock(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = -7f
                    translationY = 30.dp.toPx()
                    scaleX = 0.9f
                    scaleY = 0.9f
                    alpha = 0.40f
                },
            accent = SkyoraColors.CyanSoft,
        )
        NotificationCardMock(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 6f
                    translationY = 16.dp.toPx()
                    scaleX = 0.95f
                    scaleY = 0.95f
                    alpha = 0.70f
                },
            accent = SkyoraColors.Warning,
        )
        NotificationCardMock(accent = SkyoraColors.Cyan)
    }
}

@Composable
private fun NotificationCardMock(
    modifier: Modifier = Modifier,
    accent: Color,
) {
    Row(
        modifier = modifier
            .width(214.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SkyoraColors.Navy.copy(alpha = 0.94f))
            .border(1.dp, SkyoraColors.BorderStrong, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SkyoraColors.TextPrimary.copy(alpha = 0.75f)),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.86f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SkyoraColors.TextSecondary.copy(alpha = 0.35f)),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.40f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.45f)),
            )
        }
    }
}

/**
 * A widget card with refresh arrows and a battery plate: "this keeps updating
 * while the app is closed, unless the phone pauses it".
 *
 * Bars rather than temperatures — onboarding runs before any forecast exists,
 * and inventing a number here would be the same kind of fake data the app
 * avoids everywhere else. Nothing in the drawing claims the device *has* a
 * restriction; the ring simply keeps turning.
 */
@Composable
fun BackgroundRefreshArt(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "backgroundRefresh")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "refreshSpin",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Turning halo: the background job, still going.
        Canvas(
            modifier = Modifier
                .size(196.dp)
                .graphicsLayer { rotationZ = spin },
        ) {
            val stroke = 1.6.dp.toPx()
            drawArc(
                color = SkyoraColors.Cyan.copy(alpha = 0.55f),
                startAngle = -70f,
                sweepAngle = 96f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = SkyoraColors.Cyan.copy(alpha = 0.18f),
                startAngle = 40f,
                sweepAngle = 58f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(
            modifier = Modifier
                .width(206.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SkyoraColors.Navy.copy(alpha = 0.94f))
                .border(1.dp, SkyoraColors.BorderStrong, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Widgets,
                    contentDescription = null,
                    tint = SkyoraColors.CyanSoft,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.52f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SkyoraColors.TextPrimary.copy(alpha = 0.72f)),
                )
            }
            // The four hourly cells of the widget, as placeholders.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                repeat(4) { index ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.62f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(SkyoraColors.TextTertiary.copy(alpha = 0.55f)),
                        )
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == 2) {
                                        SkyoraColors.Cyan.copy(alpha = 0.45f)
                                    } else {
                                        SkyoraColors.TextSecondary.copy(alpha = 0.22f)
                                    },
                                ),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.5f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(SkyoraColors.TextSecondary.copy(alpha = 0.28f)),
                        )
                    }
                }
            }
        }

        // Battery plate: the thing the slide is really about.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(SkyoraColors.NavySoft)
                .border(1.dp, SkyoraColors.BorderStrong, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.BatteryAlert,
                contentDescription = null,
                tint = SkyoraColors.Warning,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .width(38.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SkyoraColors.TextSecondary.copy(alpha = 0.4f)),
            )
        }
    }
}
