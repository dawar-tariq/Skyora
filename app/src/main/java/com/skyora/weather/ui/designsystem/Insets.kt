package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skyora.weather.ui.theme.LocalBottomBarInset
import com.skyora.weather.ui.theme.SkyoraSpacing

/**
 * Bottom padding for scrollable screen content. When the floating navigation bar
 * is present its measured height (which already includes the system navigation
 * inset) wins; otherwise the system inset applies. This keeps the last row
 * reachable on gesture- and three-button-navigation devices alike.
 */
@Composable
fun screenBottomPadding(extra: Dp = SkyoraSpacing.xl): Dp {
    val navigationBars = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBar = LocalBottomBarInset.current
    return (if (bottomBar > 0.dp) bottomBar else navigationBars) + extra
}
