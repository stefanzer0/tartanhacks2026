package com.rightguard.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rightguard.app.data.local.db.dao.IncidentDao
import com.rightguard.app.data.local.db.dao.RecordingDao
import com.rightguard.app.data.local.db.dao.TrustedContactDao
import com.rightguard.app.data.local.db.dao.UserProfileDao
import com.rightguard.app.data.local.db.entity.AiAdviceLogEntity
import com.rightguard.app.data.local.db.entity.IncidentEntity
import com.rightguard.app.data.local.db.entity.RecordingEntity
import com.rightguard.app.data.local.db.entity.TrustedContactEntity
import com.rightguard.app.data.local.db.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        TrustedContactEntity::class,
        IncidentEntity::class,
        RecordingEntity::class,
        AiAdviceLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RightGuardDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun incidentDao(): IncidentDao
    abstract fun recordingDao(): RecordingDao
}
