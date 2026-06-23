package us.bensley.fieldrelay.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import us.bensley.fieldrelay.data.BeaconDurationMode
import us.bensley.fieldrelay.data.CoordinateDisplayMode
import us.bensley.fieldrelay.data.ElevationDisplayUnit
import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.data.runtimeDurationOptions
import us.bensley.fieldrelay.location.LocationService
import us.bensley.fieldrelay.location.LocationProviderRegistry
import us.bensley.fieldrelay.location.ProviderTelemetry
import us.bensley.fieldrelay.location.TelemetryState
import us.bensley.fieldrelay.location.formatDecimalCoordinate
import us.bensley.fieldrelay.location.formatDmsLatitude
import us.bensley.fieldrelay.location.formatDmsLongitude
import us.bensley.fieldrelay.location.formatMaidenheadGrid
import us.bensley.fieldrelay.ui.common.ResponsiveContent
import us.bensley.fieldrelay.ui.home.HomeViewModel
import us.bensley.fieldrelay.ui.permissions.PermissionGate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    showDurationDialogRequest: Boolean,
    confirmStopDialogRequest: Boolean,
    requestPermissions: Boolean,
    onDurationDialogConsumed: () -> Unit,
    onConfirmStopDialogConsumed: () -> Unit,
    onPermissionsConsumed: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    var showDurationDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var permissionGateActive by remember { mutableStateOf(false) }
    var pendingDurationMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(showDurationDialogRequest, settings.reportingOn) {
        if (showDurationDialogRequest) {
            onDurationDialogConsumed()
            if (!settings.reportingOn && LocationProviderRegistry.hasEnabledProvider(settings)) showDurationDialog = true
        }
    }

    LaunchedEffect(confirmStopDialogRequest, settings.reportingOn) {
        if (confirmStopDialogRequest) {
            onConfirmStopDialogConsumed()
            if (settings.reportingOn) showStopDialog = true
        }
    }

    LaunchedEffect(requestPermissions, settings, settings.beaconDurationMode, settings.reportingOn) {
        if (requestPermissions) {
            onPermissionsConsumed()
            if (settings.reportingOn || !LocationProviderRegistry.hasEnabledProvider(settings)) {
                permissionGateActive = true
            } else if (settings.beaconDurationMode == BeaconDurationMode.ASK) {
                showDurationDialog = true
            } else {
                pendingDurationMs = settings.beaconDurationMode.toDurationMs()
                permissionGateActive = true
            }
        }
    }

    PermissionGate(
        active = permissionGateActive,
        onReady = {
            pendingDurationMs?.let {
                LocationService.start(context, it)
                pendingDurationMs = null
            }
        },
        onFinished = {
            permissionGateActive = false
            pendingDurationMs = null
        },
    )

    if (showDurationDialog) {
        DurationPickerDialog(
            onDismiss = { showDurationDialog = false },
            onConfirm = { durationMs ->
                showDurationDialog = false
                pendingDurationMs = durationMs
                permissionGateActive = true
            },
        )
    }

    if (showStopDialog) {
        StopBeaconDialog(
            onDismiss = { showStopDialog = false },
            onConfirm = {
                showStopDialog = false
                LocationService.stop(context)
            },
        )
    }

    ResponsiveContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("FieldRelay", style = MaterialTheme.typography.headlineMedium)

            StatusCard(settings = settings)

            if (!LocationProviderRegistry.hasEnabledProvider(settings)) {
                FirstRunGuidanceCard(onOpenSettings = onOpenSettings)
            }

            BeaconControls(
                settings = settings,
                onToggle = {
                    if (settings.reportingOn) {
                        LocationService.stop(context)
                    } else if (settings.beaconDurationMode == BeaconDurationMode.ASK) {
                        showDurationDialog = true
                    } else {
                        pendingDurationMs = settings.beaconDurationMode.toDurationMs()
                        permissionGateActive = true
                    }
                },
            )

            TelemetryPanel(
                telemetry = telemetry,
                coordinateDisplayMode = settings.coordinateDisplayMode,
                elevationDisplayUnit = settings.elevationDisplayUnit,
                onCoordinateDisplayMode = viewModel::setCoordinateDisplayMode,
                onElevationDisplayUnit = viewModel::setElevationDisplayUnit,
            )
        }
    }
}

@Composable
private fun FirstRunGuidanceCard(onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("First setup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Configure at least one position provider in Settings before starting the location beacon.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun StatusCard(settings: Settings) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Status", style = MaterialTheme.typography.titleMedium)
            Text(
                if (!LocationProviderRegistry.hasEnabledProvider(settings)) {
                    "No position provider configured - open Settings"
                } else {
                    configuredProviderText(settings)
                },
            )
            if (settings.reportingOn) {
                Text(
                    settings.beaconExpiresAt?.let { "Expires at ${formatLocalTime(it)}" }
                        ?: "Running indefinitely",
                )
            }
        }
    }
}

@Composable
private fun BeaconControls(
    settings: Settings,
    onToggle: () -> Unit,
) {
    val enabled = settings.reportingOn || LocationProviderRegistry.hasEnabledProvider(settings)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onToggle,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (settings.reportingOn) Color(0xFFB3261E) else Color(0xFF1B8F3A),
                contentColor = Color.White,
            ),
        ) {
            Text(if (settings.reportingOn) "Stop beacon" else "Start beacon")
        }
    }
}

@Composable
private fun TelemetryPanel(
    telemetry: TelemetryState,
    coordinateDisplayMode: CoordinateDisplayMode,
    elevationDisplayUnit: ElevationDisplayUnit,
    onCoordinateDisplayMode: (CoordinateDisplayMode) -> Unit,
    onElevationDisplayUnit: (ElevationDisplayUnit) -> Unit,
) {
    val lat = telemetry.lat
    val lon = telemetry.lon
    val positionText = positionText(
        lat = lat,
        lon = lon,
        mode = coordinateDisplayMode,
    )
    val gridText = if (lat != null && lon != null) {
        formatMaidenheadGrid(lat, lon)
    } else {
        "--"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryValue(
                value = positionText,
                onClick = if (lat != null && lon != null) {
                    {
                        onCoordinateDisplayMode(coordinateDisplayMode.next())
                    }
                } else {
                    null
                },
            )
            TelemetryRow("Grid square", gridText)
            TelemetryRow("Speed", telemetry.mph?.let { "${it.format(1)} mph" } ?: "--")
            TelemetryRow("Heading", telemetry.bearing?.let { "${it.format(0)} deg" } ?: "--")
            TelemetryRow(
                label = "Elevation",
                value = elevationText(telemetry.elev, elevationDisplayUnit),
                onClick = if (telemetry.elev != null) {
                    {
                        onElevationDisplayUnit(elevationDisplayUnit.next())
                    }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(4.dp))
            ProviderStatusRows(telemetry.providerStatuses)
        }
    }
}

@Composable
private fun ProviderStatusRows(providerStatuses: Map<String, ProviderTelemetry>) {
    if (providerStatuses.isEmpty()) {
        Text("Provider updates: none yet", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        providerStatuses.toSortedMap().forEach { (provider, status) ->
            val text = if (status.success) {
                "${formatLocalTime(status.lastReportAt)} OK"
            } else {
                val message = status.message?.let { " ($it)" }.orEmpty()
                "${formatLocalTime(status.lastReportAt)} failed$message"
            }
            TelemetryRow(provider, text)
        }
    }
}

@Composable
private fun TelemetryValue(value: String, onClick: (() -> Unit)?) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Text(
        text = value,
        modifier = modifier,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    TelemetryRow(label = label, value = value, onClick = null)
}

@Composable
private fun TelemetryRow(label: String, value: String, onClick: (() -> Unit)?) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StopBeaconDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stop beacon?") },
        text = { Text("This will turn off the active beacon.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Stop") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DurationPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var selected by remember { mutableStateOf(-1L) }
    val options = runtimeDurationOptions()
    val dialogContainer = Color(0xFF20242C)
    val dialogText = Color(0xFFE8EAED)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainer,
        titleContentColor = dialogText,
        textContentColor = dialogText,
        title = { Text("Beacon duration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = selected == option.durationMs
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selected = option.durationMs },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                dialogText
                            },
                        ),
                    ) {
                        Text(option.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("Start", color = dialogText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = dialogText)
            }
        },
    )
}

private fun positionText(lat: Double?, lon: Double?, mode: CoordinateDisplayMode): String {
    if (lat == null || lon == null) return "--"
    return when (mode) {
        CoordinateDisplayMode.ARC -> "${formatDmsLatitude(lat)} / ${formatDmsLongitude(lon)}"
        CoordinateDisplayMode.DECIMAL -> "${formatDecimalCoordinate(lat)}, ${formatDecimalCoordinate(lon)}"
    }
}

private fun CoordinateDisplayMode.next(): CoordinateDisplayMode = when (this) {
    CoordinateDisplayMode.DECIMAL -> CoordinateDisplayMode.ARC
    CoordinateDisplayMode.ARC -> CoordinateDisplayMode.DECIMAL
}

private fun elevationText(elevationMeters: Double?, unit: ElevationDisplayUnit): String {
    if (elevationMeters == null) return "--"
    return when (unit) {
        ElevationDisplayUnit.FEET -> "${(elevationMeters * 3.28084).format(0)} ft"
        ElevationDisplayUnit.METERS -> "${elevationMeters.format(0)} m"
    }
}

private fun ElevationDisplayUnit.next(): ElevationDisplayUnit = when (this) {
    ElevationDisplayUnit.METERS -> ElevationDisplayUnit.FEET
    ElevationDisplayUnit.FEET -> ElevationDisplayUnit.METERS
}

private fun configuredProviderText(settings: Settings): String {
    val providers = LocationProviderRegistry.providerNames(settings)
    return "Configured: ${providers.joinToString(", ")}"
}

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.US, this)

private fun formatLocalTime(epochMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
