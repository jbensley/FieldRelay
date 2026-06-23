package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.BuildConfig
import us.bensley.fieldrelay.data.Settings

class AprsIsPositionProvider(
    private val client: AprsIsClient,
) : PositionReportingProvider {
    override val id: String = "APRS"

    override fun isEnabled(settings: Settings): Boolean =
        hasValidAprsConfiguration(settings)

    override suspend fun reportPosition(
        fix: LocationFix,
        reportAtUtc: String,
        settings: Settings,
    ): PositionReportResult {
        if (!hasValidAprsConfiguration(settings)) {
            return PositionReportResult(
                providerId = id,
                success = false,
                message = "Missing APRS-IS configuration",
            )
        }

        val server = settings.aprsServer.trim()
        return runCatching {
            client.transmit(
                server = server,
                port = settings.aprsPort,
                loginLine = buildAprsIsLogin(settings, BuildConfig.VERSION_NAME),
                packetLine = buildAprsPositionPacket(fix, settings),
            )
        }.fold(
            onSuccess = {
                PositionReportResult(providerId = id, success = true)
            },
            onFailure = { error ->
                PositionReportResult(
                    providerId = id,
                    success = false,
                    message = error.message ?: "APRS-IS transmit failed",
                )
            },
        )
    }
}
