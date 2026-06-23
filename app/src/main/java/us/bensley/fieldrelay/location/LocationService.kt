package us.bensley.fieldrelay.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import us.bensley.fieldrelay.R
import us.bensley.fieldrelay.di.ServiceLocator
import us.bensley.fieldrelay.ui.MainActivity
import us.bensley.fieldrelay.widget.FieldRelayWidgetUpdater
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LocationService : Service() {

    companion object {
        const val ACTION_START = "us.bensley.fieldrelay.action.START"
        const val ACTION_STOP = "us.bensley.fieldrelay.action.STOP"
        const val EXTRA_DURATION_MS = "us.bensley.fieldrelay.extra.DURATION_MS"
        const val CHANNEL_ID = "fieldrelay_location"
        const val NOTIFICATION_ID = 1

        fun start(ctx: Context, durationMs: Long = -1L) {
            ContextCompat.startForegroundService(
                ctx,
                Intent(ctx, LocationService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_DURATION_MS, durationMs),
            )
        }

        fun stop(ctx: Context) {
            ctx.startService(
                Intent(ctx, LocationService::class.java).setAction(ACTION_STOP),
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var client: FusedLocationProviderClient
    private var callback: LocationCallback? = null
    private var intervalJob: Job? = null
    private var expiryJob: Job? = null
    private var currentIntervalMs: Long = 30_000L
    private var expiresAt: Long? = null
    private var providerStatuses: Map<String, ProviderTelemetry> = emptyMap()
    private var isReporting = false
    private var reportInFlight = false
    private var lastWidgetPositionText: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        client = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopReporting()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val durationMs = intent?.getLongExtra(EXTRA_DURATION_MS, -1L) ?: -1L
                startReporting(durationMs)
            }
            else -> resumeReportingFromSavedState()
        }
        return START_STICKY
    }

    private fun resumeReportingFromSavedState() {
        val settings = runBlocking(Dispatchers.IO) { ServiceLocator.settings.settings.first() }
        val savedExpiry = settings.beaconExpiresAt
        val now = System.currentTimeMillis()
        if (!settings.reportingOn) {
            stopSelf()
            return
        }
        if (savedExpiry != null && savedExpiry <= now) {
            persistBeaconState(reportingOn = false, expiresAt = null)
            clearWidgetLocation()
            refreshWidgetBlocking()
            stopSelf()
            return
        }
        startReporting(savedExpiry?.minus(now) ?: -1L)
    }

    private fun startReporting(durationMs: Long) {
        val settings = runBlocking(Dispatchers.IO) { ServiceLocator.settings.settings.first() }
        if (!LocationProviderRegistry.hasEnabledProvider(settings)) {
            persistBeaconState(reportingOn = false, expiresAt = null)
            clearWidgetLocation()
            refreshWidgetBlocking()
            stopSelf()
            return
        }

        expiresAt = expiresAtForDuration(System.currentTimeMillis(), durationMs)
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } catch (_: RuntimeException) {
            expiresAt = null
            persistBeaconState(reportingOn = false, expiresAt = null)
            clearWidgetLocation()
            refreshWidgetBlocking()
            stopSelf()
            return
        }

        persistBeaconState(reportingOn = true, expiresAt = expiresAt)
        scheduleExpiryStop()

        if (!isReporting) {
            isReporting = true
            refreshWidget()
            intervalJob = scope.launch {
                ServiceLocator.settings.settings
                    .map { it.intervalMs }
                    .distinctUntilChanged()
                    .collect { interval ->
                        currentIntervalMs = interval
                        requestUpdates(interval)
                    }
            }
        } else {
            refreshNotification()
        }
    }

    private fun stopReporting() {
        if (!isReporting) {
            persistBeaconState(reportingOn = false, expiresAt = null)
            clearWidgetLocation()
            refreshWidgetBlocking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        isReporting = false
        intervalJob?.cancel()
        intervalJob = null
        expiryJob?.cancel()
        expiryJob = null
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
        expiresAt = null
        providerStatuses = emptyMap()
        reportInFlight = false
        lastWidgetPositionText = null
        persistBeaconState(reportingOn = false, expiresAt = null)
        LocationStateHolder.reset()
        clearWidgetLocation()
        refreshWidgetBlocking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun persistBeaconState(reportingOn: Boolean, expiresAt: Long?) {
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(1_000) {
                ServiceLocator.settings.setBeaconState(reportingOn = reportingOn, expiresAt = expiresAt)
            }
        }
    }

    private fun scheduleExpiryStop() {
        expiryJob?.cancel()
        val deadline = expiresAt ?: return
        val delayMs = deadline - System.currentTimeMillis()
        if (delayMs <= 0L) {
            stopReporting()
            return
        }
        expiryJob = scope.launch {
            delay(delayMs)
            withContext(Dispatchers.Main) {
                expiryJob = null
                stopReporting()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestUpdates(intervalMs: Long) {
        callback?.let { client.removeLocationUpdates(it) }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                onFix(loc)
            }
        }
        callback = cb
        try {
            client.requestLocationUpdates(request, cb, mainLooper)
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && loc.isRecentEnoughForImmediateDisplay()) onFix(loc)
            }
        } catch (_: SecurityException) {
            val permissionStatus = ProviderTelemetry(
                    lastReportAt = System.currentTimeMillis(),
                    success = false,
                    message = "Permission revoked",
                )
            providerStatuses = providerStatuses + ("Location" to permissionStatus)
            refreshNotification()
            stopReporting()
        }
    }

    private fun onFix(loc: Location) {
        val now = System.currentTimeMillis()
        val exp = expiresAt
        if (exp != null && now >= exp) {
            stopReporting()
            return
        }
        LocationStateHolder.updateFix(loc)
        scope.launch {
            ServiceLocator.settings.setWidgetLocation(loc.latitude, loc.longitude)
            refreshWidgetIfPositionChanged(loc)
        }
        if (reportInFlight) return
        reportInFlight = true

        scope.launch {
            try {
                val settings = ServiceLocator.settings.settings.first()
                val results = ServiceLocator.positionReportingCoordinator.reportPosition(
                    fix = loc.toLocationFix(),
                    reportAtUtc = utcTimestamp(),
                    settings = settings,
                )
                val updatedStatuses = providerStatuses + results.associate { result ->
                    result.providerId to ProviderTelemetry(
                        lastReportAt = now,
                        success = result.success,
                        message = result.message,
                    )
                }
                providerStatuses = updatedStatuses
                LocationStateHolder.updateReport(loc, results, now)
                refreshNotification()
            } finally {
                reportInFlight = false
            }
        }
    }

    private fun refreshWidget() {
        scope.launch {
            FieldRelayWidgetUpdater.updateAll(this@LocationService)
        }
    }

    private fun refreshWidgetBlocking() {
        FieldRelayWidgetUpdater.updateAllBlocking(this)
    }

    private suspend fun refreshWidgetIfPositionChanged(loc: Location) {
        val positionText = "${formatDecimalCoordinate(loc.latitude)}, ${formatDecimalCoordinate(loc.longitude)}"
        if (positionText != lastWidgetPositionText) {
            lastWidgetPositionText = positionText
            FieldRelayWidgetUpdater.updateAll(this@LocationService)
        }
    }

    private fun clearWidgetLocation() {
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(1_000) {
                ServiceLocator.settings.setWidgetLocation(null, null)
            }
        }
    }

    private fun refreshNotification() {
        val mgr = NotificationManagerCompat.from(this)
        if (!mgr.areNotificationsEnabled()) return
        try {
            mgr.notify(NOTIFICATION_ID, buildNotification())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied after start; foreground notification remains posted by system.
        }
    }

    private fun buildNotification(): Notification {
        val text = buildLocationNotificationText(
            expiresAt = expiresAt,
            providerStatuses = providerStatuses,
            formatTime = ::formatLocal,
        )

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FieldRelay")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_crosshair)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Beacon",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Provides location updates to enabled providers"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
        scope.cancel()
        super.onDestroy()
    }

    private fun utcTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

    private fun formatLocal(epochMs: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

    private fun Location.toLocationFix(): LocationFix = LocationFix(
        lat = latitude,
        lon = longitude,
        elev = altitude,
        metersPerSecond = speed.toDouble(),
        bearing = bearing.toDouble(),
    )

    private fun Location.isRecentEnoughForImmediateDisplay(maxAgeMs: Long = 120_000L): Boolean {
        val ageMs = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
        return ageMs in 0..maxAgeMs
    }
}
