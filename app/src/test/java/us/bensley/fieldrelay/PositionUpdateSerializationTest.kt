package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.model.PositionUpdate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PositionUpdateSerializationTest {
    @Test
    fun serializesApiFieldNames() {
        val body = PositionUpdate(
            id = "abc123",
            reportAt = "2026-06-20 12:34:56",
            lat = 33.669,
            lon = -96.926,
            elev = 210.0,
            mph = 55.5,
            dir = 180.0,
            active = 1,
            gps = 1,
        )

        val json = Json.encodeToString(PositionUpdate.serializer(), body)

        assertEquals(
            """{"id":"abc123","report_at":"2026-06-20 12:34:56","lat":33.669,"lon":-96.926,"elev":210.0,"mph":55.5,"dir":180.0,"active":1,"gps":1}""",
            json,
        )
    }
}
