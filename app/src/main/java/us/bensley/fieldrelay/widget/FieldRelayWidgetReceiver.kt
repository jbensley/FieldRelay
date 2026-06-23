package us.bensley.fieldrelay.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import us.bensley.fieldrelay.R
import us.bensley.fieldrelay.data.CoordinateDisplayMode
import us.bensley.fieldrelay.data.Settings
import us.bensley.fieldrelay.di.ServiceLocator
import us.bensley.fieldrelay.location.LocationProviderId
import us.bensley.fieldrelay.location.LocationProviderRegistry
import us.bensley.fieldrelay.location.formatDecimalCoordinate
import us.bensley.fieldrelay.location.formatDmsLatitude
import us.bensley.fieldrelay.location.formatDmsLongitude
import us.bensley.fieldrelay.ui.MainActivity
import us.bensley.fieldrelay.weather.WeatherProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class FieldRelayWidgetReceiver : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OPEN_HOME -> {
                openMainActivity(context.applicationContext)
            }
            ACTION_OPEN_REPORT -> {
                openMainActivity(context.applicationContext, openReport = true)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(1_500) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        private const val ACTION_OPEN_HOME = "us.bensley.fieldrelay.widget.OPEN_HOME"
        private const val ACTION_OPEN_REPORT = "us.bensley.fieldrelay.widget.OPEN_REPORT"

        suspend fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FieldRelayWidgetReceiver::class.java))
            updateWidgets(context, manager, ids)
        }

        private suspend fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
        ) {
            if (ids.isEmpty()) return
            val settings = ServiceLocator.settings.settings.first()
            ids.forEach { id ->
                manager.updateAppWidget(id, buildViews(context, settings))
            }
        }

        private fun buildViews(context: Context, settings: Settings): RemoteViews =
            RemoteViews(context.packageName, R.layout.fieldrelay_widget).apply {
                val enabledProviderIds = LocationProviderRegistry.enabledProviderIds(settings)
                val weatherReportingAvailable = WeatherProviderRegistry.hasConfiguredProvider(settings)
                setTextViewText(R.id.widget_latitude_value, widgetLatitudeText(settings))
                setTextViewText(R.id.widget_longitude_value, widgetLongitudeText(settings))
                setTextViewText(R.id.widget_sn_status, "SN")
                setTextViewText(R.id.widget_aprs_status, "APRS")
                setTextColor(
                    R.id.widget_sn_status,
                    if (settings.reportingOn && LocationProviderId.SPOTTER_NETWORK in enabledProviderIds) {
                        0xFF2ECC71.toInt()
                    } else {
                        0xFF9AA0A6.toInt()
                    },
                )
                setTextColor(
                    R.id.widget_aprs_status,
                    if (settings.reportingOn && LocationProviderId.APRS in enabledProviderIds) {
                        0xFF2ECC71.toInt()
                    } else {
                        0xFF9AA0A6.toInt()
                    },
                )
                setOnClickPendingIntent(R.id.widget_toggle_area, openHomePendingIntent(context))
                setViewVisibility(
                    R.id.widget_report_divider,
                    if (weatherReportingAvailable) View.VISIBLE else View.GONE,
                )
                setViewVisibility(
                    R.id.widget_report_area,
                    if (weatherReportingAvailable) View.VISIBLE else View.GONE,
                )
                if (weatherReportingAvailable) {
                    setOnClickPendingIntent(R.id.widget_report_area, openReportPendingIntent(context))
                }
            }

        private fun widgetLatitudeText(settings: Settings): String =
            settings.widgetLat?.let {
                when (settings.coordinateDisplayMode) {
                    CoordinateDisplayMode.DECIMAL -> formatDecimalCoordinate(it)
                    CoordinateDisplayMode.ARC -> formatDmsLatitude(it)
                }
            } ?: "--"

        private fun widgetLongitudeText(settings: Settings): String =
            settings.widgetLon?.let {
                when (settings.coordinateDisplayMode) {
                    CoordinateDisplayMode.DECIMAL -> formatDecimalCoordinate(it)
                    CoordinateDisplayMode.ARC -> formatDmsLongitude(it)
                }
            } ?: "--"

        private fun openMainActivity(
            context: Context,
            showDurationDialog: Boolean = false,
            confirmStopDialog: Boolean = false,
            requestPermissions: Boolean = false,
            openReport: Boolean = false,
        ) {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("show_duration_dialog", showDurationDialog)
                    .putExtra("confirm_stop_dialog", confirmStopDialog)
                    .putExtra("request_permissions", requestPermissions)
                    .putExtra("open_report", openReport),
            )
        }

        private fun openHomePendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                100,
                Intent(context, FieldRelayWidgetReceiver::class.java).setAction(ACTION_OPEN_HOME),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun openReportPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                101,
                Intent(context, FieldRelayWidgetReceiver::class.java).setAction(ACTION_OPEN_REPORT),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

    }
}
