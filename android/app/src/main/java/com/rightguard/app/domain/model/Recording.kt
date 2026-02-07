package com.rightguard.app.domain.model

data class Recording(
    val id: Long = 0,
    val incidentId: Long,
    val filePath: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: RecordingStatus,
    val durationMs: Long = 0
)
