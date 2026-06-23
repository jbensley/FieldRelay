package us.bensley.fieldrelay.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.di.ServiceLocator
import us.bensley.fieldrelay.location.LocationStateHolder
import us.bensley.fieldrelay.location.TelemetryState
import us.bensley.fieldrelay.report.SevereReportDraft
import us.bensley.fieldrelay.report.buildSevereReportRequest
import us.bensley.fieldrelay.report.validateSevereReportDraft
import us.bensley.fieldrelay.weather.WeatherProviderRegistry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ReportSubmitState {
    data object Idle : ReportSubmitState
    data object Submitting : ReportSubmitState
    data object Submitted : ReportSubmitState
    data class Failed(val message: String) : ReportSubmitState
}

class ReportViewModel : ViewModel() {
    val settings: StateFlow<Settings> = ServiceLocator.settings.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings.DEFAULT,
    )

    val telemetry: StateFlow<TelemetryState> = combine(
        LocationStateHolder.state,
        settings,
    ) { telemetry, settings ->
        if (telemetry.lat != null && telemetry.lon != null) {
            telemetry
        } else if (settings.widgetLat != null && settings.widgetLon != null) {
            telemetry.copy(lat = settings.widgetLat, lon = settings.widgetLon)
        } else {
            telemetry
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TelemetryState(),
    )

    private val _submitState = MutableStateFlow<ReportSubmitState>(ReportSubmitState.Idle)
    val submitState: StateFlow<ReportSubmitState> = _submitState

    fun validationErrors(draft: SevereReportDraft): List<String> {
        val t = telemetry.value
        val errors = validateSevereReportDraft(
            draft = draft,
            appId = settings.value.appId,
            hasLocation = t.lat != null && t.lon != null,
        )
        return if (WeatherProviderRegistry.hasConfiguredProvider(settings.value)) {
            errors
        } else {
            listOf("Configure a weather reporting provider in Settings before submitting weather reports.") + errors
        }
    }

    fun submit(draft: SevereReportDraft) {
        val t = telemetry.value
        val lat = t.lat
        val lon = t.lon
        val errors = validationErrors(draft)
        if (errors.isNotEmpty() || lat == null || lon == null) {
            _submitState.value = ReportSubmitState.Failed(errors.joinToString("\n"))
            return
        }

        viewModelScope.launch {
            _submitState.value = ReportSubmitState.Submitting
            runCatching {
                ServiceLocator.api.submitSevereReport(
                    buildSevereReportRequest(
                        appId = settings.value.appId,
                        draft = draft,
                        latitude = lat,
                        longitude = lon,
                    ),
                )
            }.fold(
                onSuccess = { response ->
                    _submitState.value = if (response.isSuccessful) {
                        ReportSubmitState.Submitted
                    } else {
                        ReportSubmitState.Failed(response.toReportError())
                    }
                },
                onFailure = { error ->
                    _submitState.value = ReportSubmitState.Failed(error.message ?: "Weather report submission failed.")
                },
            )
        }
    }

    fun clearSubmitState() {
        _submitState.value = ReportSubmitState.Idle
    }
}

private fun retrofit2.Response<Unit>.toReportError(): String {
    val body = errorBody()?.string()?.takeIf { it.isNotBlank() }
    return when (code()) {
        401, 403 -> "Spotter Network rejected this weather report. Confirm your Application ID is valid and your account is approved to submit reports."
        else -> body ?: "Spotter Network rejected this weather report with HTTP ${code()}."
    }
}
