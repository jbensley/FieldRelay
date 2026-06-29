package us.bensley.fieldrelay

import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.location.LocationProviderId
import us.bensley.fieldrelay.location.LocationProviderRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationProviderRegistryTest {
    @Test
    fun hasNoEnabledProvidersByDefault() {
        assertFalse(LocationProviderRegistry.hasEnabledProvider(Settings.DEFAULT))
        assertEquals(emptySet<LocationProviderId>(), LocationProviderRegistry.enabledProviderIds(Settings.DEFAULT))
    }

    @Test
    fun enablesSpotterNetworkWhenToggledAndApplicationIdExists() {
        val settings = Settings.DEFAULT.copy(
            spotterNetworkEnabled = true,
            appId = "app-id",
        )

        assertTrue(LocationProviderRegistry.hasEnabledProvider(settings))
        assertEquals(
            setOf(LocationProviderId.SPOTTER_NETWORK),
            LocationProviderRegistry.enabledProviderIds(settings),
        )
    }

    @Test
    fun ignoresSpotterNetworkApplicationIdWhenToggleIsOff() {
        val settings = Settings.DEFAULT.copy(
            spotterNetworkEnabled = false,
            appId = "app-id",
        )

        assertFalse(LocationProviderRegistry.hasEnabledProvider(settings))
    }

    @Test
    fun enablesOverlandWhenToggledAndEndpointExists() {
        val settings = Settings.DEFAULT.copy(
            overlandEnabled = true,
            overlandEndpoint = "https://example.com/overland",
        )

        assertTrue(LocationProviderRegistry.hasEnabledProvider(settings))
        assertEquals(
            setOf(LocationProviderId.OVERLAND),
            LocationProviderRegistry.enabledProviderIds(settings),
        )
    }

    @Test
    fun ignoresOverlandEndpointWhenToggleIsOff() {
        val settings = Settings.DEFAULT.copy(
            overlandEnabled = false,
            overlandEndpoint = "https://example.com/overland",
        )

        assertFalse(LocationProviderRegistry.hasEnabledProvider(settings))
    }

    @Test
    fun enablesAprsWhenToggleAndCredentialsAreConfigured() {
        val settings = Settings.DEFAULT.copy(
            aprsEnabled = true,
            aprsCallsign = "N0ABC-9",
            aprsPasscode = "16272",
            aprsServer = "rotate.aprs2.net",
            aprsPort = 14580,
        )

        assertTrue(LocationProviderRegistry.hasEnabledProvider(settings))
        assertEquals(
            setOf(LocationProviderId.APRS),
            LocationProviderRegistry.enabledProviderIds(settings),
        )
    }

    @Test
    fun ignoresAprsCredentialsWhenToggleIsOff() {
        val settings = Settings.DEFAULT.copy(
            aprsEnabled = false,
            aprsCallsign = "N0ABC-9",
            aprsServer = "rotate.aprs2.net",
            aprsPort = 14580,
        )

        assertFalse(LocationProviderRegistry.hasEnabledProvider(settings))
    }
}
