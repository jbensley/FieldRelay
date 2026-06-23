package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.Settings

data class PositionReportResult(
    val providerId: String,
    val success: Boolean,
    val message: String? = null,
)

interface PositionReportingProvider {
    val id: String

    fun isEnabled(settings: Settings): Boolean = true

    suspend fun reportPosition(
        fix: LocationFix,
        reportAtUtc: String,
        settings: Settings,
    ): PositionReportResult
}
