package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyora.weather.R
import com.skyora.weather.ui.theme.Manrope
import com.skyora.weather.ui.theme.SkyoraColors
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight

/**
 * The SKYORA moon-and-cloud mark, drawn from the same geometry as the launcher
 * icon so the brand stays consistent from launcher to splash to top bar.
 */
@Composable
fun SkyoraMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlow: Boolean = true,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier = modifier.size(size).then(semanticsModifier)) {
        val unit = this.size.minDimension / 108f
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)

        if (showGlow) {
            val glowCenter = p(60f, 42f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SkyoraColors.Cyan.copy(alpha = 0.30f), Color.Transparent),
                    center = glowCenter,
                    radius = 46f * unit,
                ),
                radius = 46f * unit,
                center = glowCenter,
            )
        }

        val moonOuter = Path().apply {
            addOval(Rect(center = p(61f, 40f), radius = 15f * unit))
        }
        val moonCut = Path().apply {
            addOval(Rect(center = p(69f, 33f), radius = 13f * unit))
        }
        val crescent = Path().apply {
            fillType = PathFillType.EvenOdd
            addPath(moonOuter)
            addPath(moonCut)
        }
        drawPath(
            path = crescent,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFC9F7FF), Color(0xFF7FDCFF), Color(0xFF3B9FFF)),
                start = p(50f, 26f),
                end = p(72f, 56f),
            ),
        )

        val cloudBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFF2FDFF), Color(0xFF6DBAF7), Color(0xFF4A9BEA)),
            startY = 48f * unit,
            endY = 84f * unit,
        )
        drawCircle(cloudBrush, radius = 10f * unit, center = p(38f, 68f))
        drawCircle(cloudBrush, radius = 13f * unit, center = p(53f, 62f))
        drawCircle(cloudBrush, radius = 10f * unit, center = p(68f, 68f))
        drawRoundRect(
            brush = cloudBrush,
            topLeft = p(37f, 66f),
            size = Size(31f * unit, 14f * unit),
            cornerRadius = CornerRadius(7f * unit),
        )
        // Soft highlight on the top of the cloud.
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                center = p(53f, 55f),
                radius = 14f * unit,
            ),
            topLeft = p(39f, 49f),
            size = Size(28f * unit, 14f * unit),
        )
    }
}

@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 40.dp,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        SkyoraMark(
            size = markSize,
            contentDescription = stringResource(R.string.cd_brand),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(R.string.brand_wordmark),
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 13.sp else 15.sp,
                letterSpacing = if (compact) 3.4.sp else 4.6.sp,
                color = SkyoraColors.TextPrimary,
            )
            if (!compact) {
                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyoraColors.TextTertiary,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
fun SkyoraWordmark(
    modifier: Modifier = Modifier,
    fontSize: Int = 34,
    letterSpacing: Int = 11,
    color: Color = SkyoraColors.TextPrimary,
) {
    Text(
        text = stringResource(R.string.brand_wordmark),
        modifier = modifier,
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        letterSpacing = letterSpacing.sp,
        color = color,
    )
}
