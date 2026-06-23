package us.bensley.fieldrelay.weather

import us.bensley.fieldrelay.data.Settings

enum class WeatherProviderId(
    val displayName: String,
) {
    SPOTTER_NETWORK("Spotter Network"),
}

data class WeatherProviderStatus(
    val id: WeatherProviderId,
    val configured: Boolean,
)

object WeatherProviderRegistry {
    fun statuses(settings: Settings): List<WeatherProviderStatus> = listOf(
        WeatherProviderStatus(
            id = WeatherProviderId.SPOTTER_NETWORK,
            configured = settings.appId.isNotBlank(),
        ),
    )

    fun configuredProviderIds(settings: Settings): Set<WeatherProviderId> =
        statuses(settings)
            .filter { it.configured }
            .mapTo(mutableSetOf()) { it.id }

    fun hasConfiguredProvider(settings: Settings): Boolean =
        configuredProviderIds(settings).isNotEmpty()

    fun providerNames(settings: Settings): List<String> =
        statuses(settings)
            .filter { it.configured }
            .map { it.id.displayName }
}
