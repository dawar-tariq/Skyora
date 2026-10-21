package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders the SKYORA weather icon set.
 *
 * The vector drawables in `res/drawable/weather_*.xml` are one shared asset
 * pack: Compose, the home-screen widgets and the notification builder all pick
 * their resource through [com.skyora.weather.domain.model.SkyIcons], so a given
 * forecast hour can never be drawn differently in two places.
 */
@Composable
fun WeatherIcon(
    resId: Int,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

/**
 * Monochrome variant for status-bar notification icons, where Android only
 * supports alpha-masked white artwork.
 */
@Composable
fun WeatherIconMasked(resId: Int, tint: Color, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}
