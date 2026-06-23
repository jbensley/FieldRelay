package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.model.PositionUpdate

private const val METERS_PER_SECOND_TO_MPH = 2.23694

fun buildPositionUpdate(
    appId: String,
    reportAtUtc: String,
    fix: LocationFix,
): PositionUpdate = PositionUpdate(
    id = appId,
    reportAt = reportAtUtc,
    lat = fix.lat,
    lon = fix.lon,
    elev = fix.elev,
    mph = fix.metersPerSecond * METERS_PER_SECOND_TO_MPH,
    dir = fix.bearing,
    active = 1,
    gps = 1,
)
