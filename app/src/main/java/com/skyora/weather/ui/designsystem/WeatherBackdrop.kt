package com.skyora.weather.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.skyora.weather.domain.model.SkyMoment
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private data class StarSpec(val x: Float, val y: Float, val radius: Float, val alpha: Float, val phase: Float)
private data class CloudSpec(val x: Float, val y: Float, val width: Float, val speed: Float, val alpha: Float)
private data class DropSpec(
    val x: Float,
    val offset: Float,
    val speed: Float,
    val length: Float,
    val alpha: Float,
)
private data class FlakeSpec(
    val x: Float,
    val offset: Float,
    val speed: Float,
    val radius: Float,
    val phase: Float,
)

/**
 * A cinematic, weather- and time-aware sky.
 *
 * The palette is chosen by [SkyMoment] — a resolved (phase, condition) pair —
 * so the same forecast renders as a blue daylight sky at 13:00 and a navy
 * night sky at 01:00 for the *same city*, regardless of where the phone is.
 * See `domain/model/SkyMoment.kt` for the resolution rules.
 *
 * Everything is procedural: no image assets, and the bottom of the screen is
 * always darkened by a palette-specific scrim so white text keeps at least
 * 4.5:1 contrast on every treatment (asserted for every palette by
 * `SkyBackdropContrastTest`). Palette data lives in SkyPalettes.kt.
 */
@Composable
fun SkyoraBackdrop(
    moment: SkyMoment,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    showLandscape: Boolean = true,
) {
    val target = remember(moment) { paletteFor(moment) }

    // Two-slot blend: data arriving or a city change cross-fades instead of
    // snapping between a dark and a bright sky.
    var previous by remember { mutableStateOf(target) }
    var current by remember { mutableStateOf(target) }
    val blend = remember { Animatable(1f) }

    LaunchedEffect(target, animate) {
        if (previous == target && current == target) return@LaunchedEffect
        previous = current
        current = target
        if (!animate) {
            previous = target
            blend.snapTo(1f)
            return@LaunchedEffect
        }
        blend.snapTo(0f)
        blend.animateTo(1f, animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing))
        previous = current
        blend.snapTo(1f)
    }

    val progress = blend.value
    val palette = remember(previous, current, progress) {
        if (progress >= 1f) current else lerpPalette(previous, current, progress)
    }

    val stars = remember { generateStars(seed = 20261021, count = 76) }
    val clouds = remember(palette.cloudCount) { generateClouds(seed = 7, count = palette.cloudCount) }
    val drops = remember { generateDrops(seed = 91, count = 64) }
    val flakes = remember { generateFlakes(seed = 27, count = 44) }
    val ridges = remember { ridgePoints() }

    val transition = rememberInfiniteTransition(label = "sky")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 180_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "twinkle",
    )
    val precipitation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "precipitation",
    )
    val lightning by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lightning",
    )

    val motion = if (animate) drift else 0f
    val twinkleMotion = if (animate) twinkle else 0.35f
    val precipitationMotion = if (animate) precipitation else 0.42f
    val lightningMotion = if (animate) lightning else 0f

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        drawRect(
            brush = Brush.verticalGradient(
                // Spread keeps this on the vararg overload; the explicit stops
                // give the sky its shape instead of evenly spaced bands.
                *SKY_STOPS.mapIndexed { index, stop ->
                    stop to palette.sky.getOrElse(index) { palette.sky.last() }
                }.toTypedArray(),
                startY = 0f,
                endY = height,
            ),
            size = size,
        )

        // Sunrise / sunset band. Narrow by design: it belongs to dawn and dusk,
        // never to the whole afternoon.
        val horizon = palette.horizon
        if (horizon != null && palette.horizonAlpha > 0.01f) {
            val centerY = height * palette.horizonY
            val extent = height * 0.17f
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.45f to horizon.copy(alpha = palette.horizonAlpha),
                        0.62f to horizon.copy(alpha = palette.horizonAlpha * 0.82f),
                        1f to Color.Transparent,
                    ),
                    startY = centerY - extent,
                    endY = centerY + extent,
                ),
                topLeft = Offset(0f, centerY - extent),
                size = Size(width, extent * 2f),
            )
        }

        // Atmospheric glow: warm and low for daylight, cool and high at night.
        val glowCenter = Offset(width * 0.72f, height * palette.glowY)
        if (palette.glowAlpha > 0.005f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.glow.copy(alpha = palette.glowAlpha),
                        palette.glow.copy(alpha = palette.glowAlpha * 0.25f),
                        Color.Transparent,
                    ),
                    center = glowCenter,
                    radius = width * 1.05f,
                ),
                radius = width * 1.05f,
                center = glowCenter,
            )
        }

        if (palette.starAlpha > 0.02f) {
            drawStars(stars, twinkleMotion, palette.starAlpha)
        }

        when (palette.celestial) {
            Celestial.MOON -> drawMoon(centerX = width * 0.79f, centerY = height * 0.155f, radius = width * 0.075f)
            Celestial.SUN -> drawSunDisc(
                centerX = width * 0.82f,
                centerY = height * palette.celestialY,
                radius = width * palette.sunRadius,
                inner = palette.sunInner,
                outer = palette.sunOuter,
            )
            Celestial.NONE -> Unit
        }

        drawClouds(clouds, width, height, motion, palette)

        if (palette.fog) {
            drawFog(width, height, motion, palette.cloudColor)
        }

        if (showLandscape) {
            drawRidges(ridges, width, height, palette.ridges, palette.ridgeRim)
        }

        if (palette.rainCount > 0) {
            drawRain(drops.take(palette.rainCount), width, height, precipitationMotion)
        }
        if (palette.snowCount > 0) {
            drawSnow(flakes.take(palette.snowCount), width, height, precipitationMotion)
        }

        if (palette.flash) {
            val flashAlpha = max(0f, sin(lightningMotion * Math.PI * 6).toFloat() - 0.965f) * 5.5f
            if (flashAlpha > 0.002f) {
                drawRect(Color(0xFFDCEBFF).copy(alpha = flashAlpha.coerceIn(0f, 0.20f)))
            }
        }

        // Readability scrim: palette-specific, because a bright daylight sky
        // needs more tint than a night sky does.
        drawRect(
            brush = Brush.verticalGradient(
                *VEIL_STOPS.mapIndexed { index, stop ->
                    stop to VEIL_COLOR.copy(alpha = palette.veil.getOrElse(index) { palette.veil.last() })
                }.toTypedArray(),
            ),
            size = size,
        )
    }
}

// ---------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------

private fun generateStars(seed: Int, count: Int): List<StarSpec> {
    val random = Random(seed)
    return List(count) {
        StarSpec(
            x = random.nextFloat(),
            y = random.nextFloat() * 0.55f,
            radius = 0.6f + random.nextFloat() * 1.5f,
            alpha = 0.35f + random.nextFloat() * 0.65f,
            phase = random.nextFloat() * 6.28f,
        )
    }
}

private fun generateClouds(seed: Int, count: Int): List<CloudSpec> {
    val random = Random(seed)
    return List(count) {
        CloudSpec(
            x = random.nextFloat(),
            y = 0.08f + random.nextFloat() * 0.42f,
            width = 0.45f + random.nextFloat() * 0.55f,
            speed = 0.4f + random.nextFloat() * 1.2f,
            alpha = 0.55f + random.nextFloat() * 0.45f,
        )
    }
}

private fun generateDrops(seed: Int, count: Int): List<DropSpec> {
    val random = Random(seed)
    return List(count) {
        DropSpec(
            x = random.nextFloat(),
            offset = random.nextFloat(),
            speed = 0.55f + random.nextFloat() * 0.75f,
            length = 10f + random.nextFloat() * 22f,
            alpha = 0.18f + random.nextFloat() * 0.35f,
        )
    }
}

private fun generateFlakes(seed: Int, count: Int): List<FlakeSpec> {
    val random = Random(seed)
    return List(count) {
        FlakeSpec(
            x = random.nextFloat(),
            offset = random.nextFloat(),
            speed = 0.18f + random.nextFloat() * 0.30f,
            radius = 1.2f + random.nextFloat() * 2.4f,
            phase = random.nextFloat() * 6.28f,
        )
    }
}

private fun ridgePoints(): List<FloatArray> = listOf(
    floatArrayOf(0.72f, 0.78f, 0.52f, 0.66f, 0.40f, 0.60f, 0.48f, 0.72f, 0.56f, 0.64f, 0.86f),
    floatArrayOf(0.80f, 0.86f, 0.62f, 0.74f, 0.52f, 0.70f, 0.58f, 0.80f, 0.66f, 0.74f, 0.92f),
    floatArrayOf(0.88f, 0.93f, 0.72f, 0.82f, 0.64f, 0.80f, 0.70f, 0.88f, 0.76f, 0.84f, 0.97f),
)

private fun DrawScope.drawStars(stars: List<StarSpec>, progress: Float, alphaMultiplier: Float) {
    val minDimension = size.minDimension
    stars.forEach { star ->
        val twinkle = 0.6f + 0.4f * sin((progress * 6.28f + star.phase).toDouble()).toFloat()
        val alpha = (star.alpha * alphaMultiplier * twinkle).coerceIn(0f, 1f)
        if (alpha < 0.02f) return@forEach
        drawCircle(
            color = Color(0xFFDFF6FF).copy(alpha = alpha),
            radius = (0.8f + star.radius) * (minDimension / 700f).coerceAtLeast(0.5f),
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }
}

private fun DrawScope.drawMoon(centerX: Float, centerY: Float, radius: Float) {
    val center = Offset(centerX, centerY)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF9CE4FF).copy(alpha = 0.20f),
                Color(0xFF6FC6FF).copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = center,
            radius = radius * 5.2f,
        ),
        radius = radius * 5.2f,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF6FEFF), Color(0xFFD7F1FF), Color(0xFFAFDCF7)),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f),
            radius = radius * 1.5f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = Color(0xFF8FC2D8).copy(alpha = 0.22f),
        radius = radius * 0.22f,
        center = Offset(center.x + radius * 0.32f, center.y + radius * 0.20f),
    )
    drawCircle(
        color = Color(0xFF8FC2D8).copy(alpha = 0.16f),
        radius = radius * 0.14f,
        center = Offset(center.x - radius * 0.30f, center.y + radius * 0.34f),
    )
}

private fun DrawScope.drawSunDisc(
    centerX: Float,
    centerY: Float,
    radius: Float,
    inner: Color,
    outer: Color,
) {
    val center = Offset(centerX, centerY)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(inner.copy(alpha = 0.34f), Color.Transparent),
            center = center,
            radius = radius * 5f,
        ),
        radius = radius * 5f,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(inner, outer),
            center = Offset(center.x - radius * 0.2f, center.y - radius * 0.25f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawClouds(
    clouds: List<CloudSpec>,
    width: Float,
    height: Float,
    progress: Float,
    palette: BackdropPalette,
) {
    clouds.forEach { cloud ->
        val drift = (cloud.x + progress * 0.12f * cloud.speed) % 1.35f - 0.15f
        val cx = drift * width
        val cy = cloud.y * height
        val w = cloud.width * (width / 1080f).coerceAtLeast(0.5f)
        val h = w * 0.30f
        val alpha = (palette.cloudAlpha * cloud.alpha).coerceIn(0f, 1f)
        val brush = Brush.radialGradient(
            colors = listOf(
                palette.cloudColor.copy(alpha = alpha),
                palette.cloudColor.copy(alpha = alpha * 0.55f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = w * 0.62f,
        )
        drawOval(
            brush = brush,
            topLeft = Offset(cx - w / 2f, cy - h / 2f),
            size = Size(w, h),
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.cloudColor.copy(alpha = alpha * 0.8f),
                    Color.Transparent,
                ),
                center = Offset(cx + w * 0.12f, cy - h * 0.25f),
                radius = w * 0.36f,
            ),
            topLeft = Offset(cx - w * 0.18f, cy - h * 0.72f),
            size = Size(w * 0.62f, h * 0.9f),
        )
    }
}

private fun DrawScope.drawRidges(
    ridges: List<FloatArray>,
    width: Float,
    height: Float,
    colors: List<Color>,
    rimColor: Color,
) {
    ridges.forEachIndexed { index, points ->
        val path = Path()
        path.moveTo(0f, height)
        points.forEachIndexed { pointIndex, fraction ->
            val x = width * pointIndex / (points.size - 1).toFloat()
            path.lineTo(x, height * fraction)
        }
        path.lineTo(width, height)
        path.close()
        drawPath(path, colors.getOrElse(index) { colors.last() })

        // A faint rim light on the ridge line: warm in daylight, cyan at night.
        if (index == 0) {
            val rim = Path()
            points.forEachIndexed { pointIndex, fraction ->
                val x = width * pointIndex / (points.size - 1).toFloat()
                if (pointIndex == 0) rim.moveTo(x, height * fraction) else rim.lineTo(x, height * fraction)
            }
            drawPath(
                path = rim,
                color = rimColor.copy(alpha = 0.16f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

private fun DrawScope.drawRain(
    drops: List<DropSpec>,
    width: Float,
    height: Float,
    progress: Float,
) {
    drops.forEach { drop ->
        val y = ((drop.offset + progress * drop.speed) % 1.12f) * height
        val x = drop.x * width + y * 0.08f
        val startY = y.coerceAtLeast(height * 0.12f)
        if (startY >= height) return@forEach
        drawLine(
            color = Color(0xFFB4E9FF).copy(alpha = drop.alpha),
            start = Offset(x, startY),
            end = Offset(x - drop.length * 0.28f, startY + drop.length),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnow(
    flakes: List<FlakeSpec>,
    width: Float,
    height: Float,
    progress: Float,
) {
    flakes.forEach { flake ->
        val y = ((flake.offset + progress * flake.speed) % 1.12f) * height
        val sway = sin((progress * 6.28f * flake.speed + flake.phase).toDouble()).toFloat() * width * 0.02f
        drawCircle(
            color = Color(0xFFEAF9FF).copy(alpha = 0.55f),
            radius = flake.radius,
            center = Offset(flake.x * width + sway, y),
        )
    }
}

private fun DrawScope.drawFog(width: Float, height: Float, progress: Float, tint: Color) {
    repeat(3) { index ->
        val y = height * (0.50f + index * 0.10f) + sin((progress * 6.28f + index).toDouble()).toFloat() * height * 0.008f
        val bandHeight = height * (0.055f + index * 0.012f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    tint.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(0f, y),
            size = Size(width, bandHeight),
        )
    }
}
