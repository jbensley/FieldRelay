package us.bensley.fieldrelay.location

data class ProviderTelemetry(
    val lastReportAt: Long,
    val success: Boolean,
    val message: String? = null,
)

data class TelemetryState(
    val lat: Double? = null,
    val lon: Double? = null,
    val mph: Double? = null,
    val bearing: Double? = null,
    val elev: Double? = null,
    val providerStatuses: Map<String, ProviderTelemetry> = emptyMap(),
)
