package com.rightguard.app.domain.usecase

import android.content.Context
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.domain.model.EncounterType
import com.rightguard.app.domain.model.Incident
import com.rightguard.app.domain.model.IncidentStatus
import com.rightguard.app.service.AlertService
import com.rightguard.app.service.LocationTrackingService
import com.rightguard.app.service.RecordingForegroundService
import com.rightguard.app.service.VoiceCommandService
import javax.inject.Inject

class ActivateSafeguardModeUseCase @Inject constructor(
    private val context: Context,
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke(encounterType: EncounterType = EncounterType.GENERAL): Long {
        // Create incident record
        val incident = Incident(
            encounterType = encounterType,
            status = IncidentStatus.ACTIVE,
            startTime = System.currentTimeMillis()
        )
        val incidentId = incidentRepository.createIncident(incident)

        // Start recording service
        RecordingForegroundService.startRecording(context, incidentId)

        // Start location tracking
        LocationTrackingService.start(context, incidentId)

        // Start voice command service
        VoiceCommandService.start(context)

        // Send alert to trusted contacts
        AlertService.sendAlert(context, AlertService.TYPE_SAFEGUARD)

        return incidentId
    }
}
