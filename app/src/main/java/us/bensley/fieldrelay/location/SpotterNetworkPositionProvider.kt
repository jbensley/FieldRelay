package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.data.api.SpotterNetworkApi

class SpotterNetworkPositionProvider(
    private val api: SpotterNetworkApi,
) : PositionReportingProvider {
    override val id: String = "Spotter Network"

    override fun isEnabled(settings: Settings): Boolean =
        LocationProviderId.SPOTTER_NETWORK in LocationProviderRegistry.enabledProviderIds(settings)

    override suspend fun reportPosition(
        fix: LocationFix,
        reportAtUtc: String,
        settings: Settings,
    ): PositionReportResult {
        if (settings.appId.isBlank()) {
            return PositionReportResult(
                providerId = id,
                success = false,
                message = "Missing Application ID",
            )
        }

        val result = runCatching {
            api.updatePosition(
                buildPositionUpdate(
                    appId = settings.appId,
                    reportAtUtc = reportAtUtc,
                    fix = fix,
                ),
            )
        }
        val response = result.getOrNull()
        val error = result.exceptionOrNull()?.message
            ?: response?.takeIf { !it.success }?.let { it.message ?: "API reported failure" }

        return PositionReportResult(
            providerId = id,
            success = error == null,
            message = error,
        )
    }
}
