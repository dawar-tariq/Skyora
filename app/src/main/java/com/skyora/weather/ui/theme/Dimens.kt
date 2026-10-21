package com.skyora.weather.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tokenised spacing. Screens use these instead of ad-hoc values. */
object SkyoraSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 40.dp

    /** Horizontal gutter used by every screen. */
    val screen: Dp = 20.dp

    /** Vertical rhythm between major content sections. */
    val section: Dp = 22.dp
}

object SkyoraSizes {
    val iconButton: Dp = 46.dp
    val touchTarget: Dp = 48.dp
    val buttonHeight: Dp = 54.dp
    val cardPadding: Dp = 18.dp
    val bottomBarHeight: Dp = 66.dp
    val maxContentWidth: Dp = 640.dp
    val topBarHeight: Dp = 56.dp
}

/**
 * Height reserved at the bottom of scrollable tab content so the floating
 * navigation bar never covers the last row.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }
