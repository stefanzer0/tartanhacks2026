package com.rightguard.app.domain.usecase

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.rightguard.app.receiver.PanicModeDeviceAdminReceiver
import com.rightguard.app.service.AlertService
import javax.inject.Inject

class ActivatePanicModeUseCase @Inject constructor(
    private val context: Context
) {
    operator fun invoke(): PanicResult {
        // Send panic alert
        AlertService.sendAlert(context, AlertService.TYPE_PANIC)

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, PanicModeDeviceAdminReceiver::class.java)

        return if (dpm.isAdminActive(adminComponent)) {
            // Disable biometrics and lock device
            try {
                dpm.setKeyguardDisabledFeatures(
                    adminComponent,
                    DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS
                )
                dpm.lockNow()
                PanicResult.DEVICE_LOCKED
            } catch (e: Exception) {
                PanicResult.APP_LEVEL_LOCK
            }
        } else {
            PanicResult.APP_LEVEL_LOCK
        }
    }

    fun deactivate() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, PanicModeDeviceAdminReceiver::class.java)

        if (dpm.isAdminActive(adminComponent)) {
            try {
                dpm.setKeyguardDisabledFeatures(
                    adminComponent,
                    DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

enum class PanicResult {
    DEVICE_LOCKED,
    APP_LEVEL_LOCK
}
