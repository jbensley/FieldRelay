package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.BeaconDurationMode

enum class ActiveBeaconDurationChoice {
    APPLY_NOW,
    DEFAULT_ONLY,
    CANCEL,
}

data class BeaconDurationChangeDecision(
    val saveDefault: Boolean,
    val restartDurationMs: Long?,
)

fun decideBeaconDurationChange(
    reportingOn: Boolean,
    selectedMode: BeaconDurationMode,
    choice: ActiveBeaconDurationChoice,
): BeaconDurationChangeDecision = when {
    !reportingOn -> BeaconDurationChangeDecision(saveDefault = true, restartDurationMs = null)
    choice == ActiveBeaconDurationChoice.CANCEL -> BeaconDurationChangeDecision(saveDefault = false, restartDurationMs = null)
    choice == ActiveBeaconDurationChoice.DEFAULT_ONLY -> BeaconDurationChangeDecision(saveDefault = true, restartDurationMs = null)
    selectedMode == BeaconDurationMode.ASK -> BeaconDurationChangeDecision(saveDefault = true, restartDurationMs = null)
    else -> BeaconDurationChangeDecision(
        saveDefault = true,
        restartDurationMs = selectedMode.toDurationMs(),
    )
}
