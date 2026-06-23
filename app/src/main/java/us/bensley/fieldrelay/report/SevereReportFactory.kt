package us.bensley.fieldrelay.report

import us.bensley.fieldrelay.data.model.SevereReportRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.PI

fun buildSevereReportRequest(
    appId: String,
    draft: SevereReportDraft,
    latitude: Double,
    longitude: Double,
    now: Date = Date(),
): SevereReportRequest {
    val reportLocation = if (draft.offsetEnabled) {
        applyOffset(latitude, longitude, draft.offsetDistanceMiles, draft.offsetDirection)
    } else {
        ReportLocation(latitude = latitude, longitude = longitude)
    }
    return SevereReportRequest(
        id = appId,
        reportType = "S",
        stamp = utcTimestamp(now),
        tornado = draft.tornado.toInt(),
        funnelcloud = draft.funnelCloud.toInt(),
        wallcloud = draft.wallCloud.toInt(),
        rotation = draft.rotation.toInt(),
        hail = draft.hail.toInt(),
        wind = draft.wind.toInt(),
        flood = draft.flood.toInt(),
        flashflood = draft.flashFlood.toInt(),
        other = draft.other.toInt(),
        hailsize = if (draft.hail) draft.hailSizeInches ?: 0.0 else 0.0,
        windspeed = if (draft.wind) draft.windSpeedMph ?: 0 else 0,
        windmeasure = if (draft.wind) 1 else 0,
        stampExact = 1,
        damage = draft.damage.toInt(),
        injury = draft.injury.toInt(),
        narrative = buildNarrative(draft),
        lat = reportLocation.latitude,
        lon = reportLocation.longitude,
        gps = 1,
        nwschat = 0,
        twitter = 0,
    )
}

data class ReportLocation(val latitude: Double, val longitude: Double)

fun applyOffset(
    latitude: Double,
    longitude: Double,
    distanceMiles: Double?,
    direction: ReportDirection?,
): ReportLocation {
    if (distanceMiles == null || distanceMiles <= 0.0 || direction == null) {
        return ReportLocation(latitude = latitude, longitude = longitude)
    }
    val northMiles = distanceMiles * direction.northComponent
    val eastMiles = distanceMiles * direction.eastComponent
    val deltaLat = northMiles / MILES_PER_DEGREE_LATITUDE
    val milesPerDegreeLongitude = MILES_PER_DEGREE_LATITUDE * cos(latitude * PI / 180.0)
    val deltaLon = if (milesPerDegreeLongitude == 0.0) 0.0 else eastMiles / milesPerDegreeLongitude
    return ReportLocation(
        latitude = latitude + deltaLat,
        longitude = longitude + deltaLon,
    )
}

private fun buildNarrative(draft: SevereReportDraft): String {
    val parts = mutableListOf<String>()
    val distance = draft.offsetDistanceMiles
    val direction = draft.offsetDirection
    if (draft.offsetEnabled && distance != null && direction != null && distance > 0.0) {
        parts += "Report location offset approximately ${formatDistance(distance)} ${direction.label} of reporter location."
    }
    if (draft.floodDepthInches != null && (draft.flood || draft.flashFlood)) {
        parts += "Water depth approx ${draft.floodDepthInches} inches."
    }
    if (draft.damage) parts += "Notable storm damage observed."
    if (draft.injury) parts += "Injury reported."
    if (draft.other) parts += "Other severe impact observed."
    return parts.joinToString(" ")
}

private fun formatDistance(distance: Double): String =
    if (distance % 1.0 == 0.0) "${distance.toInt()} mi" else "$distance mi"

private fun utcTimestamp(date: Date): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(date)

private fun Boolean.toInt(): Int = if (this) 1 else 0

private const val MILES_PER_DEGREE_LATITUDE = 69.0
