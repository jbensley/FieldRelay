package us.bensley.fieldrelay

import us.bensley.fieldrelay.report.SevereReportDraft
import us.bensley.fieldrelay.report.ReportDirection
import us.bensley.fieldrelay.report.buildSevereReportRequest
import us.bensley.fieldrelay.report.validateSevereReportDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SevereReportTest {
    @Test
    fun buildsSevereWindRequest() {
        val request = buildSevereReportRequest(
            appId = "app-id",
            draft = SevereReportDraft(
                wind = true,
                windSpeedMph = 62,
                firstHandConfirmed = true,
            ),
            latitude = 33.669,
            longitude = -96.926,
            now = Date(1_782_000_000_000L),
        )

        assertEquals("app-id", request.id)
        assertEquals("S", request.reportType)
        assertEquals("2026-06-21 00:00:00", request.stamp)
        assertEquals(1, request.wind)
        assertEquals(62, request.windspeed)
        assertEquals(1, request.windmeasure)
        assertEquals(1, request.stampExact)
        assertEquals(33.669, request.lat, 0.0)
        assertEquals(-96.926, request.lon, 0.0)
        assertEquals("", request.narrative)
        assertEquals(0, request.nwschat)
        assertEquals(0, request.twitter)
    }

    @Test
    fun validatesSevereWindThreshold() {
        val errors = validateSevereReportDraft(
            draft = SevereReportDraft(
                wind = true,
                windSpeedMph = 50,
                firstHandConfirmed = true,
            ),
            appId = "app-id",
            hasLocation = true,
        )

        assertTrue(errors.any { it.contains("greater than 50 mph") })
    }

    @Test
    fun validatesFirstHandLocationAndPhenomenon() {
        val errors = validateSevereReportDraft(
            draft = SevereReportDraft(),
            appId = "",
            hasLocation = false,
        )

        assertEquals(4, errors.size)
    }

    @Test
    fun offsetsTornadoLocationAndNarrative() {
        val request = buildSevereReportRequest(
            appId = "app-id",
            draft = SevereReportDraft(
                tornado = true,
                offsetEnabled = true,
                offsetDistanceMiles = 2.0,
                offsetDirection = ReportDirection.N,
                firstHandConfirmed = true,
            ),
            latitude = 34.0,
            longitude = -97.0,
            now = Date(0L),
        )

        assertEquals(34.0289855, request.lat, 0.000001)
        assertEquals(-97.0, request.lon, 0.0)
        assertEquals("Report location offset approximately 2 mi N of reporter location.", request.narrative)
    }

    @Test
    fun ignoresStaleOffsetWhenOffsetIsDisabled() {
        val request = buildSevereReportRequest(
            appId = "app-id",
            draft = SevereReportDraft(
                tornado = true,
                offsetDistanceMiles = 2.0,
                offsetDirection = ReportDirection.N,
                firstHandConfirmed = true,
            ),
            latitude = 34.0,
            longitude = -97.0,
            now = Date(0L),
        )

        assertEquals(34.0, request.lat, 0.0)
        assertEquals(-97.0, request.lon, 0.0)
        assertEquals("", request.narrative)
    }

    @Test
    fun validatesEnabledOffsetRequiresDistanceAndDirection() {
        val errors = validateSevereReportDraft(
            draft = SevereReportDraft(
                tornado = true,
                offsetEnabled = true,
                firstHandConfirmed = true,
            ),
            appId = "app-id",
            hasLocation = true,
        )

        assertTrue(errors.any { it.contains("distance and direction") })
    }
}
