package us.bensley.fieldrelay.report

fun validateSevereReportDraft(
    draft: SevereReportDraft,
    appId: String,
    hasLocation: Boolean,
): List<String> {
    val errors = mutableListOf<String>()

    if (appId.isBlank()) errors += "Enter your Spotter Network Application ID in Settings."
    if (!hasLocation) errors += "Wait for a GPS fix before submitting a report."
    if (!draft.firstHandConfirmed) errors += "Confirm the report is first-hand and happening now."
    if (!draft.hasAnyPhenomenon) errors += "Choose at least one report type."

    if (draft.hail && (draft.hailSizeInches == null || draft.hailSizeInches <= 0.0)) {
        errors += "Choose hail size."
    }
    if (draft.offsetEnabled && (draft.offsetDistanceMiles == null || draft.offsetDirection == null)) {
        errors += "Choose both distance and direction for the location offset."
    }
    if (draft.wind && (draft.windSpeedMph == null || draft.windSpeedMph <= 50)) {
        errors += "Measured severe wind must be greater than 50 mph."
    }
    if ((draft.flood || draft.flashFlood) && (draft.floodDepthInches == null || draft.floodDepthInches < 4)) {
        errors += "Flood reports should include at least 4 inches of water depth."
    }

    return errors
}
