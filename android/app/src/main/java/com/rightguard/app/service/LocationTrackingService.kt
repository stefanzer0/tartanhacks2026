package com.rightguard.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.ui.MainActivity
import com.rightguard.app.util.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {

    @Inject lateinit var locationHelper: LocationHelper
    @Inject lateinit var incidentRepository: IncidentRepository

    private var locationJob: Job? = null
    private var incidentId: Long = 0

    companion object {
        const val CHANNEL_ID = "location_channel"
        const val NOTIFICATION_ID = 1003
        const val ACTION_START = "com.rightguard.START_LOCATION"
        const val ACTION_STOP = "com.rightguard.STOP_LOCATION"
        const val EXTRA_INCIDENT_ID = "incident_id"

        fun start(context: Context, incidentId: Long) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INCIDENT_ID, incidentId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                incidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                startTracking()
            }
            ACTION_STOP -> {
                locationJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        locationJob = lifecycleScope.launch {
            locationHelper.getLocationUpdates(intervalMs = 10_000).collect { location ->
                val incident = incidentRepository.getById(incidentId)
                if (incident != null) {
                    incidentRepository.updateIncident(
                        incident.copy(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Location tracking during incident"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RightGuard")
            .setContentText("Tracking location for safety")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        locationJob?.cancel()
        super.onDestroy()
    }
}
