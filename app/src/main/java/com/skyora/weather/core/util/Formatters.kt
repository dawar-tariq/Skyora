package com.skyora.weather.core.util

import com.skyora.weather.domain.model.TemperatureUnit
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val PLACEHOLDER = "—"

/** Converts a Celsius value into the user's selected unit. */
fun TemperatureUnit.convertFromCelsius(celsius: Double): Double = when (this) {
    TemperatureUnit.CELSIUS -> celsius
    TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
}

/** Rounded display value without any unit suffix. */
fun TemperatureUnit.displayValue(celsius: Double): Int =
    convertFromCelsius(celsius).roundToInt().let { if (it == 0) 0 else it }

/** "12°" / "54°" */
fun formatTemperature(celsius: Double?, unit: TemperatureUnit): String {
    if (celsius == null || !celsius.isFinite()) return PLACEHOLDER
    return "${unit.displayValue(celsius)}°"
}

/** Short spoken form, used in accessibility labels. */
fun spokenTemperature(celsius: Double?, unit: TemperatureUnit): String {
    if (celsius == null || !celsius.isFinite()) return PLACEHOLDER
    return "${unit.displayValue(celsius)} degrees ${if (unit == TemperatureUnit.CELSIUS) "Celsius" else "Fahrenheit"}"
}

fun formatTemperatureWithUnit(celsius: Double?, unit: TemperatureUnit): String {
    if (celsius == null || !celsius.isFinite()) return PLACEHOLDER
    return "${unit.displayValue(celsius)}${unit.symbol}"
}

fun formatWindSpeed(kmh: Double?, unit: TemperatureUnit): String {
    if (kmh == null || !kmh.isFinite()) return PLACEHOLDER
    val value = when (unit) {
        TemperatureUnit.CELSIUS -> kmh
        TemperatureUnit.FAHRENHEIT -> kmh * 0.621371
    }.roundToInt()
    return "$value ${unit.windUnit}"
}

fun formatWindValue(kmh: Double?, unit: TemperatureUnit): String {
    if (kmh == null || !kmh.isFinite()) return PLACEHOLDER
    val value = when (unit) {
        TemperatureUnit.CELSIUS -> kmh
        TemperatureUnit.FAHRENHEIT -> kmh * 0.621371
    }.roundToInt()
    return value.toString()
}

private val compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

fun windDirectionLabel(degrees: Int?): String? {
    if (degrees == null) return null
    val normalized = ((degrees % 360) + 360) % 360
    return compassPoints[((normalized + 22.5) / 45.0).toInt() % 8]
}

fun formatPercentage(value: Int?): String = value?.let { "$it%" } ?: PLACEHOLDER

fun formatChance(value: Int?): String? = value?.let { "$it%" }

fun formatPressure(hpa: Double?): String {
    if (hpa == null || !hpa.isFinite()) return PLACEHOLDER
    return "${hpa.roundToInt()} hPa"
}

fun formatUvIndex(uv: Double?): String {
    if (uv == null || !uv.isFinite()) return PLACEHOLDER
    return String.format(Locale.getDefault(), "%.1f", uv)
}

enum class UvCategory { LOW, MODERATE, HIGH, VERY_HIGH, EXTREME }

fun uvCategory(uv: Double?): UvCategory? {
    if (uv == null || !uv.isFinite()) return null
    return when {
        uv < 3 -> UvCategory.LOW
        uv < 6 -> UvCategory.MODERATE
        uv < 8 -> UvCategory.HIGH
        uv < 11 -> UvCategory.VERY_HIGH
        else -> UvCategory.EXTREME
    }
}

fun formatVisibility(meters: Double?, unit: TemperatureUnit): String {
    if (meters == null || !meters.isFinite() || meters < 0) return PLACEHOLDER
    val km = meters / 1000.0
    return when (unit) {
        TemperatureUnit.CELSIUS -> "${km.roundToInt()} km"
        TemperatureUnit.FAHRENHEIT -> "${(km * 0.621371).roundToInt()} mi"
    }
}

fun formatPrecipitation(mm: Double?): String {
    if (mm == null || !mm.isFinite()) return PLACEHOLDER
    return if (mm < 0.05) "0 mm" else String.format(Locale.getDefault(), "%.1f mm", mm)
}

fun resolveZoneId(timezone: String?, utcOffsetSeconds: Int): ZoneId =
    timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
        ?: ZoneOffset.ofTotalSeconds(utcOffsetSeconds)

fun formatHour(instant: Instant, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("h a", locale).format(instant.atZone(zone))

fun formatTimeOfDay(instant: Instant, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("h:mm a", locale).format(instant.atZone(zone))

fun formatTimeOfDay(localDateTime: LocalDateTime, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("h:mm a", locale).format(localDateTime)

fun formatDateShort(instant: Instant, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("EEE, MMM d", locale).format(instant.atZone(zone))

fun formatDateHeader(instant: Instant, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("EEE, MMM d", locale)
        .format(instant.atZone(zone))
        .uppercase(locale)

fun formatDateHeader(localDateTime: LocalDateTime, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("EEE, MMM d", locale)
        .format(localDateTime)
        .uppercase(locale)

fun formatDateTime(instant: Instant, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMM d, h:mm a", locale).format(instant.atZone(zone))

fun formatDateTime(localDateTime: LocalDateTime, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMM d, h:mm a", locale).format(localDateTime)

fun formatShortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMM d", locale).format(date)

fun formatWeekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

fun formatDaylight(sunrise: Instant?, sunset: Instant?): String? {
    if (sunrise == null || sunset == null || sunset.isBefore(sunrise)) return null
    val duration = Duration.between(sunrise, sunset)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun sameLocalHour(first: Instant, second: Instant, zone: ZoneId): Boolean {
    val a = first.atZone(zone).truncatedTo(java.time.temporal.ChronoUnit.HOURS)
    val b = second.atZone(zone).truncatedTo(java.time.temporal.ChronoUnit.HOURS)
    return a == b
}

fun isToday(date: LocalDate, zone: ZoneId): Boolean = date == LocalDate.now(zone)

/** Human age of the cached forecast. */
sealed interface UpdateAge {
    data object JustNow : UpdateAge
    data class Minutes(val minutes: Int) : UpdateAge
    data class Hours(val hours: Int) : UpdateAge
    data object Older : UpdateAge
}

fun updateAge(updatedAtMillis: Long, nowMillis: Long): UpdateAge {
    val diff = (nowMillis - updatedAtMillis).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> UpdateAge.JustNow
        diff < 3_600_000L -> UpdateAge.Minutes((diff / 60_000L).toInt())
        diff < 86_400_000L -> UpdateAge.Hours((diff / 3_600_000L).toInt())
        else -> UpdateAge.Older
    }
}
