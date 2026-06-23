package us.bensley.fieldrelay.location

data class LocationFix(
    val lat: Double,
    val lon: Double,
    val elev: Double,
    val metersPerSecond: Double,
    val bearing: Double,
)
