package com.rightguard.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rightguard.app.domain.model.EncounterType
import com.rightguard.app.domain.model.Incident
import com.rightguard.app.domain.model.IncidentStatus

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encounterType: String,
    val status: String,
    val startTime: Long,
    val endTime: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = ""
) {
    fun toDomain() = Incident(
        id = id,
        encounterType = EncounterType.valueOf(encounterType),
        status = IncidentStatus.valueOf(status),
        startTime = startTime,
        endTime = endTime,
        latitude = latitude,
        longitude = longitude,
        notes = notes
    )

    companion object {
        fun fromDomain(incident: Incident) = IncidentEntity(
            id = incident.id,
            encounterType = incident.encounterType.name,
            status = incident.status.name,
            startTime = incident.startTime,
            endTime = incident.endTime,
            latitude = incident.latitude,
            longitude = incident.longitude,
            notes = incident.notes
        )
    }
}
