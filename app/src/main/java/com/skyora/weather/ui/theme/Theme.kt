package com.skyora.weather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SkyoraColorScheme = darkColorScheme(
    primary = SkyoraColors.Cyan,
    onPrimary = SkyoraColors.OnAccent,
    primaryContainer = SkyoraColors.CyanDeep,
    onPrimaryContainer = SkyoraColors.Ink,
    secondary = SkyoraColors.Blue,
    onSecondary = SkyoraColors.TextPrimary,
    secondaryContainer = SkyoraColors.NavySoft,
    onSecondaryContainer = SkyoraColors.TextPrimary,
    tertiary = SkyoraColors.CyanSoft,
    onTertiary = SkyoraColors.OnAccent,
    background = SkyoraColors.Midnight,
    onBackground = SkyoraColors.TextPrimary,
    surface = SkyoraColors.DeepNavy,
    onSurface = SkyoraColors.TextPrimary,
    surfaceVariant = SkyoraColors.Navy,
    onSurfaceVariant = SkyoraColors.TextSecondary,
    surfaceTint = SkyoraColors.Cyan,
    inverseSurface = SkyoraColors.TextPrimary,
    inverseOnSurface = SkyoraColors.Ink,
    error = SkyoraColors.Error,
    onError = SkyoraColors.Ink,
    errorContainer = SkyoraColors.NavySoft,
    onErrorContainer = SkyoraColors.TextPrimary,
    outline = SkyoraColors.BorderStrong,
    outlineVariant = SkyoraColors.Border,
    scrim = SkyoraColors.Ink,
)

@Composable
fun SkyoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkyoraColorScheme,
        typography = SkyoraTypography,
        shapes = SkyoraShapes,
        content = content,
    )
}
