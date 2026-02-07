package com.rightguard.app.domain.model

data class Incident(
    val id: Long = 0,
    val encounterType: EncounterType,
    val status: IncidentStatus,
    val startTime: Long,
    val endTime: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = "",
    val aiAdviceLogs: List<AiAdviceLog> = emptyList(),
    val recordings: List<Recording> = emptyList()
)

data class AiAdviceLog(
    val id: Long = 0,
    val incidentId: Long,
    val query: String,
    val response: String,
    val timestamp: Long
)
