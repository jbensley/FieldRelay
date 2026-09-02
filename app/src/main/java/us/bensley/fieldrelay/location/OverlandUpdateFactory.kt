package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.model.OverlandLocationFeature
import us.bensley.fieldrelay.data.model.OverlandLocationProperties
import us.bensley.fieldrelay.data.model.OverlandPointGeometry
import us.bensley.fieldrelay.data.model.OverlandUpdate

fun buildOverlandUpdate(
    fix: LocationFix,
    reportAtUtc: String,
    deviceId: String,
): OverlandUpdate = OverlandUpdate(
    locations = listOf(
        OverlandLocationFeature(
            geometry = OverlandPointGeometry(
                coordinates = listOf(fix.lon, fix.lat),
            ),
            properties = OverlandLocationProperties(
                timestamp = reportAtUtc.toOverlandTimestamp(),
                altitude = fix.elev,
                speed = fix.metersPerSecond,
                course = fix.bearing,
                deviceId = deviceId.ifBlank { "FieldRelay Android" },
            ),
        ),
    ),
)

private fun String.toOverlandTimestamp(): String =
    trim().replace(' ', 'T').let { value ->
        if (value.endsWith("Z")) value else "${value}Z"
    }
