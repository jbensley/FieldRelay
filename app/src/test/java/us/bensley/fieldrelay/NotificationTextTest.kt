package us.bensley.fieldrelay

import us.bensley.fieldrelay.location.buildLocationNotificationText
import us.bensley.fieldrelay.location.ProviderTelemetry
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {
    @Test
    fun showsIndefiniteBeforeFirstBeaconUpdate() {
        val text = buildLocationNotificationText(
            expiresAt = null,
            providerStatuses = emptyMap(),
            formatTime = ::fakeTime,
        )

        assertEquals("On (indefinite)", text)
    }

    @Test
    fun showsFiniteExpiryAndOkBeaconUpdate() {
        val text = buildLocationNotificationText(
            expiresAt = 1L,
            providerStatuses = mapOf(
                "APRS" to ProviderTelemetry(lastReportAt = 2L, success = true),
                "Spotter Network" to ProviderTelemetry(lastReportAt = 3L, success = true),
            ),
            formatTime = ::fakeTime,
        )

        assertEquals("On until T1 - last beacon update T3 OK", text)
    }

    @Test
    fun showsFailureReasonOverOkBeaconUpdate() {
        val text = buildLocationNotificationText(
            expiresAt = null,
            providerStatuses = mapOf(
                "Spotter Network" to ProviderTelemetry(
                    lastReportAt = 2L,
                    success = false,
                    message = "network down",
                ),
            ),
            formatTime = ::fakeTime,
        )

        assertEquals("On (indefinite) - Spotter Network failed: network down", text)
    }

    private fun fakeTime(epochMs: Long): String = "T$epochMs"
}
