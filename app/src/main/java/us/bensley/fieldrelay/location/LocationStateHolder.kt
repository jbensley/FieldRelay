package us.bensley.fieldrelay.location

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocationStateHolder {
    private val _state = MutableStateFlow(TelemetryState())
    val state: StateFlow<TelemetryState> = _state

    fun updateFix(loc: Location) {
        val current = _state.value
        _state.value = current.copy(
            lat = loc.latitude,
            lon = loc.longitude,
            mph = loc.speed * 2.23694,
            bearing = loc.bearing.toDouble(),
            elev = loc.altitude,
        )
    }

    fun updateReport(loc: Location, results: List<PositionReportResult>, reportAtMs: Long) {
        val currentStatuses = _state.value.providerStatuses
        val updatedStatuses = currentStatuses + results.associate { result ->
            result.providerId to ProviderTelemetry(
                lastReportAt = reportAtMs,
                success = result.success,
                message = result.message,
            )
        }
        _state.value = TelemetryState(
            lat = loc.latitude,
            lon = loc.longitude,
            mph = loc.speed * 2.23694,
            bearing = loc.bearing.toDouble(),
            elev = loc.altitude,
            providerStatuses = updatedStatuses,
        )
    }

    fun reset() {
        _state.value = TelemetryState()
    }
}
