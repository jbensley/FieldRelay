package us.bensley.fieldrelay.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import us.bensley.fieldrelay.data.SettingsRepository
import us.bensley.fieldrelay.location.LocationService
import us.bensley.fieldrelay.permissions.canStartUnattendedLocationReporting
import us.bensley.fieldrelay.widget.FieldRelayWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import us.bensley.fieldrelay.permissions.canStartUnattendedLocationReporting

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo =
                    us.bensley.fieldrelay.data.SettingsRepository(context.applicationContext)
                val settings = repo.settings.first()
                when (val decision = resolveBootResume(settings, System.currentTimeMillis())) {
                    BootResumeDecision.ClearExpired -> {
                        clearSavedBeacon(repo, context.applicationContext)
                    }
                    is BootResumeDecision.Start -> {
                        if (!context.canStartUnattendedLocationReporting()) {
                            clearSavedBeacon(repo, context.applicationContext)
                        } else try {
                            us.bensley.fieldrelay.location.LocationService.Companion.start(context, decision.durationMs)
                        } catch (_: RuntimeException) {
                            clearSavedBeacon(repo, context.applicationContext)
                        }
                    }
                    BootResumeDecision.Skip -> Unit
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun clearSavedBeacon(repo: us.bensley.fieldrelay.data.SettingsRepository, context: Context) {
        repo.setBeaconState(reportingOn = false, expiresAt = null)
        repo.setWidgetLocation(null, null)
        us.bensley.fieldrelay.widget.FieldRelayWidgetUpdater.updateAll(context)
    }
}
