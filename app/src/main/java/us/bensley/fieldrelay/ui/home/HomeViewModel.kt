package us.bensley.fieldrelay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import us.bensley.fieldrelay.data.CoordinateDisplayMode
import us.bensley.fieldrelay.data.ElevationDisplayUnit
import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.di.ServiceLocator
import us.bensley.fieldrelay.location.LocationStateHolder
import us.bensley.fieldrelay.location.TelemetryState
import us.bensley.fieldrelay.widget.FieldRelayWidgetUpdater
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
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

    fun setCoordinateDisplayMode(value: CoordinateDisplayMode) {
        viewModelScope.launch {
            ServiceLocator.settings.setCoordinateDisplayMode(value)
            FieldRelayWidgetUpdater.updateAll(ServiceLocator.appContext)
        }
    }

    fun setElevationDisplayUnit(value: ElevationDisplayUnit) {
        viewModelScope.launch { ServiceLocator.settings.setElevationDisplayUnit(value) }
    }
}
