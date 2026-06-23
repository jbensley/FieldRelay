package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.Settings

class PositionReportingCoordinator(
    private val providers: List<PositionReportingProvider>,
) {
    suspend fun reportPosition(
        fix: LocationFix,
        reportAtUtc: String,
        settings: Settings,
    ): List<PositionReportResult> {
        val enabledProviders = providers.filter { it.isEnabled(settings) }
        if (enabledProviders.isEmpty()) {
            return listOf(
                PositionReportResult(
                    providerId = "FieldRelay",
                    success = false,
                    message = "No enabled position providers",
                ),
            )
        }
        return enabledProviders.map { provider ->
            runCatching {
                provider.reportPosition(fix = fix, reportAtUtc = reportAtUtc, settings = settings)
            }.getOrElse { error ->
                PositionReportResult(
                    providerId = provider.id,
                    success = false,
                    message = error.message ?: "Position report failed.",
                )
            }
        }
    }
}
