package us.bensley.fieldrelay.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import us.bensley.fieldrelay.data.AprsToneMode
import us.bensley.fieldrelay.data.BeaconDurationMode
import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.di.ServiceLocator
import us.bensley.fieldrelay.location.isValidAprsOffset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    val settings: StateFlow<Settings> = ServiceLocator.settings.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings.DEFAULT,
    )

    fun setAppId(value: String) {
        viewModelScope.launch { ServiceLocator.settings.setAppId(value.trim()) }
    }

    fun setSpotterNetworkEnabled(value: Boolean) {
        viewModelScope.launch { ServiceLocator.settings.setSpotterNetworkEnabled(value) }
    }

    fun setOverlandEnabled(value: Boolean) {
        viewModelScope.launch { ServiceLocator.settings.setOverlandEnabled(value) }
    }

    fun setOverlandSettings(endpoint: String, token: String, deviceId: String) {
        viewModelScope.launch {
            ServiceLocator.settings.setOverlandSettings(
                endpoint = endpoint.trim(),
                token = token.trim(),
                deviceId = deviceId.trim().ifBlank { "FieldRelay Android" },
            )
        }
    }

    fun setAprsEnabled(value: Boolean) {
        viewModelScope.launch { ServiceLocator.settings.setAprsEnabled(value) }
    }

    fun setAprsCredentials(callsign: String, passcode: String) {
        viewModelScope.launch {
            ServiceLocator.settings.setAprsCredentials(
                callsign = callsign.trim().uppercase(),
                passcode = passcode.trim(),
            )
        }
    }

    fun setAprsServer(server: String, port: Int) {
        viewModelScope.launch {
            ServiceLocator.settings.setAprsServer(
                server = server.trim(),
                port = port,
            )
        }
    }

    fun setAprsSymbol(symbolTable: String, symbolCode: String) {
        viewModelScope.launch {
            ServiceLocator.settings.setAprsSymbol(
                symbolTable = symbolTable.trim().take(1).ifBlank { "/" },
                symbolCode = symbolCode.trim().take(1).ifBlank { ">" },
            )
        }
    }

    fun setAprsPayload(
        frequencyMhz: String,
        toneMode: AprsToneMode,
        tone: String,
        offset: String,
        range: String,
        comment: String,
    ) {
        if (!isValidAprsOffset(offset.trim())) return
        viewModelScope.launch {
            ServiceLocator.settings.setAprsPayload(
                frequencyMhz = frequencyMhz.trim(),
                toneMode = toneMode,
                tone = tone.trim(),
                offset = offset.trim(),
                range = range.trim(),
                comment = comment.trim(),
            )
        }
    }

    fun setIntervalMs(value: Long) {
        viewModelScope.launch { ServiceLocator.settings.setIntervalMs(value) }
    }

    fun setBeaconDurationMode(value: BeaconDurationMode) {
        viewModelScope.launch { ServiceLocator.settings.setBeaconDurationMode(value) }
    }

    fun setAutoResume(value: Boolean) {
        viewModelScope.launch { ServiceLocator.settings.setAutoResume(value) }
    }
}
