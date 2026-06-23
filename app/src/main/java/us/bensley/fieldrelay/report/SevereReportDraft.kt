package us.bensley.fieldrelay.report

data class SevereReportDraft(
    val tornado: Boolean = false,
    val funnelCloud: Boolean = false,
    val wallCloud: Boolean = false,
    val rotation: Boolean = false,
    val hail: Boolean = false,
    val hailSizeInches: Double? = null,
    val wind: Boolean = false,
    val windSpeedMph: Int? = null,
    val flood: Boolean = false,
    val flashFlood: Boolean = false,
    val floodDepthInches: Int? = null,
    val damage: Boolean = false,
    val injury: Boolean = false,
    val other: Boolean = false,
    val offsetEnabled: Boolean = false,
    val offsetDistanceMiles: Double? = null,
    val offsetDirection: ReportDirection? = null,
    val firstHandConfirmed: Boolean = false,
) {
    val hasAnyPhenomenon: Boolean
        get() = tornado || funnelCloud || wallCloud || rotation || hail || wind ||
            flood || flashFlood || damage || injury || other
}

enum class ReportDirection(val label: String, val northComponent: Double, val eastComponent: Double) {
    N("N", 1.0, 0.0),
    NE("NE", 0.70710678118, 0.70710678118),
    E("E", 0.0, 1.0),
    SE("SE", -0.70710678118, 0.70710678118),
    S("S", -1.0, 0.0),
    SW("SW", -0.70710678118, -0.70710678118),
    W("W", 0.0, -1.0),
    NW("NW", 0.70710678118, -0.70710678118),
}
