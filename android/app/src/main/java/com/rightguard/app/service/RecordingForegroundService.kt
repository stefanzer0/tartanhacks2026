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
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.data.repository.RecordingRepository
import com.rightguard.app.domain.model.RecordingStatus
import com.rightguard.app.ui.MainActivity
import com.rightguard.app.util.EncryptedFileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingForegroundService : LifecycleService() {

    @Inject lateinit var encryptedFileManager: EncryptedFileManager
    @Inject lateinit var recordingRepository: RecordingRepository
    @Inject lateinit var incidentRepository: IncidentRepository

    private var activeRecording: Recording? = null
    private var currentRecordingId: Long = 0
    private var currentIncidentId: Long = 0
    private var videoCapture: VideoCapture<Recorder>? = null

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.rightguard.START_RECORDING"
        const val ACTION_STOP = "com.rightguard.STOP_RECORDING"
        const val EXTRA_INCIDENT_ID = "incident_id"

        fun startRecording(context: Context, incidentId: Long) {
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INCIDENT_ID, incidentId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
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
                currentIncidentId = intent.getLongExtra(EXTRA_INCIDENT_ID, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                startVideoRecording()
            }
            ACTION_STOP -> {
                stopVideoRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVideoRecording() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)

                val filename = encryptedFileManager.generateRecordingFilename(currentIncidentId)
                val file = encryptedFileManager.getRecordingFile(filename)
                val outputOptions = FileOutputOptions.Builder(file).build()

                activeRecording = videoCapture?.output
                    ?.prepareRecording(this, outputOptions)
                    ?.withAudioEnabled()
                    ?.start(ContextCompat.getMainExecutor(this)) { event ->
                        handleRecordingEvent(event, filename)
                    }

                // Save recording metadata
                lifecycleScope.launch {
                    val recording = com.rightguard.app.domain.model.Recording(
                        incidentId = currentIncidentId,
                        filePath = filename,
                        startTime = System.currentTimeMillis(),
                        status = RecordingStatus.RECORDING
                    )
                    currentRecordingId = recordingRepository.createRecording(recording)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleRecordingEvent(event: VideoRecordEvent, filename: String) {
        when (event) {
            is VideoRecordEvent.Finalize -> {
                lifecycleScope.launch {
                    val recording = recordingRepository.getById(currentRecordingId)
                    if (recording != null) {
                        val updated = recording.copy(
                            endTime = System.currentTimeMillis(),
                            status = if (event.hasError()) RecordingStatus.FAILED else RecordingStatus.STOPPED,
                            durationMs = System.currentTimeMillis() - recording.startTime
                        )
                        recordingRepository.updateRecording(updated)
                    }
                }
            }
        }
    }

    private fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active recording notification"
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
            .setContentTitle("RightGuard Active")
            .setContentText("Recording encounter...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        stopVideoRecording()
        super.onDestroy()
    }
}
