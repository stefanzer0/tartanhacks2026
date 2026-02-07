package com.rightguard.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rightguard.app.domain.model.CitizenshipStatus
import com.rightguard.app.domain.model.ImmigrationStatus
import com.rightguard.app.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val state: String,
    val citizenshipStatus: String,
    val immigrationStatus: String,
    val pinHash: String,
    val onboardingCompleted: Boolean = false
) {
    fun toDomain() = UserProfile(
        id = id,
        name = name,
        state = state,
        citizenshipStatus = CitizenshipStatus.valueOf(citizenshipStatus),
        immigrationStatus = ImmigrationStatus.valueOf(immigrationStatus),
        pinHash = pinHash,
        onboardingCompleted = onboardingCompleted
    )

    companion object {
        fun fromDomain(profile: UserProfile) = UserProfileEntity(
            id = profile.id,
            name = profile.name,
            state = profile.state,
            citizenshipStatus = profile.citizenshipStatus.name,
            immigrationStatus = profile.immigrationStatus.name,
            pinHash = profile.pinHash,
            onboardingCompleted = profile.onboardingCompleted
        )
    }
}
