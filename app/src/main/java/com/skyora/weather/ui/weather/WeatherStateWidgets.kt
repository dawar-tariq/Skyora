package com.skyora.weather.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyora.weather.R
import com.skyora.weather.core.util.formatDateTime
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.ui.designsystem.BannerTone
import com.skyora.weather.ui.designsystem.SkeletonBlock
import com.skyora.weather.ui.designsystem.SkyoraBanner
import java.time.Instant
import java.time.ZoneId

/** Shared first-load skeleton so Home and city weather feel identical. */
@Composable
fun WeatherLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(Modifier.width(150.dp).height(12.dp))
        SkeletonBlock(Modifier.width(190.dp).height(22.dp))
        Spacer(Modifier.height(4.dp))
        SkeletonBlock(Modifier.width(140.dp).height(78.dp), cornerRadius = 20.dp)
        SkeletonBlock(Modifier.width(170.dp).height(20.dp))
        SkeletonBlock(Modifier.width(210.dp).height(14.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp), cornerRadius = 24.dp)
        SkeletonBlock(Modifier.fillMaxWidth().height(250.dp), cornerRadius = 24.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.weight(1f).height(118.dp), cornerRadius = 24.dp)
            SkeletonBlock(Modifier.weight(1f).height(118.dp), cornerRadius = 24.dp)
        }
    }
}

/**
 * Warning banner shown above cached content when a refresh could not reach the
 * network. The timestamp is the API fetch time, not the render time, so the
 * user knows exactly how old the forecast is.
 */
@Composable
fun CachedWeatherBanner(
    error: WeatherError,
    updatedAtMillis: Long,
    zone: ZoneId,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(
        when (error) {
            WeatherError.Offline -> R.string.error_offline_title
            WeatherError.Timeout -> R.string.error_timeout_title
            is WeatherError.Server -> R.string.error_server_title
            WeatherError.InvalidData -> R.string.error_invalid_title
            WeatherError.Unknown -> R.string.error_unknown_title
        },
    )
    val message = stringResource(
        R.string.error_showing_cached,
        formatDateTime(Instant.ofEpochMilli(updatedAtMillis), zone),
    )
    SkyoraBanner(
        title = title,
        message = message,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Outlined.CloudOff,
        tone = BannerTone.WARNING,
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
    )
}
