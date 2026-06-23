package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.data.AprsToneMode
import us.bensley.fieldrelay.location.LocationFix
import us.bensley.fieldrelay.location.buildAprsIsLogin
import us.bensley.fieldrelay.location.buildAprsComment
import us.bensley.fieldrelay.location.buildAprsPositionPacket
import us.bensley.fieldrelay.location.formatAprsFrequency
import us.bensley.fieldrelay.location.formatAprsOffset
import us.bensley.fieldrelay.location.formatAprsTone
import us.bensley.fieldrelay.location.hasValidAprsConfiguration
import us.bensley.fieldrelay.location.isValidAprsOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AprsPacketTest {
    @Test
    fun buildsAprsIsLoginLine() {
        val settings = Settings.DEFAULT.copy(
            aprsCallsign = "n0abc-9",
            aprsPasscode = "16272",
        )

        assertEquals(
            "user N0ABC-9 pass 16272 vers FieldRelay 1.2.3",
            buildAprsIsLogin(settings, appVersion = "1.2.3"),
        )
    }

    @Test
    fun buildsUncompressedPositionPacket() {
        val packet = buildAprsPositionPacket(
            fix = LocationFix(
                lat = 33.6689069,
                lon = -96.9265723,
                elev = 210.0,
                metersPerSecond = 13.4112,
                bearing = 181.0,
            ),
            settings = Settings.DEFAULT.copy(
                aprsCallsign = "n0abc-9",
                aprsSymbolTable = "/",
                aprsSymbolCode = ">",
                aprsFrequencyMhz = "146.520",
                aprsComment = "FieldRelay Android",
            ),
        )

        assertEquals(
            "N0ABC-9>APFDLD,TCPIP*:!3340.13N/09655.59W>181/026/A=000689 146.520MHz FieldRelay Android",
            packet,
        )
    }

    @Test
    fun buildsFrequencyCommentBeforeUserComment() {
        val comment = buildAprsComment(
            Settings.DEFAULT.copy(
                aprsFrequencyMhz = "146.835",
                aprsToneMode = AprsToneMode.CTCSS,
                aprsTone = "107.2",
                aprsOffset = "-0.6",
                aprsRange = "R25m",
                aprsComment = "FieldRelay Android",
            ),
        )

        assertEquals("146.835MHz C107 -060 R25m FieldRelay Android".take(43), comment)
    }

    @Test
    fun formatsAprsToneField() {
        assertEquals(null, formatAprsTone(AprsToneMode.NONE, "107.2"))
        assertEquals("C107", formatAprsTone(AprsToneMode.CTCSS, "107.2"))
        assertEquals("C067", formatAprsTone(AprsToneMode.CTCSS, "67.0"))
        assertEquals("D023", formatAprsTone(AprsToneMode.DCS, "023"))
        assertEquals("D023", formatAprsTone(AprsToneMode.DCS, "23"))
        assertEquals(null, formatAprsTone(AprsToneMode.CTCSS, "108.0"))
        assertEquals(null, formatAprsTone(AprsToneMode.DCS, "999"))
    }

    @Test
    fun formatsAprsOffsetField() {
        assertEquals("+060", formatAprsOffset("0.6"))
        assertEquals("-060", formatAprsOffset("-0.6"))
        assertEquals("+500", formatAprsOffset("5.0"))
        assertEquals("-000", formatAprsOffset("0"))
        assertEquals(null, formatAprsOffset(""))
        assertEquals(null, formatAprsOffset("down"))
        assertEquals(null, formatAprsOffset("10.0"))
        assertTrue(isValidAprsOffset(""))
        assertTrue(isValidAprsOffset("-0.6"))
        assertFalse(isValidAprsOffset("down"))
    }

    @Test
    fun formatsAprsFrequencyField() {
        assertEquals("146.520MHz", formatAprsFrequency("146.52"))
        assertEquals("052.525MHz", formatAprsFrequency("52.525"))
        assertEquals(null, formatAprsFrequency(""))
        assertEquals(null, formatAprsFrequency("not-a-frequency"))
    }

    @Test
    fun validatesAprsConfiguration() {
        assertFalse(hasValidAprsConfiguration(Settings.DEFAULT))
        assertTrue(
            hasValidAprsConfiguration(
                Settings.DEFAULT.copy(
                    aprsEnabled = true,
                    aprsCallsign = "N0ABC-9",
                    aprsPasscode = "16272",
                    aprsServer = "rotate.aprs2.net",
                    aprsPort = 14580,
                ),
            ),
        )
    }
}
