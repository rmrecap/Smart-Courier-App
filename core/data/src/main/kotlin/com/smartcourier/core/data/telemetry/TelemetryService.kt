package com.smartcourier.core.data.telemetry

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class TelemetryService : Service() {

    @Inject lateinit var telemetryDataSource: RemoteTelemetryDataSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var countryCode: String = ""
    private var userId: String = ""

    private var lastUpdateTime = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                val intervalMs = adaptiveInterval(location.speed)
                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= intervalMs) {
                    lastUpdateTime = now
                    scope.launch { pushLocation(location) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = FusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        countryCode = intent?.getStringExtra(EXTRA_COUNTRY_CODE).orEmpty()
        userId = intent?.getStringExtra(EXTRA_USER_ID).orEmpty()

        if (countryCode.isBlank() || userId.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        startTracking()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        scope.cancel()
        super.onDestroy()
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, MIN_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private suspend fun pushLocation(location: Location) {
        ensureActive()
        val payload = TelemetryPayload(
            c = "${location.latitude},${location.longitude}",
            b = location.bearing.roundToInt(),
            h = if (location.hasSpeed()) (location.speed * 3.6).roundToInt() else 0,
            t = location.time
        )
        telemetryDataSource.updateLocation(countryCode, userId, payload)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Live Tracking", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for live courier location tracking"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, TelemetryService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Active")
            .setContentText("Updating location in real-time")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "telemetry_tracking"
        const val NOTIFICATION_ID = 9001
        const val ACTION_STOP = "com.smartcourier.action.STOP_TRACKING"
        const val EXTRA_COUNTRY_CODE = "country_code"
        const val EXTRA_USER_ID = "user_id"

        private const val MIN_INTERVAL_MS = 2000L
        private const val SPEED_THRESHOLD_VEHICLE = 10f
        private const val SPEED_THRESHOLD_RUNNING = 2f
        private const val INTERVAL_VEHICLE_MS = 2000L
        private const val INTERVAL_RUNNING_MS = 5000L
        private const val INTERVAL_WALKING_MS = 10000L
        private const val INTERVAL_STOPPED_MS = 30000L

        fun adaptiveInterval(speedMps: Float): Long = when {
            speedMps >= SPEED_THRESHOLD_VEHICLE -> INTERVAL_VEHICLE_MS
            speedMps >= SPEED_THRESHOLD_RUNNING -> INTERVAL_RUNNING_MS
            speedMps > 0f -> INTERVAL_WALKING_MS
            else -> INTERVAL_STOPPED_MS
        }

        fun start(context: Context, countryCode: String, userId: String) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                putExtra(EXTRA_COUNTRY_CODE, countryCode)
                putExtra(EXTRA_USER_ID, userId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TelemetryService::class.java))
        }
    }
}
