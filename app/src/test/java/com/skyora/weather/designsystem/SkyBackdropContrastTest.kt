package com.skyora.weather.designsystem

import androidx.compose.ui.graphics.Color
import com.skyora.weather.domain.model.SkyCondition
import com.skyora.weather.domain.model.SkyMoment
import com.skyora.weather.domain.model.SkyPhase
import com.skyora.weather.ui.designsystem.BackdropPalette
import com.skyora.weather.ui.designsystem.SKY_STOPS
import com.skyora.weather.ui.designsystem.VEIL_COLOR
import com.skyora.weather.ui.designsystem.VEIL_STOPS
import com.skyora.weather.ui.designsystem.dayPalette
import com.skyora.weather.ui.designsystem.horizonPalette
import com.skyora.weather.ui.designsystem.nightPalette
import com.skyora.weather.ui.designsystem.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the readability claim made in SkyPalettes.kt.
 *
 * A brighter daytime sky is the whole point of the fix, but `SkyoraColors`'s
 * body text is near-white, so every palette must still keep white at 4.5:1
 * (WCAG AA for normal text) once its own scrim is applied. This reads the real
 * palettes — not a copy — so changing a sky colour to something too bright
 * fails here instead of shipping an unreadable screen.
 */
class SkyBackdropContrastTest {

    private val minimumRatio = 4.5

    private fun white(): Color = Color(0xFFFFFFFF)

    private fun skyAt(palette: BackdropPalette, y: Float): Color {
        val stops = SKY_STOPS
        val colors = palette.sky
        if (y <= stops.first()) return colors.first()
        if (y >= stops.last()) return colors.last()
        for (index in 0 until stops.size - 1) {
            val low = stops[index]
            val high = stops[index + 1]
            if (y in low..high) {
                val t = (y - low) / (high - low)
                return blend(colors[index], colors[index + 1], t)
            }
        }
        return colors.last()
    }

    private fun blend(from: Color, to: Color, t: Float): Color = Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = 1f,
    )

    private fun over(base: Color, tint: Color, alpha: Float): Color = blend(base, tint, alpha)

    /** WCAG relative luminance from sRGB channels. */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        val lighter = maxOf(a, b)
        val darker = minOf(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Worst contrast anywhere in the vertical scrim, not just at the stops. */
    private fun worstContrast(palette: BackdropPalette): Double {
        var worst = Double.MAX_VALUE
        var y = 0f
        while (y <= 1.0001f) {
            var alpha = palette.veil.last()
            for (index in 0 until VEIL_STOPS.size - 1) {
                val low = VEIL_STOPS[index]
                val high = VEIL_STOPS[index + 1]
                if (y in low..high) {
                    val t = (y - low) / (high - low)
                    alpha = palette.veil[index] + (palette.veil[index + 1] - palette.veil[index]) * t
                    break
                }
            }
            val effective = over(skyAt(palette, y), VEIL_COLOR, alpha)
            worst = minOf(worst, contrast(white(), effective))
            y += 0.01f
        }
        return worst
    }

    private fun assertReadable(label: String, palette: BackdropPalette) {
        val ratio = worstContrast(palette)
        assertTrue(
            "$label: white text falls to %.2f:1 (needs %.1f:1)".format(ratio, minimumRatio),
            ratio >= minimumRatio,
        )
    }

    @Test
    fun `every daylight palette keeps white text readable`() {
        SkyCondition.entries.forEach { condition ->
            assertReadable("day/$condition", dayPalette(condition))
        }
    }

    @Test
    fun `every night palette keeps white text readable`() {
        SkyCondition.entries.forEach { condition ->
            assertReadable("night/$condition", nightPalette(condition))
        }
    }

    @Test
    fun `every sunrise and sunset palette keeps white text readable`() {
        SkyCondition.entries.forEach { condition ->
            assertReadable("dawn/$condition", horizonPalette(condition, dawn = true))
            assertReadable("dusk/$condition", horizonPalette(condition, dawn = false))
        }
    }

    @Test
    fun `every resolvable moment is covered by the contrast checks`() {
        var covered = 0
        SkyPhase.entries.forEach { phase ->
            SkyCondition.entries.forEach { condition ->
                val palette = paletteFor(SkyMoment(phase, condition))
                assertReadable("$phase/$condition", palette)
                covered++
            }
        }
        assertEquals(28, covered)
    }

    @Test
    fun `daytime skies are actually brighter than night skies`() {
        // The regression that started this: a "day" palette that was as dark as
        // the night one. Compare mean luminance of the sky gradient itself.
        fun meanSkyLuminance(palette: BackdropPalette): Double =
            palette.sky.map { luminance(it) }.average()

        SkyCondition.entries.forEach { condition ->
            val day = meanSkyLuminance(dayPalette(condition))
            val night = meanSkyLuminance(nightPalette(condition))
            assertTrue(
                "$condition day sky (%.3f) must be brighter than night (%.3f)".format(day, night),
                day > night * 2.0,
            )
        }
    }

    @Test
    fun `daytime palettes have no stars and a sun or neutral glow`() {
        SkyCondition.entries.forEach { condition ->
            val palette = dayPalette(condition)
            assertEquals("$condition day stars", 0f, palette.starAlpha, 0.001f)
            assertTrue("$condition day glow", palette.glowAlpha >= 0.08f)
        }
    }

    @Test
    fun `rain and storm palettes keep their precipitation in daylight too`() {
        assertTrue(dayPalette(SkyCondition.RAIN).rainCount > 0)
        assertTrue(dayPalette(SkyCondition.STORM).rainCount > 0)
        assertTrue(dayPalette(SkyCondition.STORM).flash)
        assertTrue(dayPalette(SkyCondition.SNOW).snowCount > 0)
        assertEquals(0, dayPalette(SkyCondition.CLEAR).rainCount)
    }
}
