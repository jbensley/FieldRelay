package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.location.LocationFix
import us.bensley.fieldrelay.location.PositionReportResult
import us.bensley.fieldrelay.location.PositionReportingCoordinator
import us.bensley.fieldrelay.location.PositionReportingProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.collections.get

class PositionReportingCoordinatorTest {
    @Test
    fun reportsToEachProviderAndNormalizesFailures() = runBlocking {
        val coordinator = PositionReportingCoordinator(
            providers = listOf(
                FakeProvider("ok") {
                    PositionReportResult(providerId = "ok", success = true)
                },
                FakeProvider("bad") {
                    error("network unavailable")
                },
            ),
        )

        val results = coordinator.reportPosition(
            fix = LocationFix(
                lat = 33.0,
                lon = -97.0,
                elev = 200.0,
                metersPerSecond = 1.0,
                bearing = 180.0,
            ),
            reportAtUtc = "2026-06-21 12:00:00",
            settings = Settings.DEFAULT,
        )

        assertEquals(2, results.size)
        assertEquals(true, results[0].success)
        assertEquals(false, results[1].success)
        assertEquals("bad", results[1].providerId)
        assertEquals("network unavailable", results[1].message)
    }

    @Test
    fun skipsDisabledProviders() = runBlocking {
        val coordinator = PositionReportingCoordinator(
            providers = listOf(
                FakeProvider("disabled", enabled = false) {
                    error("should not run")
                },
                FakeProvider("enabled") {
                    PositionReportResult(providerId = "enabled", success = true)
                },
            ),
        )

        val results = coordinator.reportPosition(
            fix = LocationFix(
                lat = 33.0,
                lon = -97.0,
                elev = 200.0,
                metersPerSecond = 1.0,
                bearing = 180.0,
            ),
            reportAtUtc = "2026-06-21 12:00:00",
            settings = Settings.DEFAULT,
        )

        assertEquals(1, results.size)
        assertEquals("enabled", results[0].providerId)
        assertEquals(true, results[0].success)
    }

    @Test
    fun returnsFailureWhenNoProvidersAreEnabled() = runBlocking {
        val coordinator = PositionReportingCoordinator(
            providers = listOf(
                FakeProvider("disabled", enabled = false) {
                    error("should not run")
                },
            ),
        )

        val results = coordinator.reportPosition(
            fix = LocationFix(
                lat = 33.0,
                lon = -97.0,
                elev = 200.0,
                metersPerSecond = 1.0,
                bearing = 180.0,
            ),
            reportAtUtc = "2026-06-21 12:00:00",
            settings = Settings.DEFAULT,
        )

        assertEquals(1, results.size)
        assertEquals(false, results[0].success)
        assertEquals("No enabled position providers", results[0].message)
    }

    private class FakeProvider(
        override val id: String,
        private val enabled: Boolean = true,
        private val result: suspend () -> PositionReportResult,
    ) : PositionReportingProvider {
        override fun isEnabled(settings: Settings): Boolean = enabled

        override suspend fun reportPosition(
            fix: LocationFix,
            reportAtUtc: String,
            settings: Settings,
        ): PositionReportResult = result()
    }
}
