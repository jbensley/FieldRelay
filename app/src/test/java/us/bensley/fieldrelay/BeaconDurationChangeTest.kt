package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.BeaconDurationMode
import us.bensley.fieldrelay.location.ActiveBeaconDurationChoice
import us.bensley.fieldrelay.location.decideBeaconDurationChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BeaconDurationChangeTest {
    @Test
    fun savesDefaultOnlyWhenReportingIsOff() {
        val decision = decideBeaconDurationChange(
            reportingOn = false,
            selectedMode = BeaconDurationMode.HOURS_1,
            choice = ActiveBeaconDurationChoice.APPLY_NOW,
        )

        assertEquals(true, decision.saveDefault)
        assertNull(decision.restartDurationMs)
    }

    @Test
    fun cancelDoesNotSaveOrRestart() {
        val decision = decideBeaconDurationChange(
            reportingOn = true,
            selectedMode = BeaconDurationMode.HOURS_1,
            choice = ActiveBeaconDurationChoice.CANCEL,
        )

        assertEquals(false, decision.saveDefault)
        assertNull(decision.restartDurationMs)
    }

    @Test
    fun defaultOnlySavesWithoutRestartingActiveBeacon() {
        val decision = decideBeaconDurationChange(
            reportingOn = true,
            selectedMode = BeaconDurationMode.HOURS_2,
            choice = ActiveBeaconDurationChoice.DEFAULT_ONLY,
        )

        assertEquals(true, decision.saveDefault)
        assertNull(decision.restartDurationMs)
    }

    @Test
    fun applyNowRestartsConcreteDuration() {
        val decision = decideBeaconDurationChange(
            reportingOn = true,
            selectedMode = BeaconDurationMode.HOURS_4,
            choice = ActiveBeaconDurationChoice.APPLY_NOW,
        )

        assertEquals(true, decision.saveDefault)
        assertEquals(14_400_000L, decision.restartDurationMs)
    }

    @Test
    fun askModeIsDefaultOnlyEvenWhenApplied() {
        val decision = decideBeaconDurationChange(
            reportingOn = true,
            selectedMode = BeaconDurationMode.ASK,
            choice = ActiveBeaconDurationChoice.APPLY_NOW,
        )

        assertEquals(true, decision.saveDefault)
        assertNull(decision.restartDurationMs)
    }
}
