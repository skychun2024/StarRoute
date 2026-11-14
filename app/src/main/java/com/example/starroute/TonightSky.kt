package com.example.starroute

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.*

internal val KoreaZone: ZoneId = ZoneId.of("Asia/Seoul")

internal data class GuideStar(val name: String, val ra: Double, val dec: Double)
internal data class SkyPosition(val altitude: Double, val azimuth: Double)
internal data class ViewingWindow(
    val start: Instant, val end: Instant, val best: Instant, val position: SkyPosition
)
internal data class TonightRecommendation(val star: StarInfo, val guide: GuideStar, val window: ViewingWindow)

private fun radians(degrees: Double) = degrees * PI / 180.0
private fun degrees(radians: Double) = radians * 180.0 / PI
private fun normalize(angle: Double) = ((angle % 360.0) + 360.0) % 360.0
private fun daysSinceJ2000(time: Instant) = (time.epochSecond - 946728000L) / 86400.0

internal fun horizontalPosition(
    latitude: Double, longitude: Double, time: Instant, ra: Double, dec: Double
): SkyPosition {
    val sidereal = 280.46061837 + 360.98564736629 * daysSinceJ2000(time)
    val hourAngle = radians(normalize(sidereal + longitude - ra))
    val lat = radians(latitude)
    val declination = radians(dec)
    val east = -cos(declination) * sin(hourAngle)
    val north = sin(declination) * cos(lat) - cos(declination) * cos(hourAngle) * sin(lat)
    val up = sin(declination) * sin(lat) + cos(declination) * cos(hourAngle) * cos(lat)
    return SkyPosition(degrees(asin(up.coerceIn(-1.0, 1.0))), normalize(degrees(atan2(east, north))))
}

private fun sunAltitude(latitude: Double, longitude: Double, time: Instant): Double {
    // US Naval Observatory approximate solar coordinates.
    val days = daysSinceJ2000(time)
    val anomaly = radians(normalize(357.529 + 0.98560028 * days))
    val longitudeOnEcliptic = radians(normalize(
        280.459 + 0.98564736 * days + 1.915 * sin(anomaly) + 0.020 * sin(2 * anomaly)
    ))
    val tilt = radians(23.439 - 0.00000036 * days)
    val ra = degrees(atan2(cos(tilt) * sin(longitudeOnEcliptic), cos(longitudeOnEcliptic)))
    val dec = degrees(asin(sin(tilt) * sin(longitudeOnEcliptic)))
    return horizontalPosition(latitude, longitude, time, ra, dec).altitude
}

internal fun tonightRecommendations(
    latitude: Double, longitude: Double, now: Instant
): List<TonightRecommendation> {
    val local = now.atZone(KoreaZone)
    val date = if (local.hour < 6) local.toLocalDate().minusDays(1) else local.toLocalDate()
    val evening = date.atTime(12, 0).atZone(KoreaZone).toInstant()
    val morning = date.plusDays(1).atTime(LocalTime.NOON).atZone(KoreaZone).toInstant()
    val samples = generateSequence(maxOf(now, evening)) { it.plusSeconds(600) }
        .takeWhile { it < morning }.toList()
    val darkness = samples.map { sunAltitude(latitude, longitude, it) <= -18.0 }

    return starCatalog.mapNotNull { star ->
        val guide = guideStars.getValue(star.id)
        var current: ViewingWindow? = null
        val windows = mutableListOf<ViewingWindow>()
        for ((index, time) in samples.withIndex()) {
            val position = horizontalPosition(latitude, longitude, time, guide.ra, guide.dec)
            if (darkness[index] && position.altitude >= 20.0) {
                val previous = current
                current = when {
                    previous == null -> ViewingWindow(time, time, time, position)
                    position.altitude > previous.position.altitude ->
                        previous.copy(end = time, best = time, position = position)
                    else -> previous.copy(end = time)
                }
            } else {
                current?.let(windows::add)
                current = null
            }
        }
        current?.let(windows::add)
        windows.maxByOrNull { it.position.altitude }?.let { TonightRecommendation(star, guide, it) }
    }.sortedByDescending { it.window.position.altitude }
}

internal fun compassDirection(azimuth: Double): String {
    val directions = listOf("북", "북동", "동", "남동", "남", "남서", "서", "북서")
    return directions[((normalize(azimuth) + 22.5) / 45).toInt() % directions.size]
}
