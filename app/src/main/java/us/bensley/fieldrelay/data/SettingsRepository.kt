package us.bensley.fieldrelay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: androidx.datastore.core.DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(
    name = "fieldrelay"
)

enum class BeaconDurationMode {
    HOURS_1,
    HOURS_2,
    HOURS_4,
    HOURS_8,
    HOURS_24,
    INDEFINITE,
    ASK;

    fun toDurationMs(): Long = when (this) {
        HOURS_1 -> 3_600_000L
        HOURS_2 -> 7_200_000L
        HOURS_4 -> 14_400_000L
        HOURS_8 -> 28_800_000L
        HOURS_24 -> 86_400_000L
        INDEFINITE -> -1L
        ASK -> error("ASK is a Settings-only meta-choice and has no runtime duration")
    }
}

enum class CoordinateDisplayMode {
    DECIMAL,
    ARC,
}

enum class ElevationDisplayUnit {
    METERS,
    FEET,
}

enum class AprsToneMode {
    NONE,
    CTCSS,
    DCS,
}

data class Settings(
    val appId: String,
    val spotterNetworkEnabled: Boolean,
    val aprsEnabled: Boolean,
    val aprsCallsign: String,
    val aprsPasscode: String,
    val aprsServer: String,
    val aprsPort: Int,
    val aprsSymbolTable: String,
    val aprsSymbolCode: String,
    val aprsFrequencyMhz: String,
    val aprsToneMode: AprsToneMode,
    val aprsTone: String,
    val aprsOffset: String,
    val aprsRange: String,
    val aprsComment: String,
    val intervalMs: Long,
    val autoResume: Boolean,
    val reportingOn: Boolean,
    val beaconDurationMode: BeaconDurationMode,
    val beaconExpiresAt: Long?,
    val widgetLat: Double?,
    val widgetLon: Double?,
    val coordinateDisplayMode: CoordinateDisplayMode,
    val elevationDisplayUnit: ElevationDisplayUnit,
) {
    companion object {
        val DEFAULT = Settings(
            appId = "",
            spotterNetworkEnabled = false,
            aprsEnabled = false,
            aprsCallsign = "",
            aprsPasscode = "",
            aprsServer = "rotate.aprs2.net",
            aprsPort = 14580,
            aprsSymbolTable = "/",
            aprsSymbolCode = ">",
            aprsFrequencyMhz = "",
            aprsToneMode = AprsToneMode.NONE,
            aprsTone = "",
            aprsOffset = "",
            aprsRange = "",
            aprsComment = "FieldRelay Android",
            intervalMs = 30_000L,
            autoResume = true,
            reportingOn = false,
            beaconDurationMode = BeaconDurationMode.INDEFINITE,
            beaconExpiresAt = null,
            widgetLat = null,
            widgetLon = null,
            coordinateDisplayMode = CoordinateDisplayMode.DECIMAL,
            elevationDisplayUnit = ElevationDisplayUnit.METERS,
        )
    }
}

class SettingsRepository(context: Context) {
    private val dataStore = context.dataStore

    private object Keys {
        val APP_ID = stringPreferencesKey("app_id")
        val SPOTTER_NETWORK_ENABLED = booleanPreferencesKey("spotter_network_enabled")
        val APRS_ENABLED = booleanPreferencesKey("aprs_enabled")
        val APRS_CALLSIGN = stringPreferencesKey("aprs_callsign")
        val APRS_PASSCODE = stringPreferencesKey("aprs_passcode")
        val APRS_SERVER = stringPreferencesKey("aprs_server")
        val APRS_PORT = longPreferencesKey("aprs_port")
        val APRS_SYMBOL_TABLE = stringPreferencesKey("aprs_symbol_table")
        val APRS_SYMBOL_CODE = stringPreferencesKey("aprs_symbol_code")
        val APRS_FREQUENCY_MHZ = stringPreferencesKey("aprs_frequency_mhz")
        val APRS_TONE_MODE = stringPreferencesKey("aprs_tone_mode")
        val APRS_TONE = stringPreferencesKey("aprs_tone")
        val APRS_OFFSET = stringPreferencesKey("aprs_offset")
        val APRS_RANGE = stringPreferencesKey("aprs_range")
        val APRS_COMMENT = stringPreferencesKey("aprs_comment")
        val INTERVAL_MS = longPreferencesKey("interval_ms")
        val AUTO_RESUME = booleanPreferencesKey("auto_resume")
        val REPORTING_ON = booleanPreferencesKey("reporting_on")
        val BEACON_DURATION_MODE = stringPreferencesKey("beacon_duration_mode")
        val BEACON_EXPIRES_AT = longPreferencesKey("beacon_expires_at")
        val WIDGET_LAT = doublePreferencesKey("widget_lat")
        val WIDGET_LON = doublePreferencesKey("widget_lon")
        val COORDINATE_DISPLAY_MODE = stringPreferencesKey("coordinate_display_mode")
        val ELEVATION_DISPLAY_UNIT = stringPreferencesKey("elevation_display_unit")
    }

    val settings: Flow<Settings> = dataStore.data.map { p ->
        val modeName = p[Keys.BEACON_DURATION_MODE]
        val mode = modeName?.let { runCatching { BeaconDurationMode.valueOf(it) }.getOrNull() }
            ?: BeaconDurationMode.INDEFINITE
        val coordinateModeName = p[Keys.COORDINATE_DISPLAY_MODE]
        val coordinateMode = coordinateModeName
            ?.let { runCatching { CoordinateDisplayMode.valueOf(it) }.getOrNull() }
            ?: CoordinateDisplayMode.DECIMAL
        val elevationUnitName = p[Keys.ELEVATION_DISPLAY_UNIT]
        val elevationUnit = elevationUnitName
            ?.let { runCatching { ElevationDisplayUnit.valueOf(it) }.getOrNull() }
            ?: ElevationDisplayUnit.METERS
        val aprsTone = p[Keys.APRS_TONE] ?: ""
        val aprsToneModeName = p[Keys.APRS_TONE_MODE]
        val aprsToneMode = aprsToneModeName
            ?.let { runCatching { AprsToneMode.valueOf(it) }.getOrNull() }
            ?: inferAprsToneMode(aprsTone)
        val expiresRaw = p[Keys.BEACON_EXPIRES_AT] ?: -1L
        Settings(
            appId = p[Keys.APP_ID] ?: "",
            spotterNetworkEnabled = p[Keys.SPOTTER_NETWORK_ENABLED] ?: false,
            aprsEnabled = p[Keys.APRS_ENABLED] ?: false,
            aprsCallsign = p[Keys.APRS_CALLSIGN] ?: "",
            aprsPasscode = p[Keys.APRS_PASSCODE] ?: "",
            aprsServer = p[Keys.APRS_SERVER] ?: "rotate.aprs2.net",
            aprsPort = (p[Keys.APRS_PORT] ?: 14580L).toInt(),
            aprsSymbolTable = p[Keys.APRS_SYMBOL_TABLE] ?: "/",
            aprsSymbolCode = p[Keys.APRS_SYMBOL_CODE] ?: ">",
            aprsFrequencyMhz = p[Keys.APRS_FREQUENCY_MHZ] ?: "",
            aprsToneMode = aprsToneMode,
            aprsTone = normalizeAprsToneValue(aprsTone, aprsToneMode),
            aprsOffset = p[Keys.APRS_OFFSET] ?: "",
            aprsRange = p[Keys.APRS_RANGE] ?: "",
            aprsComment = p[Keys.APRS_COMMENT] ?: "FieldRelay Android",
            intervalMs = p[Keys.INTERVAL_MS] ?: 30_000L,
            autoResume = p[Keys.AUTO_RESUME] ?: true,
            reportingOn = p[Keys.REPORTING_ON] ?: false,
            beaconDurationMode = mode,
            beaconExpiresAt = if (expiresRaw < 0L) null else expiresRaw,
            widgetLat = p[Keys.WIDGET_LAT],
            widgetLon = p[Keys.WIDGET_LON],
            coordinateDisplayMode = coordinateMode,
            elevationDisplayUnit = elevationUnit,
        )
    }

    suspend fun setAppId(value: String) {
        dataStore.edit { it[Keys.APP_ID] = value }
    }

    suspend fun setSpotterNetworkEnabled(value: Boolean) {
        dataStore.edit { it[Keys.SPOTTER_NETWORK_ENABLED] = value }
    }

    suspend fun setAprsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.APRS_ENABLED] = value }
    }

    suspend fun setAprsCredentials(callsign: String, passcode: String) {
        dataStore.edit {
            it[Keys.APRS_CALLSIGN] = callsign
            it[Keys.APRS_PASSCODE] = passcode
        }
    }

    suspend fun setAprsServer(server: String, port: Int) {
        dataStore.edit {
            it[Keys.APRS_SERVER] = server
            it[Keys.APRS_PORT] = port.toLong()
        }
    }

    suspend fun setAprsSymbol(symbolTable: String, symbolCode: String) {
        dataStore.edit {
            it[Keys.APRS_SYMBOL_TABLE] = symbolTable
            it[Keys.APRS_SYMBOL_CODE] = symbolCode
        }
    }

    suspend fun setAprsPayload(
        frequencyMhz: String,
        toneMode: AprsToneMode,
        tone: String,
        offset: String,
        range: String,
        comment: String,
    ) {
        dataStore.edit {
            it[Keys.APRS_FREQUENCY_MHZ] = frequencyMhz
            it[Keys.APRS_TONE_MODE] = toneMode.name
            it[Keys.APRS_TONE] = tone
            it[Keys.APRS_OFFSET] = offset
            it[Keys.APRS_RANGE] = range
            it[Keys.APRS_COMMENT] = comment
        }
    }

    suspend fun setIntervalMs(value: Long) {
        dataStore.edit { it[Keys.INTERVAL_MS] = value }
    }

    suspend fun setAutoResume(value: Boolean) {
        dataStore.edit { it[Keys.AUTO_RESUME] = value }
    }

    suspend fun setBeaconDurationMode(value: BeaconDurationMode) {
        dataStore.edit { it[Keys.BEACON_DURATION_MODE] = value.name }
    }

    suspend fun setCoordinateDisplayMode(value: CoordinateDisplayMode) {
        dataStore.edit { it[Keys.COORDINATE_DISPLAY_MODE] = value.name }
    }

    suspend fun setElevationDisplayUnit(value: ElevationDisplayUnit) {
        dataStore.edit { it[Keys.ELEVATION_DISPLAY_UNIT] = value.name }
    }

    suspend fun setBeaconState(reportingOn: Boolean, expiresAt: Long?) {
        dataStore.edit {
            it[Keys.REPORTING_ON] = reportingOn
            it[Keys.BEACON_EXPIRES_AT] = expiresAt ?: -1L
        }
    }

    suspend fun setWidgetLocation(lat: Double?, lon: Double?) {
        dataStore.edit {
            if (lat == null || lon == null) {
                it.remove(Keys.WIDGET_LAT)
                it.remove(Keys.WIDGET_LON)
            } else {
                it[Keys.WIDGET_LAT] = lat
                it[Keys.WIDGET_LON] = lon
            }
        }
    }
}

private fun inferAprsToneMode(value: String): AprsToneMode {
    val trimmed = value.trim().uppercase()
    return when {
        trimmed.startsWith("C") || trimmed.startsWith("T") -> AprsToneMode.CTCSS
        trimmed.startsWith("D") -> AprsToneMode.DCS
        trimmed.isNotBlank() -> AprsToneMode.CTCSS
        else -> AprsToneMode.NONE
    }
}

private fun normalizeAprsToneValue(value: String, mode: AprsToneMode): String {
    val trimmed = value.trim().uppercase()
    return when (mode) {
        AprsToneMode.NONE -> ""
        AprsToneMode.CTCSS -> trimmed.removePrefix("C").removePrefix("T")
        AprsToneMode.DCS -> trimmed.removePrefix("D")
    }
}
