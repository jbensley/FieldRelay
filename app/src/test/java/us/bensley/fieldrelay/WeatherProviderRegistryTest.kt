package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.weather.WeatherProviderId
import us.bensley.fieldrelay.weather.WeatherProviderRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherProviderRegistryTest {
    @Test
    fun hasNoConfiguredProvidersByDefault() {
        assertFalse(WeatherProviderRegistry.hasConfiguredProvider(Settings.DEFAULT))
        assertEquals(emptySet<WeatherProviderId>(), WeatherProviderRegistry.configuredProviderIds(Settings.DEFAULT))
    }

    @Test
    fun spotterNetworkWeatherReportingRequiresApplicationIdOnly() {
        val settings = Settings.DEFAULT.copy(
            appId = "app-id",
            spotterNetworkEnabled = false,
        )

        assertTrue(WeatherProviderRegistry.hasConfiguredProvider(settings))
        assertEquals(
            setOf(WeatherProviderId.SPOTTER_NETWORK),
            WeatherProviderRegistry.configuredProviderIds(settings),
        )
    }

    @Test
    fun aprsDoesNotConfigureWeatherReporting() {
        val settings = Settings.DEFAULT.copy(
            aprsEnabled = true,
            aprsCallsign = "N0ABC-9",
        )

        assertFalse(WeatherProviderRegistry.hasConfiguredProvider(settings))
    }
}
