package com.rightguard.app.data.repository

import com.rightguard.app.data.local.db.dao.IncidentDao
import com.rightguard.app.data.local.db.entity.AiAdviceLogEntity
import com.rightguard.app.data.local.db.entity.IncidentEntity
import com.rightguard.app.domain.model.AiAdviceLog
import com.rightguard.app.domain.model.Incident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val dao: IncidentDao
) {
    suspend fun createIncident(incident: Incident): Long {
        return dao.insert(IncidentEntity.fromDomain(incident))
    }

    suspend fun updateIncident(incident: Incident) {
        dao.update(IncidentEntity.fromDomain(incident))
    }

    suspend fun getById(id: Long): Incident? {
        return dao.getById(id)?.toDomain()
    }

    suspend fun getActiveIncident(): Incident? {
        return dao.getActiveIncident()?.toDomain()
    }

    fun observeAll(): Flow<List<Incident>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAll(): List<Incident> {
        return dao.getAll().map { it.toDomain() }
    }

    suspend fun addAiAdviceLog(log: AiAdviceLog): Long {
        return dao.insertAiAdviceLog(AiAdviceLogEntity.fromDomain(log))
    }

    suspend fun getAiAdviceLogsForIncident(incidentId: Long): List<AiAdviceLog> {
        return dao.getAiAdviceLogsForIncident(incidentId).map { it.toDomain() }
    }

    fun observeAiAdviceLogs(incidentId: Long): Flow<List<AiAdviceLog>> {
        return dao.observeAiAdviceLogsForIncident(incidentId).map { list -> list.map { it.toDomain() } }
    }
}
