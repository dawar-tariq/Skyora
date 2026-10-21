package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight

/**
 * The SKYORA moon-and-cloud mark.
 *
 * This is the same artwork as the launcher icon, exported once as a
 * transparent PNG per density (`ic_brand_mark`) so the crescent and cloud are
 * pixel-identical from the launcher to the splash screen to the top bar.
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
    Box(
        modifier = modifier.size(size).then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (showGlow) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SkyoraColors.Cyan.copy(alpha = 0.22f), Color.Transparent),
                        center = center,
                        radius = this.size.minDimension * 0.52f,
                    ),
                    radius = this.size.minDimension * 0.52f,
                    center = center,
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_brand_mark),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.94f),
            contentScale = ContentScale.Fit,
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
