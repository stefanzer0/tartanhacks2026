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
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.rightguard.app.ui.MainActivity
import com.rightguard.app.util.SpeechRecognizerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VoiceCommandService : LifecycleService() {

    private lateinit var speechHelper: SpeechRecognizerHelper
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    companion object {
        const val CHANNEL_ID = "voice_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_START = "com.rightguard.START_VOICE"
        const val ACTION_STOP = "com.rightguard.STOP_VOICE"

        private val _voiceCommands = MutableSharedFlow<VoiceCommand>(extraBufferCapacity = 10)
        val voiceCommands: SharedFlow<VoiceCommand> = _voiceCommands.asSharedFlow()

        private val _transcriptions = MutableSharedFlow<String>(extraBufferCapacity = 10)
        val transcriptions: SharedFlow<String> = _transcriptions.asSharedFlow()

        fun start(context: Context) {
            val intent = Intent(context, VoiceCommandService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceCommandService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        speechHelper = SpeechRecognizerHelper(this)
        speechHelper.initialize()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                startListening()
            }
            ACTION_STOP -> {
                speechHelper.stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startListening() {
        speechHelper.startListening(continuous = true) { text ->
            lifecycleScope.launch {
                val command = parseCommand(text)
                if (command != null) {
                    _voiceCommands.emit(command)
                } else {
                    _transcriptions.emit(text)
                }
            }
        }
    }

    private fun parseCommand(text: String): VoiceCommand? {
        val lower = text.lowercase().trim()
        return when {
            lower.contains("activate safeguard") || lower.contains("start safeguard") -> VoiceCommand.ACTIVATE_SAFEGUARD
            lower.contains("activate panic") || lower.contains("panic mode") -> VoiceCommand.ACTIVATE_PANIC
            lower.contains("end encounter") || lower.contains("stop recording") -> VoiceCommand.END_ENCOUNTER
            lower.contains("what are my rights") || lower.contains("tell me my rights") -> VoiceCommand.QUERY_RIGHTS
            lower.contains("send alert") || lower.contains("alert contacts") -> VoiceCommand.SEND_ALERT
            lower.contains("hey rightguard") || lower.contains("hey right guard") -> VoiceCommand.WAKE
            else -> null
        }
    }

    fun speakAdvice(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "advice_${System.currentTimeMillis()}")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Voice Commands",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Voice command listening"
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
            .setContentTitle("RightGuard Listening")
            .setContentText("Voice commands active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        speechHelper.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

enum class VoiceCommand {
    ACTIVATE_SAFEGUARD,
    ACTIVATE_PANIC,
    END_ENCOUNTER,
    QUERY_RIGHTS,
    SEND_ALERT,
    WAKE
}
