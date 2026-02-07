package com.rightguard.app.domain.usecase

import android.content.Context
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.domain.model.IncidentStatus
import com.rightguard.app.service.LocationTrackingService
import com.rightguard.app.service.RecordingForegroundService
import com.rightguard.app.service.VoiceCommandService
import javax.inject.Inject

class DeactivateModeUseCase @Inject constructor(
    private val context: Context,
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke(incidentId: Long) {
        // Stop all services
        RecordingForegroundService.stopRecording(context)
        LocationTrackingService.stop(context)
        VoiceCommandService.stop(context)

        // Update incident
        val incident = incidentRepository.getById(incidentId)
        if (incident != null) {
            incidentRepository.updateIncident(
                incident.copy(
                    status = IncidentStatus.ENDED,
                    endTime = System.currentTimeMillis()
                )
            )
        }
    }
}
