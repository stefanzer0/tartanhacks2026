package com.rightguard.app.domain.usecase

import android.content.Context
import com.rightguard.app.service.AlertService
import javax.inject.Inject

class SendAlertToContactsUseCase @Inject constructor(
    private val context: Context
) {
    operator fun invoke(type: String = AlertService.TYPE_SAFEGUARD) {
        AlertService.sendAlert(context, type)
    }
}
