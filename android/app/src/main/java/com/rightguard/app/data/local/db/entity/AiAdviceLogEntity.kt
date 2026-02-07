package com.rightguard.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rightguard.app.domain.model.AiAdviceLog

@Entity(
    tableName = "ai_advice_logs",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incidentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("incidentId")]
)
data class AiAdviceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentId: Long,
    val query: String,
    val response: String,
    val timestamp: Long
) {
    fun toDomain() = AiAdviceLog(
        id = id,
        incidentId = incidentId,
        query = query,
        response = response,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(log: AiAdviceLog) = AiAdviceLogEntity(
            id = log.id,
            incidentId = log.incidentId,
            query = log.query,
            response = log.response,
            timestamp = log.timestamp
        )
    }
}
