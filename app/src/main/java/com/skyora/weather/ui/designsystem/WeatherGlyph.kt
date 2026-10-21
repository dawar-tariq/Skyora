package com.skyora.weather.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.skyora.weather.domain.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hand-drawn, resolution-independent weather icons.
 *
 * Compose Canvas keeps every icon crisp at 24dp in the daily list and at 120dp
 * in the hero, without shipping a dozen raster assets.
 */
@Composable
fun WeatherGlyph(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
) {
    Canvas(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
        val unit = size.minDimension / 100f
        withTransform({
            translate(
                left = (size.width - 100f * unit) / 2f,
                top = (size.height - 100f * unit) / 2f,
            )
            scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero)
        }) {
            drawWeatherGlyph(condition, isDay)
        }
    }
}

private fun DrawScope.drawWeatherGlyph(condition: WeatherCondition, isDay: Boolean) {
    val cloudLightTop = Color(0xFFF4FCFF)
    val cloudLightBottom = Color(0xFF9CC3DE)
    val cloudDarkTop = Color(0xFFCADAE6)
    val cloudDarkBottom = Color(0xFF6F8798)

    when (condition) {
        WeatherCondition.CLEAR -> {
            if (isDay) drawSun(54f, 52f, 22f) else drawMoon(54f, 52f, 25f)
        }

        WeatherCondition.MAINLY_CLEAR -> {
            if (isDay) drawSun(62f, 38f, 17f) else drawMoon(63f, 38f, 18f)
            drawCloud(50f, 63f, 54f, cloudLightTop, cloudLightBottom)
        }

        WeatherCondition.PARTLY_CLOUDY -> {
            if (isDay) drawSun(66f, 34f, 15f) else drawMoon(66f, 34f, 16f)
            drawCloud(48f, 62f, 58f, cloudLightTop, cloudLightBottom)
        }

        WeatherCondition.OVERCAST -> {
            drawCloud(57f, 50f, 46f, cloudDarkTop.copy(alpha = 0.75f), cloudDarkBottom.copy(alpha = 0.75f))
            drawCloud(47f, 63f, 58f, cloudLightTop, cloudLightBottom)
        }

        WeatherCondition.FOG -> {
            drawCloud(50f, 54f, 54f, cloudLightTop, cloudLightBottom)
            drawFogLines(50f, 77f)
        }

        WeatherCondition.DRIZZLE -> {
            drawCloud(50f, 54f, 56f, cloudLightTop, cloudLightBottom)
            drawDrizzle(50f, 74f)
        }

        WeatherCondition.FREEZING_DRIZZLE -> {
            drawCloud(50f, 54f, 56f, cloudLightTop, cloudLightBottom)
            drawDrizzle(50f, 74f)
            drawSnowflake(34f, 84f, 3.2f)
        }

        WeatherCondition.RAIN -> {
            drawCloud(50f, 54f, 58f, cloudLightTop, cloudLightBottom)
            drawRain(50f, 74f, count = 3, length = 12f)
        }

        WeatherCondition.FREEZING_RAIN -> {
            drawCloud(50f, 54f, 58f, cloudLightTop, cloudLightBottom)
            drawRain(50f, 74f, count = 3, length = 12f, color = Color(0xFFBDEBFF))
            drawSnowflake(34f, 88f, 3f)
        }

        WeatherCondition.SNOW -> {
            drawCloud(50f, 53f, 56f, cloudLightTop, cloudLightBottom)
            drawSnowflakes(50f, 80f)
        }

        WeatherCondition.SNOW_GRAINS -> {
            drawCloud(50f, 53f, 56f, cloudLightTop, cloudLightBottom)
            repeat(3) { index ->
                drawCircle(
                    color = Color(0xFFE7F7FF),
                    radius = 2.3f,
                    center = Offset(40f + index * 10f, 82f + (index % 2) * 4f),
                )
            }
        }

        WeatherCondition.RAIN_SHOWERS -> {
            if (isDay) drawSun(66f, 34f, 15f) else drawMoon(66f, 34f, 16f)
            drawCloud(47f, 60f, 56f, cloudLightTop, cloudLightBottom)
            drawRain(48f, 76f, count = 2, length = 10f)
        }

        WeatherCondition.SNOW_SHOWERS -> {
            if (isDay) drawSun(66f, 34f, 15f) else drawMoon(66f, 34f, 16f)
            drawCloud(47f, 60f, 56f, cloudLightTop, cloudLightBottom)
            drawSnowflake(42f, 82f, 3.2f)
            drawSnowflake(55f, 84f, 2.8f)
        }

        WeatherCondition.THUNDERSTORM -> {
            drawCloud(50f, 52f, 58f, cloudDarkTop, cloudDarkBottom)
            drawBolt()
            drawRain(52f, 78f, count = 2, length = 8f, alpha = 0.75f)
        }

        WeatherCondition.THUNDERSTORM_HAIL -> {
            drawCloud(50f, 52f, 58f, cloudDarkTop, cloudDarkBottom)
            drawBolt(offsetX = -2f)
            drawCircle(Color(0xFFE8F8FF), radius = 2.4f, center = Offset(64f, 82f))
            drawCircle(Color(0xFFE8F8FF), radius = 2.0f, center = Offset(72f, 76f))
        }

        WeatherCondition.UNKNOWN -> {
            drawCloud(50f, 55f, 54f, cloudLightTop, cloudLightBottom)
        }
    }
}

private fun DrawScope.drawSun(cx: Float, cy: Float, radius: Float, withRays: Boolean = true) {
    val center = Offset(cx, cy)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66FFD98A), Color.Transparent),
            center = center,
            radius = radius * 2.6f,
        ),
        radius = radius * 2.6f,
        center = center,
    )
    if (withRays) {
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val startRadius = radius + 5.5f
            val endRadius = radius + 11.5f
            drawLine(
                color = Color(0xFFFFD27A),
                start = Offset(
                    cx + cos(angle).toFloat() * startRadius,
                    cy + sin(angle).toFloat() * startRadius,
                ),
                end = Offset(
                    cx + cos(angle).toFloat() * endRadius,
                    cy + sin(angle).toFloat() * endRadius,
                ),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round,
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF6D0), Color(0xFFFFC44D)),
            center = Offset(cx - radius * 0.25f, cy - radius * 0.3f),
            radius = radius * 1.4f,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawMoon(cx: Float, cy: Float, radius: Float) {
    val center = Offset(cx, cy)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x4D9FE9FF), Color.Transparent),
            center = center,
            radius = radius * 2.2f,
        ),
        radius = radius * 2.2f,
        center = center,
    )
    val outer = Path().apply { addOval(Rect(center = center, radius = radius)) }
    val cut = Path().apply {
        addOval(Rect(center = Offset(cx + radius * 0.52f, cy - radius * 0.5f), radius = radius * 0.9f))
    }
    val crescent = Path().apply {
        fillType = PathFillType.EvenOdd
        addPath(outer)
        addPath(cut)
    }
    drawPath(
        path = crescent,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE9FBFF), Color(0xFF9BDAFF), Color(0xFF5FB4F4)),
            start = Offset(cx - radius, cy - radius),
            end = Offset(cx + radius, cy + radius),
        ),
    )
    drawCircle(
        color = Color(0xFFD7F4FF),
        radius = 1.6f,
        center = Offset(cx - radius * 1.45f, cy + radius * 0.55f),
    )
    drawCircle(
        color = Color(0xFFD7F4FF).copy(alpha = 0.65f),
        radius = 1.0f,
        center = Offset(cx - radius * 0.9f, cy - radius * 0.9f),
    )
}

private fun DrawScope.drawCloud(
    cx: Float,
    cy: Float,
    width: Float,
    topColor: Color,
    bottomColor: Color,
) {
    val radius = width * 0.17f
    val brush = Brush.verticalGradient(
        colors = listOf(topColor, bottomColor),
        startY = cy - width * 0.3f,
        endY = cy + width * 0.3f,
    )
    drawCircle(brush, radius * 1.1f, Offset(cx - width * 0.25f, cy + width * 0.04f))
    drawCircle(brush, radius * 1.45f, Offset(cx - width * 0.02f, cy - width * 0.08f))
    drawCircle(brush, radius * 1.15f, Offset(cx + width * 0.25f, cy + width * 0.03f))
    drawRoundRect(
        brush = brush,
        topLeft = Offset(cx - width * 0.40f, cy + width * 0.01f),
        size = Size(width * 0.80f, width * 0.25f),
        cornerRadius = CornerRadius(width * 0.125f),
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(cx - width * 0.06f, cy - width * 0.14f),
            radius = width * 0.34f,
        ),
        topLeft = Offset(cx - width * 0.34f, cy - width * 0.22f),
        size = Size(width * 0.56f, width * 0.26f),
    )
}

private fun DrawScope.drawRain(
    cx: Float,
    startY: Float,
    count: Int,
    length: Float,
    color: Color = Color(0xFF7ADFFF),
    alpha: Float = 1f,
) {
    val spacing = 9f
    val startX = cx - (count - 1) * spacing / 2f
    repeat(count) { index ->
        val x = startX + index * spacing
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(x, startY),
            end = Offset(x - 3f, startY + length),
            strokeWidth = 3.1f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawDrizzle(cx: Float, startY: Float) {
    val spacing = 8f
    repeat(3) { index ->
        val x = cx - spacing + index * spacing
        drawLine(
            color = Color(0xFF8FE4FF),
            start = Offset(x, startY),
            end = Offset(x - 1f, startY + 5f),
            strokeWidth = 2.6f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnowflakes(cx: Float, startY: Float) {
    drawSnowflake(cx - 9f, startY, 3.4f)
    drawSnowflake(cx + 1f, startY + 6f, 3.0f)
    drawSnowflake(cx + 11f, startY - 2f, 3.6f)
}

private fun DrawScope.drawSnowflake(cx: Float, cy: Float, radius: Float) {
    val color = Color(0xFFE8FAFF)
    repeat(3) { index ->
        val angle = Math.toRadians(index * 60.0 + 30.0)
        val dx = cos(angle).toFloat() * radius
        val dy = sin(angle).toFloat() * radius
        drawLine(
            color = color,
            start = Offset(cx - dx, cy - dy),
            end = Offset(cx + dx, cy + dy),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawFogLines(cx: Float, startY: Float) {
    val widths = listOf(38f, 48f, 32f)
    widths.forEachIndexed { index, width ->
        drawLine(
            color = Color(0xFFBBD9E8).copy(alpha = 0.65f - index * 0.12f),
            start = Offset(cx - width / 2f, startY + index * 7f),
            end = Offset(cx + width / 2f, startY + index * 7f),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBolt(offsetX: Float = 0f) {
    val bolt = Path().apply {
        moveTo(52f + offsetX, 66f)
        lineTo(44f + offsetX, 82f)
        lineTo(51f + offsetX, 82f)
        lineTo(46f + offsetX, 95f)
        lineTo(62f + offsetX, 77f)
        lineTo(54f + offsetX, 77f)
        lineTo(59f + offsetX, 66f)
        close()
    }
    drawPath(bolt, Color(0xFFFFD166))
    drawPath(
        bolt,
        Color(0xFFFFE6A3),
        style = Stroke(width = 1.2f, cap = StrokeCap.Round),
    )
}
