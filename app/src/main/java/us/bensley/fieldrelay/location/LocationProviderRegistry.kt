package us.bensley.fieldrelay.location

import us.bensley.fieldrelay.data.Settings

enum class LocationProviderId(
    val displayName: String,
    val shortName: String,
) {
    SPOTTER_NETWORK("Spotter Network", "SN"),
    OVERLAND("Overland", "OVR"),
    APRS("APRS", "APRS"),
}

data class LocationProviderStatus(
    val id: LocationProviderId,
    val configured: Boolean,
)

object LocationProviderRegistry {
    fun statuses(settings: Settings): List<LocationProviderStatus> = listOf(
        LocationProviderStatus(
            id = LocationProviderId.SPOTTER_NETWORK,
            configured = settings.spotterNetworkEnabled &&
                settings.appId.isNotBlank(),
        ),
        LocationProviderStatus(
            id = LocationProviderId.OVERLAND,
            configured = settings.overlandEnabled &&
                settings.overlandEndpoint.isNotBlank(),
        ),
        LocationProviderStatus(
            id = LocationProviderId.APRS,
            configured = settings.aprsEnabled &&
                settings.aprsCallsign.isNotBlank() &&
                settings.aprsPasscode.isNotBlank() &&
                settings.aprsServer.isNotBlank() &&
                settings.aprsPort in 1..65_535,
        ),
    )

    fun enabledProviderIds(settings: Settings): Set<LocationProviderId> =
        statuses(settings)
            .filter { it.configured }
            .mapTo(mutableSetOf()) { it.id }

    fun hasEnabledProvider(settings: Settings): Boolean =
        enabledProviderIds(settings).isNotEmpty()

    fun providerNames(settings: Settings): List<String> =
        statuses(settings)
            .filter { it.configured }
            .map { it.id.displayName }
}
