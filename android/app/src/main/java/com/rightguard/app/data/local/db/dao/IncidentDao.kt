package com.rightguard.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rightguard.app.data.local.db.entity.AiAdviceLogEntity
import com.rightguard.app.data.local.db.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: IncidentEntity): Long

    @Update
    suspend fun update(incident: IncidentEntity)

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getById(id: Long): IncidentEntity?

    @Query("SELECT * FROM incidents ORDER BY startTime DESC")
    fun observeAll(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents ORDER BY startTime DESC")
    suspend fun getAll(): List<IncidentEntity>

    @Query("SELECT * FROM incidents WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveIncident(): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiAdviceLog(log: AiAdviceLogEntity): Long

    @Query("SELECT * FROM ai_advice_logs WHERE incidentId = :incidentId ORDER BY timestamp ASC")
    suspend fun getAiAdviceLogsForIncident(incidentId: Long): List<AiAdviceLogEntity>

    @Query("SELECT * FROM ai_advice_logs WHERE incidentId = :incidentId ORDER BY timestamp ASC")
    fun observeAiAdviceLogsForIncident(incidentId: Long): Flow<List<AiAdviceLogEntity>>
}
