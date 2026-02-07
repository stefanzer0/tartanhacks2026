package com.rightguard.app.data.repository

import com.rightguard.app.data.local.db.dao.RecordingDao
import com.rightguard.app.data.local.db.entity.RecordingEntity
import com.rightguard.app.domain.model.Recording
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val dao: RecordingDao
) {
    suspend fun createRecording(recording: Recording): Long {
        return dao.insert(RecordingEntity.fromDomain(recording))
    }

    suspend fun updateRecording(recording: Recording) {
        dao.update(RecordingEntity.fromDomain(recording))
    }

    suspend fun getByIncidentId(incidentId: Long): List<Recording> {
        return dao.getByIncidentId(incidentId).map { it.toDomain() }
    }

    fun observeByIncidentId(incidentId: Long): Flow<List<Recording>> {
        return dao.observeByIncidentId(incidentId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getById(id: Long): Recording? {
        return dao.getById(id)?.toDomain()
    }
}
