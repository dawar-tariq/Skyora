package com.skyora.weather.ui.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyora.weather.R
import com.skyora.weather.core.util.formatChance
import com.skyora.weather.core.util.formatDateHeader
import com.skyora.weather.core.util.formatDaylight
import com.skyora.weather.core.util.formatHour
import com.skyora.weather.core.util.formatPercentage
import com.skyora.weather.core.util.formatPressure
import com.skyora.weather.core.util.formatTemperature
import com.skyora.weather.core.util.formatTemperatureWithUnit
import com.skyora.weather.core.util.formatTimeOfDay
import com.skyora.weather.core.util.formatUvIndex
import com.skyora.weather.core.util.formatWeekday
import com.skyora.weather.core.util.formatWindSpeed
import com.skyora.weather.core.util.spokenTemperature
import com.skyora.weather.core.util.updateAge
import com.skyora.weather.core.util.uvCategory
import com.skyora.weather.core.util.UpdateAge
import com.skyora.weather.core.util.windDirectionLabel
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.model.sunTimes
import com.skyora.weather.domain.model.WmoWeatherCodes
import com.skyora.weather.ui.designsystem.SaveCityChip
import com.skyora.weather.ui.designsystem.SkyoraCard
import com.skyora.weather.ui.designsystem.WeatherIcon
import com.skyora.weather.ui.designsystem.weatherIconRes
import com.skyora.weather.ui.designsystem.SkyoraDivider
import com.skyora.weather.ui.designsystem.SkyoraSectionHeader
import com.skyora.weather.ui.designsystem.SkyoraStatusChip
import com.skyora.weather.ui.designsystem.labelRes
import com.skyora.weather.ui.designsystem.uvCategoryLabelRes
import com.skyora.weather.ui.designsystem.weatherDescriptionRes
import com.skyora.weather.ui.theme.SkyoraColors
import com.skyora.weather.ui.theme.SkyoraTextStyles
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * The complete weather dashboard, shared by Home and the city weather screen so
 * both present identical data with one source of truth.
 */
@Composable
fun WeatherDetailsContent(
    snapshot: WeatherSnapshot,
    unit: TemperatureUnit,
    now: LocalDateTime,
    nowMillis: Long,
    isSaved: Boolean,
    onToggleSaved: (() -> Unit)?,
    onAttributionClick: () -> Unit,
    modifier: Modifier = Modifier,
    isStale: Boolean = false,
) {
    val zone = snapshot.zoneId()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        WeatherHero(
            snapshot = snapshot,
            unit = unit,
            now = now,
            isSaved = isSaved,
            onToggleSaved = onToggleSaved,
        )
        HourlyForecastCard(snapshot = snapshot, unit = unit, zone = zone)
        DailyForecastCard(snapshot = snapshot, unit = unit, zone = zone)
        WeatherMetricsGrid(snapshot = snapshot, unit = unit, zone = zone)
        SunCard(snapshot = snapshot, zone = zone, nowMillis = nowMillis)
        UpdatedAttributionFooter(
            updatedAtMillis = snapshot.fetchedAtEpochMillis,
            nowMillis = nowMillis,
            zone = zone,
            isStale = isStale,
            onAttributionClick = onAttributionClick,
        )
    }
}

@Composable
fun WeatherHero(
    snapshot: WeatherSnapshot,
    unit: TemperatureUnit,
    now: LocalDateTime,
    isSaved: Boolean,
    onToggleSaved: (() -> Unit)?,
) {
    val current = snapshot.current
    val condition = WmoWeatherCodes.conditionFor(current.weatherCode)
    val description = stringResource(weatherDescriptionRes(current.weatherCode))
    val temperature = formatTemperature(current.temperatureC, unit)
    val feelsLike = formatTemperatureWithUnit(current.feelsLikeC, unit)
    val today = snapshot.today()
    val high = formatTemperature(today?.tempMaxC, unit)
    val low = formatTemperature(today?.tempMinC, unit)
    val fontScale = LocalDensity.current.fontScale
    val temperatureStyle = if (fontScale > 1.25f) {
        SkyoraTextStyles.HeroTemperatureCompact
    } else {
        SkyoraTextStyles.HeroTemperature
    }
    val spokenSummary = stringResource(
        R.string.cd_current_conditions,
        description,
        spokenTemperature(current.temperatureC, unit),
        spokenTemperature(current.feelsLikeC, unit),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateHeader(now) + " · " + formatTimeOfDay(now),
                    style = SkyoraTextStyles.SectionLabel,
                    color = SkyoraColors.TextSecondary,
                )
                // The condition is shown once, under the temperature. Printing it
                // here too made the hero read "Clear sky" twice in a row.
            }
            WeatherIcon(
                resId = weatherIconRes(
                    condition = condition,
                    epochMillis = current.timeEpochMillis,
                    zone = snapshot.zoneId(),
                    sunTimes = snapshot.sunTimes(),
                ),
                size = 62.dp,
                contentDescription = description,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = temperature,
            style = temperatureStyle,
            color = SkyoraColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.semantics { contentDescription = spokenSummary },
        )
        Text(
            text = stringResource(weatherDescriptionRes(current.weatherCode)),
            style = MaterialTheme.typography.titleLarge,
            color = SkyoraColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_feels_like, feelsLike) +
                "  ·  " + stringResource(R.string.home_high_low_compact, high, low),
            style = MaterialTheme.typography.bodyMedium,
            color = SkyoraColors.TextSecondary,
        )
        if (onToggleSaved != null) {
            Spacer(Modifier.height(12.dp))
            SaveCityChip(saved = isSaved, onClick = onToggleSaved)
        }
    }
}

@Composable
fun HourlyForecastCard(
    snapshot: WeatherSnapshot,
    unit: TemperatureUnit,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    if (snapshot.hourly.isEmpty()) return
    SkyoraCard(modifier = modifier.fillMaxWidth()) {
        SkyoraSectionHeader(
            title = stringResource(R.string.home_hourly_title),
            trailing = {
                Text(
                    text = stringResource(R.string.home_hourly_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyoraColors.TextTertiary,
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            itemsIndexed(
                items = snapshot.hourly,
                key = { _, hour -> hour.timeEpochMillis },
            ) { index, hour ->
                HourlyCell(
                    hourIndex = index,
                    timeEpochMillis = hour.timeEpochMillis,
                    temperatureC = hour.temperatureC,
                    weatherCode = hour.weatherCode,
                    iconRes = weatherIconRes(snapshot, hour),
                    precipitationProbability = hour.precipitationProbability,
                    unit = unit,
                    zone = zone,
                )
            }
        }
    }
}

@Composable
private fun HourlyCell(
    hourIndex: Int,
    timeEpochMillis: Long,
    temperatureC: Double,
    weatherCode: Int,
    iconRes: Int,
    precipitationProbability: Int?,
    unit: TemperatureUnit,
    zone: ZoneId,
) {
    val isNow = hourIndex == 0
    val condition = WmoWeatherCodes.conditionFor(weatherCode)
    val description = stringResource(weatherDescriptionRes(weatherCode))
    val timeLabel = if (isNow) {
        stringResource(R.string.home_now)
    } else {
        formatHour(Instant.ofEpochMilli(timeEpochMillis), zone)
    }
    val temperature = formatTemperature(temperatureC, unit)
    val chance = formatChance(precipitationProbability)
    val shape = RoundedCornerShape(16.dp)
    val cellModifier = Modifier
        .width(64.dp)
        .clip(shape)
        .background(if (isNow) SkyoraColors.Cyan.copy(alpha = 0.10f) else Color.Transparent)
        .then(
            if (isNow) Modifier.border(1.dp, SkyoraColors.Cyan.copy(alpha = 0.32f), shape) else Modifier,
        )
        .padding(vertical = 10.dp, horizontal = 4.dp)
        .semantics(mergeDescendants = true) {}

    Column(modifier = cellModifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
            color = if (isNow) SkyoraColors.CyanSoft else SkyoraColors.TextTertiary,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        WeatherIcon(
            resId = iconRes,
            size = 26.dp,
            contentDescription = description,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = temperature,
            style = MaterialTheme.typography.labelLarge,
            color = SkyoraColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
        Spacer(Modifier.height(3.dp))
        if (chance != null && (precipitationProbability ?: 0) >= 20) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = SkyoraColors.Cyan.copy(alpha = 0.8f),
                    modifier = Modifier.size(9.dp),
                )
                Text(
                    text = chance,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.sp),
                    color = SkyoraColors.CyanSoft,
                )
            }
        } else {
            Spacer(Modifier.height(13.dp))
        }
    }
}

@Composable
fun DailyForecastCard(
    snapshot: WeatherSnapshot,
    unit: TemperatureUnit,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    if (snapshot.daily.isEmpty()) return
    val today = LocalDate.now(zone)
    val weekMin = snapshot.daily.minOf { it.tempMinC }
    val weekMax = snapshot.daily.maxOf { it.tempMaxC }
    SkyoraCard(modifier = modifier.fillMaxWidth()) {
        SkyoraSectionHeader(title = stringResource(R.string.home_daily_title))
        Spacer(Modifier.height(6.dp))
        snapshot.daily.forEachIndexed { index, day ->
            val date = day.localDate()
            val condition = WmoWeatherCodes.conditionFor(day.weatherCode)
            val description = stringResource(weatherDescriptionRes(day.weatherCode))
            val dayLabel = when {
                date == null -> day.dateIso
                date == today -> stringResource(R.string.forecast_today)
                else -> formatWeekday(date)
            }
            val high = formatTemperature(day.tempMaxC, unit)
            val low = formatTemperature(day.tempMinC, unit)
            val chance = formatChance(day.precipitationProbabilityMax)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (date == today) SkyoraColors.CyanSoft else SkyoraColors.TextPrimary,
                    modifier = Modifier.weight(0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                WeatherIcon(
                    resId = weatherIconRes(day),
                    size = 26.dp,
                    contentDescription = description,
                )
                Row(
                    modifier = Modifier.width(46.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    if (chance != null && (day.precipitationProbabilityMax ?: 0) >= 20) {
                        Icon(
                            Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = SkyoraColors.Cyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(9.dp),
                        )
                        Text(
                            text = chance,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.sp),
                            color = SkyoraColors.CyanSoft,
                        )
                    }
                }
                TemperatureRangeBar(
                    minC = day.tempMinC,
                    maxC = day.tempMaxC,
                    weekMinC = weekMin,
                    weekMaxC = weekMax,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(4.dp)
                        .padding(horizontal = 6.dp),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = high,
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyoraColors.TextPrimary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = low,
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyoraColors.TextTertiary,
                        maxLines = 1,
                    )
                }
            }
            if (index != snapshot.daily.lastIndex) {
                SkyoraDivider()
            }
        }
    }
}

@Composable
fun TemperatureRangeBar(
    minC: Double,
    maxC: Double,
    weekMinC: Double,
    weekMaxC: Double,
    modifier: Modifier = Modifier,
) {
    val range = (weekMaxC - weekMinC).coerceAtLeast(0.1)
    val startFraction = ((minC - weekMinC) / range).toFloat().coerceIn(0f, 1f)
    val endFraction = ((maxC - weekMinC) / range).toFloat().coerceIn(0f, 1f)
    Canvas(modifier) {
        val height = size.height
        val corner = androidx.compose.ui.geometry.CornerRadius(height / 2f)
        drawRoundRect(
            color = SkyoraColors.TextTertiary.copy(alpha = 0.22f),
            size = size,
            cornerRadius = corner,
        )
        val left = size.width * startFraction
        val right = (size.width * endFraction).coerceAtLeast(left + height)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(SkyoraColors.Cyan, SkyoraColors.Blue),
            ),
            topLeft = Offset(left, 0f),
            size = Size(right - left, height),
            cornerRadius = corner,
        )
    }
}

@Composable
fun WeatherMetricsGrid(
    snapshot: WeatherSnapshot,
    unit: TemperatureUnit,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    val current = snapshot.current
    val today = snapshot.today()
    val uvValue = today?.uvIndexMax
    val uvCategoryValue = uvCategory(uvValue)
    val direction = windDirectionLabel(current.windDirectionDegrees)
    val metrics = listOf(
        MetricData(
            icon = Icons.Outlined.Thermostat,
            label = stringResource(R.string.metric_feels_like),
            value = formatTemperature(current.feelsLikeC, unit),
            caption = null,
        ),
        MetricData(
            icon = Icons.Outlined.WaterDrop,
            label = stringResource(R.string.metric_humidity),
            value = formatPercentage(current.humidityPercent),
            caption = stringResource(R.string.metric_humidity_caption),
        ),
        MetricData(
            icon = Icons.Outlined.Air,
            label = stringResource(R.string.metric_wind),
            value = formatWindSpeed(current.windSpeedKmh, unit),
            caption = if (direction != null) {
                stringResource(R.string.metric_wind_caption, direction)
            } else {
                stringResource(R.string.metric_wind_calm)
            },
        ),
        MetricData(
            icon = Icons.Outlined.WbSunny,
            label = stringResource(R.string.metric_uv),
            value = formatUvIndex(uvValue),
            caption = if (uvCategoryValue != null) {
                stringResource(uvCategoryLabelRes(uvCategoryValue))
            } else {
                null
            },
        ),
        MetricData(
            icon = Icons.Outlined.Speed,
            label = stringResource(R.string.metric_pressure),
            value = formatPressure(current.pressureHpa),
            caption = stringResource(R.string.metric_pressure_caption),
        ),
        MetricData(
            icon = Icons.Outlined.Cloud,
            label = stringResource(R.string.metric_cloud),
            value = formatPercentage(current.cloudCoverPercent),
            caption = stringResource(R.string.metric_cloud_caption),
        ),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        SkyoraSectionHeader(title = stringResource(R.string.home_details_title))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { metric ->
                        MetricTile(metric = metric, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class MetricData(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val caption: String?,
)

@Composable
private fun MetricTile(metric: MetricData, modifier: Modifier = Modifier) {
    SkyoraCard(
        modifier = modifier.heightIn(min = 118.dp),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(SkyoraColors.Cyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    tint = SkyoraColors.Cyan,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = metric.label.uppercase(Locale.getDefault()),
                style = SkyoraTextStyles.SectionLabel.copy(letterSpacing = 0.8.sp),
                color = SkyoraColors.TextTertiary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = metric.value,
            style = SkyoraTextStyles.MetricValue,
            color = SkyoraColors.TextPrimary,
            maxLines = 1,
        )
        if (metric.caption != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = metric.caption,
                style = MaterialTheme.typography.bodySmall,
                color = SkyoraColors.TextTertiary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SunCard(
    snapshot: WeatherSnapshot,
    zone: ZoneId,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    val today = snapshot.today() ?: return
    val sunrise = today.sunrise()
    val sunset = today.sunset()
    val daylight = formatDaylight(sunrise, sunset)
    val progress = if (sunrise != null && sunset != null && sunset.isAfter(sunrise)) {
        val total = (sunset.toEpochMilli() - sunrise.toEpochMilli()).toFloat()
        ((nowMillis - sunrise.toEpochMilli()) / total).coerceIn(0f, 1f)
    } else {
        null
    }
    SkyoraCard(modifier = modifier.fillMaxWidth()) {
        SkyoraSectionHeader(
            title = stringResource(R.string.home_sun_section),
            trailing = {
                if (daylight != null) {
                    Text(
                        text = stringResource(R.string.home_daylight, daylight),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyoraColors.TextSecondary,
                    )
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        SunArc(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_sunrise),
                    style = SkyoraTextStyles.SectionLabel,
                    color = SkyoraColors.TextTertiary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = sunrise?.let { formatTimeOfDay(it, zone) } ?: stringResource(R.string.value_unavailable),
                    style = MaterialTheme.typography.titleSmall,
                    color = SkyoraColors.TextPrimary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.home_sunset),
                    style = SkyoraTextStyles.SectionLabel,
                    color = SkyoraColors.TextTertiary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = sunset?.let { formatTimeOfDay(it, zone) } ?: stringResource(R.string.value_unavailable),
                    style = MaterialTheme.typography.titleSmall,
                    color = SkyoraColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun SunArc(progress: Float?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pad = 10.dp.toPx()
        val start = Offset(pad, size.height - pad)
        val control = Offset(size.width / 2f, -size.height * 0.85f)
        val end = Offset(size.width - pad, size.height - pad)

        fun pointAt(t: Float): Offset {
            val inverse = 1f - t
            return Offset(
                x = inverse * inverse * start.x + 2f * inverse * t * control.x + t * t * end.x,
                y = inverse * inverse * start.y + 2f * inverse * t * control.y + t * t * end.y,
            )
        }

        val track = Path().apply {
            moveTo(start.x, start.y)
            quadraticBezierTo(control.x, control.y, end.x, end.y)
        }
        drawPath(
            path = track,
            color = SkyoraColors.BorderStrong,
            style = Stroke(
                width = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 8.dp.toPx())),
            ),
        )
        drawLine(
            color = SkyoraColors.Border,
            start = Offset(0f, size.height - 1f),
            end = Offset(size.width, size.height - 1f),
            strokeWidth = 1.dp.toPx(),
        )

        if (progress != null) {
            val steps = (progress * 48f).toInt().coerceAtLeast(1)
            val progressPath = Path().apply {
                val first = pointAt(0f)
                moveTo(first.x, first.y)
                for (index in 1..steps) {
                    val point = pointAt(progress * index / steps)
                    lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = progressPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(SkyoraColors.Cyan.copy(alpha = 0.5f), SkyoraColors.Cyan),
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )

            val position = pointAt(progress)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SkyoraColors.Cyan.copy(alpha = 0.35f),
                        SkyoraColors.Cyan.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    center = position,
                    radius = 22.dp.toPx(),
                ),
                radius = 22.dp.toPx(),
                center = position,
            )
            drawCircle(color = SkyoraColors.Cyan, radius = 4.dp.toPx(), center = position)
            drawCircle(color = Color.White, radius = 1.4.dp.toPx(), center = position)
        }
    }
}

@Composable
fun UpdatedAttributionFooter(
    updatedAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId,
    isStale: Boolean,
    onAttributionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val updatedText = when (val age = updateAge(updatedAtMillis, nowMillis)) {
        UpdateAge.JustNow -> stringResource(R.string.home_updated_just_now)
        is UpdateAge.Minutes -> stringResource(R.string.home_updated_minutes, age.minutes)
        is UpdateAge.Hours -> stringResource(R.string.home_updated_hours, age.hours)
        UpdateAge.Older -> stringResource(
            R.string.home_updated_absolute,
            com.skyora.weather.core.util.formatDateTime(Instant.ofEpochMilli(updatedAtMillis), zone),
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isStale) {
            SkyoraStatusChip(text = stringResource(R.string.home_stale_badge))
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = updatedText,
            style = MaterialTheme.typography.bodySmall,
            color = SkyoraColors.TextTertiary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.home_attribution),
            style = MaterialTheme.typography.labelMedium,
            color = SkyoraColors.CyanSoft.copy(alpha = 0.85f),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.action_learn_more),
                    onClick = onAttributionClick,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun ConditionLabel(code: Int): String =
    stringResource(weatherDescriptionRes(code))

@Composable
fun ShortConditionLabel(code: Int): String =
    stringResource(WmoWeatherCodes.conditionFor(code).labelRes())
