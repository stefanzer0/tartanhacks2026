package com.rightguard.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rightguard.app.domain.model.Recording
import com.rightguard.app.domain.model.RecordingStatus

@Entity(
    tableName = "recordings",
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
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentId: Long,
    val filePath: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String,
    val durationMs: Long = 0
) {
    fun toDomain() = Recording(
        id = id,
        incidentId = incidentId,
        filePath = filePath,
        startTime = startTime,
        endTime = endTime,
        status = RecordingStatus.valueOf(status),
        durationMs = durationMs
    )

    companion object {
        fun fromDomain(recording: Recording) = RecordingEntity(
            id = recording.id,
            incidentId = recording.incidentId,
            filePath = recording.filePath,
            startTime = recording.startTime,
            endTime = recording.endTime,
            status = recording.status.name,
            durationMs = recording.durationMs
        )
    }
}
