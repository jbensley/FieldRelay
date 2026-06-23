package us.bensley.fieldrelay.location

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

private const val MAIDENHEAD_MAX_LAT = 90.0
private const val MAIDENHEAD_MAX_LON = 180.0

fun formatDecimalCoordinate(value: Double): String =
    String.format(Locale.US, "%.7f", value)

fun formatAprsLatitude(latitude: Double): String {
    require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
    val hemisphere = if (latitude >= 0.0) 'N' else 'S'
    val parts = degreesAndRoundedMinutes(latitude)
    return String.format(Locale.US, "%02d%05.2f%c", parts.degrees, parts.minutes, hemisphere)
}

fun formatAprsLongitude(longitude: Double): String {
    require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
    val hemisphere = if (longitude >= 0.0) 'E' else 'W'
    val parts = degreesAndRoundedMinutes(longitude)
    return String.format(Locale.US, "%03d%05.2f%c", parts.degrees, parts.minutes, hemisphere)
}

fun formatDmsLatitude(latitude: Double): String {
    require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
    val hemisphere = if (latitude >= 0.0) 'N' else 'S'
    return "${formatDms(abs(latitude), 2)}$hemisphere"
}

fun formatDmsLongitude(longitude: Double): String {
    require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
    val hemisphere = if (longitude >= 0.0) 'E' else 'W'
    return "${formatDms(abs(longitude), 3)}$hemisphere"
}

fun formatMaidenheadGrid(latitude: Double, longitude: Double, precision: Int = 6): String {
    require(latitude > -MAIDENHEAD_MAX_LAT && latitude < MAIDENHEAD_MAX_LAT) {
        "Latitude must be greater than -90 and less than 90."
    }
    require(longitude >= -MAIDENHEAD_MAX_LON && longitude <= MAIDENHEAD_MAX_LON) {
        "Longitude must be between -180 and 180."
    }
    require(precision == 4 || precision == 6) { "Only 4- and 6-character grid locators are supported." }

    val normalizedLon = if (longitude == MAIDENHEAD_MAX_LON) {
        360.0 - 1e-12
    } else {
        longitude + 180.0
    }
    val normalizedLat = latitude + 90.0

    val fieldLon = floor(normalizedLon / 20.0).toInt()
    val fieldLat = floor(normalizedLat / 10.0).toInt()
    val squareLon = floor((normalizedLon % 20.0) / 2.0).toInt()
    val squareLat = floor(normalizedLat % 10.0).toInt()

    val grid = StringBuilder()
        .append(('A'.code + fieldLon).toChar())
        .append(('A'.code + fieldLat).toChar())
        .append(squareLon)
        .append(squareLat)

    if (precision == 6) {
        val subsquareLon = floor(((normalizedLon % 2.0) / (2.0 / 24.0))).toInt().coerceIn(0, 23)
        val subsquareLat = floor(((normalizedLat % 1.0) / (1.0 / 24.0))).toInt().coerceIn(0, 23)
        grid
            .append(('A'.code + subsquareLon).toChar())
            .append(('A'.code + subsquareLat).toChar())
    }

    return grid.toString()
}

private data class DegreesMinutes(val degrees: Int, val minutes: Double)

private fun degreesAndRoundedMinutes(value: Double): DegreesMinutes {
    val absolute = abs(value)
    var degrees = floor(absolute).toInt()
    var minutes = round((absolute - degrees) * 60.0 * 100.0) / 100.0
    if (minutes >= 60.0) {
        degrees += 1
        minutes = 0.0
    }
    return DegreesMinutes(degrees, minutes)
}

private fun formatDms(absoluteDegrees: Double, degreeWidth: Int): String {
    var degrees = floor(absoluteDegrees).toInt()
    val minuteValue = (absoluteDegrees - degrees) * 60.0
    var minutes = floor(minuteValue).toInt()
    var seconds = round((minuteValue - minutes) * 60.0).toInt()

    if (seconds == 60) {
        seconds = 0
        minutes += 1
    }
    if (minutes == 60) {
        minutes = 0
        degrees += 1
    }

    return String.format(Locale.US, "%0${degreeWidth}d\u00B0%02d'%02d\"", degrees, minutes, seconds)
}
