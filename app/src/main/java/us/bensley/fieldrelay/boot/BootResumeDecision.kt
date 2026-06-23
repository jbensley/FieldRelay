package us.bensley.fieldrelay.boot

import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.location.LocationProviderRegistry

sealed interface BootResumeDecision {
    data object Skip : BootResumeDecision
    data object ClearExpired : BootResumeDecision
    data class Start(val durationMs: Long) : BootResumeDecision
}

fun resolveBootResume(settings: us.bensley.fieldrelay.data.Settings, nowMs: Long): BootResumeDecision {
    if (!settings.autoResume || !settings.reportingOn || !us.bensley.fieldrelay.location.LocationProviderRegistry.hasEnabledProvider(settings)) {
        return BootResumeDecision.Skip
    }

    val expiresAt = settings.beaconExpiresAt ?: return BootResumeDecision.Start(durationMs = -1L)
    return if (nowMs < expiresAt) {
        BootResumeDecision.Start(durationMs = expiresAt - nowMs)
    } else {
        BootResumeDecision.ClearExpired
    }
}
