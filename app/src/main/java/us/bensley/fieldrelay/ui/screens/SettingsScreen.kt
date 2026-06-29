package us.bensley.fieldrelay.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import us.bensley.fieldrelay.BuildConfig
import us.bensley.fieldrelay.data.AprsToneMode
import us.bensley.fieldrelay.data.BeaconDurationMode
import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.data.settingsLabel
import us.bensley.fieldrelay.location.AprsToneOption
import us.bensley.fieldrelay.location.ActiveBeaconDurationChoice
import us.bensley.fieldrelay.location.LocationService
import us.bensley.fieldrelay.location.aprsToneOptions
import us.bensley.fieldrelay.location.decideBeaconDurationChange
import us.bensley.fieldrelay.location.isValidAprsOffset
import us.bensley.fieldrelay.permissions.hasBackgroundLocationPermission
import us.bensley.fieldrelay.permissions.hasNotificationPermission
import us.bensley.fieldrelay.ui.common.ResponsiveContent
import us.bensley.fieldrelay.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class IntervalOption(val label: String, val value: Long)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var appIdText by remember { mutableStateOf("") }
    var appIdVisible by remember { mutableStateOf(false) }
    var overlandEndpointText by remember { mutableStateOf("") }
    var overlandTokenText by remember { mutableStateOf("") }
    var overlandTokenVisible by remember { mutableStateOf(false) }
    var overlandDeviceIdText by remember { mutableStateOf("") }
    var aprsCallsignText by remember { mutableStateOf("") }
    var aprsPasscodeText by remember { mutableStateOf("") }
    var aprsPasscodeVisible by remember { mutableStateOf(false) }
    var aprsServerText by remember { mutableStateOf("") }
    var aprsPortText by remember { mutableStateOf("") }
    var aprsSymbolTableText by remember { mutableStateOf("") }
    var aprsSymbolCodeText by remember { mutableStateOf("") }
    var aprsFrequencyText by remember { mutableStateOf("") }
    var aprsToneMode by remember { mutableStateOf(AprsToneMode.NONE) }
    var aprsToneText by remember { mutableStateOf("") }
    var aprsOffsetText by remember { mutableStateOf("") }
    var aprsRangeText by remember { mutableStateOf("") }
    var aprsCommentText by remember { mutableStateOf("") }
    var pendingDurationMode by remember { mutableStateOf<BeaconDurationMode?>(null) }

    LaunchedEffect(settings.appId) {
        appIdText = ""
        appIdVisible = false
    }

    LaunchedEffect(settings.overlandEndpoint, settings.overlandToken, settings.overlandDeviceId) {
        overlandEndpointText = settings.overlandEndpoint
        overlandTokenText = ""
        overlandTokenVisible = false
        overlandDeviceIdText = settings.overlandDeviceId
    }

    LaunchedEffect(
        settings.aprsCallsign,
        settings.aprsPasscode,
        settings.aprsServer,
        settings.aprsPort,
        settings.aprsSymbolTable,
        settings.aprsSymbolCode,
        settings.aprsFrequencyMhz,
        settings.aprsToneMode,
        settings.aprsTone,
        settings.aprsOffset,
        settings.aprsRange,
        settings.aprsComment,
    ) {
        aprsCallsignText = settings.aprsCallsign
        aprsPasscodeText = ""
        aprsPasscodeVisible = false
        aprsServerText = settings.aprsServer
        aprsPortText = settings.aprsPort.toString()
        aprsSymbolTableText = settings.aprsSymbolTable
        aprsSymbolCodeText = settings.aprsSymbolCode
        aprsFrequencyText = settings.aprsFrequencyMhz
        aprsToneMode = settings.aprsToneMode
        aprsToneText = settings.aprsTone
        aprsOffsetText = settings.aprsOffset
        aprsRangeText = settings.aprsRange
        aprsCommentText = settings.aprsComment
    }

    ResponsiveContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            SpotterNetworkSettingsSection(
                settings = settings,
                appIdText = appIdText,
                onAppIdText = { appIdText = it },
                appIdVisible = appIdVisible,
                onToggleAppIdVisible = { appIdVisible = !appIdVisible },
                onEnabled = viewModel::setSpotterNetworkEnabled,
                onSaveAppId = { viewModel.setAppId(appIdText.trim()) },
                onRemoveAppId = {
                    viewModel.setAppId("")
                    viewModel.setSpotterNetworkEnabled(false)
                },
            )

            HorizontalDivider()

            OverlandSettingsSection(
                settings = settings,
                endpointText = overlandEndpointText,
                onEndpointText = { overlandEndpointText = it },
                tokenText = overlandTokenText,
                onTokenText = { overlandTokenText = it },
                tokenVisible = overlandTokenVisible,
                onToggleTokenVisible = { overlandTokenVisible = !overlandTokenVisible },
                deviceIdText = overlandDeviceIdText,
                onDeviceIdText = { overlandDeviceIdText = it },
                onEnabled = viewModel::setOverlandEnabled,
                onSave = {
                    viewModel.setOverlandSettings(
                        overlandEndpointText,
                        overlandTokenText.ifBlank { settings.overlandToken },
                        overlandDeviceIdText,
                    )
                },
                onRemove = {
                    viewModel.setOverlandSettings("", "", "FieldRelay Android")
                    viewModel.setOverlandEnabled(false)
                },
            )

            HorizontalDivider()

            AprsSettingsSection(
                settings = settings,
                callsignText = aprsCallsignText,
                onCallsignText = { aprsCallsignText = it },
                passcodeText = aprsPasscodeText,
                onPasscodeText = { aprsPasscodeText = it },
                passcodeVisible = aprsPasscodeVisible,
                onTogglePasscodeVisible = { aprsPasscodeVisible = !aprsPasscodeVisible },
                serverText = aprsServerText,
                onServerText = { aprsServerText = it },
                portText = aprsPortText,
                onPortText = { aprsPortText = it },
                symbolTableText = aprsSymbolTableText,
                onSymbolTableText = { aprsSymbolTableText = it },
                symbolCodeText = aprsSymbolCodeText,
                onSymbolCodeText = { aprsSymbolCodeText = it },
                frequencyText = aprsFrequencyText,
                onFrequencyText = { aprsFrequencyText = it },
                toneMode = aprsToneMode,
                onToneMode = {
                    aprsToneMode = it
                    aprsToneText = aprsToneOptions(it).firstOrNull()?.value.orEmpty()
                },
                toneText = aprsToneText,
                onToneText = { aprsToneText = it },
                offsetText = aprsOffsetText,
                onOffsetText = { aprsOffsetText = it },
                rangeText = aprsRangeText,
                onRangeText = { aprsRangeText = it },
                commentText = aprsCommentText,
                onCommentText = { aprsCommentText = it },
                onEnabled = viewModel::setAprsEnabled,
                onSaveCredentials = {
                    viewModel.setAprsCredentials(aprsCallsignText, aprsPasscodeText)
                },
                onRemoveCredentials = {
                    viewModel.setAprsCredentials("", "")
                    viewModel.setAprsEnabled(false)
                },
                onSaveServer = {
                    viewModel.setAprsServer(aprsServerText, aprsPortText.toIntOrNull() ?: 0)
                },
                onSaveSymbol = {
                    viewModel.setAprsSymbol(aprsSymbolTableText, aprsSymbolCodeText)
                },
                onSavePayload = {
                    viewModel.setAprsPayload(
                        frequencyMhz = aprsFrequencyText,
                        toneMode = aprsToneMode,
                        tone = aprsToneText,
                        offset = aprsOffsetText,
                        range = aprsRangeText,
                        comment = aprsCommentText,
                    )
                },
            )

            HorizontalDivider()

            AdaptiveOptionGroups(
                intervalContent = {
                    OptionGroup(title = "Update interval") {
                        intervalOptions().forEach { option ->
                            RadioRow(
                                label = option.label,
                                selected = settings.intervalMs == option.value,
                                onClick = { viewModel.setIntervalMs(option.value) },
                            )
                        }
                    }
                },
                durationContent = {
                    OptionGroup(title = "Beacon duration") {
                        BeaconDurationMode.entries.forEach { mode ->
                            RadioRow(
                                label = mode.settingsLabel(),
                                selected = settings.beaconDurationMode == mode,
                                onClick = {
                                    if (settings.reportingOn && mode != settings.beaconDurationMode) {
                                        pendingDurationMode = mode
                                    } else {
                                        viewModel.setBeaconDurationMode(mode)
                                    }
                                },
                            )
                        }
                    }
                },
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Resume on boot", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Restart the location beacon after phone reboot when background location and notification permissions are available.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = settings.autoResume,
                    onCheckedChange = viewModel::setAutoResume,
                )
            }

            HorizontalDivider()

            Text(
                text = buildStatusLine(
                    reportingOn = settings.reportingOn,
                    beaconExpiresAt = settings.beaconExpiresAt,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (BuildConfig.DEBUG) {
                HorizontalDivider()
                DiagnosticsSection(
                    settings = settings,
                    fineLocationGranted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
                    backgroundLocationGranted = context.hasBackgroundLocationPermission(),
                    notificationsGranted = context.hasNotificationPermission(),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    pendingDurationMode?.let { mode ->
        ActiveBeaconDurationDialog(
            mode = mode,
            onCancel = { pendingDurationMode = null },
            onDefaultOnly = {
                applyDurationDecision(
                    context = context,
                    viewModel = viewModel,
                    reportingOn = settings.reportingOn,
                    mode = mode,
                    choice = ActiveBeaconDurationChoice.DEFAULT_ONLY,
                )
                pendingDurationMode = null
            },
            onApplyNow = {
                applyDurationDecision(
                    context = context,
                    viewModel = viewModel,
                    reportingOn = settings.reportingOn,
                    mode = mode,
                    choice = ActiveBeaconDurationChoice.APPLY_NOW,
                )
                pendingDurationMode = null
            },
        )
    }
}

@Composable
private fun ActiveBeaconDurationDialog(
    mode: BeaconDurationMode,
    onCancel: () -> Unit,
    onDefaultOnly: () -> Unit,
    onApplyNow: () -> Unit,
) {
    val concrete = mode != BeaconDurationMode.ASK
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Change active beacon?") },
        text = {
            Text(
                if (concrete) {
                    "Save ${mode.settingsLabel()} as the default and apply it to the current beacon from now?"
                } else {
                    "Save Ask me each time as the default? The current beacon will keep its existing duration."
                },
            )
        },
        confirmButton = {
            if (concrete) {
                TextButton(onClick = onApplyNow) { Text("Apply now") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDefaultOnly) { Text("Default only") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        },
    )
}

private fun applyDurationDecision(
    context: android.content.Context,
    viewModel: SettingsViewModel,
    reportingOn: Boolean,
    mode: BeaconDurationMode,
    choice: ActiveBeaconDurationChoice,
) {
    val decision = decideBeaconDurationChange(
        reportingOn = reportingOn,
        selectedMode = mode,
        choice = choice,
    )
    if (decision.saveDefault) viewModel.setBeaconDurationMode(mode)
    decision.restartDurationMs?.let { LocationService.start(context, it) }
}

@Composable
private fun SpotterNetworkSettingsSection(
    settings: Settings,
    appIdText: String,
    onAppIdText: (String) -> Unit,
    appIdVisible: Boolean,
    onToggleAppIdVisible: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onSaveAppId: () -> Unit,
    onRemoveAppId: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Spotter Network", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (settings.appId.isBlank()) {
                        "No Spotter Network Application ID saved."
                    } else {
                        "Application ID saved."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = settings.spotterNetworkEnabled,
                enabled = settings.appId.isNotBlank(),
                onCheckedChange = onEnabled,
            )
        }
        Text(
            "Enable Spotter Network when you want the location beacon to send position updates there. Weather reports still require a saved Application ID.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = appIdText,
            onValueChange = onAppIdText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(if (settings.appId.isBlank()) "Application ID" else "New Application ID") },
            visualTransformation = if (appIdVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onToggleAppIdVisible) {
                    Icon(
                        imageVector = if (appIdVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (appIdVisible) {
                            "Hide Application ID"
                        } else {
                            "Show Application ID"
                        },
                    )
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSaveAppId,
                enabled = appIdText.trim().isNotBlank() && appIdText.trim() != settings.appId,
            ) {
                Text(if (settings.appId.isBlank()) "Save" else "Replace")
            }
            if (settings.appId.isNotBlank()) {
                TextButton(onClick = onRemoveAppId) {
                    Text("Remove")
                }
            }
        }
    }
}


@Composable
private fun OverlandSettingsSection(
    settings: Settings,
    endpointText: String,
    onEndpointText: (String) -> Unit,
    tokenText: String,
    onTokenText: (String) -> Unit,
    tokenVisible: Boolean,
    onToggleTokenVisible: () -> Unit,
    deviceIdText: String,
    onDeviceIdText: (String) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Overland", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (settings.overlandEndpoint.isBlank()) {
                        "No Overland endpoint saved."
                    } else {
                        "Overland endpoint saved."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = settings.overlandEnabled,
                enabled = settings.overlandEndpoint.isNotBlank(),
                onCheckedChange = onEnabled,
            )
        }
        Text(
            "Enable Overland to POST beacon locations to a compatible Overland endpoint. A bearer token is optional and depends on your server.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = endpointText,
            onValueChange = onEndpointText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(if (settings.overlandEndpoint.isBlank()) "Endpoint URL" else "New endpoint URL") },
        )
        OutlinedTextField(
            value = tokenText,
            onValueChange = onTokenText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(if (settings.overlandToken.isBlank()) "Bearer token optional" else "New bearer token") },
            visualTransformation = if (tokenVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onToggleTokenVisible) {
                    Icon(
                        imageVector = if (tokenVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (tokenVisible) {
                            "Hide Overland token"
                        } else {
                            "Show Overland token"
                        },
                    )
                }
            },
        )
        OutlinedTextField(
            value = deviceIdText,
            onValueChange = onDeviceIdText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Device ID") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSave,
                enabled = endpointText.trim().isNotBlank() &&
                    (endpointText.trim() != settings.overlandEndpoint ||
                        tokenText.trim() != settings.overlandToken ||
                        deviceIdText.trim() != settings.overlandDeviceId),
            ) {
                Text(if (settings.overlandEndpoint.isBlank()) "Save" else "Replace")
            }
            if (settings.overlandEndpoint.isNotBlank()) {
                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun AprsSettingsSection(
    settings: Settings,
    callsignText: String,
    onCallsignText: (String) -> Unit,
    passcodeText: String,
    onPasscodeText: (String) -> Unit,
    passcodeVisible: Boolean,
    onTogglePasscodeVisible: () -> Unit,
    serverText: String,
    onServerText: (String) -> Unit,
    portText: String,
    onPortText: (String) -> Unit,
    symbolTableText: String,
    onSymbolTableText: (String) -> Unit,
    symbolCodeText: String,
    onSymbolCodeText: (String) -> Unit,
    frequencyText: String,
    onFrequencyText: (String) -> Unit,
    toneMode: AprsToneMode,
    onToneMode: (AprsToneMode) -> Unit,
    toneText: String,
    onToneText: (String) -> Unit,
    offsetText: String,
    onOffsetText: (String) -> Unit,
    rangeText: String,
    onRangeText: (String) -> Unit,
    commentText: String,
    onCommentText: (String) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onSaveCredentials: () -> Unit,
    onRemoveCredentials: () -> Unit,
    onSaveServer: () -> Unit,
    onSaveSymbol: () -> Unit,
    onSavePayload: () -> Unit,
) {
    val offsetValid = isValidAprsOffset(offsetText)
    var advancedVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("APRS-IS", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (settings.aprsCallsign.isBlank() || settings.aprsPasscode.isBlank()) {
                        "No APRS-IS identity saved."
                    } else {
                        "APRS-IS identity saved for ${settings.aprsCallsign}."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = settings.aprsEnabled,
                enabled = settings.aprsCallsign.isNotBlank() && settings.aprsPasscode.isNotBlank(),
                onCheckedChange = onEnabled,
            )
        }
        Text(
            "Use APRS-IS only with your own licensed callsign and matching APRS-IS passcode.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = callsignText,
            onValueChange = onCallsignText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Callsign / SSID") },
        )
        OutlinedTextField(
            value = passcodeText,
            onValueChange = onPasscodeText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(if (settings.aprsPasscode.isBlank()) "APRS-IS passcode" else "New APRS-IS passcode") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (passcodeVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasscodeVisible) {
                    Icon(
                        imageVector = if (passcodeVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passcodeVisible) {
                            "Hide APRS-IS passcode"
                        } else {
                            "Show APRS-IS passcode"
                        },
                    )
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSaveCredentials,
                enabled = callsignText.trim().isNotBlank() &&
                    passcodeText.trim().isNotBlank() &&
                    (callsignText.trim() != settings.aprsCallsign ||
                        passcodeText.trim() != settings.aprsPasscode),
            ) {
                Text(if (settings.aprsCallsign.isBlank() || settings.aprsPasscode.isBlank()) "Save" else "Replace")
            }
            if (settings.aprsCallsign.isNotBlank() || settings.aprsPasscode.isNotBlank()) {
                TextButton(onClick = onRemoveCredentials) {
                    Text("Remove")
                }
            }
        }
        Text(
            "Default APRS-IS server: ${settings.aprsServer}:${settings.aprsPort}. Default symbol: ${settings.aprsSymbolTable}${settings.aprsSymbolCode}.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { advancedVisible = !advancedVisible }) {
            Text(if (advancedVisible) "Hide advanced APRS settings" else "Show advanced APRS settings")
        }
        if (advancedVisible) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Only change these if you know you need a different APRS-IS server or map symbol.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serverText,
                        onValueChange = onServerText,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Server") },
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = onPortText,
                        modifier = Modifier.weight(0.45f),
                        singleLine = true,
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Button(
                    onClick = onSaveServer,
                    enabled = serverText.trim().isNotBlank() && (portText.toIntOrNull() ?: 0) in 1..65_535,
                ) {
                    Text("Save APRS server")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = symbolTableText,
                        onValueChange = { onSymbolTableText(it.take(1)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Symbol table") },
                    )
                    OutlinedTextField(
                        value = symbolCodeText,
                        onValueChange = { onSymbolCodeText(it.take(1)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Symbol") },
                    )
                }
                Button(onClick = onSaveSymbol) {
                    Text("Save APRS symbol")
                }
            }
        }
        Text("APRS payload", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = frequencyText,
            onValueChange = onFrequencyText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Listening frequency MHz") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AprsToneModeMenu(
                selected = toneMode,
                onSelect = onToneMode,
                modifier = Modifier.weight(1f),
            )
            AprsToneValueMenu(
                mode = toneMode,
                selected = toneText,
                onSelect = onToneText,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = offsetText,
                onValueChange = { onOffsetText(it.take(8)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Offset MHz") },
                isError = !offsetValid,
                supportingText = {
                    if (!offsetValid) Text("Enter a signed MHz value, e.g. -0.6 or 5.0.")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        OutlinedTextField(
            value = rangeText,
            onValueChange = { onRangeText(it.take(8)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Range") },
        )
        OutlinedTextField(
            value = commentText,
            onValueChange = { onCommentText(it.take(43)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("APRS comment") },
        )
        Button(
            onClick = onSavePayload,
            enabled = offsetValid,
        ) {
            Text("Save APRS payload")
        }
    }
}

@Composable
private fun AprsToneModeMenu(
    selected: AprsToneMode,
    onSelect: (AprsToneMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
        ) {
            Text(selected.toneModeLabel())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AprsToneMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.toneModeLabel()) },
                    onClick = {
                        expanded = false
                        onSelect(mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun AprsToneValueMenu(
    mode: AprsToneMode,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = aprsToneOptions(mode)
    val selectedOption = options.firstOrNull { it.value == selected }
    val label = when {
        mode == AprsToneMode.NONE -> "No tone selected"
        selectedOption != null -> selectedOption.label
        else -> "Choose tone"
    }

    Box(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = options.isNotEmpty(),
            onClick = { expanded = true },
        ) {
            Text(label)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                AprsToneMenuItem(
                    option = option,
                    onSelect = {
                        expanded = false
                        onSelect(option.value)
                    },
                )
            }
        }
    }
}

@Composable
private fun AprsToneMenuItem(
    option: AprsToneOption,
    onSelect: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(option.label) },
        onClick = onSelect,
    )
}

private fun AprsToneMode.toneModeLabel(): String = when (this) {
    AprsToneMode.NONE -> "No tone"
    AprsToneMode.CTCSS -> "CTCSS"
    AprsToneMode.DCS -> "DCS"
}

@Composable
private fun AdaptiveOptionGroups(
    intervalContent: @Composable () -> Unit,
    durationContent: @Composable () -> Unit,
) {
    BoxWithConstraints {
        if (maxWidth >= 640.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) { intervalContent() }
                Column(modifier = Modifier.weight(1f)) { durationContent() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                intervalContent()
                HorizontalDivider()
                durationContent()
            }
        }
    }
}

@Composable
private fun OptionGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

private fun intervalOptions(): List<IntervalOption> = listOf(
    IntervalOption("30 seconds", 30_000L),
    IntervalOption("1 minute", 60_000L),
    IntervalOption("2 minutes", 120_000L),
    IntervalOption("5 minutes", 300_000L),
)

@Composable
private fun DiagnosticsSection(
    settings: Settings,
    fineLocationGranted: Boolean,
    backgroundLocationGranted: Boolean,
    notificationsGranted: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Debug diagnostics", style = MaterialTheme.typography.titleMedium)
        DiagnosticRow("App ID configured", settings.appId.isNotBlank().yesNo())
        DiagnosticRow("Beacon on", settings.reportingOn.yesNo())
        DiagnosticRow("Interval", "${settings.intervalMs / 1_000} seconds")
        DiagnosticRow("Duration mode", settings.beaconDurationMode.name)
        DiagnosticRow(
            "Beacon expiry",
            settings.beaconExpiresAt?.let { "${formatLocalTime(it)} ($it)" } ?: "Indefinite / none",
        )
        DiagnosticRow("Fine location", fineLocationGranted.grantedDenied())
        DiagnosticRow("Background location", backgroundLocationGranted.grantedDenied())
        DiagnosticRow("Notifications", notificationsGranted.grantedDenied())
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun buildStatusLine(reportingOn: Boolean, beaconExpiresAt: Long?): String {
    if (!reportingOn) return "Beacon: Off"
    val suffix = beaconExpiresAt?.let { "until ${formatLocalTime(it)}" } ?: "indefinitely"
    return "Beacon: On $suffix"
}

private fun formatLocalTime(epochMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun android.content.Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Boolean.yesNo(): String = if (this) "Yes" else "No"

private fun Boolean.grantedDenied(): String = if (this) "Granted" else "Denied"
