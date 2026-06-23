package us.bensley.fieldrelay

import us.bensley.fieldrelay.boot.BootResumeDecision
import us.bensley.fieldrelay.boot.resolveBootResume
import us.bensley.fieldrelay.data.BeaconDurationMode
import us.bensley.fieldrelay.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class BootResumeDecisionTest {
    @Test
    fun skipsWhenAutoResumeDisabled() {
        val decision = resolveBootResume(settings(autoResume = false), nowMs = 1_000L)

        assertEquals(BootResumeDecision.Skip, decision)
    }

    @Test
    fun skipsWhenReportingWasOff() {
        val decision = resolveBootResume(settings(reportingOn = false), nowMs = 1_000L)

        assertEquals(BootResumeDecision.Skip, decision)
    }

    @Test
    fun skipsWhenNoProviderConfigured() {
        val decision = resolveBootResume(settings(appId = ""), nowMs = 1_000L)

        assertEquals(BootResumeDecision.Skip, decision)
    }

    @Test
    fun startsWhenOnlyAprsIsConfigured() {
        val decision = resolveBootResume(
            settings(
                appId = "",
                aprsEnabled = true,
                aprsCallsign = "N0ABC-9",
                aprsPasscode = "16272",
            ),
            nowMs = 1_000L,
        )

        assertEquals(BootResumeDecision.Start(durationMs = -1L), decision)
    }

    @Test
    fun startsIndefiniteBeacon() {
        val decision = resolveBootResume(settings(beaconExpiresAt = null), nowMs = 1_000L)

        assertEquals(BootResumeDecision.Start(durationMs = -1L), decision)
    }

    @Test
    fun startsFiniteBeaconWithRemainingDuration() {
        val decision = resolveBootResume(settings(beaconExpiresAt = 7_000L), nowMs = 1_000L)

        assertEquals(BootResumeDecision.Start(durationMs = 6_000L), decision)
    }

    @Test
    fun clearsExpiredBeacon() {
        val decision = resolveBootResume(settings(beaconExpiresAt = 1_000L), nowMs = 1_000L)

        assertEquals(BootResumeDecision.ClearExpired, decision)
    }

    private fun settings(
        appId: String = "app-id",
        spotterNetworkEnabled: Boolean = true,
        aprsEnabled: Boolean = false,
        aprsCallsign: String = "",
        aprsPasscode: String = "",
        autoResume: Boolean = true,
        reportingOn: Boolean = true,
        beaconExpiresAt: Long? = null,
    ) = Settings.DEFAULT.copy(
        appId = appId,
        spotterNetworkEnabled = spotterNetworkEnabled,
        aprsEnabled = aprsEnabled,
        aprsCallsign = aprsCallsign,
        aprsPasscode = aprsPasscode,
        intervalMs = 30_000L,
        autoResume = autoResume,
        reportingOn = reportingOn,
        beaconDurationMode = BeaconDurationMode.INDEFINITE,
        beaconExpiresAt = beaconExpiresAt,
        widgetLat = null,
        widgetLon = null,
    )
}
