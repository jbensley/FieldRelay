package us.bensley.fieldrelay

import us.bensley.fieldrelay.location.LocationFix
import us.bensley.fieldrelay.location.buildPositionUpdate
import org.junit.Assert.assertEquals
import org.junit.Test

class PositionUpdateFactoryTest {
    @Test
    fun buildsPositionUpdateFromFix() {
        val update = buildPositionUpdate(
            appId = "app-id",
            reportAtUtc = "2026-06-20 12:34:56",
            fix = LocationFix(
                lat = 33.669,
                lon = -96.926,
                elev = 210.0,
                metersPerSecond = 10.0,
                bearing = 180.0,
            ),
        )

        assertEquals("app-id", update.id)
        assertEquals("2026-06-20 12:34:56", update.reportAt)
        assertEquals(33.669, update.lat, 0.0)
        assertEquals(-96.926, update.lon, 0.0)
        assertEquals(210.0, update.elev, 0.0)
        assertEquals(22.3694, update.mph, 0.0001)
        assertEquals(180.0, update.dir, 0.0)
        assertEquals(1, update.active)
        assertEquals(1, update.gps)
    }
}
