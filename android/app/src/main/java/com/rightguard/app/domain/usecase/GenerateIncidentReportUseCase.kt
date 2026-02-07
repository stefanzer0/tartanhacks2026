package com.rightguard.app.domain.usecase

import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.data.repository.RecordingRepository
import com.rightguard.app.domain.model.AiAdviceLog
import com.rightguard.app.domain.model.Incident
import com.rightguard.app.domain.model.Recording
import javax.inject.Inject

data class IncidentReport(
    val incident: Incident,
    val recordings: List<Recording>,
    val aiLogs: List<AiAdviceLog>
)

class GenerateIncidentReportUseCase @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val recordingRepository: RecordingRepository
) {
    suspend operator fun invoke(incidentId: Long): IncidentReport? {
        val incident = incidentRepository.getById(incidentId) ?: return null
        val recordings = recordingRepository.getByIncidentId(incidentId)
        val aiLogs = incidentRepository.getAiAdviceLogsForIncident(incidentId)

        return IncidentReport(
            incident = incident,
            recordings = recordings,
            aiLogs = aiLogs
        )
    }
}
