package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyora.weather.R
import com.skyora.weather.ui.theme.Manrope
import com.skyora.weather.ui.theme.SkyoraColors
import androidx.compose.ui.res.stringResource

data class SkyoraNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val SkyoraDestinations = listOf(
    SkyoraNavDestination(
        route = "home",
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    SkyoraNavDestination(
        route = "search",
        labelRes = R.string.nav_search,
        selectedIcon = Icons.Outlined.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
    SkyoraNavDestination(
        route = "saved",
        labelRes = R.string.nav_saved,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
    ),
)

@Composable
fun SkyoraBottomNavigation(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xF2071120), Color(0xFF050A16)),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SkyoraColors.Border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkyoraDestinations.forEach { destination ->
                val selected = destination.route == currentRoute
                val label = stringResource(destination.labelRes)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onSelect(destination.route) },
                        )
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 26.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) SkyoraColors.Cyan.copy(alpha = 0.16f) else Color.Transparent,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                            tint = if (selected) SkyoraColors.Cyan else SkyoraColors.TextTertiary,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontFamily = Manrope,
                        fontSize = 10.sp,
                        color = if (selected) SkyoraColors.CyanSoft else SkyoraColors.TextTertiary,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                    )
                }
            }
        }
    }
}
