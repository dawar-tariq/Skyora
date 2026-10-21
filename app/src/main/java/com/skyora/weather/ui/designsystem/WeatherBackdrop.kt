package com.skyora.weather.ui.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private enum class Celestial { MOON, SUN, NONE }

private data class BackdropPalette(
    val sky: List<Color>,
    val glow: Color,
    val glowAlpha: Float,
    val starAlpha: Float,
    val celestial: Celestial,
    val cloudColor: Color,
    val cloudAlpha: Float,
    val cloudCount: Int,
    val ridges: List<Color>,
    val rainCount: Int,
    val snowCount: Int,
    val fog: Boolean,
    val flash: Boolean,
)

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
 * A cinematic, weather-reactive sky built from gradients, stars, clouds and
 * layered ridges. Everything is procedural: it scales from a small phone to a
 * tablet without a single image asset and never distracts from the text above
 * it (a vignette keeps AA contrast for the content).
 */
@Composable
fun SkyoraBackdrop(
    family: WeatherVisualFamily,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    showLandscape: Boolean = true,
) {
    val palette = remember(family, isDay) { backdropPalette(family, isDay) }
    val stars = remember { generateStars(seed = 20261021, count = 76) }
    val clouds = remember(palette.cloudCount) { generateClouds(seed = 7, count = palette.cloudCount) }
    val drops = remember { generateDrops(seed = 91, count = 48) }
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
                colors = palette.sky,
                startY = 0f,
                endY = height,
            ),
            size = size,
        )

        // Horizon glow.
        val glowCenter = Offset(width * 0.72f, height * 0.58f)
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

        if (palette.starAlpha > 0.02f) {
            drawStars(stars, twinkleMotion, palette.starAlpha)
        }

        when (palette.celestial) {
            Celestial.MOON -> drawMoon(centerX = width * 0.79f, centerY = height * 0.155f, radius = width * 0.075f)
            Celestial.SUN -> drawSunDisc(centerX = width * 0.82f, centerY = height * 0.145f, radius = width * 0.058f)
            Celestial.NONE -> Unit
        }

        drawClouds(clouds, width, height, motion, palette)

        if (palette.fog) {
            drawFog(width, height, motion)
        }

        if (showLandscape) {
            drawRidges(ridges, width, height, palette.ridges)
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

        // Readability scrim.
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0x66050A16),
                0.22f to Color(0x14050A16),
                0.58f to Color(0x1A050A16),
                1.0f to Color(0xD9050A16),
            ),
            size = size,
        )
    }
}

private fun backdropPalette(family: WeatherVisualFamily, isDay: Boolean): BackdropPalette {
    if (isDay) {
        return when (family) {
            WeatherVisualFamily.CLEAR -> BackdropPalette(
                sky = listOf(Color(0xFF071E36), Color(0xFF0E3A58), Color(0xFF1E5C7A), Color(0xFF0D2A3D)),
                glow = Color(0xFF8FE4FF),
                glowAlpha = 0.22f,
                starAlpha = 0f,
                celestial = Celestial.SUN,
                cloudColor = Color(0xFFC7E1EF),
                cloudAlpha = 0.06f,
                cloudCount = 2,
                ridges = listOf(Color(0xFF0B2334), Color(0xFF071927), Color(0xFF04111B)),
                rainCount = 0,
                snowCount = 0,
                fog = false,
                flash = false,
            )

            WeatherVisualFamily.CLOUDY -> BackdropPalette(
                sky = listOf(Color(0xFF0A2032), Color(0xFF123449), Color(0xFF244C63), Color(0xFF0E2534)),
                glow = Color(0xFF8FC4D8),
                glowAlpha = 0.12f,
                starAlpha = 0f,
                celestial = Celestial.NONE,
                cloudColor = Color(0xFFC1D6E2),
                cloudAlpha = 0.12f,
                cloudCount = 4,
                ridges = listOf(Color(0xFF0B2333), Color(0xFF071A27), Color(0xFF04121C)),
                rainCount = 0,
                snowCount = 0,
                fog = false,
                flash = false,
            )

            WeatherVisualFamily.RAIN -> BackdropPalette(
                sky = listOf(Color(0xFF071A28), Color(0xFF0D2938), Color(0xFF1C4051), Color(0xFF0A1E2B)),
                glow = Color(0xFF74A9C0),
                glowAlpha = 0.12f,
                starAlpha = 0f,
                celestial = Celestial.NONE,
                cloudColor = Color(0xFFA8BCC8),
                cloudAlpha = 0.14f,
                cloudCount = 5,
                ridges = listOf(Color(0xFF0A1E2C), Color(0xFF061621), Color(0xFF030F17)),
                rainCount = 42,
                snowCount = 0,
                fog = false,
                flash = false,
            )

            WeatherVisualFamily.SNOW -> BackdropPalette(
                sky = listOf(Color(0xFF0A2236), Color(0xFF143A52), Color(0xFF2C5B73), Color(0xFF102B3D)),
                glow = Color(0xFFB6E6F7),
                glowAlpha = 0.16f,
                starAlpha = 0f,
                celestial = Celestial.NONE,
                cloudColor = Color(0xFFD8E9F2),
                cloudAlpha = 0.15f,
                cloudCount = 4,
                ridges = listOf(Color(0xFF123246), Color(0xFF0A2232), Color(0xFF061521)),
                rainCount = 0,
                snowCount = 38,
                fog = false,
                flash = false,
            )

            WeatherVisualFamily.STORM -> BackdropPalette(
                sky = listOf(Color(0xFF04121E), Color(0xFF0A2231), Color(0xFF173A49), Color(0xFF071521)),
                glow = Color(0xFF7FA3B4),
                glowAlpha = 0.10f,
                starAlpha = 0f,
                celestial = Celestial.NONE,
                cloudColor = Color(0xFF7E94A2),
                cloudAlpha = 0.18f,
                cloudCount = 6,
                ridges = listOf(Color(0xFF071825), Color(0xFF041019), Color(0xFF020A11)),
                rainCount = 50,
                snowCount = 0,
                fog = false,
                flash = true,
            )

            WeatherVisualFamily.FOG -> BackdropPalette(
                sky = listOf(Color(0xFF0B2130), Color(0xFF153647), Color(0xFF2B4E5D), Color(0xFF102733)),
                glow = Color(0xFFA9C7D2),
                glowAlpha = 0.14f,
                starAlpha = 0f,
                celestial = Celestial.NONE,
                cloudColor = Color(0xFFD3E1E7),
                cloudAlpha = 0.12f,
                cloudCount = 3,
                ridges = listOf(Color(0xFF0D2634), Color(0xFF081B26), Color(0xFF040F17)),
                rainCount = 0,
                snowCount = 0,
                fog = true,
                flash = false,
            )
        }
    }

    return when (family) {
        WeatherVisualFamily.CLEAR -> BackdropPalette(
            sky = listOf(Color(0xFF02050C), Color(0xFF061225), Color(0xFF0B2C44), Color(0xFF071726)),
            glow = Color(0xFF2FB8E8),
            glowAlpha = 0.18f,
            starAlpha = 0.95f,
            celestial = Celestial.MOON,
            cloudColor = Color(0xFF9DBBD6),
            cloudAlpha = 0.05f,
            cloudCount = 2,
            ridges = listOf(Color(0xFF081625), Color(0xFF050E1A), Color(0xFF03080F)),
            rainCount = 0,
            snowCount = 0,
            fog = false,
            flash = false,
        )

        WeatherVisualFamily.CLOUDY -> BackdropPalette(
            sky = listOf(Color(0xFF03070F), Color(0xFF071322), Color(0xFF0D2030), Color(0xFF06101B)),
            glow = Color(0xFF2A6C88),
            glowAlpha = 0.10f,
            starAlpha = 0.20f,
            celestial = Celestial.MOON,
            cloudColor = Color(0xFF9CB4C5),
            cloudAlpha = 0.10f,
            cloudCount = 4,
            ridges = listOf(Color(0xFF081623), Color(0xFF050F19), Color(0xFF03090F)),
            rainCount = 0,
            snowCount = 0,
            fog = false,
            flash = false,
        )

        WeatherVisualFamily.RAIN -> BackdropPalette(
            sky = listOf(Color(0xFF02060C), Color(0xFF06101B), Color(0xFF0B1E2A), Color(0xFF050D15)),
            glow = Color(0xFF2A6078),
            glowAlpha = 0.10f,
            starAlpha = 0f,
            celestial = Celestial.NONE,
            cloudColor = Color(0xFF8097A8),
            cloudAlpha = 0.14f,
            cloudCount = 5,
            ridges = listOf(Color(0xFF071624), Color(0xFF040E17), Color(0xFF02080E)),
            rainCount = 42,
            snowCount = 0,
            fog = false,
            flash = false,
        )

        WeatherVisualFamily.SNOW -> BackdropPalette(
            sky = listOf(Color(0xFF06101C), Color(0xFF0B2136), Color(0xFF13344A), Color(0xFF071522)),
            glow = Color(0xFF8EC9DE),
            glowAlpha = 0.13f,
            starAlpha = 0.10f,
            celestial = Celestial.MOON,
            cloudColor = Color(0xFFC5DAE6),
            cloudAlpha = 0.12f,
            cloudCount = 4,
            ridges = listOf(Color(0xFF0C2436), Color(0xFF071926), Color(0xFF040F18)),
            rainCount = 0,
            snowCount = 38,
            fog = false,
            flash = false,
        )

        WeatherVisualFamily.STORM -> BackdropPalette(
            sky = listOf(Color(0xFF01040A), Color(0xFF050C15), Color(0xFF0D1B27), Color(0xFF03080D)),
            glow = Color(0xFF4E6A82),
            glowAlpha = 0.09f,
            starAlpha = 0f,
            celestial = Celestial.NONE,
            cloudColor = Color(0xFF5E6E7C),
            cloudAlpha = 0.2f,
            cloudCount = 6,
            ridges = listOf(Color(0xFF050F18), Color(0xFF030A11), Color(0xFF01050A)),
            rainCount = 50,
            snowCount = 0,
            fog = false,
            flash = true,
        )

        WeatherVisualFamily.FOG -> BackdropPalette(
            sky = listOf(Color(0xFF040B12), Color(0xFF0A1A24), Color(0xFF142B37), Color(0xFF08131C)),
            glow = Color(0xFF7E9FAD),
            glowAlpha = 0.12f,
            starAlpha = 0.05f,
            celestial = Celestial.MOON,
            cloudColor = Color(0xFFB6CBD6),
            cloudAlpha = 0.10f,
            cloudCount = 3,
            ridges = listOf(Color(0xFF0A1B26), Color(0xFF061119), Color(0xFF030A0F)),
            rainCount = 0,
            snowCount = 0,
            fog = true,
            flash = false,
        )
    }
}

private fun generateStars(seed: Int, count: Int): List<StarSpec> {
    val random = Random(seed)
    return List(count) {
        StarSpec(
            x = random.nextFloat(),
            y = random.nextFloat() * 0.55f,
            radius = 0.6f + random.nextFloat() * 1.5f,
            alpha = 0.25f + random.nextFloat() * 0.6f,
            phase = random.nextFloat() * 6.28f,
        )
    }
}

private fun generateClouds(seed: Int, count: Int): List<CloudSpec> {
    val random = Random(seed)
    return List(count) { index ->
        CloudSpec(
            x = 0.1f + random.nextFloat() * 0.9f,
            y = 0.15f + random.nextFloat() * 0.32f + index * 0.025f,
            width = 260f + random.nextFloat() * 320f,
            speed = 0.35f + random.nextFloat() * 0.8f,
            alpha = 0.5f + random.nextFloat() * 0.5f,
        )
    }
}

private fun generateDrops(seed: Int, count: Int): List<DropSpec> {
    val random = Random(seed)
    return List(count) {
        DropSpec(
            x = random.nextFloat(),
            offset = random.nextFloat(),
            speed = 1.4f + random.nextFloat() * 1.8f,
            length = 18f + random.nextFloat() * 26f,
            alpha = 0.18f + random.nextFloat() * 0.30f,
        )
    }
}

private fun generateFlakes(seed: Int, count: Int): List<FlakeSpec> {
    val random = Random(seed)
    return List(count) {
        FlakeSpec(
            x = random.nextFloat(),
            offset = random.nextFloat(),
            speed = 0.5f + random.nextFloat() * 0.7f,
            radius = 2.2f + random.nextFloat() * 3.6f,
            phase = random.nextFloat() * 6.28f,
        )
    }
}

private fun ridgePoints(): List<FloatArray> = listOf(
    floatArrayOf(0.70f, 0.63f, 0.68f, 0.59f, 0.66f, 0.72f, 0.64f, 0.70f, 0.74f),
    floatArrayOf(0.79f, 0.72f, 0.77f, 0.69f, 0.75f, 0.82f, 0.74f, 0.80f, 0.84f),
    floatArrayOf(0.90f, 0.84f, 0.88f, 0.81f, 0.86f, 0.93f, 0.85f, 0.90f, 0.94f),
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

private fun DrawScope.drawSunDisc(centerX: Float, centerY: Float, radius: Float) {
    val center = Offset(centerX, centerY)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE0A8).copy(alpha = 0.30f), Color.Transparent),
            center = center,
            radius = radius * 5f,
        ),
        radius = radius * 5f,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF4D6), Color(0xFFFFD08A)),
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

        // A faint cyan rim light on the ridge line.
        if (index == 0) {
            val rim = Path()
            points.forEachIndexed { pointIndex, fraction ->
                val x = width * pointIndex / (points.size - 1).toFloat()
                if (pointIndex == 0) rim.moveTo(x, height * fraction) else rim.lineTo(x, height * fraction)
            }
            drawPath(
                path = rim,
                color = Color(0xFF7FD9F2).copy(alpha = 0.16f),
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

private fun DrawScope.drawFog(width: Float, height: Float, progress: Float) {
    repeat(3) { index ->
        val y = height * (0.50f + index * 0.10f) + sin((progress * 6.28f + index).toDouble()).toFloat() * height * 0.008f
        val bandHeight = height * (0.055f + index * 0.012f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFD6E7EF).copy(alpha = 0.07f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(0f, y),
            size = Size(width, bandHeight),
        )
    }
}
