package us.bensley.fieldrelay

import us.bensley.fieldrelay.location.formatAprsLatitude
import us.bensley.fieldrelay.location.formatAprsLongitude
import us.bensley.fieldrelay.location.formatDecimalCoordinate
import us.bensley.fieldrelay.location.formatDmsLatitude
import us.bensley.fieldrelay.location.formatDmsLongitude
import us.bensley.fieldrelay.location.formatMaidenheadGrid
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateFormatsTest {
    @Test
    fun formatsDecimalCoordinatesWithSevenDecimals() {
        assertEquals("33.6691234", formatDecimalCoordinate(33.6691234))
        assertEquals("-96.9264940", formatDecimalCoordinate(-96.926494))
        assertEquals("33.6690000", formatDecimalCoordinate(33.669000))
    }

    @Test
    fun formatsAprsCoordinates() {
        assertEquals("3340.14N", formatAprsLatitude(33.669))
        assertEquals("09655.56W", formatAprsLongitude(-96.926))
    }

    @Test
    fun rollsAprsMinutesAtSixty() {
        assertEquals("3400.00N", formatAprsLatitude(33.999999))
        assertEquals("09700.00W", formatAprsLongitude(-96.999999))
    }

    @Test
    fun formatsDmsCoordinates() {
        assertEquals("33\u00B040'08\"N", formatDmsLatitude(33.669))
        assertEquals("096\u00B055'34\"W", formatDmsLongitude(-96.926))
    }

    @Test
    fun formatsMaidenheadGridSquare() {
        assertEquals("EM13MQ", formatMaidenheadGrid(33.669, -96.926))
        assertEquals("EM13", formatMaidenheadGrid(33.669, -96.926, precision = 4))
    }
}
