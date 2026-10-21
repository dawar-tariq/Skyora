package com.skyora.weather.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyora.weather.domain.model.SkyCondition
import com.skyora.weather.domain.model.SkyMoment
import com.skyora.weather.domain.model.SkyMoments
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.model.sunTimes
import java.time.ZoneId

/**
 * The background moment for a screen that has (or is waiting for) a snapshot.
 *
 * While the first snapshot is still loading the phase comes from the device
 * clock and the condition is neutral, so the app never opens on a dark night
 * sky at midday — and never claims a clear sky it has not fetched.
 */
@Composable
fun rememberSkyMoment(
    snapshot: WeatherSnapshot?,
    nowMillis: Long,
    fallbackZone: ZoneId? = null,
): SkyMoment {
    val zone = remember(fallbackZone, snapshot?.timezone, snapshot?.utcOffsetSeconds) {
        snapshot?.zoneId() ?: fallbackZone ?: ZoneId.systemDefault()
    }
    return remember(snapshot, nowMillis, zone) {
        if (snapshot != null) {
            SkyMoments.resolve(
                condition = snapshot.currentCondition(),
                instantMillis = nowMillis,
                zone = snapshot.zoneId(),
                sunTimes = snapshot.sunTimes(),
            )
        } else {
            SkyMoment(
                phase = SkyMoments.phaseForLocalClock(nowMillis, zone),
                condition = SkyCondition.CLOUDY,
            )
        }
    }
}

/**
 * Moment for a surface with no forecast at all (splash, onboarding, settings,
 * permission, search). Follows the device clock instead of assuming night.
 */
@Composable
fun rememberAmbientSkyMoment(
    nowMillis: Long,
    condition: SkyCondition = SkyCondition.CLEAR,
    zone: ZoneId = ZoneId.systemDefault(),
): SkyMoment = remember(nowMillis, condition, zone) {
    SkyMoment(phase = SkyMoments.phaseForLocalClock(nowMillis, zone), condition = condition)
}
