package us.bensley.fieldrelay

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import us.bensley.fieldrelay.data.model.OverlandUpdate
import us.bensley.fieldrelay.location.LocationFix
import us.bensley.fieldrelay.location.buildOverlandUpdate

class OverlandUpdateFactoryTest {
    @Test
    fun buildsGeoJsonLocationFeature() {
        val update = buildOverlandUpdate(
            fix = LocationFix(
                lat = 33.669,
                lon = -96.926,
                elev = 210.0,
                metersPerSecond = 13.4,
                bearing = 181.0,
            ),
            reportAtUtc = "2026-06-29 12:34:56",
            deviceId = "field-unit-1",
        )

        val feature = update.locations.single()
        assertEquals("Feature", feature.type)
        assertEquals("Point", feature.geometry.type)
        assertEquals(listOf(-96.926, 33.669), feature.geometry.coordinates)
        assertEquals("2026-06-29T12:34:56Z", feature.properties.timestamp)
        assertEquals("field-unit-1", feature.properties.deviceId)
    }

    @Test
    fun serializesOverlandFieldNames() {
        val update = buildOverlandUpdate(
            fix = LocationFix(
                lat = 33.669,
                lon = -96.926,
                elev = 210.0,
                metersPerSecond = 13.4,
                bearing = 181.0,
            ),
            reportAtUtc = "2026-06-29 12:34:56",
            deviceId = "field-unit-1",
        )

        val json = Json.encodeToString<OverlandUpdate>(update)

        assert(json.contains("\"locations\""))
        assert(json.contains("\"coordinates\":[-96.926,33.669]"))
        assert(json.contains("\"device_id\":\"field-unit-1\""))
    }
}
