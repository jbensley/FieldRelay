package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.BeaconDurationMode
import us.bensley.fieldrelay.location.expiresAtForDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BeaconTimingTest {
    @Test
    fun durationModesMapToExpectedMilliseconds() {
        assertEquals(3_600_000L, BeaconDurationMode.HOURS_1.toDurationMs())
        assertEquals(7_200_000L, BeaconDurationMode.HOURS_2.toDurationMs())
        assertEquals(14_400_000L, BeaconDurationMode.HOURS_4.toDurationMs())
        assertEquals(28_800_000L, BeaconDurationMode.HOURS_8.toDurationMs())
        assertEquals(86_400_000L, BeaconDurationMode.HOURS_24.toDurationMs())
        assertEquals(-1L, BeaconDurationMode.INDEFINITE.toDurationMs())
    }

    @Test
    fun askModeHasNoRuntimeDuration() {
        val result = runCatching { BeaconDurationMode.ASK.toDurationMs() }

        assertEquals(true, result.isFailure)
    }

    @Test
    fun positiveDurationProducesExpiry() {
        assertEquals(4_000L, expiresAtForDuration(nowMs = 1_000L, durationMs = 3_000L))
    }

    @Test
    fun indefiniteDurationHasNoExpiry() {
        assertNull(expiresAtForDuration(nowMs = 1_000L, durationMs = -1L))
    }
}
