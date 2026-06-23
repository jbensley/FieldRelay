package us.bensley.fieldrelay.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import us.bensley.fieldrelay.location.TelemetryState
import us.bensley.fieldrelay.location.formatDecimalCoordinate
import us.bensley.fieldrelay.report.ReportDirection
import us.bensley.fieldrelay.report.ReportLocation
import us.bensley.fieldrelay.report.SevereReportDraft
import us.bensley.fieldrelay.report.applyOffset
import us.bensley.fieldrelay.report.hailSizeOptions
import us.bensley.fieldrelay.report.reportDistanceOptions
import us.bensley.fieldrelay.ui.common.ResponsiveContent
import us.bensley.fieldrelay.ui.report.ReportSubmitState
import us.bensley.fieldrelay.ui.report.ReportViewModel
import us.bensley.fieldrelay.weather.WeatherProviderRegistry

@Composable
fun ReportScreen(
    weatherReportingAvailable: Boolean,
    viewModel: ReportViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val providerNames = WeatherProviderRegistry.providerNames(settings)
    var draft by remember { mutableStateOf(SevereReportDraft()) }
    var pendingValidationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(submitState) {
        if (submitState is ReportSubmitState.Submitted) {
            draft = SevereReportDraft()
            pendingValidationErrors = emptyList()
            showConfirm = false
        }
    }

    ResponsiveContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Weather Report", style = MaterialTheme.typography.headlineMedium)

            if (!weatherReportingAvailable) {
                WeatherReportingUnavailableCard()
                return@Column
            }

            Text(
                text = "Submitting to: ${providerNames.joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
            )

            ReportLocationCard(telemetry = telemetry, draft = draft)
            ReportTypeCard(draft = draft, onDraft = { draft = it })
            ReportDetailsCard(draft = draft, onDraft = { draft = it })
            SubmissionCard(draft = draft, onDraft = { draft = it })

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = submitState !is ReportSubmitState.Submitting,
                onClick = {
                    val errors = viewModel.validationErrors(draft)
                    if (errors.isEmpty()) {
                        showConfirm = true
                    } else {
                        pendingValidationErrors = errors
                    }
                },
            ) {
                Text(if (submitState is ReportSubmitState.Submitting) "Submitting..." else "Review and submit")
            }
        }
    }

    if (pendingValidationErrors.isNotEmpty()) {
        MessageDialog(
            title = "Weather report needs attention",
            message = pendingValidationErrors.joinToString("\n"),
            onDismiss = { pendingValidationErrors = emptyList() },
        )
    }

    if (showConfirm) {
        ConfirmReportDialog(
            draft = draft,
            telemetry = telemetry,
            onDismiss = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                viewModel.submit(draft)
            },
        )
    }

    when (val state = submitState) {
        ReportSubmitState.Idle,
        ReportSubmitState.Submitting -> Unit
        ReportSubmitState.Submitted -> MessageDialog(
            title = "Weather report submitted",
            message = "Spotter Network accepted the weather report.",
            onDismiss = {
                viewModel.clearSubmitState()
            },
        )
        is ReportSubmitState.Failed -> MessageDialog(
            title = "Weather report not submitted",
            message = state.message,
            onDismiss = { viewModel.clearSubmitState() },
        )
    }
}

@Composable
private fun WeatherReportingUnavailableCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "No weather provider configured",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Configure Spotter Network in Settings to submit weather reports. Future weather providers such as mPING should appear here when configured.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ReportLocationCard(telemetry: TelemetryState, draft: SevereReportDraft) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Location", style = MaterialTheme.typography.titleMedium)
            Text(reportLocationText(telemetry, draft))
            Text("Weather reports use the current GPS fix unless an offset is selected below.")
        }
    }
}

@Composable
private fun ReportTypeCard(
    draft: SevereReportDraft,
    onDraft: (SevereReportDraft) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("What are you reporting?", style = MaterialTheme.typography.titleMedium)
            CheckRow("Tornado", draft.tornado) { onDraft(draft.copy(tornado = it)) }
            CheckRow("Funnel cloud", draft.funnelCloud) { onDraft(draft.copy(funnelCloud = it)) }
            CheckRow("Wall cloud / rotation", draft.wallCloud || draft.rotation) {
                onDraft(draft.copy(wallCloud = it, rotation = it))
            }
            CheckRow("Hail", draft.hail) { onDraft(draft.copy(hail = it)) }
            CheckRow("Measured severe wind", draft.wind) { onDraft(draft.copy(wind = it)) }
            CheckRow("Flooding", draft.flood) { onDraft(draft.copy(flood = it)) }
            CheckRow("Flash flooding", draft.flashFlood) { onDraft(draft.copy(flashFlood = it)) }
            CheckRow("Damage", draft.damage) { onDraft(draft.copy(damage = it)) }
            CheckRow("Injury", draft.injury) { onDraft(draft.copy(injury = it)) }
            CheckRow("Other severe impact", draft.other) { onDraft(draft.copy(other = it)) }
        }
    }
}

@Composable
private fun ReportDetailsCard(
    draft: SevereReportDraft,
    onDraft: (SevereReportDraft) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Details", style = MaterialTheme.typography.titleMedium)

            if (draft.tornado || draft.funnelCloud || draft.wallCloud || draft.rotation) {
                ToggleRow("Use location offset", draft.offsetEnabled) { enabled ->
                    onDraft(
                        if (enabled) {
                            draft.copy(offsetEnabled = true)
                        } else {
                            draft.copy(
                                offsetEnabled = false,
                                offsetDistanceMiles = null,
                                offsetDirection = null,
                            )
                        },
                    )
                }
                if (draft.offsetEnabled) {
                    Text("Distance from current location")
                    DistanceMenu(
                        selected = draft.offsetDistanceMiles,
                        onSelect = { onDraft(draft.copy(offsetDistanceMiles = it)) },
                    )
                    Text("Direction from current location")
                    DirectionCompassSelector(
                        selected = draft.offsetDirection,
                        onSelect = { onDraft(draft.copy(offsetDirection = it)) },
                    )
                }
            }

            if (draft.hail) {
                HailSizeMenu(
                    selected = draft.hailSizeInches,
                    onSelect = { onDraft(draft.copy(hailSizeInches = it)) },
                )
            }

            if (draft.wind) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft.windSpeedMph?.toString() ?: "",
                    onValueChange = { onDraft(draft.copy(windSpeedMph = it.toIntOrNull())) },
                    label = { Text("Measured wind speed / gust mph") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            if (draft.flood || draft.flashFlood) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft.floodDepthInches?.toString() ?: "",
                    onValueChange = { onDraft(draft.copy(floodDepthInches = it.toIntOrNull())) },
                    label = { Text("Water depth in inches") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            Text("Narrative is generated from the selected report details.")
        }
    }
}

@Composable
private fun DistanceMenu(selected: Double?, onSelect: (Double) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selected?.let { "${it.formatDistance()} mi" } ?: "Choose distance"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
        ) {
            Text(selectedLabel)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            reportDistanceOptions().forEach { distance ->
                DropdownMenuItem(
                    text = { Text("${distance.formatDistance()} mi") },
                    onClick = {
                        expanded = false
                        onSelect(distance)
                    },
                )
            }
        }
    }
}

@Composable
private fun DirectionCompassSelector(
    selected: ReportDirection?,
    onSelect: (ReportDirection) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DirectionCompassRow(
            directions = listOf(ReportDirection.NW, ReportDirection.N, ReportDirection.NE),
            selected = selected,
            onSelect = onSelect,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DirectionButton(
                direction = ReportDirection.W,
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
            DirectionButton(
                direction = ReportDirection.E,
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
        DirectionCompassRow(
            directions = listOf(ReportDirection.SW, ReportDirection.S, ReportDirection.SE),
            selected = selected,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun DirectionCompassRow(
    directions: List<ReportDirection>,
    selected: ReportDirection?,
    onSelect: (ReportDirection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        directions.forEach { direction ->
            DirectionButton(
                direction = direction,
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DirectionButton(
    direction: ReportDirection,
    selected: ReportDirection?,
    onSelect: (ReportDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selected == direction
    OutlinedButton(
        modifier = modifier,
        onClick = { onSelect(direction) },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Text(direction.label)
    }
}

@Composable
private fun HailSizeMenu(selected: Double?, onSelect: (Double) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = hailSizeOptions()
        .firstOrNull { it.inches == selected }
        ?.label
        ?: "Choose hail size"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
        ) {
            Text(selectedLabel)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            hailSizeOptions().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option.inches)
                    },
                )
            }
        }
    }
}

@Composable
private fun SubmissionCard(
    draft: SevereReportDraft,
    onDraft: (SevereReportDraft) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Submission", style = MaterialTheme.typography.titleMedium)
            CheckRow(
                label = "This is first-hand and happening now",
                checked = draft.firstHandConfirmed,
                onChecked = { onDraft(draft.copy(firstHandConfirmed = it)) },
            )
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ConfirmReportDialog(
    draft: SevereReportDraft,
    telemetry: TelemetryState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit weather report?") },
        text = {
            Text(
                "This will submit a first-hand, current weather report to Spotter Network at ${reportLocationText(telemetry, draft)}.\n\n${reportSummary(draft)}",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

private fun reportLocationText(telemetry: TelemetryState, draft: SevereReportDraft): String {
    val lat = telemetry.lat
    val lon = telemetry.lon
    return if (lat != null && lon != null) {
        val reportLocation = if (draft.offsetEnabled) {
            applyOffset(lat, lon, draft.offsetDistanceMiles, draft.offsetDirection)
        } else {
            ReportLocation(lat, lon)
        }
        "${formatDecimalCoordinate(reportLocation.latitude)}, ${formatDecimalCoordinate(reportLocation.longitude)}"
    } else {
        "No GPS fix yet"
    }
}

private fun reportSummary(draft: SevereReportDraft): String {
    val types = listOfNotNull(
        "tornado".takeIf { draft.tornado },
        "funnel cloud".takeIf { draft.funnelCloud },
        "wall cloud / rotation".takeIf { draft.wallCloud || draft.rotation },
        "hail".takeIf { draft.hail },
        "measured wind".takeIf { draft.wind },
        "flooding".takeIf { draft.flood },
        "flash flooding".takeIf { draft.flashFlood },
        "damage".takeIf { draft.damage },
        "injury".takeIf { draft.injury },
        "other".takeIf { draft.other },
    ).joinToString(", ")
    val offset = if (draft.offsetEnabled && draft.offsetDistanceMiles != null && draft.offsetDirection != null) {
        "\nOffset: ${draft.offsetDistanceMiles.formatDistance()} mi ${draft.offsetDirection.label} of current location"
    } else {
        ""
    }
    return "Types: $types$offset"
}

private fun Double.formatDistance(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
