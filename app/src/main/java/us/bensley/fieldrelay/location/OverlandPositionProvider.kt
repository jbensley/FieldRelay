package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.Settings

class OverlandPositionProvider(
    private val client: OverlandClient,
) : PositionReportingProvider {
    override val id: String = "Overland"

    override fun isEnabled(settings: Settings): Boolean =
        LocationProviderId.OVERLAND in LocationProviderRegistry.enabledProviderIds(settings)

    override suspend fun reportPosition(
        fix: LocationFix,
        reportAtUtc: String,
        settings: Settings,
    ): PositionReportResult {
        val endpoint = settings.overlandEndpoint.trim()
        if (endpoint.isBlank()) {
            return PositionReportResult(providerId = id, success = false, message = "Missing Overland endpoint")
        }

        val result = runCatching {
            client.publish(
                endpoint = endpoint,
                token = settings.overlandToken,
                update = buildOverlandUpdate(
                    fix = fix,
                    reportAtUtc = reportAtUtc,
                    deviceId = settings.overlandDeviceId,
                ),
            )
        }
        val response = result.getOrNull()
        val error = result.exceptionOrNull()?.message
            ?: response?.error
            ?: response?.takeUnless { it.result.equals("ok", ignoreCase = true) }?.let { "Overland reported failure" }

        return PositionReportResult(providerId = id, success = error == null, message = error)
    }
}
