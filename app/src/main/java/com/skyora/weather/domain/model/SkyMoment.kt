package com.skyora.weather.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The time-of-day axis of the background treatment.
 *
 * [DAWN] and [DUSK] are narrow windows around sunrise/sunset, not "the whole
 * afternoon" or "the whole night": an 17:00 slot with a 18:45 sunset stays
 * [DAY] so the sky never looks like sunset for hours.
 */
enum class SkyPhase {
    NIGHT,
    DAWN,
    DAY,
    DUSK,
    ;

    val isNight: Boolean get() = this == NIGHT
}

/** The weather axis of the background treatment, independent of [SkyPhase]. */
enum class SkyCondition {
    CLEAR,
    CLOUDY,
    OVERCAST,
    RAIN,
    STORM,
    SNOW,
    FOG,
}

/**
 * A resolved background "moment": what the sky should look like for one
 * location at one instant.
 *
 * This is deliberately separate from the icon mapping in [SkyIcons]. The two
 * share the same sunrise/sunset lookup, so they cannot disagree about whether
 * it is day, but the background additionally needs dawn/dusk, a weather family
 * and a fallback that is not "night by default".
 */
data class SkyMoment(
    val phase: SkyPhase,
    val condition: SkyCondition,
) {
    val isNight: Boolean get() = phase.isNight

    companion object {
        /** Used before any forecast has loaded: neutral, daylight-leaning. */
        val Fallback = SkyMoment(phase = SkyPhase.DAY, condition = SkyCondition.CLOUDY)
    }
}

/** Maps a weather condition onto the background's weather axis. */
fun WeatherCondition.skyCondition(): SkyCondition = when (this) {
    WeatherCondition.CLEAR,
    WeatherCondition.MAINLY_CLEAR,
    -> SkyCondition.CLEAR

    WeatherCondition.PARTLY_CLOUDY,
    WeatherCondition.UNKNOWN,
    -> SkyCondition.CLOUDY

    WeatherCondition.OVERCAST -> SkyCondition.OVERCAST

    WeatherCondition.FOG -> SkyCondition.FOG

    WeatherCondition.DRIZZLE,
    WeatherCondition.FREEZING_DRIZZLE,
    WeatherCondition.RAIN,
    WeatherCondition.FREEZING_RAIN,
    WeatherCondition.RAIN_SHOWERS,
    -> SkyCondition.RAIN

    WeatherCondition.SNOW,
    WeatherCondition.SNOW_GRAINS,
    WeatherCondition.SNOW_SHOWERS,
    -> SkyCondition.SNOW

    WeatherCondition.THUNDERSTORM,
    WeatherCondition.THUNDERSTORM_HAIL,
    -> SkyCondition.STORM
}

/**
 * Resolves [SkyMoment]s from the location's own time and sun data.
 *
 * Rules:
 *  - the instant + timezone decide the phase, so a phone in another timezone
 *    cannot make a city look like night;
 *  - sunrise/sunset windows are read from the same [SunTimes] map the icons use;
 *  - missing sun data falls back to 06:00-18:00 local (via [SkyIcons.isDaylight])
 *    and simply has no dawn/dusk window;
 *  - nothing here invents weather: the condition comes from the snapshot.
 */
object SkyMoments {

    /** Warm horizon starts before sunrise and fades shortly after it. */
    const val DAWN_LEAD_MS: Long = 40 * 60_000L
    const val DAWN_TAIL_MS: Long = 30 * 60_000L

    /** Sunset gets a slightly longer run-up, matching how the sky looks. */
    const val DUSK_LEAD_MS: Long = 50 * 60_000L
    const val DUSK_TAIL_MS: Long = 25 * 60_000L

    fun resolve(
        condition: WeatherCondition,
        instantMillis: Long,
        zone: ZoneId,
        sunTimes: Map<LocalDate, SunTimes>,
    ): SkyMoment = SkyMoment(
        phase = phaseAt(instantMillis, zone, sunTimes),
        condition = condition.skyCondition(),
    )

    fun resolve(
        snapshot: WeatherSnapshot,
        instantMillis: Long = System.currentTimeMillis(),
    ): SkyMoment = resolve(
        condition = snapshot.currentCondition(),
        instantMillis = instantMillis,
        zone = snapshot.zoneId(),
        sunTimes = snapshot.sunTimes(),
    )

    /**
     * Phase for [instantMillis].
     *
     * The sun windows are checked first so that the 25 minutes after sunset are
     * still [DUSK] even though [SkyIcons.isDaylight] already reports night.
     */
    fun phaseAt(
        instantMillis: Long,
        zone: ZoneId,
        sunTimes: Map<LocalDate, SunTimes>,
    ): SkyPhase {
        val localDate = Instant.ofEpochMilli(instantMillis).atZone(zone).toLocalDate()

        // Polar windows can belong to the neighbouring calendar day, so a
        // narrow search is used instead of only "today".
        for (date in listOf(localDate.minusDays(1), localDate, localDate.plusDays(1))) {
            val sun = sunTimes[date] ?: continue
            val sunrise = sun.sunriseEpochMillis
            val sunset = sun.sunsetEpochMillis
            if (sunrise != null && instantMillis in (sunrise - DAWN_LEAD_MS)..(sunrise + DAWN_TAIL_MS)) {
                return SkyPhase.DAWN
            }
            if (sunset != null && instantMillis in (sunset - DUSK_LEAD_MS)..(sunset + DUSK_TAIL_MS)) {
                return SkyPhase.DUSK
            }
        }

        return if (SkyIcons.isDaylight(instantMillis, zone, sunTimes)) {
            SkyPhase.DAY
        } else {
            SkyPhase.NIGHT
        }
    }

    /**
     * Phase for a surface with no forecast at all (onboarding, settings,
     * permission screens). Uses the device clock, which is the only honest
     * signal available, instead of assuming night.
     */
    fun phaseForLocalClock(
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): SkyPhase {
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zone).hour
        return if (hour in 6 until 18) SkyPhase.DAY else SkyPhase.NIGHT
    }
}
