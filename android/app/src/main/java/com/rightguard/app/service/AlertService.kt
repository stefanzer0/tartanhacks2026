package com.rightguard.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.rightguard.app.data.repository.TrustedContactRepository
import com.rightguard.app.util.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlertService : Service() {

    @Inject lateinit var trustedContactRepository: TrustedContactRepository
    @Inject lateinit var locationHelper: LocationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_SEND_ALERT = "com.rightguard.SEND_ALERT"
        const val EXTRA_MESSAGE_TYPE = "message_type"
        const val TYPE_SAFEGUARD = "safeguard"
        const val TYPE_PANIC = "panic"

        fun sendAlert(context: Context, type: String) {
            val intent = Intent(context, AlertService::class.java).apply {
                action = ACTION_SEND_ALERT
                putExtra(EXTRA_MESSAGE_TYPE, type)
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SEND_ALERT) {
            val type = intent.getStringExtra(EXTRA_MESSAGE_TYPE) ?: TYPE_SAFEGUARD
            serviceScope.launch {
                sendAlerts(type)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun sendAlerts(type: String) {
        val contacts = trustedContactRepository.getAll()
        if (contacts.isEmpty()) return

        locationHelper.getLastKnownLocation { location ->
            val locationText = location?.let {
                "\nLocation: ${locationHelper.formatLocationForSms(it)}"
            } ?: ""

            val message = when (type) {
                TYPE_PANIC -> "URGENT: I activated RightGuard PANIC mode. I may need immediate help.$locationText"
                else -> "I activated RightGuard safeguard mode during a law enforcement encounter. This is an automated safety alert.$locationText"
            }

            val smsManager = getSystemService(SmsManager::class.java)
            for (contact in contacts) {
                try {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        contact.phoneNumber,
                        null,
                        parts,
                        null,
                        null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
