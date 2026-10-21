package com.skyora.weather.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.skyora.weather.domain.model.SkyCondition
import com.skyora.weather.domain.model.SkyMoment
import com.skyora.weather.domain.model.SkyPhase

/*
 * Palette data for the time-aware backdrop.
 *
 * Deliberately free of Compose runtime imports: the renderer lives in
 * WeatherBackdrop.kt, while the numbers live here so `SkyBackdropContrastTest`
 * can assert that every stop of every palette keeps white text at AA contrast.
 */

internal enum class Celestial { MOON, SUN, NONE }

/** Stop positions shared by every sky gradient (top to bottom). */
internal val SKY_STOPS = listOf(0f, 0.16f, 0.34f, 0.62f, 1f)

/** Scrim stops; the bottom third is deliberately dark so cards stay AA-legible. */
internal val VEIL_STOPS = listOf(0f, 0.22f, 0.58f, 1f)

/** Base colour of the readability scrim. */
internal val VEIL_COLOR = Color(0xFF050A16)

internal data class BackdropPalette(
    val sky: List<Color>,
    /** Alpha of the readability scrim at [VEIL_STOPS]. */
    val veil: List<Float>,
    val glow: Color,
    val glowAlpha: Float,
    val glowY: Float,
    val starAlpha: Float,
    val celestial: Celestial,
    val sunInner: Color = Color(0xFFFFF4D6),
    val sunOuter: Color = Color(0xFFFFCF8A),
    val cloudColor: Color,
    val cloudAlpha: Float,
    val cloudCount: Int,
    val ridges: List<Color>,
    val ridgeRim: Color,
    val rainCount: Int,
    val snowCount: Int,
    val fog: Boolean,
    val flash: Boolean,
    /** Sunrise/sunset band: colour, alpha and centre height (0-1). */
    val horizon: Color? = null,
    val horizonAlpha: Float = 0f,
    val horizonY: Float = 0.66f,
) {
    /** Draws the low sun disc for daylight palettes. */
    val sunRadius: Float get() = if (celestial == Celestial.SUN) 0.058f else 0f
    val celestialY: Float get() = if (celestial == Celestial.SUN) 0.145f else 0.155f
}

internal fun paletteFor(moment: SkyMoment): BackdropPalette = when (moment.phase) {
    SkyPhase.DAY -> dayPalette(moment.condition)
    SkyPhase.NIGHT -> nightPalette(moment.condition)
    SkyPhase.DAWN -> horizonPalette(moment.condition, dawn = true)
    SkyPhase.DUSK -> horizonPalette(moment.condition, dawn = false)
}

/**
 * Daylight palettes.
 *
 * These are real daylight skies — blue above, deeper blue toward the horizon —
 * not the midnight navy used before. The scrims below are the lightest values
 * that keep white body text at 4.5:1 or better; see the contrast test.
 */
internal fun dayPalette(condition: SkyCondition): BackdropPalette = when (condition) {
    SkyCondition.CLEAR -> BackdropPalette(
        sky = listOf(Color(0xFF5AA8D8), Color(0xFF4891C6), Color(0xFF2E73A4), Color(0xFF1B4E76), Color(0xFF0D2A44)),
        veil = listOf(0.28f, 0.20f, 0.15f, 0.80f),
        glow = Color(0xFFFFE9BD),
        glowAlpha = 0.30f,
        glowY = 0.18f,
        starAlpha = 0f,
        celestial = Celestial.SUN,
        cloudColor = Color(0xFFEAF5FC),
        cloudAlpha = 0.10f,
        cloudCount = 3,
        ridges = listOf(Color(0xFF3A6D93), Color(0xFF2C5678), Color(0xFF1F4058)),
        ridgeRim = Color(0xFFFFF3DA),
        rainCount = 0,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.CLOUDY -> BackdropPalette(
        sky = listOf(Color(0xFF63A8CE), Color(0xFF5498C1), Color(0xFF3D7DA5), Color(0xFF2A5D80), Color(0xFF173D57)),
        veil = listOf(0.30f, 0.22f, 0.16f, 0.80f),
        glow = Color(0xFFFFF0D2),
        glowAlpha = 0.22f,
        glowY = 0.20f,
        starAlpha = 0f,
        celestial = Celestial.SUN,
        cloudColor = Color(0xFFF2F8FD),
        cloudAlpha = 0.26f,
        cloudCount = 5,
        ridges = listOf(Color(0xFF3E7196), Color(0xFF2F5A7C), Color(0xFF20415A)),
        ridgeRim = Color(0xFFFFF3DA),
        rainCount = 0,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.OVERCAST -> BackdropPalette(
        sky = listOf(Color(0xFF7E97A8), Color(0xFF728B9C), Color(0xFF5E7A8C), Color(0xFF476071), Color(0xFF2C4152)),
        veil = listOf(0.34f, 0.26f, 0.18f, 0.80f),
        glow = Color(0xFFE8F0F6),
        glowAlpha = 0.14f,
        glowY = 0.24f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFFDCE7EE),
        cloudAlpha = 0.24f,
        cloudCount = 6,
        ridges = listOf(Color(0xFF4A6475), Color(0xFF38505F), Color(0xFF253844)),
        ridgeRim = Color(0xFFE9F2F7),
        rainCount = 0,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.RAIN -> BackdropPalette(
        sky = listOf(Color(0xFF6C8598), Color(0xFF5F7A8D), Color(0xFF4D6879), Color(0xFF38505F), Color(0xFF22353F)),
        veil = listOf(0.24f, 0.24f, 0.18f, 0.82f),
        glow = Color(0xFFCFE0EC),
        glowAlpha = 0.10f,
        glowY = 0.26f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFFC3D3DE),
        cloudAlpha = 0.22f,
        cloudCount = 6,
        ridges = listOf(Color(0xFF33505F), Color(0xFF26404D), Color(0xFF1A303B)),
        ridgeRim = Color(0xFFDCEBF3),
        rainCount = 52,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.STORM -> BackdropPalette(
        sky = listOf(Color(0xFF5A7285), Color(0xFF50687B), Color(0xFF3F5769), Color(0xFF2C4052), Color(0xFF182633)),
        veil = listOf(0.26f, 0.26f, 0.20f, 0.84f),
        glow = Color(0xFFD5E3EC),
        glowAlpha = 0.08f,
        glowY = 0.28f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFFAFC1CC),
        cloudAlpha = 0.26f,
        cloudCount = 7,
        ridges = listOf(Color(0xFF2C4655), Color(0xFF20323D), Color(0xFF16242C)),
        ridgeRim = Color(0xFFD3E5EF),
        rainCount = 60,
        snowCount = 0,
        fog = false,
        flash = true,
    )

    SkyCondition.SNOW -> BackdropPalette(
        sky = listOf(Color(0xFF7E9CB3), Color(0xFF7392AA), Color(0xFF5E7E96), Color(0xFF456378), Color(0xFF2B4456)),
        veil = listOf(0.34f, 0.26f, 0.18f, 0.80f),
        glow = Color(0xFFEAF4FA),
        glowAlpha = 0.16f,
        glowY = 0.22f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFFE6F1F7),
        cloudAlpha = 0.20f,
        cloudCount = 4,
        ridges = listOf(Color(0xFF4A6B80), Color(0xFF385366), Color(0xFF253A49)),
        ridgeRim = Color(0xFFFFFFFF),
        rainCount = 0,
        snowCount = 38,
        fog = false,
        flash = false,
    )

    SkyCondition.FOG -> BackdropPalette(
        sky = listOf(Color(0xFF88949C), Color(0xFF7D8A93), Color(0xFF6B7983), Color(0xFF54636D), Color(0xFF36444E)),
        veil = listOf(0.36f, 0.28f, 0.20f, 0.82f),
        glow = Color(0xFFF0F4F6),
        glowAlpha = 0.16f,
        glowY = 0.30f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFFE9EEF1),
        cloudAlpha = 0.18f,
        cloudCount = 3,
        ridges = listOf(Color(0xFF56666F), Color(0xFF414F57), Color(0xFF2C383E)),
        ridgeRim = Color(0xFFF2F7F9),
        rainCount = 0,
        snowCount = 0,
        fog = true,
        flash = false,
    )
}

/** Night palettes: the brand's original midnight treatment, unchanged in spirit. */
internal fun nightPalette(condition: SkyCondition): BackdropPalette = when (condition) {
    SkyCondition.CLEAR -> BackdropPalette(
        sky = listOf(Color(0xFF02050C), Color(0xFF061225), Color(0xFF0B2C44), Color(0xFF071726), Color(0xFF02060D)),
        veil = listOf(0.34f, 0.10f, 0.10f, 0.84f),
        glow = Color(0xFF2FB8E8),
        glowAlpha = 0.18f,
        glowY = 0.58f,
        starAlpha = 0.95f,
        celestial = Celestial.MOON,
        cloudColor = Color(0xFF9DBBD6),
        cloudAlpha = 0.05f,
        cloudCount = 2,
        ridges = listOf(Color(0xFF081625), Color(0xFF050E1A), Color(0xFF03080F)),
        ridgeRim = Color(0xFF7FD9F2),
        rainCount = 0,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.CLOUDY -> BackdropPalette(
        sky = listOf(Color(0xFF03070F), Color(0xFF071322), Color(0xFF0D2030), Color(0xFF06101B), Color(0xFF03070E)),
        veil = listOf(0.36f, 0.12f, 0.12f, 0.84f),
        glow = Color(0xFF2A6C88),
        glowAlpha = 0.10f,
        glowY = 0.56f,
        starAlpha = 0.20f,
        celestial = Celestial.MOON,
        cloudColor = Color(0xFF9CB4C5),
        cloudAlpha = 0.10f,
        cloudCount = 4,
        ridges = listOf(Color(0xFF081623), Color(0xFF050F19), Color(0xFF03090F)),
        ridgeRim = Color(0xFF7FD9F2),
        rainCount = 0,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.OVERCAST -> BackdropPalette(
        sky = listOf(Color(0xFF03060D), Color(0xFF050E18), Color(0xFF0A1A26), Color(0xFF050C14), Color(0xFF02060A)),
        veil = listOf(0.36f, 0.14f, 0.14f, 0.85f),
        glow = Color(0xFF2C5B72),
        glowAlpha = 0.08f,
        glowY = 0.58f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFF8CA2B1),
        cloudAlpha = 0.14f,
        cloudCount = 5,
        ridges = listOf(Color(0xFF071320), Color(0xFF040C15), Color(0xFF02070C)),
        ridgeRim = Color(0xFF7FD9F2),
        rainCount = 0,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.RAIN -> BackdropPalette(
        sky = listOf(Color(0xFF02060C), Color(0xFF06101B), Color(0xFF0B1E2A), Color(0xFF050D15), Color(0xFF02060B)),
        veil = listOf(0.36f, 0.14f, 0.14f, 0.86f),
        glow = Color(0xFF2A6078),
        glowAlpha = 0.10f,
        glowY = 0.58f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFF8097A8),
        cloudAlpha = 0.14f,
        cloudCount = 5,
        ridges = listOf(Color(0xFF071624), Color(0xFF040E17), Color(0xFF02080E)),
        ridgeRim = Color(0xFF7FD9F2),
        rainCount = 42,
        snowCount = 0,
        fog = false,
        flash = false,
    )

    SkyCondition.STORM -> BackdropPalette(
        sky = listOf(Color(0xFF01040A), Color(0xFF050C15), Color(0xFF0D1B27), Color(0xFF03080D), Color(0xFF010407)),
        veil = listOf(0.38f, 0.16f, 0.16f, 0.86f),
        glow = Color(0xFF4E6A82),
        glowAlpha = 0.09f,
        glowY = 0.58f,
        starAlpha = 0f,
        celestial = Celestial.NONE,
        cloudColor = Color(0xFF5E6E7C),
        cloudAlpha = 0.20f,
        cloudCount = 6,
        ridges = listOf(Color(0xFF050F18), Color(0xFF030A11), Color(0xFF01050A)),
        ridgeRim = Color(0xFF7FD9F2),
        rainCount = 50,
        snowCount = 0,
        fog = false,
        flash = true,
    )

    SkyCondition.SNOW -> BackdropPalette(
        sky = listOf(Color(0xFF06101C), Color(0xFF0B2136), Color(0xFF13344A), Color(0xFF071522), Color(0xFF040C13)),
        veil = listOf(0.36f, 0.14f, 0.14f, 0.84f),
        glow = Color(0xFF8EC9DE),
        glowAlpha = 0.13f,
        glowY = 0.56f,
        starAlpha = 0.10f,
        celestial = Celestial.MOON,
        cloudColor = Color(0xFFC5DAE6),
        cloudAlpha = 0.12f,
        cloudCount = 4,
        ridges = listOf(Color(0xFF0C2436), Color(0xFF071926), Color(0xFF040F18)),
        ridgeRim = Color(0xFF7FD9F2),
        rainCount = 0,
        snowCount = 38,
        fog = false,
        flash = false,
    )

    SkyCondition.FOG -> BackdropPalette(
        sky = listOf(Color(0xFF040B12), Color(0xFF0A1A24), Color(0xFF142B37), Color(0xFF08131C), Color(0xFF03090E)),
        veil = listOf(0.36f, 0.14f, 0.14f, 0.84f),
        glow = Color(0xFF7E9FAD),
        glowAlpha = 0.12f,
        glowY = 0.58f,
        starAlpha = 0.05f,
        celestial = Celestial.MOON,
        cloudColor = Color(0xFFB6CBD6),
        cloudAlpha = 0.10f,
        cloudCount = 3,
        ridges = listOf(Color(0xFF0A1B26), Color(0xFF061119), Color(0xFF030A0F)),
        ridgeRim = Color(0xFF7FD9F2),
        rainCount = 0,
        snowCount = 0,
        fog = true,
        flash = false,
    )
}

/**
 * Sunrise and sunset: a restrained amber-to-blue sky with a warm horizon band.
 * Precipitation still shows through, so a rainy dawn is not painted as a clear
 * one.
 */
internal fun horizonPalette(condition: SkyCondition, dawn: Boolean): BackdropPalette {
    val base = if (dawn) {
        BackdropPalette(
            sky = listOf(Color(0xFF22395A), Color(0xFF2E4A6B), Color(0xFF4A5F7C), Color(0xFF6E665F), Color(0xFF2A3038)),
            veil = listOf(0.24f, 0.24f, 0.22f, 0.84f),
            glow = Color(0xFFFFC48A),
            glowAlpha = 0.20f,
            glowY = 0.60f,
            starAlpha = 0.05f,
            celestial = Celestial.NONE,
            cloudColor = Color(0xFFF6DCC4),
            cloudAlpha = 0.18f,
            cloudCount = 4,
            ridges = listOf(Color(0xFF3C4A5C), Color(0xFF2C3947), Color(0xFF1C2735)),
            ridgeRim = Color(0xFFFFDCAF),
            rainCount = 0,
            snowCount = 0,
            fog = false,
            flash = false,
            horizon = Color(0xFFFFC48A),
            horizonAlpha = 0.26f,
            horizonY = 0.66f,
        )
    } else {
        BackdropPalette(
            sky = listOf(Color(0xFF1A2B45), Color(0xFF243B58), Color(0xFF3E4A63), Color(0xFF6B5148), Color(0xFF2A2833)),
            veil = listOf(0.24f, 0.24f, 0.22f, 0.86f),
            glow = Color(0xFFFF9E5C),
            glowAlpha = 0.30f,
            glowY = 0.62f,
            starAlpha = 0.10f,
            celestial = Celestial.NONE,
            cloudColor = Color(0xFFE8C9B4),
            cloudAlpha = 0.16f,
            cloudCount = 4,
            ridges = listOf(Color(0xFF343E4E), Color(0xFF26303D), Color(0xFF18202A)),
            ridgeRim = Color(0xFFFFC79B),
            rainCount = 0,
            snowCount = 0,
            fog = false,
            flash = false,
            horizon = Color(0xFFFF9E5C),
            horizonAlpha = 0.30f,
            horizonY = 0.68f,
        )
    }

    return when (condition) {
        SkyCondition.CLEAR, SkyCondition.CLOUDY -> base
        SkyCondition.OVERCAST -> base.copy(
            cloudAlpha = 0.26f,
            cloudCount = 6,
            horizonAlpha = base.horizonAlpha * 0.6f,
            glowAlpha = base.glowAlpha * 0.6f,
        )
        SkyCondition.RAIN -> base.copy(
            cloudColor = Color(0xFFC9D4DD),
            cloudAlpha = 0.24f,
            cloudCount = 6,
            rainCount = 46,
            horizonAlpha = base.horizonAlpha * 0.5f,
        )
        SkyCondition.STORM -> base.copy(
            cloudColor = Color(0xFFA9B7C2),
            cloudAlpha = 0.28f,
            cloudCount = 7,
            rainCount = 56,
            flash = true,
            horizonAlpha = base.horizonAlpha * 0.4f,
        )
        SkyCondition.SNOW -> base.copy(
            cloudColor = Color(0xFFEAF2F7),
            cloudAlpha = 0.22f,
            cloudCount = 5,
            snowCount = 34,
            horizonAlpha = base.horizonAlpha * 0.6f,
        )
        SkyCondition.FOG -> base.copy(fog = true, cloudAlpha = 0.20f, horizonAlpha = base.horizonAlpha * 0.7f)
    }
}

/** Blends two palettes so a data arrival or city change never snaps. */
internal fun lerpPalette(from: BackdropPalette, to: BackdropPalette, t: Float): BackdropPalette {
    val clamped = t.coerceIn(0f, 1f)
    val structural = if (clamped < 0.5f) from else to
    return BackdropPalette(
        sky = List(SKY_STOPS.size) { index -> lerp(from.sky[index], to.sky[index], clamped) },
        veil = List(VEIL_STOPS.size) { index -> from.veil[index] + (to.veil[index] - from.veil[index]) * clamped },
        glow = lerp(from.glow, to.glow, clamped),
        glowAlpha = from.glowAlpha + (to.glowAlpha - from.glowAlpha) * clamped,
        glowY = from.glowY + (to.glowY - from.glowY) * clamped,
        starAlpha = from.starAlpha + (to.starAlpha - from.starAlpha) * clamped,
        celestial = structural.celestial,
        sunInner = lerp(from.sunInner, to.sunInner, clamped),
        sunOuter = lerp(from.sunOuter, to.sunOuter, clamped),
        cloudColor = lerp(from.cloudColor, to.cloudColor, clamped),
        cloudAlpha = from.cloudAlpha + (to.cloudAlpha - from.cloudAlpha) * clamped,
        cloudCount = structural.cloudCount,
        ridges = List(3) { index -> lerp(from.ridges[index], to.ridges[index], clamped) },
        ridgeRim = lerp(from.ridgeRim, to.ridgeRim, clamped),
        rainCount = structural.rainCount,
        snowCount = structural.snowCount,
        fog = structural.fog,
        flash = structural.flash,
        horizon = to.horizon ?: from.horizon,
        horizonAlpha = from.horizonAlpha + (to.horizonAlpha - from.horizonAlpha) * clamped,
        horizonY = from.horizonY + (to.horizonY - from.horizonY) * clamped,
    )
}


